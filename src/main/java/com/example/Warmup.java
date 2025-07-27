package com.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import com.example.model.Payment;
import com.example.repository.PaymentRepository;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class Warmup {

    private static final Logger LOG = Logger.getLogger(Warmup.class);

    @Inject
    PaymentRepository paymentRepository;
    @Inject
    PurgePaymentsResource purgePaymentsResource;
    @Inject
    PaymentSummaryResource paymentSummaryResource;

    void onStart(@Observes StartupEvent ev) {
        paymentRepository.inicializarMongo(ev);

        Payment payment = new Payment();
        payment.setCorrelationId(UUID.randomUUID());
        payment.setAmount(new java.math.BigDecimal("1.00"));
        payment.setType(1);

        // Cria uma lista de Unis para salvar os pagamentos
        List<Uni<Void>> saveOperations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            saveOperations.add(
                paymentRepository.save(payment)
                    .invoke(() -> LOG.info("Payment repository warmed up successfully"))
                    .onFailure().invoke(failure -> LOG.error("Failed to warm up payment repository", failure))
            );
        }

        // Combina todas as operações de salvamento e executa a purga após a conclusão
        Uni.combine().all().unis(saveOperations)
            .combinedWith(ignored -> null) // Ignora os resultados individuais
            .flatMap(ignored -> {
                // Após salvar os pagamentos, aquece o resumo
                return paymentSummaryResource.getPaymentSummary("2020-07-10T12:34:56.000Z", "2026-08-10T12:35:56.000Z")
                    .invoke(summary -> LOG.info("Payment summary warmed up"))
                    .onFailure().invoke(failure -> LOG.error("Failed to warm up payment summary", failure));
            })
            .flatMap(ignored -> {
                // Após o resumo, executa a purga
                return purgePaymentsResource.purgeAllPayments()
                    .invoke(result -> LOG.info("All payments purged successfully"))
                    .onFailure().invoke(failure -> LOG.error("Failed to purge payments", failure));
            })
            .subscribe().with(
                ignored -> LOG.info("Warmup process completed successfully"),
                failure -> LOG.error("Warmup process failed", failure)
            );
    }
}
