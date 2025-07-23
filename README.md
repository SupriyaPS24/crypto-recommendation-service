#  Crypto Recommendation Service

A microservice built with **Spring Boot** that provides intelligent crypto investment insights based on price trends. 
The service reads CSV data of historical crypto prices, performs analysis such as normalized range calculations, and exposes several well-documented REST endpoints for statistics and recommendations.

---

## Features

-  Load and parse crypto price data from CSV files.
-  Calculate **oldest**, **newest**, **min**, and **max** prices for any supported crypto.
-  Compute **normalized range** and return sorted crypto rankings.
-  Identify the crypto with the **highest normalized range on a specific date**.
-  API documentation using **Swagger (OpenAPI)**.
-  Dockerized for easy deployment.
-  **Rate limiting** using `Bucket4j` (100 requests per minute per IP (can be configured)).
-  Global **exception handling** with descriptive error responses.

---

### Prerequisites 

* Docker
 `or`
* JDK and Maven

### Technologies

| Layer             | Technology                      |
|-------------------|---------------------------------|
| Language          | Java 17                         |
| Framework         | Spring Boot 3                   |
| CSV Parsing       | OpenCSV                         |
| Databse           | H2                              |
| API Documentation | Swagger/OpenAPI (springdoc)     |
| Rate Limiting     | Bucket4j                        |
| Containerization  | Docker (Temurin Alpine)         |
| Build Tool        | Maven                           |
| Testing           | JUnit + Mockito (85%+ coverage) |

### Schema Details

### Table: crypto_price

| Column    | Type          | Description                                  |
|-----------|---------------|----------------------------------------------|
| id        | BIGINT (PK)   | Primary key, auto-generated                   |
| timestamp | TIMESTAMP     | The timestamp (Instant) when price was recorded |
| symbol    | VARCHAR       | Cryptocurrency symbol (e.g., BTC, ETH)       |
| price     | DECIMAL       | Price value of the cryptocurrency             |

This table stores all cryptocurrency price data loaded from CSV files.

---

### Table: loaded_file

| Column       | Type          | Description                                                        |
|--------------|---------------|------------------------------------------------------------------|
| filename     | VARCHAR (PK)  | Name of the CSV file that has been processed                      |
| loaded_at    | TIMESTAMP     | Timestamp when the file was last loaded into the DB               |
| last_modified| BIGINT        | Last modified time of the CSV file in epoch milliseconds          |

The LoadedFile entity represents a record of a CSV file that has already been processed (loaded) into the database. 
This entity is used to avoid reloading the same CSV file repeatedly if its content has not changed.

### Endpoints Details

# API Endpoints Summary

| Endpoint                          | Method | Description                                          | Parameters                                     | Expected Response                     |
|----------------------------------|--------|------------------------------------------------------|-----------------------------------------------|-------------------------------------|
| `/cryptos/{symbol}/stats`         | GET    | Get oldest, newest, min, and max prices for a crypto symbol, optionally filtered by date range | `symbol` (String, required)<br>`from` (YYYY-MM-DD, optional)<br>`to` (YYYY-MM-DD, optional) | JSON with `oldestPrice`, `newestPrice`, `minPrice`, `maxPrice` |
| `/cryptos/normalized-range/all`  | GET    | Get all supported cryptos sorted descending by normalized range over optional date range | `from` (YYYY-MM-DD, optional)<br>`to` (YYYY-MM-DD, optional) | JSON array of objects `{symbol, normalizedRange}` |
| `/cryptos/normalized-range/max`  | GET    | Get the crypto with the highest normalized range on a specific date | `date` (YYYY-MM-DD, required)                | JSON object `{symbol, normalizedRange}` |

---

## Considerations

- **Scalability:**  
  The service supports an initial set of 5 cryptocurrencies but is designed to easily scale by updating the data source (e.g., CSV). It safely rejects unsupported crypto symbols in incoming requests.

- **Flexible Timeframes:**  
  Endpoints allow filtering data by date ranges such as 1 month, 6 months, or 1 year. If no dates are specified, the entire dataset is used. This flexibility enables tailored investment analysis windows.

