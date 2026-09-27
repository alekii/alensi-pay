package com.alekii.alensipay.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alekii.alensipay.config.SecurityConfig;
import com.alekii.alensipay.domain.PaymentStatus;
import com.alekii.alensipay.dto.PaymentResponse;
import com.alekii.alensipay.security.ApiKeyAuthenticationFilter;
import com.alekii.alensipay.service.PaymentService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {PaymentController.class, WebhookController.class})
@Import({SecurityConfig.class, ApiKeyAuthenticationFilter.class})
@TestPropertySource(properties = "app.security.api-key=test-key")
class PaymentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void initiatePaymentRequiresApiKey() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "idem-1")
                        .content("""
                                {"provider":"mpesa","phoneNumber":"254712345678","amount":100.00,"currency":"KES"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhookEndpointIsPublic() throws Exception {
        when(paymentService.handleWebhook(eq("mpesa"), any())).thenReturn(new PaymentResponse(
                "pay-1",
                "MPESA",
                "254712345678",
                BigDecimal.valueOf(100),
                "KES",
                PaymentStatus.COMPLETED,
                "MPESA-pay-1",
                0,
                null,
                null,
                OffsetDateTime.now(),
                null
        ));

        mockMvc.perform(post("/api/webhooks/mpesa")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"providerReference":"MPESA-pay-1","status":"COMPLETED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(paymentService).handleWebhook(eq("mpesa"), any());
    }
}
