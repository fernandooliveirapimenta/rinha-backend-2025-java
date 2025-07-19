package com.example;

import com.example.model.Payment;
import com.example.repository.PaymentProcessorHttpClient;
import com.example.repository.PaymentRepository;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;

@Path("/payments")
public class PaymentsResource {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    PaymentProcessorHttpClient paymentProcessorHttpClient;

    @POST
    public Uni<Response> createPayment(Payment payment) {
        if (payment.getCorrelationId() == null
                || payment.getAmount() == null
                || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Correlation ID is required and amount must be greater than zero.")
                    .build());
        }

        return paymentProcessorHttpClient.processPayment(payment)
            .onFailure().invoke(throwable -> {
                System.err.println("Failed to process payment: " + throwable.getMessage());
            })
            .flatMap(result -> paymentRepository.save(payment))
            .replaceWith(Response.ok().build());
    }
}