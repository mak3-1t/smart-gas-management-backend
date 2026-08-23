package com.gasmanagement.security;

import com.gasmanagement.model.User;
import com.gasmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service load User từ MongoDB để Spring Security authenticate.
 * Được sử dụng trong AuthenticationManager và JwtAuthFilter.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user theo username (dùng cho login).
     * Spring Security gọi method này khi authenticate.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy user với username: " + username));
        return new CustomUserDetails(user);
    }

    /**
     * Load user theo email (hỗ trợ login bằng email).
     * DEV 1 có thể gọi method này từ AuthService.
     */
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy user với email: " + email));
        return new CustomUserDetails(user);
    }
}
