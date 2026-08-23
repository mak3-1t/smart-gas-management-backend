package com.gasmanagement.security;

import com.gasmanagement.model.User;
import com.gasmanagement.model.enums.AccountStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Wrapper của User model để tích hợp với Spring Security.
 * Implements UserDetails để Spring Security có thể sử dụng.
 */
public class CustomUserDetails implements UserDetails {

    @Getter
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // ─────────────── Spring Security UserDetails ───────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Prefix ROLE_ là convention của Spring Security
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != AccountStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == AccountStatus.ACTIVE;
    }

    // ─────────────── Convenience Getters ───────────────

    public String getUserId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getRole() {
        return user.getRole().name();
    }
}
