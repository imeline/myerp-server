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

        // 아이디 또는 비밀번호 가 잘못된 경우 예외 처리
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
        saveOrUpdateRefreshToken(erpAccountId, refreshToken);

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

    @Auditable(type = LogType.LOGIN, messageEl = "'로그아웃 처리: uuid='+ #args[0].uuid()")
    @Override
    @Transactional
    public void logout(String uuid, RefreshTokenRequest request) {
        RefreshTokenRow refreshTokenRow =
                refreshTokenMapper.findRefreshTokenRowByUuid(uuid)
                        .orElseThrow(() ->
                                             new GlobalException(
                                                     ErrorStatus.REFRESH_TOKEN_NOT_FOUND));
        String dbRefreshToken = refreshTokenRow.token();
        String reqRefreshToken = jwtTokenProvider.stripBearer(request.refreshToken());

        // 1) 요청으로 들어온 리프레시 토큰과 DB 저장값 비교
        if (dbRefreshToken == null || dbRefreshToken.isBlank()
                || !dbRefreshToken.equals(reqRefreshToken)) {
            throw new GlobalException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }

        // 2) 검증 통과하면 해당 계정의 리프레시 토큰 삭제
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

    @Transactional
    protected void saveOrUpdateRefreshToken(long erpAccountId, String refreshToken) {
        // DB에는 Bearer 제거한 raw 리프레시 토큰 저장
        String refreshRaw = jwtTokenProvider.stripBearer(refreshToken);

        boolean existsRefreshToken = refreshTokenMapper.existsByErpAccountId(erpAccountId);

        if (existsRefreshToken) {
            // 기존 토큰 있으면 업데이트
            int affectedRowCount = refreshTokenMapper.updateTokenByErpAccountId(
                    erpAccountId, refreshRaw);
            requireOneRowAffected(affectedRowCount,
                                  ErrorStatus.UPDATE_REFRESH_TOKEN_FAIL);
        } else {
            // 없으면 새로 생성
            long refreshTokenId = refreshTokenMapper.nextId();
            RefreshToken entity = RefreshToken.issue(
                    refreshTokenId,
                    erpAccountId,
                    refreshRaw
            );
            int affectedRowCount = refreshTokenMapper.save(entity);
            requireOneRowAffected(affectedRowCount,
                                  ErrorStatus.CREATE_REFRESH_TOKEN_FAIL);
        }
    }
}
