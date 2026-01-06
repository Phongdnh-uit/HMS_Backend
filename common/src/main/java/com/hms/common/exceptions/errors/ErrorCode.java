package com.hms.common.exceptions.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // === General Errors (2000-2099) ===
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

    // === OTP Errors (2100-2199) ===
    OTP_EXPIRED(2100, HttpStatus.BAD_REQUEST, "OTP Expired"),
    OTP_INVALID(2101, HttpStatus.BAD_REQUEST, "OTP Invalid"),

    // === Account Errors (2200-2299) ===
    ACCOUNT_LOCKED(2200, HttpStatus.LOCKED, "Account Locked"),
    ACCOUNT_DISABLED(2201, HttpStatus.FORBIDDEN, "Account Disabled"),
    EMAIL_NOT_VERIFIED(2202, HttpStatus.PRECONDITION_REQUIRED, "Email Not Verified"),

    // === Operation Errors (2300-2399) ===
    OPERATION_NOT_ALLOWED(2300, HttpStatus.BAD_REQUEST, "Operation Not Allowed"),

    // === Patient Service Errors (3000-3099) ===
    PATIENT_NOT_FOUND(3000, HttpStatus.NOT_FOUND, "Patient Not Found"),

    // === Medicine Service Errors (3100-3199) ===
    MEDICINE_NOT_FOUND(3100, HttpStatus.NOT_FOUND, "Medicine Not Found"),
    MEDICINE_IN_USE(3101, HttpStatus.CONFLICT, "Medicine In Use"),
    INSUFFICIENT_STOCK(3102, HttpStatus.BAD_REQUEST, "Insufficient Stock"),
    CATEGORY_NOT_FOUND(3103, HttpStatus.NOT_FOUND, "Category Not Found"),

    // === Appointment Service Errors (3200-3299) ===
    APPOINTMENT_NOT_FOUND(3200, HttpStatus.NOT_FOUND, "Appointment Not Found"),
    APPOINTMENT_NOT_COMPLETED(3201, HttpStatus.BAD_REQUEST, "Appointment Not Completed"),
    APPOINTMENT_CONFLICT(3202, HttpStatus.CONFLICT, "Appointment Time Conflict"),

    // === Medical Exam Service Errors (3300-3399) ===
    EXAM_NOT_FOUND(3300, HttpStatus.NOT_FOUND, "Medical Exam Not Found"),
    EXAM_EXISTS(3301, HttpStatus.CONFLICT, "Medical Exam Already Exists"),
    EXAM_NOT_MODIFIABLE(3302, HttpStatus.BAD_REQUEST, "Exam Cannot Be Modified After 24 Hours"),
    PRESCRIPTION_NOT_FOUND(3303, HttpStatus.NOT_FOUND, "Prescription Not Found"),
    PRESCRIPTION_EXISTS(3304, HttpStatus.CONFLICT, "Prescription Already Exists"),
    STOCK_DECREMENT_FAILED(3305, HttpStatus.SERVICE_UNAVAILABLE, "Stock Decrement Failed - Saga Rollback Executed"),

    // === Employee Service Errors (3400-3499) ===
    EMPLOYEE_NOT_FOUND(3400, HttpStatus.NOT_FOUND, "Employee Not Found"),
    DOCTOR_NOT_FOUND(3401, HttpStatus.NOT_FOUND, "Doctor Not Found"),

    // === Billing Service Errors (3500-3599) ===
    INVOICE_NOT_FOUND(3500, HttpStatus.NOT_FOUND, "Invoice Not Found"),
    INVOICE_EXISTS(3501, HttpStatus.CONFLICT, "Invoice Already Exists"),
    INVOICE_CANCELLED(3502, HttpStatus.BAD_REQUEST, "Invoice Is Cancelled"),
    INVOICE_ALREADY_PAID(3503, HttpStatus.CONFLICT, "Invoice Already Paid"),
    PAYMENT_NOT_FOUND(3504, HttpStatus.NOT_FOUND, "Payment Not Found"),
    DUPLICATE_PAYMENT(3505, HttpStatus.CONFLICT, "Duplicate Payment - Idempotency Key Already Used");

    private final int code;
    private final HttpStatus httpCode;
    private final String message;

    ErrorCode(int code, HttpStatus httpCode, String message) {
        this.code = code;
        this.httpCode = httpCode;
        this.message = message;
    }
}
