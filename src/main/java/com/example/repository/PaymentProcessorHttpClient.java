package com.example.repository;


import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.*;
import org.jboss.logging.Logger;

import com.example.model.Payment;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class PaymentProcessorHttpClient {

    private static final Logger LOG = Logger.getLogger(PaymentProcessorHttpClient.class);

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
    private static final String HEALTH_ENDPOINT = "/payments/service-health";

    // Configurações
    private static final int BASE_PAYMENT_TIMEOUT = 10; // 10 segundos
    private static final long HEALTH_CHECK_INTERVAL = 5100; // 5 segundos

    @PostConstruct
    void initialize() {
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

        startHealthChecks();
    }

   

    private String[] extracted(String url) {
        return url.replace("http://", "").split(":");
    }

    private void startHealthChecks() {
        vertx.setPeriodic(HEALTH_CHECK_INTERVAL, id -> {
            checkServiceHealth(primaryClient, PRIMARY_SERVICE_URL, primaryHealthy, primaryMinResponseTime);
            checkServiceHealth(fallbackClient, FALLBACK_SERVICE_URL, fallbackHealthy, fallbackMinResponseTime);
        });
    }

    private void checkServiceHealth(WebClient client, String baseUrl, 
                                  AtomicBoolean healthFlag, AtomicLong minResponseTime) {
        client.get(HEALTH_ENDPOINT)
            .send()
            .subscribe()
            .with(
                response -> {
                    if (response.statusCode() == 200) {
                        JsonObject body = response.bodyAsJsonObject();
                        healthFlag.set(!body.getBoolean("failing", true));
                        long newMinTime = body.getLong("minResponseTime", 100L);
                        minResponseTime.set(newMinTime); 
                        // LOG.infof("Updated minResponseTime for %s: %dms", baseUrl, newMinTime);
                    } else {
                        LOG.warnf("H c for %s status %d", 
                                 baseUrl, response.statusCode());
                    }
                },
                failure -> {
                    LOG.errorf("Health check failed for %s: %s", baseUrl, failure.getMessage());
                    healthFlag.set(false);
                }
            );
    }

    @Fallback(fallbackMethod = "processPaymentWithFallback")
    public Uni<String> processPayment(Payment request) {

        // LOG.debugf("Using dynamic timeout for primary service: %dms", dynamicTimeout);

        return primaryClient.post(PAYMENTS_ENDPOINT)
            .sendJson(request)
            .onItem().transform(response -> {
                if (response.statusCode() == 200) {
                    return response.bodyAsJsonObject().getString("message");
                } else {
                    String errorDetails = String.format(
                        "Payment failed - Status: %d, Body: %s",
                        response.statusCode(),
                        response.bodyAsString()
                    );
            
                    throw new RuntimeException("Failed to process payment with primary service " + errorDetails);
                }
            });
    }

    public Uni<String> processPaymentWithFallback(Payment request) {



        // LOG.debugf("Using dynamic timeout for fallback service: %dms", dynamicTimeout);

        return fallbackClient.post(PAYMENTS_ENDPOINT)
            .sendJson(request)
            .onItem().transform(response -> {
                if (response.statusCode() == 200) {
                    request.setType(2);
                    // LOG.warn("Payment processed with fallback service (higher fees may apply)");
                    return response.bodyAsJsonObject().getString("message");
                } else {
                     String errorDetails = String.format(
                        "Payment failed - Status: %d, Body: %s",
                        response.statusCode(),
                        response.bodyAsString()
                    );
                    throw new RuntimeException("Failed to process payment with fallback service " + errorDetails);
                }
            });
    }
}
