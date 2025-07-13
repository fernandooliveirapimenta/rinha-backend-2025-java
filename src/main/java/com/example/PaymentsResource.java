package com.example;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.math.BigDecimal;
import java.time.Duration;


import com.example.model.Payment;
import com.example.repository.PaymentProcessorHttpClient;
import com.example.repository.PaymentRepository;

@Path("/payments")
public class PaymentsResource {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    PaymentProcessorHttpClient paymentProcessorHttpClient;

    @POST
    public Response createPayment(Payment payment) {
        if(payment.getCorrelationId() == null 
          && payment.getAmount() != null
          && payment.getAmount().compareTo(BigDecimal.ZERO) <= 0
           ) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Correlation ID is required and amount must be greater than zero.")
                    .build();
            
           }
        
        paymentProcessorHttpClient.processPayment(payment)
                .onFailure().invoke(throwable -> {
                    // Log the error or handle it as needed
                    System.err.println("Failed to process payment: " + throwable.getMessage());
                })
                .await().atMost(Duration.ofSeconds(10));
        paymentRepository.save(payment);

        return Response.ok().build();
    }

    @GET
    public List<Payment> listPayments() {
        return paymentRepository.listAll();
    }
}