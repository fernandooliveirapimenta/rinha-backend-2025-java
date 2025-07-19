package com.example;

import com.example.repository.PaymentRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.bson.Document;
import io.smallrye.mutiny.Uni;

@Path("/payments-summary")
public class PaymentSummaryResource {

    @Inject
    PaymentRepository paymentRepository;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Document> getPaymentSummary(
            @QueryParam("from") String fromDate,
            @QueryParam("to") String toDate) {
        return paymentRepository.getPaymentSummary(fromDate, toDate);
    }
}