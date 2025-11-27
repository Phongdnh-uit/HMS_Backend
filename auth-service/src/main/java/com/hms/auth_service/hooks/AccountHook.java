package com.hms.auth_service.hooks;

import com.hms.auth_service.dtos.account.AccountRequest;
import com.hms.auth_service.dtos.account.AccountResponse;
import com.hms.auth_service.entities.Account;
import com.hms.auth_service.repositories.AccountRepository;
import com.hms.common.dtos.PageResponse;
import com.hms.common.enums.RoleEnum;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.hooks.GenericHook;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class AccountHook implements GenericHook<Account, String, AccountRequest, AccountResponse> {

    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;

    @Override
    public void enrichFindAll(PageResponse<AccountResponse> response) {

    }

    @Override
    public void enrichFindById(AccountResponse response) {

    }

    @Override
    public void validateCreate(AccountRequest input, Map<String, Object> context) {

    }

    @Override
    public void enrichCreate(AccountRequest input, Account entity, Map<String, Object> context) {
        enrich(input, entity);
    }

    @Override
    public void afterCreate(Account entity, AccountResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateUpdate(String s, AccountRequest input, Account existingEntity, Map<String, Object> context) {

    }

    @Override
    public void enrichUpdate(AccountRequest input, Account entity, Map<String, Object> context) {
        enrich(input, entity);
    }

    @Override
    public void afterUpdate(Account entity, AccountResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateDelete(String s) {
        // System need at least one ADMIN account
        var account = accountRepository.findById(s).orElseThrow();
        if (account.getRole() == RoleEnum.ADMIN) {
            var adminCount = accountRepository.count(
                    (root, query, cb) -> cb.equal(root.get("role"), RoleEnum.ADMIN)
            );
            if (adminCount <= 1) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, Map.of(
                        "message", "System need at least one ADMIN account"
                ));
            }
        }
    }

    @Override
    public void afterDelete(String s) {

    }

    @Override
    public void validateBulkDelete(Iterable<String> strings) {
        // System need at least one ADMIN account
        var adminCount = accountRepository.count(
                (root, query, cb) -> cb.equal(root.get("role"), RoleEnum.ADMIN)
        );
        var deletingAdminCount = accountRepository.count(
                (root, query, cb) -> cb.and(
                        root.get("role").in(RoleEnum.ADMIN),
                        root.get("id").in(strings)
                )
        );
        if (adminCount - deletingAdminCount < 1) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, Map.of(
                    "message", "System need at least one ADMIN account"
            ));
        }
    }

    @Override
    public void afterBulkDelete(Iterable<String> strings) {

    }

    void enrich(AccountRequest request, Account account) {
        account.setPassword(passwordEncoder.encode(request.getPassword()));
    }
}
