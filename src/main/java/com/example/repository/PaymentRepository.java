package com.example.repository;

import com.example.model.Payment;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.time.Instant;

import java.util.ArrayList;
import java.util.List;

import java.time.format.DateTimeParseException;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Accumulators.sum;

@ApplicationScoped
public class PaymentRepository {

    @Inject
    MongoClient mongoClient;

    private final String DATABASE_NAME = "paymentsDB";
    private final String COLLECTION_NAME = "payments";

    private MongoDatabase database;

    private MongoCollection<Document> collection;

    @PostConstruct
    void inicializarMongo() {
        this.database = mongoClient.getDatabase(DATABASE_NAME);
        this.collection = database.getCollection(COLLECTION_NAME);

        //criando indices se necessário
        //criando unique index para correlationId
        collection.createIndex(new Document("correlationId", 1), 
        new IndexOptions().unique(true));
        collection.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("type"),
                    Indexes.ascending("requestedAt")
                ),
                new IndexOptions().name("type_requestedAt_idx").background(true)
            );
        //criando index para requestedAt com type

    }

    public void save(Payment payment) {

        if(payment.getCorrelationId() != null 
          && payment.getAmount() != null
           && payment.getAmount().compareTo(BigDecimal.ZERO) > 0
           ) {
            Document doc = new Document("correlationId", payment.getCorrelationId())
                .append("amount", payment.getAmount())
                .append("requestedAt", payment.getRequestedAt())       
                .append("type", payment.getType());
        collection.insertOne(doc);
        }
       
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
            payment.setRequestedAt(doc.getDate("requestedAt").toInstant());
            payment.setType(doc.getInteger("type", 1)); // Default to type 1
            payments.add(payment);
        }
        return payments;
    }

    public Document getPaymentSummary(
             String fromDate,
             String toDate) {

        MongoDatabase database = mongoClient.getDatabase("paymentsDB");
        MongoCollection<Document> collection = database.getCollection("payments");

        Instant from = parseInstant(fromDate, "'from' é obrigatório");
        Instant to = parseInstant(toDate, "'to' é obrigatório");

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(match(and(
            gte("requestedAt", from),
            lte("requestedAt", to)
        )));
        pipeline.add(group(
            "$type",
            sum("count", 1), 
            sum("total", "$amount")  
        ));
        pipeline.add(project(new Document()
            .append("type", new Document("$cond", 
                new Document("if", new Document("$eq", List.of("$_id", 1)))
                    .append("then", "default")
                    .append("else", "fallback"))
            )
            .append("totalRequests", "$count")
            .append("totalAmount", "$total")
            .append("_id", 0)
        ));

        Document defaultStats = new Document()
            .append("totalRequests", 0)
            .append("totalAmount", 0.0);
        
        Document fallbackStats = new Document()
            .append("totalRequests", 0)
            .append("totalAmount", 0.0);

        for (Document doc : collection.aggregate(pipeline)) {
            String type = doc.getString("type");

            if ("default".equals(type)) {
                defaultStats
                    .put("totalRequests", doc.getInteger("totalRequests"));
                   defaultStats .put("totalAmount", doc.get("totalAmount", Decimal128.class));
            } else if ("fallback".equals(type)) {
                fallbackStats
                    .put("totalRequests", doc.getInteger("totalRequests"));
                fallbackStats.put("totalAmount", doc.get("totalAmount", Decimal128.class));
            }
        }

        return new Document()
            .append("default", defaultStats)
            .append("fallback", fallbackStats);
    }

    private Instant parseInstant(String dateStr, String errorMsg) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException(errorMsg);
        }
        try {
            return Instant.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de data inválido. Use ISO-8601 (ex: 2023-01-01T00:00:00Z)");
        }
    }


}