- **Data Validation:**  
  The service validates inputs thoroughly, returning appropriate error responses for unsupported symbols or invalid date ranges, ensuring robust and predictable API behavior.

- **Sorted Insights:**  
  The normalized range endpoint provides a descending sorted list of cryptos by their volatility metric, helping users identify the most promising or volatile assets at a glance.

- **Market Leader Identification:**  
  The max normalized range endpoint helps spot the top performing crypto on any given day, useful for daily market insights.

---

## Additional Considerations

- **New Cryptocurrencies:**  
  As new cryptos emerge regularly, the service must be maintained to include them in the supported list (in application.properties) to ensure recommendations stay relevant.

- **Investment Timeframes:**  
  Different cryptocurrencies may require different analysis periods for safe investment decisions. The service supports variable date ranges to accommodate such differences.

- **Rate Limiting:**  
  To maintain service stability and prevent abuse, all `/cryptos/*` endpoints enforce a rate limit of 100 requests per minute per IP address.

##  Run Instructions

###  1. Manual (Without Docker)

### Build the JAR file
```bash
mvn clean package
```
### Run the application
```bash
mvn spring-boot:run
```
###  2. Docker
`includes a multi-stage docker file seperating build and run stage`
Build the Docker image:

```bash
docker build -t crypto-recommendation .
```
***Run Docker container
```bash
docker run -p 8080:8080 crypto-recommendation
```

###  Kubernetes Deployment

You can deploy the service on **Kubernetes** using the provided manifest files:

- `deployment.yaml`
- `service.yaml`

###  1. Apply Manifests

```bash
kubectl apply -f deployment.yml
```
```bash
kubectl apply -f service.yml
```
### 2. Access the service.

**Get the NodePort:**
```bash
kubectl get service crypto-recommendation-service
```
Then visit:
` http://localhost:`<nodePort> (e.g., http://localhost:30000 if that's the port exposed) `

Note: this is for local later can be enhanced (No DockerHub or cloud needed — just your local image and Kubernetes (via Minikube or Docker Desktop))


## Testing

###  

Use `curl` or Postman to test endpoints. Example:

### 1. Get Price Statistics for a Crypto Symbol

**Request:**

```bash
curl http://localhost:8080/cryptos/BTC/stats
```
Expected response:

`{
  "symbol": "BTC",
  "oldest": 46813.21,
  "newest": 47300.10,
  "min": 45000.00,
  "max": 48200.50
}`

### 2. Get Normalized Range for All Cryptocurrencies

**Request:**

```bash
curl http://localhost:8080/cryptos/normalized-range/all
```
Expected response:

`[
  {
    "symbol": "BTC",
    "normalizedRange": 0.25
  },
  {
    "symbol": "ETH",
    "normalizedRange": 0.20
  }
]
`

### 3. Get Crypto with Max Normalized Range on a Specific Date

**Request:**

```bash
curl "http://localhost:8080/cryptos/normalized-range/max?date=2022-01-23"
```
Expected response:

`{
"symbol": "ETH",
"normalizedRange": 0.15
}
`

### Rate Limiting Testing
Each IP is allowed 100 requests per minute to /cryptos/* endpoints.

 Example Script (Exceeding Limit):
```bash 
for i in {1..105}
do
  echo "Request #$i"
  curl -s -w "\nStatus: %{http_code}\n" http://localhost:8080/cryptos/BTC/stats
done
```
Expected Result:<br>

`First 100 responses: 200 OK` <br>
`After 100th request (within 1 minute): 429 Too Many Requests
`

## Swagger API Documentation

Full API documentation is available via Swagger UI at:
`http://localhost:8080/swagger-ui/index.html`

### Future Improvements

- H2 can be migrated to an actual structured database.
- Introduce caching to avoid loading CSV data from the database every time.
- Implement logging for better traceability and debugging.
- Add integration tests to ensure system reliability.
- Enhance security measures.