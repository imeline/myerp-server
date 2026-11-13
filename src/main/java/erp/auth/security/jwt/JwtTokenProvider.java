package erp.auth.security.jwt;

import erp.auth.security.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public static final String CLAIM_UUID = "uuid";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_NAME = "name";
    public static final String CLAIM_TENANT_ID = "tenant_id";
    public static final String BEARER_PREFIX = "Bearer ";
    private final Environment env;

    // *파라미터를 UserPrincipal로 안 받아도 되나?
    //  UserPrincipal은 UserDetails를 구현하므로
    // `UserDetails userDetails = principal;` - 업캐스팅 가능. 더 유연한 코드
    private String buildToken(UserDetails userDetails, long expMillis) {
        Date now = new Date();

        JwtBuilder builder = Jwts.builder()
                .setId(UUID.randomUUID().toString()) // 같은 초에 여러 번 발급돼도 토큰이 겹치지 않게
                .setSubject(userDetails.getUsername())
                .claim(CLAIM_ROLE, userDetails.getAuthorities()
                        .iterator().next().getAuthority())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expMillis))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256);

        // UserPrincipal 객체 타입이면 tenant_id 넣기
        if (userDetails instanceof UserPrincipal principal) {
            builder.claim(CLAIM_TENANT_ID, principal.getTenantId());

            if (principal.getUuid() != null) {
                builder.claim(CLAIM_UUID, principal.getUuid());
            }
            if (principal.getName() != null) {
                builder.claim(CLAIM_NAME, principal.getName());
            }
        }

        return BEARER_PREFIX + builder.compact();
    }

    public String generateAccessToken(UserDetails userDetails) {
        Long expMillis = env.getProperty("jwt.access-expiration-time", Long.class,
                                         1000L * 60 * 60); // 기본값: 1시간
        return buildToken(userDetails, expMillis);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Long expMillis = env.getProperty("jwt.refresh-expiration-time", Long.class,
                                         1000L * 60 * 60 * 24 * 14); // 기본값: 14일
        return buildToken(userDetails, expMillis);
    }

    // 보통 시큐리티에서 알아서 SecurityContextHolder.getContext().getAuthentication()
    // 내부에 있는 Authentication.getAuthorities()를 확인해서
    // SecurityConfig에서 requestMatchers 등에 사용하는데,
    // 우리는 userDetails.username 을 uuid로 바꾸기 위해
    // extractUuid을 JwtAuthenticationFilter에서 사용
    public String extractUuid(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // authService 등에서 사용하기 위해서 만듦
    public String extractRole(String token) {
        return extractAllClaims(token).get(CLAIM_ROLE, String.class);
    }

    public String extractName(String token) {
        return extractAllClaims(token).get(CLAIM_NAME, String.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Long extractTenantId(String token) {
        return extractAllClaims(token).get(CLAIM_TENANT_ID, Long.class);
    }

    public Claims parseClaimsOrThrow(String token) {
        String raw = stripBearer(token);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("JWT is missing");
        }
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(raw)   // 만료면 ExpiredJwtException, 위조/형식오류면 JwtException
                .getBody();
    }

    public void validateTokenOrThrow(String token) {
        parseClaimsOrThrow(token);
    }

    public boolean validateToken(String token) {
        try {
            validateTokenOrThrow(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 만료도 "유효하지 않음"으로 보겠다면 false
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return parseClaimsOrThrow(token);
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(env.getProperty("jwt.secret-key"));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String stripBearer(String token) {
        if (token == null) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return trimmed.substring(BEARER_PREFIX.length()).trim();
        }
        return trimmed;
    }
}
