// AuthServiceImpl.java
package erp.auth.service;

import static erp.global.util.RowCountGuards.requireOneRowAffected;

import erp.account.dto.internal.LoginUserInfoRow;
import erp.account.dto.request.ErpAccountSaveRequest;
import erp.account.enums.ErpAccountRole;
import erp.account.service.ErpAccountService;
import erp.auth.domain.RefreshToken;
import erp.auth.dto.internal.RefreshTokenRow;
import erp.auth.dto.request.LoginRequest;
import erp.auth.dto.request.RefreshTokenRequest;
import erp.auth.dto.request.SignupRequest;
import erp.auth.dto.response.LoginResponse;
import erp.auth.mapper.RefreshTokenMapper;
import erp.auth.security.jwt.JwtTokenProvider;
import erp.auth.security.model.UserPrincipal;
import erp.company.validation.CompanyValidator;
import erp.employee.dto.request.EmployeeSaveRequest;
import erp.employee.service.EmployeeService;
import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.log.audit.Auditable;
import erp.log.dto.request.LogSaveRequest;
import erp.log.enums.LogType;
import erp.log.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LogService logService;
    private final EmployeeService employeeService;
    private final ErpAccountService erpAccountService;
    private final CompanyValidator companyValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenMapper refreshTokenMapper;

    // 회원가입은 계정/직원 생성이라 WORK로 기록
    @Auditable(type = LogType.WORK, messageEl = "'회원가입 처리: ' + #args[0].loginEmail() + ' / ' + #args[0].name()")
    @Override
    @Transactional
    public void signup(SignupRequest request, long companyId, ErpAccountRole role) {
        companyValidator.validCompanyIdIfPresent(companyId);
        // Employee 생성
        EmployeeSaveRequest employeeSaveRequest =
                EmployeeSaveRequest.of(
                        request.empNo(),
                        request.name(),
                        request.phone(),
                        request.departmentId(),
                        request.positionId()
                );
        long employeeId = employeeService.saveEmployee(employeeSaveRequest, companyId);

        // ErpAccount 생성
        ErpAccountSaveRequest erpAccountSaveRequest =
                ErpAccountSaveRequest.of(
                        employeeId,
                        request.loginEmail(),
                        request.password(),
                        role
                );
        erpAccountService.saveErpAccount(erpAccountSaveRequest, companyId);
    }

    // 로그인 로그는 @Auditable 대신 직접 남김
    // -> 로그인 시점엔 토큰이 없어 @Auditable로는 회사·사용자가 비어서 저장됨 (회사 관리자가 조회할 수 없음)
    @Override
    public LoginResponse login(LoginRequest request) {
        final String email = request.loginEmail().trim().toLowerCase();
        // 왜 비밀 번호 검증 전에 jwt를 생성하기 위한 데이터도 같이(join) 조회했는가?
        // -> 실패까지 JOIN해도 단일 인덱스+PK 조인이라 성능상 손해 거의 없음
        // -> 두 단계 분리는 실패가 매우 많을 때만 이득, 정상 로그인은 오히려 느림
        final LoginUserInfoRow row;
        try {
            row = erpAccountService.findLoginRowByLoginEmail(email);
        } catch (GlobalException ex) {
            // 없는 계정 → 어느 회사인지 알 수 없으므로 회사 없이 기록
            saveAuthLog(false, "로그인 실패(없는 계정): " + email, null, null, null, ex);
            throw ex;
        }

        // 이메일 또는 비밀번호가 잘못된 경우 예외 처리
        if (!passwordEncoder.matches(request.password(), row.passwordHash())) {
            GlobalException ex = new GlobalException(ErrorStatus.INVALID_LOGIN_CREDENTIALS);
            saveAuthLog(false, "로그인 실패(비밀번호 불일치): " + email, row.tenantId(), row.uuid(), row.name(), ex);
            throw ex;
        }

        // 로그인 성공 시 JWT 토큰 생성
        UserDetails userDetails = UserPrincipal.of(
                row.uuid(),
                row.role(),
                row.name(),
                null, // 사용되지 않는 민감정보 제거
                row.tenantId()
        );
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        long erpAccountId = row.erpAccountId();
        saveRefreshToken(erpAccountId, refreshToken);

        saveAuthLog(true, "로그인: " + email, row.tenantId(), row.uuid(), row.name(), null);

        // 왜 userDetails가 아니라 LoginRow에서 데이터를 가져오는가?
        // -> 응답(LoginResponse)은 DB 조회 결과(row) 기준으로 채워야 책임이 명확함
        // -> UserPrincipal은 인증 컨텍스트 전용이므로 API 응답에 끌고 오면 역할이 섞임
        return LoginResponse.of(accessToken, refreshToken, row.uuid(), row.role(), row.name());
    }

    // 재발급 로그도 직접 남김 (회사·사용자 기록)
    // 주의: 메시지에 리프레시 토큰 원문을 넣지 말 것 (로그를 보는 사람이 남의 토큰을 볼 수 있음)
    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        try {
            LoginResponse response = refreshInternal(request);
            // refreshInternal에서 유효성 검증을 통과한 토큰이므로 회사 ID 추출 가능
            Long tenantId = jwtTokenProvider.extractTenantId(jwtTokenProvider.stripBearer(request.refreshToken()));
            saveAuthLog(true, "토큰 재발급: " + response.name(), tenantId, response.uuid(), response.name(), null);
            return response;
        } catch (GlobalException ex) {
            saveAuthLog(false, "토큰 재발급 실패", null, null, null, ex);
            throw ex;
        }
    }

    private LoginResponse refreshInternal(RefreshTokenRequest request) {
        final String reqRefresh = request.refreshToken();
        final String rawRefresh = jwtTokenProvider.stripBearer(reqRefresh);

        // 1) DB에서 리프레시 토큰 행 조회
        RefreshToken refreshRow = refreshTokenMapper.findByToken(rawRefresh)
                .orElseThrow(() -> new GlobalException(ErrorStatus.INVALID_REFRESH_TOKEN));

        // 2) 리프레시 토큰 만료 검증
        if (!jwtTokenProvider.validateToken(rawRefresh)) {
            // 만료된 토큰이면 DB에서 제거 후 예외
            int affectedRowCount = refreshTokenMapper.deleteById(refreshRow.getRefreshTokenId());
            requireOneRowAffected(affectedRowCount, ErrorStatus.DELETE_REFRESH_TOKEN_FAIL);
            throw new GlobalException(ErrorStatus.EXPIRED_REFRESH_TOKEN);
        }

        // 3) 리프레시 토큰에서 사용자 정보 추출 (DB 재조회 없이)
        final String uuid = jwtTokenProvider.extractUuid(rawRefresh);
        final String role = jwtTokenProvider.extractRole(rawRefresh);
        final String name = jwtTokenProvider.extractName(rawRefresh);
        final Long tenantId = jwtTokenProvider.extractTenantId(rawRefresh);

        UserDetails userDetails = UserPrincipal.of(uuid, role, name, null, tenantId);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

        return LoginResponse.of(newAccessToken, reqRefresh, uuid, role, name);
    }

    @Auditable(type = LogType.LOGIN, messageEl = "'로그아웃'")
    @Override
    @Transactional
    public void logout(String uuid, RefreshTokenRequest request) {
        // 기기(브라우저)마다 토큰이 따로 있으므로, 요청한 토큰이 이 사용자의 것인지 확인 후 그 하나만 삭제
        // → 이 기기만 로그아웃되고 다른 기기의 로그인은 유지
        String reqRefreshToken = jwtTokenProvider.stripBearer(request.refreshToken());
        RefreshTokenRow refreshTokenRow =
                refreshTokenMapper.findRefreshTokenRowByUuidAndToken(uuid, reqRefreshToken)
                        .orElseThrow(() ->
                                             new GlobalException(
                                                     ErrorStatus.INVALID_REFRESH_TOKEN));

        int affected = refreshTokenMapper.deleteById(refreshTokenRow.refreshTokenId());
        requireOneRowAffected(affected, ErrorStatus.DELETE_REFRESH_TOKEN_FAIL);
    }

    /**
     * 로그인·토큰 재발급 로그 저장 (회사·사용자 직접 지정)
     * - 로그 저장 실패가 로그인 자체를 막지 않도록 예외는 삼킴
     */
    private void saveAuthLog(boolean success, String message,
                             Long tenantId, String uuid, String name, GlobalException ex) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("result", success ? "SUCCESS" : "FAIL");
            if (ex != null) {
                payload.put("errorCode", ex.getStatus().getStatus());
            }
            logService.saveAs(new LogSaveRequest(LogType.LOGIN, success, message, payload), tenantId, uuid, name);
        } catch (Exception e) {
            log.warn("auth log write failed: {}", e.toString());
        }
    }

    /** 한 계정이 동시에 로그인해 둘 수 있는 기기(브라우저) 수. 넘으면 가장 오래된 로그인부터 끊음 */
    private static final int MAX_SESSIONS_PER_ACCOUNT = 5;

    /**
     * 로그인할 때마다 리프레시 토큰을 새 행으로 저장 (여러 기기 동시 로그인)
     * - 예전 방식(계정당 1개를 덮어쓰기)은 다른 곳에서 로그인하면 기존 기기의 자동 로그인이 풀렸음
     * - 비밀번호 변경·초기화 시에는 그 계정의 모든 토큰을 지움 (PasswordServiceImpl)
     */
    private void saveRefreshToken(long erpAccountId, String refreshToken) {
        // DB에는 Bearer 제거한 raw 리프레시 토큰 저장
        String refreshRaw = jwtTokenProvider.stripBearer(refreshToken);

        long refreshTokenId = refreshTokenMapper.nextId();
        RefreshToken entity = RefreshToken.issue(refreshTokenId, erpAccountId, refreshRaw);
        int affectedRowCount = refreshTokenMapper.save(entity);
        requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_REFRESH_TOKEN_FAIL);

        // 오래된 로그인 정리 (만료돼 남은 행도 여기서 함께 지워짐)
        refreshTokenMapper.deleteOldestBeyond(erpAccountId, MAX_SESSIONS_PER_ACCOUNT);
    }
}
