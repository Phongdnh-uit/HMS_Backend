package com.hms.medical_exam_service.repositories;

import com.hms.medical_exam_service.entities.LabOrder;
import com.hms.medical_exam_service.entities.LabOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, String> {

    List<LabOrder> findByMedicalExamIdOrderByOrderDateDesc(String medicalExamId);

    List<LabOrder> findByPatientIdOrderByOrderDateDesc(String patientId);

    Page<LabOrder> findByStatus(LabOrderStatus status, Pageable pageable);

    Optional<LabOrder> findByOrderNumber(String orderNumber);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(l.orderNumber, 13) AS int)), 0) FROM LabOrder l WHERE l.orderNumber LIKE :prefix%")
    Integer findMaxOrderNumberForPrefix(String prefix);

    // Count orders by status for dashboard
    long countByStatus(LabOrderStatus status);

    // Find orders with incomplete results
    @Query("SELECT DISTINCT lo FROM LabOrder lo JOIN lo.results r WHERE lo.status != 'COMPLETED' AND lo.status != 'CANCELLED'")
    List<LabOrder> findPendingOrders();
}
