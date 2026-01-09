package com.hms.common.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Utility class for controller/API testing with MockMvc.
 * Provides fluent API for building and executing HTTP requests in tests.
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * @Autowired
 * private MockMvc mockMvc;
 * 
 * @Test
 * void createPatient() throws Exception {
 *     PatientRequest request = new PatientRequest();
 *     request.setName("John Doe");
 *     
 *     MockMvcTestUtils.post(mockMvc, "/patients", request)
 *         .andExpect(status().isOk())
 *         .andExpect(jsonPath("$.data.name").value("John Doe"));
 * }
 * }
 * </pre>
 */
public final class MockMvcTestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private MockMvcTestUtils() {
        // Utility class
    }

    /**
     * Convert object to JSON string.
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Parse JSON string to object.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    /**
     * Perform GET request.
     */
    public static ResultActions get(MockMvc mockMvc, String url) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    /**
     * Perform GET request with query parameters.
     */
    public static ResultActions get(MockMvc mockMvc, String url, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder request = 
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON);
        
        params.forEach(request::param);
        
        return mockMvc.perform(request);
    }

    /**
     * Perform GET request with user context headers.
     */
    public static ResultActions getWithUser(MockMvc mockMvc, String url, String userId, String role) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId)
                .header("X-User-Role", role)
        );
    }

    /**
     * Perform POST request with JSON body.
     */
    public static ResultActions post(MockMvc mockMvc, String url, Object body) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body))
        );
    }

    /**
     * Perform POST request with JSON body and user context.
     */
    public static ResultActions postWithUser(MockMvc mockMvc, String url, Object body, 
                                              String userId, String role) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body))
                .header("X-User-ID", userId)
                .header("X-User-Role", role)
        );
    }

    /**
     * Perform PUT request with JSON body.
     */
    public static ResultActions put(MockMvc mockMvc, String url, Object body) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body))
        );
    }

    /**
     * Perform PUT request with JSON body and user context.
     */
    public static ResultActions putWithUser(MockMvc mockMvc, String url, Object body,
                                             String userId, String role) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body))
                .header("X-User-ID", userId)
                .header("X-User-Role", role)
        );
    }

    /**
     * Perform PATCH request with JSON body.
     */
    public static ResultActions patch(MockMvc mockMvc, String url, Object body) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body))
        );
    }

    /**
     * Perform DELETE request.
     */
    public static ResultActions delete(MockMvc mockMvc, String url) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url)
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    /**
     * Perform DELETE request with user context.
     */
    public static ResultActions deleteWithUser(MockMvc mockMvc, String url, 
                                                String userId, String role) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId)
                .header("X-User-Role", role)
        );
    }

    /**
     * Create a query params map builder.
     */
    public static ParamsBuilder params() {
        return new ParamsBuilder();
    }

    /**
     * Builder for query parameters.
     */
    public static class ParamsBuilder {
        private final Map<String, String> params = new HashMap<>();

        public ParamsBuilder add(String key, String value) {
            params.put(key, value);
            return this;
        }

        public ParamsBuilder add(String key, Object value) {
            params.put(key, String.valueOf(value));
            return this;
        }

        public Map<String, String> build() {
            return params;
        }
    }
}
