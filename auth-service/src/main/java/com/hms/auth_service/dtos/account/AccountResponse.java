package com.hms.auth_service.dtos.account;

import com.hms.common.enums.RoleEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountResponse {
    private String email;
    private RoleEnum role;
    private boolean emailVerified;
}
