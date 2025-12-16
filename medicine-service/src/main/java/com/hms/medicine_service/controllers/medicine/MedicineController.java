package com.hms.medicine_service.controllers.medicine;

import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.services.CrudService;
import com.hms.medicine_service.dtos.medicine.MedicineRequest;
import com.hms.medicine_service.dtos.medicine.MedicineResponse;
import com.hms.medicine_service.dtos.medicine.StockUpdateRequest;
import com.hms.medicine_service.dtos.medicine.StockUpdateResponse;
import com.hms.medicine_service.entities.Medicine;
import com.hms.medicine_service.repositories.MedicineRepository;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RequestMapping("/api/medicines")
@RestController
public class MedicineController extends GenericController<Medicine, String, MedicineRequest, MedicineResponse> {

    private final MedicineRepository medicineRepository;

    public MedicineController(
            CrudService<Medicine, String, MedicineRequest, MedicineResponse> service,
            MedicineRepository medicineRepository) {
        super(service);
        this.medicineRepository = medicineRepository;
    }

    /**
     * Override to match API contract: GET /api/medicines (instead of /api/medicines/all)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MedicineResponse>>> listMedicines(
            Pageable pageable,
            @RequestParam(value = "search", required = false) @Nullable String search) {
        Specification<Medicine> specification = RSQLJPASupport.toSpecification(search);
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(pageable, specification)));
    }

    /**
     * Update medicine stock using delta value.
     * Positive quantity adds stock, negative quantity deducts stock.
     * 
     * @param id Medicine ID
     * @param request Stock update request with delta quantity
     * @return Updated stock information
     */
    @PatchMapping("/{id}/stock")
    @Transactional
    public ResponseEntity<ApiResponse<StockUpdateResponse>> updateStock(
            @PathVariable String id,
            @Valid @RequestBody StockUpdateRequest request) {
        
        // Find medicine or throw 404
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Medicine not found"));
        
        // Validate delta is non-zero
        if (request.getDelta() == 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Delta must be non-zero");
        }
        
        // Calculate new quantity
        long currentQuantity = medicine.getQuantity() != null ? medicine.getQuantity() : 0L;
        long newQuantity = currentQuantity + request.getDelta();
        
        // Validate no negative stock
        if (newQuantity < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_STOCK, 
                    "Insufficient stock. Available: " + currentQuantity + ", Requested deduction: " + Math.abs(request.getDelta()));
        }
        
        // Update stock
        medicine.setQuantity(newQuantity);
        Medicine saved = medicineRepository.save(medicine);
        
        // Build response
        StockUpdateResponse response = StockUpdateResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .quantity(saved.getQuantity())
                .updatedAt(Instant.now())
                .build();
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}