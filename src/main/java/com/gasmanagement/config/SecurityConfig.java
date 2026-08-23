package com.gasmanagement.config;

import com.gasmanagement.security.CustomUserDetailsService;
import com.gasmanagement.security.JwtAuthEntryPoint;
import com.gasmanagement.security.JwtAuthFilter;
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
 * Spring Security Configuration.
 *
 * Phân quyền theo UserRole:
 *  - CUSTOMER  : xem sản phẩm, đặt hàng, quản lý cart (DEV 1)
 *  - STAFF     : xử lý giao hàng
 *  - MANAGER   : quản lý sản phẩm, tồn kho, phê duyệt đơn hàng
 *  - SYSTEM_ADMIN : toàn quyền hệ thống
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity           // Bật @PreAuthorize, @PostAuthorize cho method-level security
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthEntryPoint authEntryPoint;
    private final CustomUserDetailsService userDetailsService;

    // ─────────────── SECURITY FILTER CHAIN ───────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (không cần vì dùng stateless JWT)
            .csrf(AbstractHttpConfigurer::disable)

            // Cấu hình CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless session (không dùng HTTP session)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Custom 401 response (JSON thay vì HTML)
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint))

            // Phân quyền endpoints
            .authorizeHttpRequests(auth -> auth

                // ──── PUBLIC endpoints (không cần token) ────
                .requestMatchers("/api/v1/auth/**").permitAll()             // Login/Register (DEV 1)
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()   // Xem sản phẩm
                .requestMatchers(HttpMethod.GET, "/api/v1/products").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/brands/**").permitAll()     // Xem thương hiệu
                .requestMatchers(HttpMethod.GET, "/api/v1/brands").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll() // Xem danh mục
                .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()

                // ──── MANAGER only ────
                .requestMatchers(HttpMethod.POST,   "/api/v1/products/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.POST,   "/api/v1/products").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/products/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/products/**").hasRole("MANAGER")
                .requestMatchers("/api/v1/products/all").hasRole("MANAGER")

                .requestMatchers(HttpMethod.POST,   "/api/v1/brands/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.POST,   "/api/v1/brands").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/brands/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/brands/**").hasRole("MANAGER")

                .requestMatchers(HttpMethod.POST,   "/api/v1/categories/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.POST,   "/api/v1/categories").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/categories/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("MANAGER")

                .requestMatchers("/api/v1/inventory/**").hasAnyRole("MANAGER", "SYSTEM_ADMIN")

                // ──── Manager Order Approval ────
                .requestMatchers("/api/v1/manager/**").hasAnyRole("MANAGER", "SYSTEM_ADMIN")

                // ──── MANAGER + STAFF (Delivery flow) ────
                .requestMatchers("/api/v1/deliveries/**").hasAnyRole("MANAGER", "STAFF")
                .requestMatchers("/api/v1/staff/**").hasAnyRole("MANAGER", "STAFF", "SYSTEM_ADMIN")

                // ──── SYSTEM_ADMIN only ────
                .requestMatchers("/api/v1/admin/**").hasRole("SYSTEM_ADMIN")

                // ──── Tất cả request còn lại phải authenticate ────
                .anyRequest().authenticated()
            )

            // Thêm JWT filter trước UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Custom authentication provider
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // ─────────────── CORS ───────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Cho phép từ localhost (dev) và domain production
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://*.smart-gas.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ─────────────── AUTH PROVIDER & ENCODER ───────────────

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
