# Order Service

Orders, order history and tracking for the ecom platform (HLD 3.4, PRD 4).
Port **8084**. Uses the User Service to validate login tokens and the Product Service for product names and prices.

## Customer endpoints (`/api/v1/orders`, `Authorization: Bearer <token>`)

| Method | Path | Description |
|---|---|---|
| POST | `/` | Place an order: `{"items": [{"productId": 1, "quantity": 2}], "deliveryAddress": {...}}` |
| GET | `/` | My orders, newest first |
| GET | `/{id}` | One of my orders, with items and tracking history |
| POST | `/{id}/cancel` | Cancel an order that has not been paid |

Prices are fetched from the Product Service and copied onto the order; the total is calculated on the server, in paise.

## Internal endpoints (`/internal/api/v1/orders`, `X-Internal-Token: <secret>`)

| Method | Path | Called by |
|---|---|---|
| POST | `/{id}/payment-completed` | Payment Service: `{"paymentId": "1", "amount": 49900}`. Amount must equal the order total |
| PUT | `/{id}/status` | Warehouse / ops tools: `{"status": "SHIPPED", "note": "AWB 123"}` |

Status flow: `CREATED → PAID → SHIPPED → DELIVERED`, and `CREATED → CANCELLED`.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `USERSERVICE_URL` | `http://localhost:8082` | Token validation |
| `PRODUCTSERVICE_URL` | `http://localhost:8080` | Product names and prices |
| `INTERNAL_API_TOKEN` | – | Shared secret for `/internal/**`; rejected until set. Never commit |

## Run

```bash
./gradlew build
java -jar build/libs/orderservice-0.0.1-SNAPSHOT.jar                                  # H2
java -jar build/libs/orderservice-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql   # MySQL
```
