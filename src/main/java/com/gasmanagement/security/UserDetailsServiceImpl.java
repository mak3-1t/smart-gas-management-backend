package com.gasmanagement.security;

import com.gasmanagement.model.User;
import com.gasmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Load UserDetails từ MongoDB dựa trên username hoặc email.
 * Gán GrantedAuthority theo User.role để @PreAuthorize hoạt động.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy user: " + username));

        // Gán role với prefix ROLE_ để Spring Security nhận
        String roleAuthority = "ROLE_" + user.getRole().name();
        
        boolean enabled = user.getStatus() != com.gasmanagement.model.enums.AccountStatus.DISABLED;
        boolean accountNonLocked = user.getStatus() != com.gasmanagement.model.enums.AccountStatus.LOCKED;

        return UserDetailsImpl.build(
                user, 
                List.of(new SimpleGrantedAuthority(roleAuthority)), 
                enabled, 
                accountNonLocked
        );
    }
}
