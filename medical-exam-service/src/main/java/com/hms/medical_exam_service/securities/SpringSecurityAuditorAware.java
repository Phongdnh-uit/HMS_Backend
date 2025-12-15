package com.hms.medical_exam_service.securities;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA Auditing implementation that provides current user ID
 * for @CreatedBy and @LastModifiedBy fields.
 * 
 * User ID is extracted from UserContext which is populated
 * by UserContextFilter from gateway-forwarded headers.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        UserContext.User user = UserContext.getUser();
        return Optional.ofNullable(user != null ? user.getId() : null);
    }
}
