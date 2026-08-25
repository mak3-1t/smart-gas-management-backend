package com.gasmanagement.security;

import com.gasmanagement.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
public class UserDetailsImpl extends org.springframework.security.core.userdetails.User {
    
    private final String id;

    public UserDetailsImpl(String username, String password, boolean enabled, 
                           boolean accountNonExpired, boolean credentialsNonExpired, 
                           boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, 
                           String id) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
    }

    public static UserDetailsImpl build(User user, Collection<? extends GrantedAuthority> authorities, boolean enabled, boolean accountNonLocked) {
        return new UserDetailsImpl(
                user.getUsername(),
                user.getPasswordHash(),
                enabled,
                true,
                true,
                accountNonLocked,
                authorities,
                user.getId()
        );
    }
}
