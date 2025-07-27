package com.example;

import com.example.model.Payment;
import com.example.repository.PaymentProcessorHttpClient;
import com.example.repository.PaymentRepository;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("/payments")
public class PaymentsResource {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    PaymentProcessorHttpClient paymentProcessorHttpClient;

    @Inject
    EventBus eventBus;

    @POST
    public Uni<Response> createPayment(Payment payment) {

        return Uni.createFrom().voidItem()
        .invoke(() -> eventBus.publish("process-payment", payment))
        .replaceWith(Response.accepted().build());
        
    }
}