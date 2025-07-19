package com.example.repository;

import com.example.model.Payment;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WorkerVertexPaymentProcessor {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    PaymentProcessorHttpClient paymentProcessorHttpClient;

    @ConsumeEvent(value = "process-payment")
    public Uni<Void> processPayment(Payment payment) {
     
        return Uni.createFrom().voidItem(); // Simula um processamento bem-sucedido
    }

}
