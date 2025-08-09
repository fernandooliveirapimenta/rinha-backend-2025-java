package com.example.repository;

import com.example.model.Payment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.runtime.StartupEvent;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeParseException;


@ApplicationScoped
public class PaymentRepository {

    @Inject
    ReactiveMongoClient mongoClient;

    @ConfigProperty(name = "cria-indice", defaultValue = "0")
    int criaIndice;

    private final String DATABASE_NAME = "paymentsDB";
    private final String COLLECTION_NAME = "payments";

    private ReactiveMongoDatabase database;
    private ReactiveMongoCollection<Document> collection;

  
    public void inicializarMongo(@Observes StartupEvent event) {
        this.database = mongoClient.getDatabase(DATABASE_NAME);
        this.collection = database.getCollection(COLLECTION_NAME);

        // Índices (executar apenas uma vez, idealmente via migration)
        
        if (criaIndice == 1) {
           System.out.println("indices mongoooooooo");
           collection.createIndex(new Document("correlationId", 1)).subscribe().with(x -> {});
           collection.createIndex(
                    new Document("type", 1).append("requestedAt", 1)
            ).subscribe().with(x -> {});
        }
        
    }

    public Uni<Void> save(Payment payment) {
      
        Document doc = new Document("correlationId", payment.getCorrelationId().toString())
                .append("amount", payment.getAmount())
                .append("requestedAt", payment.getRequestedAt())
                .append("type", 1);
        return collection.insertOne(doc).replaceWithVoid();
    
    }

     public Uni<Void> saveMany(List<Payment> payments) {

        List<Document> docs = payments.stream()
                .map(payment -> new Document("correlationId", payment.getCorrelationId().toString())
                        .append("amount", payment.getAmount())
                        .append("requestedAt", payment.getRequestedAt())
                        // .append("type", payment.getType())
                        .append("type", 1))

                .collect(Collectors.toList());
        return collection.insertMany(docs).replaceWithVoid();
    
    }

    public Uni<Document> getPaymentSummary(String fromDate, String toDate) {
        if(fromDate == null || fromDate.isBlank()) {
            // Se não for passado, assume 1º de janeiro de 2023
           fromDate= "2020-07-12T12:34:56.000Z";
        }
          if(toDate == null || toDate.isBlank()) {
           toDate= "2030-07-12T12:34:56.000Z";
        }

        Instant from = parseInstant(fromDate, "'from' é obrigatório");
        Instant to = parseInstant(toDate, "'to' é obrigatório");

        List<Document> pipeline = new ArrayList<>();
        pipeline.add(new Document("$match", new Document("requestedAt", new Document("$gte", from).append("$lte", to))));
        pipeline.add(new Document("$group", new Document("_id", "$type")
                .append("count", new Document("$sum", 1))
                .append("total", new Document("$sum", "$amount"))));
        pipeline.add(new Document("$project", new Document("type", new Document("$cond",
                new Document("if", new Document("$eq", List.of("$_id", 1)))
                        .append("then", "default")
                        .append("else", "fallback")))
                .append("totalRequests", "$count")
                .append("totalAmount", "$total")
                .append("_id", 0)));

        Document defaultStats = new Document()
                .append("totalRequests", 0)
                .append("totalAmount", 0.0);

        Document fallbackStats = new Document()
                .append("totalRequests", 0)
                .append("totalAmount", 0.0);

        return collection.aggregate(pipeline)
                .collect().asList()
                .map(list -> {
                    for (Document doc : list) {
                        String type = doc.getString("type");
                        if ("default".equals(type)) {
                            defaultStats.put("totalRequests", doc.getInteger("totalRequests"));
                            defaultStats.put("totalAmount", doc.get("totalAmount", Decimal128.class));
                        } else if ("fallback".equals(type)) {
                            fallbackStats.put("totalRequests", doc.getInteger("totalRequests"));
                            fallbackStats.put("totalAmount", doc.get("totalAmount", Decimal128.class));
                        }
                    }
                    return new Document()
                            .append("default", defaultStats)
                            .append("fallback", fallbackStats);
                });
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

    public Uni<Void> deleteAllPayments() {
        return collection.deleteMany(new Document()).replaceWithVoid();
    }
}