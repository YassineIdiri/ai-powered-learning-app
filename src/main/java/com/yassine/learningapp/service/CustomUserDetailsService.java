package com.yassine.learningapp.service;

import com.yassine.learningapp.entity.User;
import com.yassine.learningapp.repository.UserRepository;
import com.yassine.learningapp.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("USER_NOT_FOUND"));

        if (user.isLocked()) throw new LockedException("ACCOUNT_LOCKED");
        if (!user.isActive()) throw new DisabledException("ACCOUNT_DISABLED");

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.isActive(),
                user.isLocked(),
                user.getRole()
        );
    }
}
