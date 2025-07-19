package com.example.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import com.example.model.Payment;


import java.time.Instant;
import java.util.UUID;

// @QuarkusTest
public class PaymentProcessorHttpClientTest {


    // @Inject
    // PaymentProcessorHttpClient paymentProcessorHttpClient;

    // @Test
    // void testProcessPaymentReal() {
    //     Payment payment = new Payment();
    //     payment.setCorrelationId(UUID.randomUUID());
    //     payment.setAmount(new java.math.BigDecimal("100.00"));
    //     payment.setRequestedAt(Instant.now());
    //     payment.setType(1);

        // Simula chamada real (vai depender do serviço estar rodando)
       
        // String result = paymentProcessorHttpClient.processPayment(payment)
        //         .await().atMost(java.time.Duration.ofSeconds(10));
         
    // }
}