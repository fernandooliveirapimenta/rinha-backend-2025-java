package com.example;


import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.bson.Document;

import com.example.repository.PaymentRepository;


@Path("/payments-summary")
public class PaymentSummaryResource {
   
    @Inject
    PaymentRepository paymentRepository;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Document getPaymentSummary(
            @QueryParam("from") String fromDate,
            @QueryParam("to") String toDate) {

        return paymentRepository.getPaymentSummary(fromDate, toDate);
        
    }

}