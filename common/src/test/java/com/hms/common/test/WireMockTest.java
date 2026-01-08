package com.hms.common.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Base test class for testing Feign clients with WireMock.
 * Provides methods to stub HTTP responses for external service calls.
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * @SpringBootTest(properties = {
 *     "feign.client.config.patient-service.url=http://localhost:${wiremock.server.port}"
 * })
 * class PatientClientTest extends WireMockTest {
 *     
 *     @Autowired
 *     private PatientClient patientClient;
 *     
 *     @Test
 *     void getPatient_shouldReturnPatient() {
 *         stubGetJson("/patients/1", 200, "{\"id\": 1, \"name\": \"John\"}");
 *         
 *         PatientResponse response = patientClient.getPatient(1L);
 *         
 *         assertThat(response.getName()).isEqualTo("John");
 *     }
 * }
 * }
 * </pre>
 */
public abstract class WireMockTest {

    protected static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(
            WireMockConfiguration.wireMockConfig()
                .dynamicPort()
        );
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    /**
     * Get the WireMock server port.
     */
    protected int getWireMockPort() {
        return wireMockServer.port();
    }

    /**
     * Get the WireMock base URL.
     */
    protected String getWireMockUrl() {
        return "http://localhost:" + wireMockServer.port();
    }

    // ==================== STUB HELPERS ====================

    /**
     * Stub a GET request to return JSON response.
     */
    protected void stubGetJson(String url, int status, String responseBody) {
        wireMockServer.stubFor(
            get(urlEqualTo(url))
                .willReturn(aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(responseBody))
        );
    }

    /**
     * Stub a GET request with path pattern to return JSON response.
     */
    protected void stubGetJsonPattern(String urlPattern, int status, String responseBody) {
        wireMockServer.stubFor(
            get(urlMatching(urlPattern))
                .willReturn(aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(responseBody))
        );
    }

    /**
     * Stub a POST request to return JSON response.
     */
    protected void stubPostJson(String url, int status, String responseBody) {
        wireMockServer.stubFor(
            post(urlEqualTo(url))
                .willReturn(aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(responseBody))
        );
    }

    /**
     * Stub a POST request with specific request body.
     */
    protected void stubPostJsonWithBody(String url, String requestBody, int status, String responseBody) {
        wireMockServer.stubFor(
            post(urlEqualTo(url))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(responseBody))
        );
    }

    /**
     * Stub a PUT request to return JSON response.
     */
    protected void stubPutJson(String url, int status, String responseBody) {
        wireMockServer.stubFor(
            put(urlEqualTo(url))
                .willReturn(aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(responseBody))
        );
    }

    /**
     * Stub a DELETE request.
     */
    protected void stubDelete(String url, int status) {
        wireMockServer.stubFor(
            delete(urlEqualTo(url))
                .willReturn(aResponse()
                    .withStatus(status))
        );
    }

    /**
     * Stub a request to return an error.
     */
    protected void stubError(String url, int status, String errorMessage) {
        String errorBody = String.format(
            "{\"error\": \"%s\", \"status\": %d}", 
            errorMessage, 
            status
        );
        wireMockServer.stubFor(
            get(urlEqualTo(url))
                .willReturn(aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(errorBody))
        );
    }

    /**
     * Stub a request to simulate timeout.
     */
    protected void stubTimeout(String url, int delayMillis) {
        wireMockServer.stubFor(
            get(urlEqualTo(url))
                .willReturn(aResponse()
                    .withFixedDelay(delayMillis)
                    .withStatus(HttpStatus.OK.value()))
        );
    }

    /**
     * Stub a request to simulate network failure.
     */
    protected void stubNetworkFailure(String url) {
        wireMockServer.stubFor(
            get(urlEqualTo(url))
                .willReturn(aResponse()
                    .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER))
        );
    }

    // ==================== VERIFICATION HELPERS ====================

    /**
     * Verify that a GET request was made.
     */
    protected void verifyGet(String url) {
        wireMockServer.verify(getRequestedFor(urlEqualTo(url)));
    }

    /**
     * Verify that a GET request was made N times.
     */
    protected void verifyGet(String url, int times) {
        wireMockServer.verify(times, getRequestedFor(urlEqualTo(url)));
    }

    /**
     * Verify that a POST request was made.
     */
    protected void verifyPost(String url) {
        wireMockServer.verify(postRequestedFor(urlEqualTo(url)));
    }

    /**
     * Verify that a POST request was made with specific body.
     */
    protected void verifyPostWithBody(String url, String bodyPattern) {
        wireMockServer.verify(
            postRequestedFor(urlEqualTo(url))
                .withRequestBody(containing(bodyPattern))
        );
    }

    /**
     * Verify no requests were made to URL.
     */
    protected void verifyNoRequests(String url) {
        wireMockServer.verify(0, getRequestedFor(urlEqualTo(url)));
    }
}
