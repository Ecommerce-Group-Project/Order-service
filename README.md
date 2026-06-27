# Order Service

The **Order Service** is a Spring Boot microservice responsible for managing product orders in the E-Commerce platform. It interacts with the **Product Service** to fetch product details and publish order placement events to **RabbitMQ** for downstream processing (e.g., inventory, notifications).

---

## Features

- **Order Management**: Create orders with automated price calculation.
- **Microservices Communication**: REST client communication with the Product Service.
- **Event-Driven Integration**: Publishes message events to RabbitMQ upon order creation.
- **Validation**: Incoming order requests validation (quantity limits, ID checks).
- **Error Handling**: Standardized error responses.
- **In-Memory Testing Fallback**: Allows service runtime test fallback when the external Product Service is unavailable.

---

## Tech Stack

- **Java**: 17
- **Framework**: Spring Boot 3.2.0
- **Database**: PostgreSQL (Production) / H2 Database (Testing)
- **Messaging**: RabbitMQ (Spring AMQP)
- **API Documentation**: Springdoc OpenAPI / Swagger UI
- **Build Tool**: Maven

---

## Package Structure

```
src/main/java/com/ecommerce/order_service/
├── config/                 # RabbitMQ configurations
├── controller/             # REST Controller endpoints
├── dto/                    # Data Transfer Objects (Request/Response)
├── entity/                 # Database Entity mappings
├── exception/              # Global REST Exception Handlers
├── messaging/              # Message event publishers
├── repository/             # Spring Data JPA Repository
└── service/                # Business logic implementation
```

---

## Configuration

The application properties are defined in `src/main/resources/application.properties`. You can override properties using environment variables:

| Property | Default Value | Description |
|---|---|---|
| `server.port` | `8082` | Port on which the Order Service runs |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/order_db` | PostgreSQL Database Connection URL |
| `spring.rabbitmq.host` | `localhost` | RabbitMQ Host |
| `product.service.url` | `http://localhost:8081` | Product Service base URL |
| `product.service.mock-fallback` | `true` | Mock fallback product if Product Service is down |

---

## Running the Application

### 1. Start External Infrastructure
Use Docker Compose to run PostgreSQL and RabbitMQ:
```bash
docker-compose up -d
```

### 2. Run the Application
Start the Spring Boot application using Maven:
```bash
./mvnw spring-boot:run
```

The service will start on port `8082`.
- **Swagger UI**: Access interactive API documentation at [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)

---

## API Endpoints

### Create Order
- **URL**: `/orders`
- **Method**: `POST`
- **Content-Type**: `application/json`
- **Request Body**:
  ```json
  {
    "customerId": 1,
    "productId": 100,
    "quantity": 2
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "orderId": 1,
    "customerId": 1,
    "productId": 100,
    "productName": "Product Name",
    "quantity": 2,
    "totalPrice": 200.0,
    "orderDate": "2026-06-27T19:30:00",
    "status": "CREATED"
  }
  ```

---

## Messaging Architecture

Upon successful order creation, the service publishes an event to RabbitMQ:
- **Exchange**: `order.exchange` (Direct Exchange)
- **Routing Key**: `order.created`
- **Queue**: `order.queue` (bound to exchange via routing key)

**Message Payload Format**:
```json
{
  "orderId": 1,
  "customerId": 1,
  "productName": "Product Name",
  "totalPrice": 200.0
}
```

---

## Running Tests

Unit and integration tests are configured to run with an in-memory H2 Database and mocked endpoints so that no external infrastructure (PostgreSQL/RabbitMQ) is required:
```bash
./mvnw clean test
```
