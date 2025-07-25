package com.example.repository;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;


import com.example.model.Payment;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class WorkerVertexPaymentProcessor {

    private static final Logger LOG = Logger.getLogger(WorkerVertexPaymentProcessor.class);


    @Inject
    PaymentRepository paymentRepository;

    @Inject
    PaymentProcessorHttpClient paymentProcessorHttpClient;

    @Inject
    EventBus eventBus;

    @ConsumeEvent(value = "process-payment", blocking = false)
    public Uni<Void> processPayment(Payment payment) {
    //   LOG.infof("processando correlationId: %s", payment.getCorrelationId());
        if (todosIndisponiveis()) {
            eventBus.publish("process-payment", payment);
            return Uni.createFrom().voidItem();
        } else if (primaryDisponivelEFallbackIndisponivel()) {
            return paymentProcessorHttpClient.processPayment(payment)
                    .onFailure().invoke(throwable -> {
                        eventBus.publish("process-payment", payment);
                    })
                    .flatMap(result -> paymentRepository.save(payment))
                    .replaceWithVoid();

        } else if (primaryIndisponivelEFallbackDisponivel()) {
            return paymentProcessorHttpClient.processPaymentWithFallback(payment)
                    .onFailure().invoke(throwable -> {
                        eventBus.publish("process-payment", payment);
                    })
                    .flatMap(result -> paymentRepository.save(payment))
                    .replaceWithVoid();
        } else {
            //ambos disponiveis
            //latencia do primary é pelo menos 40% maior que a do fallback
         if(paymentProcessorHttpClient.primaryMinResponseTime.get() 
             >= paymentProcessorHttpClient.fallbackMinResponseTime.get() 
                 + 0.4 * paymentProcessorHttpClient.fallbackMinResponseTime.get()) {
            return paymentProcessorHttpClient.processPaymentWithFallback(payment)
                    .onFailure().invoke(throwable -> {
                        eventBus.publish("process-payment", payment);
                    })
                    .flatMap(result -> paymentRepository.save(payment))
                    .replaceWithVoid(); 
        } else {
            //primary esta com latencia aceitavel
            return paymentProcessorHttpClient.processPayment(payment)
                .onFailure().invoke(throwable -> {
                    eventBus.publish("process-payment", payment);
                })
                .flatMap(result -> paymentRepository.save(payment))
                .replaceWithVoid();
        }
    }
        
    }

    private boolean primaryDisponivelEFallbackIndisponivel() {
        return paymentProcessorHttpClient.primaryHealthy.get() && !paymentProcessorHttpClient.fallbackHealthy.get();
    }

    private boolean primaryIndisponivelEFallbackDisponivel() {
        return !paymentProcessorHttpClient.primaryHealthy.get()
                && paymentProcessorHttpClient.fallbackHealthy.get();
    }

    private boolean todosIndisponiveis() {
        return !paymentProcessorHttpClient.primaryHealthy.get() && !paymentProcessorHttpClient.fallbackHealthy.get();
    }

}
