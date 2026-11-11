package erp.global.config;

import erp.auth.security.filter.AccessLogFilter;
import erp.auth.security.filter.CompanyActiveGuardFilter;
import erp.auth.security.filter.JwtAuthenticationFilter;
import erp.auth.security.filter.RequestIdFilter;
import erp.auth.security.filter.TenantScopeGuardFilter;
import erp.global.exception.RestAccessDeniedHandler;
import erp.global.exception.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity // 시큐리티 활성화
@EnableMethodSecurity // @PreAuthorize, @Secured 같은 메서드 레벨 보안을 사용
@RequiredArgsConstructor
public class SecurityConfig {

    // JWT 검증하고, 유저 정보를 SecurityContext에 저장하는 필터
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    // tenant_id 위변조 방지 필터
    private final TenantScopeGuardFilter tenantScopeGuardFilter;
    // 회사가 활성 상태인지 검사하는 필터
    private final CompanyActiveGuardFilter companyActiveGuardFilter;
    private final CorsConfig corsConfig;
    private final RequestIdFilter requestIdFilter;
    private final AccessLogFilter accessLogFilter;
    // 401 error 처리 핸들러
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    // 403 error 처리 핸들러
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // 세션을 안 쓰기 때문에 CSRF 보호 비활성화
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh"
                        ).permitAll() // 계정 등록은 /sys/**(SYS), /admin/**(ADMIN) 경로로 이동해 공개하지 않음
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html",
                                         "/swagger-resources/**").permitAll()
                        .requestMatchers("/api/v1/sys/**").hasRole("SYS")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/user/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated() // 위에 것 외 모든 요청은 로그인 사용자만 접근 가능
                )
                // 세션을 사용 안하므로 STATELESS 모드로 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 제일 먼저 있어야 모든 로그/예외에 붙음
                .addFilterBefore(requestIdFilter, SecurityContextHolderFilter.class)
                // access log 기록 필터
                // -> finally에서 쓰기 때문에, 체인 도는 동안 JWT/테넌트가 세팅되고, 그걸 사용해 DB에 기록 가능
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                //→ 그래야 로그인 요청이 아니라도 JWT 인증을 먼저 처리할 수 있음
                .addFilterBefore(jwtAuthenticationFilter,
                                 UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantScopeGuardFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(companyActiveGuardFilter, TenantScopeGuardFilter.class)
                .addFilterBefore(accessLogFilter, CompanyActiveGuardFilter.class)
                // CORS 허용 필터 추가 (프론트에서 토큰 보낼 수 있게)
                .addFilter(corsConfig.corsFilter());

        return http.build(); // 설정된 모든 보안 정책을 적용한 SecurityFilterChain 반환
    }
}
