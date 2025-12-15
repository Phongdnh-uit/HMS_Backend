# API Endpoint Implementation Gaps

**Analysis Date:** December 15, 2025  
**Services:** Auth, Patient, Medicine, HR, Appointment  
**Overall Coverage:** 72% (33/46 endpoints)

---

## 📋 Quick Action Summary

| Category | Count | Action Required |
|----------|-------|-----------------|
| ✅ **Already Exist** (routing only) | 2 | Update API Gateway routes |
| 🔴 **Need Implementation** | 4 | Add new endpoints |
| ✅ **Fully Covered** | 33 | No action |

### Endpoints Already Exist (Just Need Routing)
1. ✅ `POST /api/auth/accounts` → exists at `POST /accounts` (AccountController)
2. ✅ `GET /api/hr/doctors` → exists at `GET /api/hr/schedules/doctors` (ScheduleController)

### Endpoints Requiring Implementation
1. 🔴 `GET /api/auth/me` - Get current user profile
2. 🔴 `GET /api/patients/me` - Get own patient profile
3. 🔴 `PATCH /api/patients/me` - Update own patient profile
4. 🔴 `PATCH /api/medicines/{id}/stock` - Update inventory stock

---

## Summary Table

| Service | Implemented | Missing | Coverage | Priority |
|---------|-------------|---------|----------|----------|
| **Appointment** | 8/8 | 0 | ✅ 100% | - |
| **HR** | 12/15 | 3 | 80% | 🟡 Medium |
| **Patient** | 5/7 | 2 | 71% | 🔴 High |
| **Auth** | 5/6 | 1 | 83% | 🟡 Medium |
| **Medicine** | 5/10 | 5 | 50% | 🔴 High |

**Note:** HR and Auth coverage improved after discovering existing endpoints.

---

## ✅ RESOLVED - Already Exist (Need Routing Only)

### 1. POST /api/auth/accounts
- **Status:** ✅ EXISTS at `POST /accounts`
- **Controller:** `AccountController.java` (extends GenericController)
- **Path:** `/accounts` (base path)
- **Action:** Update API Gateway to route `/api/auth/accounts` → `/accounts`
- **Implementation:** Already complete via GenericController inheritance
- **Capabilities:** Full CRUD (create, read, update, delete) via GenericController

### 2. GET /api/hr/doctors  
- **Status:** ✅ EXISTS at `GET /api/hr/schedules/doctors`
- **Controller:** `ScheduleController.java` lines 54-66
- **Current Path:** `/hr/schedules/doctors`
- **Action:** Update API Gateway to route `/api/hr/doctors` → `/api/hr/schedules/doctors`
- **Implementation:** Fully functional with filters (date range, status, doctorId, departmentId)

---

## 🚨 CRITICAL Endpoints (Must Implement)

### 1. Auth Service - GET /api/auth/me

#### `GET /api/auth/me`
- **Priority:** CRITICAL
- **Access:** Authenticated
- **Purpose:** Get current user profile
- **Impact:** Frontend cannot display user info, role-based UI rendering fails
- **Status:** 🔴 MISSING - Needs implementation
- **Notes:** SecurityUtil.getCurrentUserId() exists but no endpoint exposes it
- **Implementation:** Add to `AuthController.java`

```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<AccountResponse>> getCurrentUser() {
    String userId = SecurityUtil.getCurrentUserId();
    Account account = accountService.findById(userId);
    AccountResponse response = accountMapper.entityToResponse(account);
    return ResponseEntity.ok(ApiResponse.ok(response));
}
```

---

### 2. Patient Service - Self-Service

#### `GET /api/patients/me`
- **Priority:** CRITICAL
- **Access:** PATIENT role
- **Purpose:** Get own patient profile without knowing patient ID
- **Impact:** Patient portal cannot function, patients cannot view their profile
- **Status:** 🔴 MISSING - Needs implementation
- **Required:** Add `findByAccountId(String accountId)` to PatientRepository
- **Implementation:** Add to `PatientController.java`

```java
// PatientRepository.java - add this method
Optional<Patient> findByAccountId(String accountId);

// PatientController.java - add this endpoint
@GetMapping("/me")
public ResponseEntity<ApiResponse<PatientResponse>> getMyProfile() {
    String accountId = SecurityUtil.getCurrentUserId();
    Patient patient = patientRepository.findByAccountId(accountId)
        .orElseThrow(() -> new ApiException(ErrorCode.PATIENT_NOT_FOUND));
    PatientResponse response = patientMapper.entityToResponse(patient);
    return ResponseEntity.ok(ApiResponse.ok(response));
}
```

