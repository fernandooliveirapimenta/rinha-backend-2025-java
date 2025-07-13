# Quarkus DocumentDB Payments

Este projeto é uma aplicação Java 21 utilizando Quarkus e DocumentDB para gerenciar pagamentos.

## Estrutura do Projeto

<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-mutiny-web-client</artifactId>
  <version>4.4.7</version> <!-- Use the latest compatible version -->
</dependency>

```
quarkus-documentdb-payments
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           ├── PaymentsResource.java
│   │   │           ├── model
│   │   │           │   └── Payment.java
│   │   │           └── repository
│   │   │               └── PaymentRepository.java
│   │   └── resources
│   │       └── application.properties
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Endpoints

### POST /payments

Este endpoint permite criar um novo pagamento.

#### Request Body

```json
{
  "correlationId": "4a7901b8-7d26-4d9d-aa19-4dc1c7cf60b3",
  "amount": 19.90
}
```

#### Response

- **HTTP 200**: Retorna um status de sucesso.

## Configuração do Ambiente

1. **Docker**: Certifique-se de que o Docker está instalado e em execução.
2. **Docker Compose**: Utilize o Docker Compose para iniciar os serviços.

## Executando o Projeto

Para executar o projeto, utilize o seguinte comando:

```bash
docker-compose up
```

```bash
docker-compose up -d --build
```


Isso iniciará a aplicação Quarkus e o serviço DocumentDB.

## Dependências

O projeto utiliza as seguintes dependências:

- Quarkus
- Driver do DocumentDB

## Contribuição

Sinta-se à vontade para contribuir com melhorias ou correções.