package com.example.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Payment {
    private UUID correlationId;
    private BigDecimal amount;
    private Instant requestedAt;
   // private int type = 1; //1 default, 2 fallback

    public Payment() {
        requestedAt = Instant.now();
    }

    // public int getType() {
    //     return type;
    // }
    // public void setType(int type) {
    //     this.type = type;
    // }
    public UUID getCorrelationId() {
        return correlationId;
    }
    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }   

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }


}