package com.example.repository;




import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.model.Payment;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.PostConstruct;
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

    @Inject
    Vertx vertx;

    private final List<Payment> paymentBuffer = new CopyOnWriteArrayList<>();
    // private static final int BATCH_SIZE = 100;
    private static final long BATCH_INTERVAL_MS = 12;


    @PostConstruct
    public void initializeBatchProcessor() {
        vertx.setPeriodic(BATCH_INTERVAL_MS, id -> processBatch());
    }

    private synchronized void addToBatch(Payment payment) {
        paymentBuffer.add(payment);
        // if (paymentBuffer.size() >= BATCH_SIZE) {
            // processBatch();
        // }
    }

    private synchronized void processBatch() {
        if (!paymentBuffer.isEmpty()) {
            List<Payment> batch = new ArrayList<>(paymentBuffer);
            paymentBuffer.clear(); // Limpa o buffer após copiar


            paymentRepository.saveMany(batch)
            .subscribe().with(
                success -> {
                    // paymentBuffer.removeAll(batch); // Limpa apenas os itens salvos
                    // System.out.println("Batch processado com sucesso.");
                },
                failure -> {
                    System.err.println("Falha batch ");
                }
            );
        }
    }

    @ConsumeEvent(value = "process-payment", blocking = true, ordered = false)
    public Uni<Void> processPaymentWorker(Payment payment) {

        return paymentProcessorHttpClient.processPayment(payment)
                .onFailure().invoke(t -> eventBus.publish("process-payment", payment))
                .invoke(() -> addToBatch(payment))
                .replaceWithVoid();
       
        

        // boolean primaryHealthy = paymentProcessorHttpClient.primaryHealthy.get();
        // boolean fallbackHealthy = paymentProcessorHttpClient.fallbackHealthy.get();
        // if (primaryHealthy && fallbackHealthy) {
        //     // Ambos disponíveis, mas vamos verificar a latência
        //     boolean primaryRuim = latenciaDoPrimaryEstaRuim();
        //     if (primaryRuim) {
        //         return paymentProcessorHttpClient.processPaymentWithFallback(payment)
        //             .onFailure().invoke(t -> eventBus.publish("process-payment", payment))
        //             .replaceWith(paymentRepository.save(payment))
        //             .replaceWithVoid();
        //     } else {
        //         return paymentProcessorHttpClient.processPayment(payment)
        //             .onFailure().invoke(t -> eventBus.publish("process-payment", payment))
        //             .replaceWith(paymentRepository.save(payment))
        //             .replaceWithVoid();
        //     }
        // }
       
        // // Só primary disponível
        // if (primaryHealthy && !fallbackHealthy) {
        //     return paymentProcessorHttpClient.processPayment(payment)
        //         .onFailure().invoke(t -> eventBus.publish("process-payment", payment))
        //         .replaceWith(paymentRepository.save(payment))
        //         .replaceWithVoid();
        // }

        // // Só fallback disponível
        // if (!primaryHealthy && fallbackHealthy) {
        //     return paymentProcessorHttpClient.processPaymentWithFallback(payment)
        //         .onFailure().invoke(t -> eventBus.publish("process-payment", payment))
        //         .replaceWith(paymentRepository.save(payment))
        //         .replaceWithVoid();
        // }

        //  // Todos indisponíveis: reenvia para fila e retorna
        //  eventBus.publish("process-payment", payment);
        //  return Uni.createFrom().voidItem();
        
    }




}
