package com.hms.auth_service.repositories;

import com.hms.auth_service.entities.Account;
import com.hms.common.enums.RoleEnum;
import com.hms.common.repositories.SimpleRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends SimpleRepository<Account, String> {
    boolean existsByEmail(String email);
    boolean existsByRole(RoleEnum role);
}
