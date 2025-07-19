package com.example.repository;

import com.example.model.Payment;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WorkerVertexPaymentProcessor {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    PaymentProcessorHttpClient paymentProcessorHttpClient;

    @Inject
    EventBus eventBus;

    @ConsumeEvent(value = "process-payment", blocking = false)
    public Uni<Void> processPayment(Payment payment) {
    //se os dois estiverem indisponiveis envia novamente para fila
      if(!paymentProcessorHttpClient.primaryHealthy.get() && !paymentProcessorHttpClient.fallbackHealthy.get()) {
        eventBus.publish("process-payment", payment);
        return Uni.createFrom().voidItem();
      }

      //se defaul estiver indisponivel e fallback disponivel envia para fallback
      if(!paymentProcessorHttpClient.primaryHealthy.get() 
          && paymentProcessorHttpClient.fallbackHealthy.get()) {
        return paymentProcessorHttpClient.processPaymentWithFallback(payment)
            .onFailure().invoke(throwable -> {
                eventBus.publish("process-payment", payment);
            })
            .flatMap(result -> paymentRepository.save(payment))
            .replaceWithVoid();
        }

        //default disponivel porem timout muito alto
        return paymentProcessorHttpClient.processPayment(payment)
            .onFailure().invoke(throwable -> {
                eventBus.publish("process-payment", payment);
            })
            .flatMap(result -> paymentRepository.save(payment))
            .replaceWithVoid();
        // return Uni.createFrom().voidItem(); // Simula um processamento bem-sucedido
    }

}
