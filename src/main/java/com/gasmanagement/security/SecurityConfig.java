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

/**
 * Spring Security configuration.
 *
 * <ul>
 *   <li>Stateless session (JWT)</li>
 *   <li>@PreAuthorize enabled</li>
 *   <li>Public: /api/auth/**, /actuator/health</li>
 *   <li>Role-based: MANAGER vs STAFF vs CUSTOMER</li>
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
            // Tắt CSRF (API stateless không cần)
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless: không lưu session
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Phân quyền endpoint
            .authorizeHttpRequests(auth -> auth
                // ─── Public endpoints ───
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // ─── Staff endpoints ───
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/accept").hasRole("STAFF")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/reject").hasRole("STAFF")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/start").hasRole("STAFF")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/complete").hasRole("STAFF")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/fail").hasRole("STAFF")
                .requestMatchers(HttpMethod.POST, "/api/deliveries/*/cylinder-exchange").hasRole("STAFF")

                // ─── Manager endpoints ───
                .requestMatchers(HttpMethod.POST, "/api/deliveries/assign").hasRole("MANAGER")
                .requestMatchers(HttpMethod.GET,  "/api/deliveries").hasRole("MANAGER")
                .requestMatchers(HttpMethod.GET,  "/api/deliveries/waiting").hasRole("MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/staff/*/profile").hasRole("MANAGER")
                .requestMatchers(HttpMethod.GET,  "/api/staff").hasRole("MANAGER")
                .requestMatchers(HttpMethod.GET,  "/api/staff/available").hasRole("MANAGER")

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
