package com.example;

import com.example.repository.PaymentRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;

@Path("/purge-payments")
public class PurgePaymentsResource {

    @Inject
    PaymentRepository paymentRepository;

    @POST
    public Uni<Response> purgeAllPayments() {
        return paymentRepository.deleteAllPayments()
            .replaceWith(Response.ok().build());
    }
}