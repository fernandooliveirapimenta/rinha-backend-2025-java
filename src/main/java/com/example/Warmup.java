package com.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.UUID;

import org.jboss.logging.Logger;

import com.example.model.Payment;
import com.example.repository.PaymentRepository;

import io.quarkus.runtime.StartupEvent;

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
        // Warm up the resources to ensure they are ready for use

        for (int i = 0; i < 10; i++) {
           paymentRepository.save(payment)
            .subscribe().with(
                success -> 
                    {
                    LOG.info("Payment repository warmed up successfully");
                    paymentSummaryResource.getPaymentSummary("2020-07-10T12:34:56.000Z", "2026-08-10T12:35:56.000Z")
                    .subscribe().with(
                        summary -> {
                            LOG.info("Payment summary warmed up");
                            purgePaymentsResource.purgeAllPayments()
                                .subscribe().with(
                                    result -> LOG.info("All payments purged successfully"),
                                    failure -> LOG.error("Failed to purge payments", failure)
                                );
                    },
                        failure -> LOG.error("Failed to warm up payment summary", failure)
                    );
                },

                failure -> LOG.error("Failed to warm up payment repository", failure)
            );

        }
    }
   

}
