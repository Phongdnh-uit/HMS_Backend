package com.hms.auth_service.securities;

import com.hms.auth_service.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Set;

@RequiredArgsConstructor
@Component
public class CustomUserDetailsService implements UserDetailsService {
    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String credential) throws UsernameNotFoundException {
        var user = accountRepository.findOne(
                        (root, query, cb) -> cb.or(
                                cb.equal(root.get("email"), credential)
                        )
                )
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + credential));
        if (!user.isEmailVerified()) {
            throw new UsernameNotFoundException("Email not verified for user with email: " + credential);
        }
        return CustomUserDetails.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole().name())
                .authorities(Set.of())
                .build();
    }
}
