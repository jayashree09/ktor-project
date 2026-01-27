# Country-Based Product API

A Kotlin/Ktor service for managing products and discounts. The main challenge here was ensuring that the same discount can't be applied twice to a product, even when multiple requests come in at the same time.

## How Concurrency Works

Instead of checking if a discount already exists in the application code, we just try to insert it and let PostgreSQL handle the rest. The database has a composite primary key on `(product_id, discount_id)` which prevents duplicates at the database level.

When multiple requests try to apply the same discount:
- The first one succeeds (200 OK)
- All others get a primary key violation error from PostgreSQL (409 Conflict)
- Only one row ends up in the database

This way we don't need any locks or application-level checks - the database constraint does all the work.

## What It Does

- Manages a product catalog with country-specific VAT rates (Sweden 25%, Germany 19%, France 20%)
- Applies discounts to products in a way that's safe under concurrent load
- Calculates final prices including VAT and all applied discounts
- Validates discount IDs and percentages
- Handles errors with proper HTTP status codes

The code is split into routes (HTTP handling), service (business logic), and repository (database access) layers to keep things organized.

## Prerequisites

- JDK 17 or higher
- Docker & Docker Compose (for PostgreSQL)
- Gradle (wrapper included)

## Getting Started

First, make sure PostgreSQL is running. If Docker is installed:

```bash
docker-compose up -d
```

Give it a few seconds to start up, then run the application:

```bash
./gradlew run
```

The API will be available at `http://localhost:8080`.

To run the tests (including the concurrency test that fires 50 simultaneous requests):

```bash
./gradlew test
```

## Using the API

### Get Products by Country

Country names are case-insensitive, so "sweden", "SWEDEN", and "Sweden" all work the same way. Only Sweden, Germany, and France are supported.

```bash
curl "http://localhost:8080/products?country=Sweden"
```

**Response:**
```json
[
  {
    "id": "prod-1",
    "name": "Swedish Chair",
    "basePrice": 100.0,
    "country": "Sweden",
    "discounts": [],
    "finalPrice": 125.0
  }
]
```

### Apply Discount

```bash
curl -X PUT "http://localhost:8080/products/prod-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId":"SUMMER2025","percent":10.0}'
```

**First time applying a discount (200 OK):**
```json
{
  "id": "prod-1",
  "name": "Swedish Chair",
  "basePrice": 100.0,
  "country": "Sweden",
  "discounts": [
    {
      "discountId": "SUMMER2025",
      "percent": 10.0
    }
  ],
  "finalPrice": 112.5
}
```

**Trying to apply the same discount again (409 Conflict):**
```json
{
  "error": "Discount already applied",
  "details": "Discount 'SUMMER2025' is already applied to product 'prod-1'"
}
```

**Product doesn't exist (404 Not Found):**
```json
{
  "error": "Product not found",
  "details": "Product with ID 'prod-999' does not exist"
}
```

**Invalid input (400 Bad Request):**
```json
{
  "error": "Validation error",
  "details": "Total discount would exceed 100%. Current total: 60.0%, Adding: 50.0%"
}
```

Or for invalid discount ID format:
```json
{
  "error": "Validation error",
  "details": "Discount ID can only contain alphanumeric characters, hyphens, and underscores"
}
```

### Verify Discount Applied

```bash
curl "http://localhost:8080/products?country=Sweden"
```

**Response:**
```json
[
  {
    "id": "prod-1",
    "name": "Swedish Chair",
    "basePrice": 100.0,
    "country": "Sweden",
    "discounts": [
      {
        "discountId": "SUMMER2025",
        "percent": 10.0
      }
    ],
    "finalPrice": 112.5
  }
]
```

## Validation Rules

Discount IDs can only contain letters, numbers, hyphens, and underscores (max 100 characters). So `SUMMER2025` is fine, but `"SALE 2025"` (has a space) or `"SALE!"` (has special char) will be rejected.

