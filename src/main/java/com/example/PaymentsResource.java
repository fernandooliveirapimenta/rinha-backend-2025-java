package com.example;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.List;

import com.example.model.Payment;
import com.example.repository.PaymentRepository;

@Path("/payments")
public class PaymentsResource {

    @Inject
    PaymentRepository paymentRepository;

    @POST
    public Response createPayment(Payment payment) {
        paymentRepository.save(payment);
        return Response.ok().build();
    }

    @GET
    public List<Payment> listPayments() {
        return paymentRepository.listAll();
    }
}