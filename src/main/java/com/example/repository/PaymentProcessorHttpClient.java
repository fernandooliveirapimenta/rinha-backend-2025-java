package com.example.repository;


import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
// import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
// import org.jboss.logging.Logger;

import com.example.model.Payment;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PaymentProcessorHttpClient {

    // private static final Logger LOG = Logger.getLogger(PaymentProcessorHttpClient.class);

    @Inject
    Vertx vertx;

    private WebClient primaryClient;
    private WebClient fallbackClient;

    public final AtomicBoolean primaryHealthy = new AtomicBoolean(true);
    public final AtomicBoolean fallbackHealthy = new AtomicBoolean(true);
    public final AtomicLong primaryMinResponseTime = new AtomicLong(3000); // default 300ms
    public final AtomicLong fallbackMinResponseTime = new AtomicLong(3000); // default 300ms

    @ConfigProperty(name = "payment-processor-default") 
    String PRIMARY_SERVICE_URL;
    @ConfigProperty(name = "payment-processor-fallback") 
    String FALLBACK_SERVICE_URL;
    private static final String PAYMENTS_ENDPOINT = "/payments";
    private static final String PURGE_PAYMENTS_ENDPOINT = "/admin/purge-payments";
    // private static final String HEALTH_ENDPOINT = "/payments/service-health";

    // Configurações
    private static final int BASE_PAYMENT_TIMEOUT = 10; // 10 segundos
    // private static final long HEALTH_CHECK_INTERVAL = 5100; // 5 segundos

    void onStart(@Observes StartupEvent ev) {
        this.primaryClient = WebClient.create(vertx, 
            new WebClientOptions()
                .setDefaultHost(extracted(PRIMARY_SERVICE_URL)[0])
                .setDefaultPort(Integer.parseInt(extracted(PRIMARY_SERVICE_URL)[1]))
                .setConnectTimeout(1000)
                .setIdleTimeout(BASE_PAYMENT_TIMEOUT));

        this.fallbackClient = WebClient.create(vertx, 
            new WebClientOptions()
                .setDefaultHost(extracted(FALLBACK_SERVICE_URL)[0])
                .setDefaultPort(Integer.parseInt(extracted(FALLBACK_SERVICE_URL)[1]))   
                .setConnectTimeout(1000)
                .setIdleTimeout(BASE_PAYMENT_TIMEOUT));

        // startHealthChecks();

        //warmup http
        

        List<Uni<Void>> paymentUnis = new ArrayList<>();
        for (int i = 0; i <= 3; i++) {
            Payment payment = new Payment();
            payment.setAmount(new java.math.BigDecimal("1.00"));
            payment.setCorrelationId(UUID.randomUUID());
            paymentUnis.add(processPayment(payment).invoke(() -> {
                System.out.println("Payment processor warmed up successfully: " + payment.getCorrelationId());
            }).onFailure().invoke(failure -> {
                System.out.println("Failed to warm up payment processor: " + failure.getMessage());
            }));
        }

        Uni.combine().all().unis(paymentUnis)
            .with(unis -> null) // Combine os resultados das Unis (ou substitua null por lógica específica)
            .onItem().ignore().andSwitchTo(primaryPurgePayments())
            .subscribe().with(
                success -> {
                    System.out.println("Primary purge payments warmed up successfully.");
                }, 
                failure -> {
                    System.out.println("Failed to warm up primary purge payments: " + failure.getMessage());
                }
            );

        
    }

   

    private String[] extracted(String url) {
        return url.replace("http://", "").split(":");
    }

    // private void startHealthChecks() {
    //     vertx.setPeriodic(HEALTH_CHECK_INTERVAL, id -> {
    //         checkServiceHealth(primaryClient, PRIMARY_SERVICE_URL, primaryHealthy, primaryMinResponseTime);
    //         checkServiceHealth(fallbackClient, FALLBACK_SERVICE_URL, fallbackHealthy, fallbackMinResponseTime);
    //     });
    // }

    // private void checkServiceHealth(WebClient client, String baseUrl, 
    //                               AtomicBoolean healthFlag, AtomicLong minResponseTime) {
    //     client.get(HEALTH_ENDPOINT)
    //         .send()
    //         .subscribe()
    //         .with(
    //             response -> {
    //                 if (response.statusCode() == 200) {
    //                     JsonObject body = response.bodyAsJsonObject();
    //                     healthFlag.set(!body.getBoolean("failing", true));
    //                     long newMinTime = body.getLong("minResponseTime", 100L);
    //                     minResponseTime.set(newMinTime); 
    //                     // LOG.infof("Updated minResponseTime for %s: %dms", baseUrl, newMinTime);
    //                 } else {
    //                     LOG.warnf("H c for %s status %d", 
    //                              baseUrl, response.statusCode());
    //                 }
    //             },
    //             failure -> {
    //                 LOG.errorf("Health check failed for %s: %s", baseUrl, failure.getMessage());
    //                 healthFlag.set(false);
    //             }
    //         );
    // }

    // @Fallback(fallbackMethod = "processPaymentWithFallback")
    public Uni<Void> processPayment(Payment request) {

        return primaryClient.post(PAYMENTS_ENDPOINT)
            .sendJson(request)
            .onItem().transform(response -> {
                if(response.statusCode() > 300 ){
                    throw new RuntimeException("Failed to process payment with primary service ");
                } else {
                  return null;
                }
            });
    }

    public Uni<Void> processPaymentWithFallback(Payment request) {

        return fallbackClient.post(PAYMENTS_ENDPOINT)
            .sendJson(request)
            .onItem().transform(response -> {
                if(response.statusCode() > 300 ){
                    throw new RuntimeException("Failed to process payment with fallback service ");
                } else {
                  return null;
                }
            });
    }

    public Uni<Void> primaryPurgePayments() {
        return primaryClient.post(PURGE_PAYMENTS_ENDPOINT)
            // .putHeader("Content-Type", "application/json")
            .putHeader("X-Rinha-Token", "123")
            .send()
            .onItem().transform(response -> {
                if (response.statusCode() > 300) {
                    throw new RuntimeException("Failed to purge payments with primary service");
                } else {
                    System.out.println("purge primary ok");
                    return null;
                }
            });
    }
}