#### `PATCH /api/patients/me`
- **Priority:** IMPORTANT
- **Access:** PATIENT role
- **Purpose:** Allow patients to update limited profile fields
- **Impact:** Patients must contact reception for simple updates
- **Status:** 🔴 MISSING - Needs implementation
- **Allowed Fields:** phoneNumber, address, allergies, emergency contact info
- **Restricted Fields:** fullName, email, dateOfBirth, identificationNumber (require staff)

```java
@PatchMapping("/me")
public ResponseEntity<ApiResponse<PatientResponse>> updateMyProfile(
        @Valid @RequestBody PatientSelfUpdateRequest request) {
    String accountId = SecurityUtil.getCurrentUserId();
    Patient patient = patientRepository.findByAccountId(accountId)
        .orElseThrow(() -> new ApiException(ErrorCode.PATIENT_NOT_FOUND));
    
    // Only allow updating specific fields
    if (request.getPhoneNumber() != null) patient.setPhoneNumber(request.getPhoneNumber());
    if (request.getAddress() != null) patient.setAddress(request.getAddress());
    if (request.getAllergies() != null) patient.setAllergies(request.getAllergies());
    if (request.getRelativeFullName() != null) patient.setRelativeFullName(request.getRelativeFullName());
    if (request.getRelativePhoneNumber() != null) patient.setRelativePhoneNumber(request.getRelativePhoneNumber());
    if (request.getRelativeRelationship() != null) patient.setRelativeRelationship(request.getRelativeRelationship());
    
    patient = patientRepository.save(patient);
    PatientResponse response = patientMapper.entityToResponse(patient);
    return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", response));
}
```

---

### 3. Medicine Service - Inventory Management

#### `PATCH /api/medicines/{id}/stock`
- **Priority:** CRITICAL
- **Access:** ADMIN, Internal (Medical Exam Service)
- **Purpose:** Update inventory with delta (+/- quantity) and reason
- **Impact:** Cannot track medicine consumption when prescriptions are created
- **Status:** 🔴 MISSING - Needs implementation
- **Workaround:** Can use `PUT /api/medicines/{id}` but updates entire entity (not ideal)
- **Notes:** Medicine entity has `quantity` field but no dedicated stock update endpoint
- **Implementation:** Add to `MedicineController.java`

**Request DTO:**
```java
public class StockUpdateRequest {
    @NotNull
    private Integer delta; // Can be negative for deduction
    
    private String reason; // e.g., "Prescription for patient P001", "Inventory adjustment"
}
```

**Controller:**
```java
@PatchMapping("/{id}/stock")
public ResponseEntity<ApiResponse<MedicineResponse>> updateStock(
        @PathVariable String id,
        @Valid @RequestBody StockUpdateRequest request) {
    Medicine medicine = medicineService.updateStock(id, request.getDelta(), request.getReason());
    MedicineResponse response = medicineMapper.entityToResponse(medicine);
    return ResponseEntity.ok(ApiResponse.ok("Stock updated successfully", response));
}
```

**Service Logic:**
```java
public Medicine updateStock(String id, Integer delta, String reason) {
    Medicine medicine = medicineRepository.findById(id)
        .orElseThrow(() -> new ApiException(ErrorCode.MEDICINE_NOT_FOUND));
    
    long newStock = medicine.getQuantity() + delta;
    
    if (newStock < 0) {
        throw new ApiException(ErrorCode.INSUFFICIENT_STOCK, 
            "Cannot deduct " + Math.abs(delta) + " units. Current stock: " + medicine.getQuantity());
    }
    
    medicine.setQuantity(newStock);
    
    // Log stock change for audit trail
    log.info("Stock updated for medicine {}: {} → {} (delta: {}, reason: {})",
        medicine.getName(), medicine.getQuantity() - delta, newStock, delta, reason);
    
    return medicineRepository.save(medicine);
}
```

---

## ℹ️ OPTIONAL Endpoints (Already Covered)

The following "missing" endpoints are actually covered by GenericController or alternative implementations:

