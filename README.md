# Country-Based Product API

A Kotlin/Ktor service that manages products and discounts with database-enforced concurrency safety.

## Features

- Product catalog with country-specific VAT rates
- Idempotent discount application with concurrency guarantees
- PostgreSQL-based persistence with unique constraints
- Thread-safe discount operations
- Comprehensive input validation and error handling
- Protection against business logic violations (>100% discounts)
- Case-insensitive country handling
- Maximum discount limits per product
- **Optimized query performance** - Single JOIN query instead of N+1 pattern

## Prerequisites

- JDK 17 or higher
- Docker & Docker Compose (for PostgreSQL)
- Gradle (wrapper included)

## Quick Start

### 1. Start PostgreSQL

```bash
docker-compose up -d
```

Wait for PostgreSQL to be ready (about 5-10 seconds).

### 2. Build and Run

```bash
./gradlew run
```

The API will start on `http://localhost:8080`

### 3. Run Tests

```bash
./gradlew test
```

The test suite includes a concurrency test that simulates 50 simultaneous discount applications.

## API Usage

### Get Products by Country

**Note:** Country names are case-insensitive. Supported countries: Sweden, Germany, France.

```bash
curl "http://localhost:8080/products?country=Sweden"
# Also works: sweden, SWEDEN, sWeDeN
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

**Success Response (200 OK) - First Application:**
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

**Conflict Response (409 Conflict) - Already Applied:**
```json
{
  "error": "Discount already applied",
  "details": "Discount 'SUMMER2025' is already applied to product 'prod-1'"
}
```

**Not Found Response (404 Not Found):**
```json
{
  "error": "Product not found",
  "details": "Product with ID 'prod-999' does not exist"
}
```

**Validation Error Response (400 Bad Request):**
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

The API enforces the following validation rules:

### Discount ID Rules:
- Cannot be blank or empty
- Maximum length: 100 characters
- Allowed characters: Letters (A-Z, a-z), numbers (0-9), hyphens (-), underscores (_)
- Example valid IDs: `SUMMER2025`, `LOYALTY_10`, `EARLY-BIRD`
- Example invalid IDs: `"SUMMER 2025"` (space), `"SALE!"` (special char), `""` (empty)

### Discount Percentage Rules:
- Must be greater than 0 (zero not allowed)
- Cannot exceed 100%
- Total cumulative discounts on a product cannot exceed 100%
- Example: If product has 60% discount, cannot add another 50% discount

### Product Limits:
- Maximum 20 discounts per product
- Once limit reached, no additional discounts can be applied

### Country Rules:
- Only Sweden, Germany, and France are supported
- Country names are case-insensitive
- Unsupported countries return 400 Bad Request

## Error Responses

| HTTP Status | Scenario | Example Response |
|-------------|----------|------------------|
| 200 OK | Discount applied successfully | ProductResponse with updated discounts |
| 400 Bad Request | Invalid input (ID format, percentage, total >100%, max discounts) | `{"error": "Validation error", "details": "..."}` |
| 404 Not Found | Product doesn't exist | `{"error": "Product not found", "details": "Product with ID '...' does not exist"}` |
| 409 Conflict | Discount already applied | `{"error": "Discount already applied", "details": "..."}` |
| 503 Service Unavailable | Database error | `{"error": "Service unavailable", "details": "..."}` |

## Price Calculation

The final price is calculated as:

```
finalPrice = basePrice Ã— (1 - totalDiscount%) Ã— (1 + VAT%)
```

**VAT Rates:**
- Sweden: 25%
- Germany: 19%
- France: 20%

**Example:**
- Base Price: 100.0
- Discount: 10%
- VAT (Sweden): 25%
- Final Price: 100 Ã— 0.90 Ã— 1.25 = **112.5**

## Concurrency Testing

Test concurrent discount applications:

```bash
# Terminal 1-5: Apply the same discount simultaneously
for i in {1..5}; do
  curl -X PUT "http://localhost:8080/products/prod-1/discount" \
    -H "Content-Type: application/json" \
    -d '{"discountId":"TEST123","percent":5.0}' &
done
wait
```

Only one request will succeed (200 OK), others will return 409 Conflict.

### Advanced Test Scenarios

**Test 1: Validate >100% total discount rejection**
```bash
# Apply 60% discount
curl -X PUT "http://localhost:8080/products/prod-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId":"BIG1","percent":60.0}'

# Try to apply 50% discount (should fail - total would be 110%)
curl -X PUT "http://localhost:8080/products/prod-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId":"BIG2","percent":50.0}'
```

**Test 2: Invalid discount ID format**
```bash
# Should fail - contains space
curl -X PUT "http://localhost:8080/products/prod-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId":"INVALID DISCOUNT","percent":10.0}'
```

**Test 3: Case-insensitive country matching**
```bash
curl "http://localhost:8080/products?country=sweden"
curl "http://localhost:8080/products?country=SWEDEN"
curl "http://localhost:8080/products?country=Sweden"
# All three return the same results
```

## Project Structure

```
.
â”œâ”€â”€ src/
â”‚   â”œâ”€â”€ main/kotlin/com/example/
â”‚   â”‚   â”œâ”€â”€ Application.kt
â”‚   â”‚   â”œâ”€â”€ models/
â”‚   â”‚   â”œâ”€â”€ database/
â”‚   â”‚   â”œâ”€â”€ repository/
â”‚   â”‚   â”œâ”€â”€ routes/
â”‚   â”‚   â””â”€â”€ validation/
â”‚   â””â”€â”€ test/kotlin/com/example/
â”‚       â””â”€â”€ HttpConcurrencyTest.kt
â”œâ”€â”€ build.gradle.kts
â”œâ”€â”€ docker-compose.yml
â”œâ”€â”€ README.md
â””â”€â”€ ARCHITECTURE.md
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DATABASE_URL | jdbc:postgresql://localhost:5432/productdb | Database connection URL |
| DATABASE_USER | postgres | Database username |
| DATABASE_PASSWORD | postgres | Database password |

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
