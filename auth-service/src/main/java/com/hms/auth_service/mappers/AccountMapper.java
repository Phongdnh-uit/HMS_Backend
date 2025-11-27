package com.hms.auth_service.mappers;

import com.hms.auth_service.dtos.account.AccountRequest;
import com.hms.auth_service.dtos.account.AccountResponse;
import com.hms.auth_service.entities.Account;
import com.hms.common.mappers.GenericMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper extends GenericMapper<Account, AccountRequest, AccountResponse> {
}