### Medicine Service
- `PATCH /medicines/{id}` - Can use `PUT` from GenericController
- `GET /medicines` with filters - Implemented as `GET /medicines/all` with RSQL support

### HR Service  
- Similar patterns apply for Department/Employee list endpoints

---

## ✅ Extra Internal APIs (Keep - Not in Spec)

These endpoints exist for microservice communication and saga patterns:

### HR Service
- `GET /hr/schedules/by-doctor-date` - Used by appointment-service to validate availability
- `PATCH /hr/schedules/{id}/status` - Used by appointment-service to mark schedules as BOOKED

### Appointment Service
- `POST /appointments/bulk-cancel` - Used by hr-service (saga pattern)
- `POST /appointments/bulk-restore` - Compensation for saga rollback
- `GET /appointments/count` - Validation checks before schedule deletion

**Recommendation:** Document these in a separate "Internal Microservice APIs" section.

---

## 📋 Implementation Checklist

### Week 1 - Critical Endpoints

- [ ] **Auth Service**
  - [ ] Add `GET /api/auth/me` to AuthController
  - [ ] Verify `POST /api/auth/accounts` route in API Gateway
  
- [ ] **Patient Service**
  - [ ] Add `GET /api/patients/me` to PatientController
  - [ ] Add `PatientRepository.findByAccountId()` method
  - [ ] Test with PATIENT role token
  
- [ ] **Medicine Service**
  - [ ] Create `StockUpdateRequest` DTO
  - [ ] Add `PATCH /medicines/{id}/stock` to MedicineController
  - [ ] Implement stock validation (cannot go negative)
  - [ ] Add audit logging for stock changes

### Week 2 - Important Endpoints

- [ ] **Patient Service**
  - [ ] Create `PatientSelfUpdateRequest` DTO (restricted fields only)
  - [ ] Add `PATCH /patients/me` to PatientController
  - [ ] Add field restriction validation
  - [ ] Test field restriction enforcement

- [ ] **HR Service**
  - [ ] Add `GET /hr/doctors` to EmployeeController (or create DoctorController)
  - [ ] Test with department filter

### Week 3 - Testing & Documentation

- [ ] Integration tests for all new endpoints
- [ ] Update API documentation (api-contracts-complete.md)
- [ ] Add "Internal Microservice APIs" section
- [ ] Postman collection updates
- [ ] Frontend team notification of new endpoints

---

## 🔍 API Gateway Configuration Needed

Ensure the following routes are configured in `api-gateway`:

```yaml
# Medicine Categories Route
- id: medicine-categories
  uri: lb://MEDICINE-SERVICE
  predicates:
    - Path=/api/medicines/categories/**
  filters:
    - RewritePath=/api/medicines/categories/(?<segment>.*), /categories/$\{segment}

# Auth Accounts Route (if not already configured)
- id: auth-accounts
  uri: lb://AUTH-SERVICE
  predicates:
    - Path=/api/auth/accounts/**
  filters:
    - RewritePath=/api/auth/accounts/(?<segment>.*), /accounts/$\{segment}
```

---

## 📊 Impact Analysis

### Before Implementation
- **Patient Portal:** Non-functional (cannot get/update own profile)
- **Staff Onboarding:** Manual database insertion only
- **Inventory Tracking:** Not automated
- **Doctor Listing:** Requires complex RSQL query from frontend

### After Implementation (Critical + Important)
- ✅ **Patient Portal:** Fully functional self-service
- ✅ **Staff Onboarding:** Admin panel can create accounts
- ✅ **Inventory Tracking:** Automated via Medical Exam Service
- ✅ **Doctor Listing:** Simple dedicated endpoint
- ✅ **API Coverage:** 89% (41/46 endpoints)

---

## 🎯 Success Metrics

| Metric | Current | After Critical | After Important | Target |
|--------|---------|----------------|-----------------|--------|
| API Coverage | 72% | 83% | 89% | 95% |
| Patient Self-Service | ❌ | ✅ | ✅ | ✅ |
| Staff Onboarding | ❌ | ✅ | ✅ | ✅ |
| Inventory Automation | ❌ | ✅ | ✅ | ✅ |
| Services at 100% | 1/5 | 3/5 | 4/5 | 5/5 |

---

**Last Updated:** December 15, 2025  
**Next Review:** After critical endpoints implementation
