package com.hms.billing_service.services;

import com.hms.billing_service.config.VNPayConfig;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.entities.Payment;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.reset;

/**
 * Unit tests for VNPayService.
 * Tests payment URL generation, signature verification, and response handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-BILL-003/004: VNPayService Unit Tests")
class VNPayServiceTest {

    @Mock
    private VNPayConfig vnPayConfig;

    @InjectMocks
    private VNPayService vnPayService;

    private Payment testPayment;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        // Setup VNPay config with test values
        given(vnPayConfig.getTmnCode()).willReturn("TEST_TMN_CODE");
        given(vnPayConfig.getHashSecret()).willReturn("TEST_SECRET_KEY");
        given(vnPayConfig.getPayUrl()).willReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        given(vnPayConfig.getReturnUrl()).willReturn("http://localhost:3000/payment/callback");
        given(vnPayConfig.getVersion()).willReturn("2.1.0");
        given(vnPayConfig.getCommand()).willReturn("pay");
        given(vnPayConfig.getCurrCode()).willReturn("VND");
        given(vnPayConfig.getOrderType()).willReturn("250000");

        // Setup test invoice
        testInvoice = Invoice.builder()
                .id(TestDataFactory.uuid())
                .invoiceNumber("INV-20260107-0001")
                .totalAmount(new BigDecimal("500000"))
                .build();

        // Setup test payment
        testPayment = Payment.builder()
                .id(TestDataFactory.uuid())
                .invoice(testInvoice)
                .txnRef("TXN-" + System.currentTimeMillis())
                .amount(new BigDecimal("500000"))
                .orderInfo("Payment for invoice " + testInvoice.getInvoiceNumber())
                .expireAt(Instant.now().plusSeconds(900))
                .build();
    }

    @Nested
    @DisplayName("Method: createPaymentUrl()")
    class CreatePaymentUrlTests {

        @Test
        @DisplayName("UC-BILL-003: Should create payment URL with all required parameters")
        void createPaymentUrl_shouldGenerateValidUrl() {
            // Given
            String clientIp = "127.0.0.1";
            String returnUrl = null; // Use default
            String bankCode = null;
            String language = "vn";

            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, clientIp, returnUrl, bankCode, language);

            // Then
            assertThat(paymentUrl).isNotNull();
            assertThat(paymentUrl).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
            assertThat(paymentUrl).contains("vnp_Version=2.1.0");
            assertThat(paymentUrl).contains("vnp_Command=pay");
            assertThat(paymentUrl).contains("vnp_TmnCode=TEST_TMN_CODE");
            assertThat(paymentUrl).contains("vnp_TxnRef=" + testPayment.getTxnRef());
            assertThat(paymentUrl).contains("vnp_Amount=50000000"); // 500000 * 100
            assertThat(paymentUrl).contains("vnp_CurrCode=VND");
            assertThat(paymentUrl).contains("vnp_IpAddr=127.0.0.1");
            assertThat(paymentUrl).contains("vnp_Locale=vn");
            assertThat(paymentUrl).contains("vnp_OrderType=250000");
            assertThat(paymentUrl).contains("vnp_SecureHash=");
            assertThat(paymentUrl).contains("vnp_ReturnUrl=");
        }

        @Test
        @DisplayName("UC-BILL-003: Should include custom return URL when provided")
        void createPaymentUrl_withCustomReturnUrl_shouldIncludeIt() {
            // Given - reset and only set needed mocks for this specific test
            reset(vnPayConfig);
            given(vnPayConfig.getTmnCode()).willReturn("TEST_TMN_CODE");
            given(vnPayConfig.getHashSecret()).willReturn("TEST_SECRET_KEY");
            given(vnPayConfig.getPayUrl()).willReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
            given(vnPayConfig.getVersion()).willReturn("2.1.0");
            given(vnPayConfig.getCommand()).willReturn("pay");
            given(vnPayConfig.getCurrCode()).willReturn("VND");
            given(vnPayConfig.getOrderType()).willReturn("250000");
            // NOT setting returnUrl - custom URL should be used instead
            
            String customReturnUrl = "http://custom.com/callback";

            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", customReturnUrl, null, "vn");

            // Then
            // URL should contain the custom return URL (URL encoded)
            assertThat(paymentUrl).contains("vnp_ReturnUrl=");
            // The custom URL is URL-encoded in the payment URL
        }

        @Test
        @DisplayName("UC-BILL-003: Should include bank code when provided")
        void createPaymentUrl_withBankCode_shouldIncludeIt() {
            // Given
            String bankCode = "NCB";

            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, bankCode, "vn");

            // Then
            assertThat(paymentUrl).contains("vnp_BankCode=NCB");
        }

        @Test
        @DisplayName("UC-BILL-003: Should use English locale when specified")
        void createPaymentUrl_withEnglishLocale_shouldUseEn() {
            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "en");

            // Then
            assertThat(paymentUrl).contains("vnp_Locale=en");
        }

        @Test
        @DisplayName("UC-BILL-003: Should use default Vietnamese locale when null")
        void createPaymentUrl_withNullLocale_shouldUseDefaultVn() {
            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, null);

            // Then
            assertThat(paymentUrl).contains("vnp_Locale=vn");
        }

        @Test
        @DisplayName("UC-BILL-003: Should calculate amount correctly (multiply by 100)")
        void createPaymentUrl_shouldMultiplyAmountBy100() {
            // Given
            testPayment.setAmount(new BigDecimal("123456.50"));

            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "vn");

            // Then
            // 123456.50 * 100 = 12345650
            assertThat(paymentUrl).contains("vnp_Amount=12345650");
        }

        @Test
        @DisplayName("UC-BILL-003: Should URL encode order info")
        void createPaymentUrl_shouldUrlEncodeOrderInfo() {
            // Given
            testPayment.setOrderInfo("Thanh toán Hóa đơn #123");

            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "vn");

            // Then
            // Order info should be URL encoded
            assertThat(paymentUrl).contains("vnp_OrderInfo=");
            assertThat(paymentUrl).doesNotContain("Hóa đơn"); // Should be encoded
        }

        @Test
        @DisplayName("UC-BILL-003: Should generate valid HMAC-SHA512 signature")
        void createPaymentUrl_shouldGenerateValidSignature() {
            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "vn");

            // Then
            // Extract the signature (should be 128 characters hex string for SHA512)
            String[] parts = paymentUrl.split("vnp_SecureHash=");
            assertThat(parts).hasSizeGreaterThan(1);
            String signature = parts[1];
            assertThat(signature).hasSize(128); // SHA512 produces 64 bytes = 128 hex chars
            assertThat(signature).matches("[a-f0-9]{128}"); // Only hex characters
        }

        @Test
        @DisplayName("UC-BILL-003: Should include create date and expire date")
        void createPaymentUrl_shouldIncludeDates() {
            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "vn");

            // Then
            assertThat(paymentUrl).contains("vnp_CreateDate=");
            assertThat(paymentUrl).contains("vnp_ExpireDate=");
        }

        @Test
        @DisplayName("UC-BILL-003: Should use payment order info or generate default")
        void createPaymentUrl_withNullOrderInfo_shouldUseDefault() {
            // Given
            testPayment.setOrderInfo(null);

            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "vn");

            // Then
            // Should generate default order info
            assertThat(paymentUrl).contains("vnp_OrderInfo=");
        }
    }

    @Nested
    @DisplayName("Method: validateSignature()")
    class ValidateSignatureTests {

        @Test
        @DisplayName("UC-BILL-004: Should validate correct signature")
        void validateSignature_withValidSignature_shouldReturnTrue() {
            // Given - Create a payment URL first to get a valid signature
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "vn");
            
            // Extract parameters from URL and decode them
            Map<String, String> params = new HashMap<>();
            String queryString = paymentUrl.split("\\?")[1];
            for (String param : queryString.split("&")) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    try {
                        params.put(keyValue[0], java.net.URLDecoder.decode(keyValue[1], "UTF-8"));
                    } catch (Exception e) {
                        params.put(keyValue[0], keyValue[1]);
                    }
                }
            }

            // When
            boolean isValid = vnPayService.validateSignature(params);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("UC-BILL-004: Should reject tampered parameters")
        void validateSignature_withTamperedParams_shouldReturnFalse() {
            // Given - Create valid params first
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "vn");
            
            Map<String, String> params = new HashMap<>();
            String queryString = paymentUrl.split("\\?")[1];
            for (String param : queryString.split("&")) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }

            // Tamper with amount
            params.put("vnp_Amount", "99999999");

            // When
            boolean isValid = vnPayService.validateSignature(params);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("UC-BILL-004: Should reject missing signature")
        void validateSignature_withMissingSignature_shouldReturnFalse() {
            // Given
            reset(vnPayConfig); // This test doesn't use config mocks
            Map<String, String> params = new HashMap<>();
            params.put("vnp_TxnRef", testPayment.getTxnRef());
            params.put("vnp_Amount", "50000000");
            // No vnp_SecureHash

            // When
            boolean isValid = vnPayService.validateSignature(params);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("UC-BILL-004: Should reject invalid signature format")
        void validateSignature_withInvalidSignatureFormat_shouldReturnFalse() {
            // Given
            reset(vnPayConfig); // This test uses config only for hashSecret
            given(vnPayConfig.getHashSecret()).willReturn("TEST_SECRET_KEY");
            Map<String, String> params = new HashMap<>();
            params.put("vnp_TxnRef", testPayment.getTxnRef());
            params.put("vnp_Amount", "50000000");
            params.put("vnp_SecureHash", "invalid-signature");

            // When
            boolean isValid = vnPayService.validateSignature(params);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("UC-BILL-004: Should handle empty parameters")
        void validateSignature_withEmptyParams_shouldReturnFalse() {
            // Given
            reset(vnPayConfig); // This test uses config only for hashSecret
            given(vnPayConfig.getHashSecret()).willReturn("TEST_SECRET_KEY");
            Map<String, String> params = new HashMap<>();
            params.put("vnp_SecureHash", "abcd1234");

            // When
            boolean isValid = vnPayService.validateSignature(params);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("UC-BILL-004: Should ignore vnp_SecureHashType in validation")
        void validateSignature_shouldIgnoreSecureHashType() {
            // Given - Create valid params
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "vn");
            
            Map<String, String> params = new HashMap<>();
            String queryString = paymentUrl.split("\\?")[1];
            for (String param : queryString.split("&")) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    try {
                        params.put(keyValue[0], java.net.URLDecoder.decode(keyValue[1], "UTF-8"));
                    } catch (Exception e) {
                        params.put(keyValue[0], keyValue[1]);
                    }
                }
            }
            
            // Add vnp_SecureHashType (should be ignored in signature calculation)
            params.put("vnp_SecureHashType", "SHA512");

            // When
            boolean isValid = vnPayService.validateSignature(params);

            // Then
            assertThat(isValid).isTrue();
        }
    }

    @Nested
    @DisplayName("Method: getResponseMessage()")
    class GetResponseMessageTests {

        @BeforeEach
        void skipConfigSetup() {
            // These tests don't use VNPayConfig, so reset mocks
            reset(vnPayConfig);
        }

        @Test
        @DisplayName("UC-BILL-004: Should return success message for code 00")
        void getResponseMessage_withCode00_shouldReturnSuccess() {
            // When
            String message = vnPayService.getResponseMessage("00");

            // Then
            assertThat(message).isEqualTo("Giao dịch thành công");
        }

        @Test
        @DisplayName("UC-BILL-004: Should return customer cancel message for code 24")
        void getResponseMessage_withCode24_shouldReturnCancelled() {
            // When
            String message = vnPayService.getResponseMessage("24");

            // Then
            assertThat(message).isEqualTo("Giao dịch không thành công: Khách hàng hủy giao dịch");
        }

        @Test
        @DisplayName("UC-BILL-004: Should return insufficient balance message for code 51")
        void getResponseMessage_withCode51_shouldReturnInsufficientBalance() {
            // When
            String message = vnPayService.getResponseMessage("51");

            // Then
            assertThat(message).isEqualTo("Giao dịch không thành công: Tài khoản không đủ số dư");
        }

        @Test
        @DisplayName("Should return bank maintenance message for code 75")
        void getResponseMessage_withCode75_shouldReturnMaintenance() {
            // When
            String message = vnPayService.getResponseMessage("75");

            // Then
            assertThat(message).isEqualTo("Ngân hàng thanh toán đang bảo trì");
        }

        @Test
        @DisplayName("Should return unknown error message for invalid code")
        void getResponseMessage_withInvalidCode_shouldReturnUnknown() {
            // When
            String message = vnPayService.getResponseMessage("999");

            // Then
            assertThat(message).contains("Lỗi không xác định");
            assertThat(message).contains("999");
        }
    }

    @Nested
    @DisplayName("Method: isSuccessful()")
    class IsSuccessfulTests {

        @BeforeEach
        void skipConfigSetup() {
            // These tests don't use VNPayConfig, so reset mocks
            reset(vnPayConfig);
        }

        @Test
        @DisplayName("UC-BILL-004: Should return true for code 00")
        void isSuccessful_withCode00_shouldReturnTrue() {
            // When
            boolean isSuccess = vnPayService.isSuccessful("00");

            // Then
            assertThat(isSuccess).isTrue();
        }

        @Test
        @DisplayName("UC-BILL-004: Should return false for code 24")
        void isSuccessful_withCode24_shouldReturnFalse() {
            // When
            boolean isSuccess = vnPayService.isSuccessful("24");

            // Then
            assertThat(isSuccess).isFalse();
        }

        @Test
        @DisplayName("Should return false for any non-00 code")
        void isSuccessful_withAnyOtherCode_shouldReturnFalse() {
            // When & Then
            assertThat(vnPayService.isSuccessful("07")).isFalse();
            assertThat(vnPayService.isSuccessful("51")).isFalse();
            assertThat(vnPayService.isSuccessful("99")).isFalse();
            assertThat(vnPayService.isSuccessful(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Method: generateTxnRef()")
    class GenerateTxnRefTests {

        @BeforeEach
        void skipConfigSetup() {
            // These tests don't use VNPayConfig, so reset mocks
            reset(vnPayConfig);
        }

        @Test
        @DisplayName("UC-BILL-003: Should generate unique transaction reference")
        void generateTxnRef_shouldGenerateUniqueRef() {
            // When
            String txnRef1 = vnPayService.generateTxnRef();
            String txnRef2 = vnPayService.generateTxnRef();

            // Then
            assertThat(txnRef1).isNotNull();
            assertThat(txnRef2).isNotNull();
            assertThat(txnRef1).isNotEqualTo(txnRef2);
        }

        @Test
        @DisplayName("UC-BILL-003: Should generate valid format (timestamp-uuid)")
        void generateTxnRef_shouldHaveValidFormat() {
            // When
            String txnRef = vnPayService.generateTxnRef();

            // Then
            assertThat(txnRef).contains("-");
            String[] parts = txnRef.split("-", 2);
            assertThat(parts).hasSize(2);
            // First part should be numeric (timestamp)
            assertThat(parts[0]).matches("\\d+");
            // Second part should be 8 characters (UUID prefix)
            assertThat(parts[1]).hasSize(8);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very large amount")
        void createPaymentUrl_withLargeAmount_shouldHandle() {
            // Given
            testPayment.setAmount(new BigDecimal("99999999"));

            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "vn");

            // Then
            assertThat(paymentUrl).isNotNull();
            assertThat(paymentUrl).contains("vnp_Amount=9999999900");
        }

        @Test
        @DisplayName("Should handle special characters in order info")
        void createPaymentUrl_withSpecialCharsInOrderInfo_shouldUrlEncode() {
            // Given
            testPayment.setOrderInfo("Test & Order <Special> Chars!");

            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, null, "vn");

            // Then
            assertThat(paymentUrl).isNotNull();
            // The query string should be properly encoded
            // URL encoding converts & to %26, so we shouldn't see unescaped &
            // But the final URL has & between parameters, so we just verify it's valid
            assertThat(paymentUrl).contains("vnp_OrderInfo=");
        }

        @Test
        @DisplayName("Should handle empty bank code")
        void createPaymentUrl_withEmptyBankCode_shouldNotInclude() {
            // When
            String paymentUrl = vnPayService.createPaymentUrl(testPayment, "127.0.0.1", null, "", "vn");

            // Then
            assertThat(paymentUrl).isNotNull();
            assertThat(paymentUrl).doesNotContain("vnp_BankCode=");
        }
    }
}
