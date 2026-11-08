package com.gasmanagement.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration.
 *
 * <ul>
 *   <li>Stateless session (JWT)</li>
 *   <li>CORS filter enabled for Frontend clients</li>
 *   <li>@PreAuthorize enabled</li>
 *   <li>Public: /api/auth/**, /api/products/** (GET), /actuator/health</li>
 *   <li>Role-based: CUSTOMER vs STAFF vs MANAGER vs SYSTEM_ADMIN</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // Bật @PreAuthorize trên Controller
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Bật CORS cho phép kết nối từ Frontend
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Tắt CSRF (API stateless không cần)
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless: không lưu session
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Phân quyền endpoint
            .authorizeHttpRequests(auth -> auth
                // ─── Public endpoints ───
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/payos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vouchers/usable").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // ─── Customer endpoints ───
                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/api/cart/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET,  "/api/orders/my-orders").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/orders/*/cancel").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/reviews").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/payments/*/proof").hasRole("CUSTOMER")

                // ─── Staff endpoints ───
                .requestMatchers(HttpMethod.POST, "/api/payments/*/confirm").hasAnyRole("STAFF", "MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/accept").hasRole("STAFF")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/reject").hasRole("STAFF")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/start").hasRole("STAFF")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/complete").hasRole("STAFF")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/fail").hasRole("STAFF")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/cylinder-exchange").hasRole("STAFF")

                // ─── Manager & System Admin endpoints ───
                .requestMatchers(HttpMethod.GET, "/api/manager/settings").hasAnyRole("MANAGER", "SYSTEM_ADMIN", "CUSTOMER")
                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/payments/*/verify").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/assign").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/deliveries").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/deliveries/waiting").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/staff/*/profile").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/staff").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/staff/available").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/dashboard/**").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/orders/all").hasAnyRole("MANAGER", "SYSTEM_ADMIN")

                // ─── Admin endpoints ───
                .requestMatchers(HttpMethod.GET, "/api/admin/accounts").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/customers").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/managers").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/accounts").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admin/accounts/*/password").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/accounts/*").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/reset-transactional-data").hasAnyRole("MANAGER", "SYSTEM_ADMIN")
                .requestMatchers("/api/admin/**").hasRole("SYSTEM_ADMIN")

                // ─── Tất cả còn lại phải auth ───
                .anyRequest().authenticated()
            )

            // Thêm JWT filter trước UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Dùng custom AuthenticationProvider
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
