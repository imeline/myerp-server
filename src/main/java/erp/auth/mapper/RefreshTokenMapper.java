package erp.auth.mapper;

import erp.auth.domain.RefreshToken;
import erp.auth.dto.internal.RefreshTokenRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface RefreshTokenMapper {

    long nextId();

    int save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(@Param("token") String token);

    /** 로그아웃: 이 사용자의 토큰인지 확인하며 조회 */
    Optional<RefreshTokenRow> findRefreshTokenRowByUuidAndToken(@Param("uuid") String uuid,
                                                                @Param("token") String token);

    /** 계정의 토큰 중 최신 keep개만 남기고 삭제 (동시 로그인 기기 수 제한) */
    int deleteOldestBeyond(@Param("erpAccountId") long erpAccountId, @Param("keep") int keep);

    int deleteById(@Param("refreshTokenId") long refreshTokenId);

    /** 비밀번호 변경·초기화 시 그 계정의 로그인 세션(리프레시 토큰) 끊기 */
    int deleteByErpAccountId(@Param("erpAccountId") long erpAccountId);
}