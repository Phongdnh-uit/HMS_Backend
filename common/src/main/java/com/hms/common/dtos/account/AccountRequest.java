package com.hms.common.dtos.account;

import com.hms.common.dtos.Action;
import com.hms.common.enums.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AccountRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required", groups = {Action.Create.class})
    @Size(min = 8, message = "Password must be at least 8 characters", groups = {Action.Create.class})
    private String password;

    private RoleEnum role;
}
