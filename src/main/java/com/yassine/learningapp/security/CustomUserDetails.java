package com.yassine.learningapp.security;

import com.yassine.learningapp.dto.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;


//Dif entre implement et extend
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final boolean active;
    private final boolean locked;
    private final Role role;

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convention Spring: "ROLE_" + role
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        // Utilisé si un jour tu ajoutes login/password classique
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // à adapter si besoin
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
