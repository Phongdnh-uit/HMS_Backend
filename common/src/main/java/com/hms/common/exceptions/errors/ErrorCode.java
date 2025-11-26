package com.hms.common.exceptions.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    VALIDATION_ERROR(2000, HttpStatus.BAD_REQUEST, "Validation Error"),
    RESOURCE_EXISTS(2001, HttpStatus.CONFLICT, "Resource Exists"),
    RESOURCE_NOT_FOUND(2002, HttpStatus.NOT_FOUND, "Resource Not Found"),
    AUTHENTICATION_REQUIRED(2003, HttpStatus.UNAUTHORIZED, "Authentication Required"),
    FORBIDDEN(2004, HttpStatus.FORBIDDEN, "Forbidden"),
    TOKEN_EXPIRED(2005, HttpStatus.UNAUTHORIZED, "Token Expired"),
    TOKEN_INVALID(2006, HttpStatus.UNAUTHORIZED, "Token Invalid"),
    INVALID_CREDENTIALS(2007, HttpStatus.UNAUTHORIZED, "Invalid Credentials"),
    VERIFICATION_CODE_INVALID(2008, HttpStatus.BAD_REQUEST, "Verification Code Invalid"),
    VERIFICATION_CODE_EXPIRED(2009, HttpStatus.BAD_REQUEST, "Verification Code Expired"),
    UPLOAD_FAILED(2010, HttpStatus.BAD_REQUEST, "Upload Failed"),
    SIGNATURE_INVALID(2011, HttpStatus.BAD_REQUEST, "Signature Invalid"),
    OAUTH2_ERROR(2012, HttpStatus.UNAUTHORIZED, "OAuth2 Error"),
    INTERNAL_SERVER_ERROR(2099, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),
    OTP_EXPIRED(2100, HttpStatus.BAD_REQUEST, "OTP Expired"),
    OTP_INVALID(2101, HttpStatus.BAD_REQUEST, "OTP Invalid"),
    ACCOUNT_LOCKED(2200, HttpStatus.LOCKED, "Account Locked"),
    ACCOUNT_DISABLED(2201, HttpStatus.FORBIDDEN, "Account Disabled"),
    EMAIL_NOT_VERIFIED(2202, HttpStatus.PRECONDITION_REQUIRED, "Email Not Verified"),
    OPERATION_NOT_ALLOWED(2300, HttpStatus.BAD_REQUEST, "Operation Not Allowed");

    private final int code;
    private final HttpStatus httpCode;
    private final String message;

    ErrorCode(int code, HttpStatus httpCode, String message) {
        this.code = code;
        this.httpCode = httpCode;
        this.message = message;
    }
}
