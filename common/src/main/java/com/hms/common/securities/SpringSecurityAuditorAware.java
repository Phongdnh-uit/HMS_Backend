package com.hms.common.securities;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        UserContext.User user = UserContext.getUser();
        if (user == null) {
            return Optional.of("system"); // Default auditor for inter-service calls
        }
        return Optional.ofNullable(user.getId());
    }
}
