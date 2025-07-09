package com.example.repository;

import com.example.model.Payment;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PaymentRepository {

    @Inject
    MongoClient mongoClient;

    private final String DATABASE_NAME = "paymentsDB";
    private final String COLLECTION_NAME = "payments";

    public void save(Payment payment) {
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        Document doc = new Document("correlationId", payment.getCorrelationId())
                .append("amount", payment.getAmount());
        collection.insertOne(doc);
    }

    public List<Payment> listAll() {
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        List<Payment> payments = new ArrayList<>();
        for (Document doc : collection.find()) {
            Payment payment = new Payment();
            payment.setCorrelationId(doc.getString("correlationId"));
            Decimal128 decimalValue = doc.get("amount", Decimal128.class);
            payment.setAmount(decimalValue != null ? decimalValue.bigDecimalValue() : BigDecimal.ZERO);
            payments.add(payment);
        }
        return payments;
    }

    // Additional methods for retrieving payments can be added here
}