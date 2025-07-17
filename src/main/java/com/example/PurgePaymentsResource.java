package com.example;

import com.example.repository.PaymentRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/purge-payments")
public class PurgePaymentsResource {

    @Inject
    PaymentRepository paymentRepository;

    @POST
    public Response purgeAllPayments() {
        paymentRepository.deleteAllPayments();
        return Response.ok().build();
    }
}