Discount percentages must be greater than 0 and not exceed 100%. Also, the total of all discounts on a product can't exceed 100% - so if we already have 60% off, we can't add another 50%.

Each product can have up to 20 discounts. After that, no more can be added.

Only Sweden, Germany, and France are supported for now. Country names are case-insensitive.

## Error Responses

| HTTP Status | Scenario | Example Response |
|-------------|----------|------------------|
| 200 OK | Discount applied successfully | ProductResponse with updated discounts |
| 400 Bad Request | Invalid input (ID format, percentage, total >100%, max discounts) | `{"error": "Validation error", "details": "..."}` |
| 404 Not Found | Product doesn't exist | `{"error": "Product not found", "details": "Product with ID '...' does not exist"}` |
| 409 Conflict | Discount already applied | `{"error": "Discount already applied", "details": "..."}` |
| 503 Service Unavailable | Database error | `{"error": "Service unavailable", "details": "..."}` |

## Price Calculation

Final price = base price × (1 - total discount%) × (1 + VAT%)

VAT rates: Sweden 25%, Germany 19%, France 20%.

Example: A product with base price 100.0, 10% discount, in Sweden:
- After discount: 100 × 0.90 = 90.0
- After VAT: 90.0 × 1.25 = 112.5

## Testing Concurrency

We can test what happens when multiple requests try to apply the same discount at once. Fire off a few requests in parallel - only one should succeed with 200 OK, the rest should get 409 Conflict. The test suite does this automatically with 50 concurrent requests.

We can test various scenarios - try applying discounts that would exceed 100% total, use invalid discount ID formats, or test with different country name cases. The test suite covers most of these cases.

## Project Structure

```
.
├── src/
│   ├── main/kotlin/com/example/
│   │   ├── Application.kt
│   │   ├── models/
│   │   │   ├── Product.kt
│   │   │   ├── Discount.kt
│   │   │   ├── DiscountResult.kt
│   │   │   └── VatRules.kt
│   │   ├── database/
│   │   │   ├── DatabaseConfig.kt
│   │   │   └── Tables.kt
│   │   ├── repository/
│   │   │   └── ProductRepository.kt
│   │   ├── service/
│   │   │   └── ProductService.kt
│   │   ├── routes/
│   │   │   └── ProductRoutes.kt
│   │   ├── validation/
│   │   │   ├── CountryValidator.kt
│   │   │   └── DiscountValidator.kt
│   │   └── exceptions/
│   │       └── ProductException.kt
│   └── test/kotlin/com/example/
│       ├── HttpConcurrencyTest.kt
│       └── ConcurrencyTest.kt
├── build.gradle.kts
├── docker-compose.yml
├── README.md
└── ARCHITECTURE.md
```

The code is organized into routes (HTTP stuff), service (business logic), repository (database access), models (data structures), validation (input checks), and database (table definitions).

## Configuration

We can override database settings with environment variables:
- `DATABASE_URL` (default: `jdbc:postgresql://localhost:5432/productdb`)
- `DATABASE_USER` (default: `postgres`)
- `DATABASE_PASSWORD` (default: `postgres`)

The app uses PostgreSQL's default transaction isolation level (READ COMMITTED), which works fine for this use case since we're relying on the primary key constraint for concurrency safety.

## Stopping the Application

1. Stop the Ktor server: `Ctrl+C`
2. Stop PostgreSQL: `docker-compose down`
3. Remove data volumes: `docker-compose down -v`

## Troubleshooting

**Connection refused errors:**
- Ensure PostgreSQL is running: `docker-compose ps`
- Check logs: `docker-compose logs postgres`

**Port 8080 already in use:**
- Change the port in `Application.kt`: `embeddedServer(Netty, port = 8081, ...)`

**Tests failing:**
- Ensure PostgreSQL is running before running tests
- Reset database: `docker-compose down -v && docker-compose up -d`

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed design explanation and sequence diagrams.
