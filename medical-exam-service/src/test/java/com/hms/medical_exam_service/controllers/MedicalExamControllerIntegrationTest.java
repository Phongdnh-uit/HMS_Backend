package com.hms.medical_exam_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.medical_exam_service.dtos.exam.MedicalExamRequest;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:medical_testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class MedicalExamControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MedicalExamRepository medicalExamRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        medicalExamRepository.deleteAll();
    }

    @Test
    @DisplayName("Create Exam: Should create new exam when appointment is valid")
    void createExamSuccess() throws Exception {
        // Given
        MedicalExamRequest request = new MedicalExamRequest();
        request.setAppointmentId("appt-success"); // Hook mock will treat this as valid if configured
        request.setDiagnosis("Flu");
        request.setNotes("Rest and water");

        // When & Then
        mockMvc.perform(post("/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.id", is("appt-success")))
                .andExpect(jsonPath("$.data.diagnosis", is("Flu")));

        // Verify persistance
        assertTrue(medicalExamRepository.existsByAppointmentId("appt-success"));
    }

    @Test
    @DisplayName("Delete Exam: Should be forbidden by hook")
    void deleteExamForbidden() throws Exception {
        // Given
        MedicalExam exam = new MedicalExam();
        exam.setAppointmentId("appt-delete");
        final MedicalExam savedExam = medicalExamRepository.save(exam);

        // When & Then
        // Generic Exception Handler might not be auto-configured in this slice, so we check for the bubbling exception
        jakarta.servlet.ServletException exception = org.junit.jupiter.api.Assertions.assertThrows(
            jakarta.servlet.ServletException.class,
            () -> mockMvc.perform(delete("/exams/" + savedExam.getId()))
        );
        
        org.junit.jupiter.api.Assertions.assertTrue(exception.getCause() instanceof com.hms.common.exceptions.errors.ApiException);
        com.hms.common.exceptions.errors.ApiException apiException = (com.hms.common.exceptions.errors.ApiException) exception.getCause();
        org.junit.jupiter.api.Assertions.assertEquals(com.hms.common.exceptions.errors.ErrorCode.OPERATION_NOT_ALLOWED, apiException.getErrorCode());
    }
    
    @Test
    @DisplayName("Get By Appointment: Should return exam")
    void getByAppointmentSuccess() throws Exception {
        // Given
        MedicalExam exam = new MedicalExam();
        exam.setAppointmentId("appt-get");
        exam.setDiagnosis("Healthy");
        medicalExamRepository.save(exam);

        // When & Then
        mockMvc.perform(get("/exams/by-appointment/appt-get"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diagnosis", is("Healthy")));
    }
    
    @Test
    @DisplayName("Get By Appointment: Should return 404 if not found")
    void getByAppointmentNotFound() throws Exception {
        // When & Then
        jakarta.servlet.ServletException exception = org.junit.jupiter.api.Assertions.assertThrows(
            jakarta.servlet.ServletException.class,
            () -> mockMvc.perform(get("/exams/by-appointment/appt-unknown"))
        );

        org.junit.jupiter.api.Assertions.assertTrue(exception.getCause() instanceof com.hms.common.exceptions.errors.ApiException);
        com.hms.common.exceptions.errors.ApiException apiException = (com.hms.common.exceptions.errors.ApiException) exception.getCause();
        org.junit.jupiter.api.Assertions.assertEquals(com.hms.common.exceptions.errors.ErrorCode.EXAM_NOT_FOUND, apiException.getErrorCode());
    }
}
