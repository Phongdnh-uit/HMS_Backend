package com.hms.auth_service.securities;


import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

@Builder
@Getter
public class CustomUserDetails implements UserDetails {
    private String id;
    private String password;
    private String email;
    private String role;
    private Set<? extends GrantedAuthority> authorities;

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }
}
