package com.example

import com.example.database.DatabaseConfig
import com.example.models.Product
import com.example.models.ProductResponse
import com.example.repository.ProductRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConcurrencyTest {
    
    @Test
    fun `same discount with different percentage should be rejected`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val productRepository = ProductRepository()
        val productId = "test-prod-immutable-${System.currentTimeMillis()}"
        val discountId = "SUMMER2025"
        
        productRepository.createProduct(
            Product(
                id = productId,
                name = "Test Product",
                basePrice = 100.0,
                country = "Sweden"
            )
        )
        
        val response1 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"$discountId","percent":10.0}""")
        }
        assertEquals(HttpStatusCode.OK, response1.status)
        
        val response2 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"$discountId","percent":20.0}""")
        }
        
        assertEquals(HttpStatusCode.BadRequest, response2.status)
        val errorBody = response2.bodyAsText()
        assertTrue(errorBody.contains("already applied") || errorBody.contains(discountId))
        
        val getResponse = client.get("/products?country=Sweden")
        val products = Json.decodeFromString<List<ProductResponse>>(getResponse.bodyAsText())
        val product = products.first { it.id == productId }
        
        assertEquals(1, product.discounts.size)
        assertEquals(10.0, product.discounts[0].percent)
        
        println(" Immutability Test: Discount percentage cannot be changed once applied")
    }
    
    @Test
    fun `applying same discount with same percentage is idempotent`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val productRepository = ProductRepository()
        val productId = "test-prod-1-${System.currentTimeMillis()}"
        val discountId = "LOYALTY10"
        
        productRepository.createProduct(
            Product(
                id = productId,
                name = "Test Product",
                basePrice = 100.0,
                country = "Sweden"
            )
        )
        
        val response1 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"$discountId","percent":15.0}""")
        }
        assertEquals(HttpStatusCode.OK, response1.status)
        
        val product1 = Json.decodeFromString<ProductResponse>(response1.bodyAsText())
        assertEquals(1, product1.discounts.size)
        assertEquals(discountId, product1.discounts[0].discountId)
        assertEquals(15.0, product1.discounts[0].percent)
        
        val response2 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"$discountId","percent":15.0}""")
        }
        
        assertEquals(HttpStatusCode.Conflict, response2.status)
        val errorBody = response2.bodyAsText()
        assertTrue(errorBody.contains("Discount already applied"))
        
        val getResponse = client.get("/products?country=Sweden")
        val products = Json.decodeFromString<List<ProductResponse>>(getResponse.bodyAsText())
        val product = products.first { it.id == productId }
        
        assertEquals(1, product.discounts.size)
        assertEquals(discountId, product.discounts[0].discountId)
        
        println(" Idempotency Test: Same request returns 409 Conflict for already applied discount")
    }
    
    @Test
    fun `concurrent discount applications should apply only once`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val productRepository = ProductRepository()
        val productId = "test-prod-2-${System.currentTimeMillis()}"
        val discountId = "CONCURRENT_TEST_${System.currentTimeMillis()}"
        val concurrentRequests = 50
        
        productRepository.createProduct(
            Product(
                id = productId,
                name = "Test Product",
                basePrice = 100.0,
                country = "Sweden"
            )
        )
        
        val results = coroutineScope {
            (1..concurrentRequests).map {
                async {
                    client.put("/products/$productId/discount") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"discountId":"$discountId","percent":10.0}""")
                    }
                }
            }.awaitAll()
        }
        
        val successCount = results.count { it.status == HttpStatusCode.OK }
        val conflictCount = results.count { it.status == HttpStatusCode.Conflict }
        
        assertEquals(1, successCount, "Exactly one request should succeed")
        assertEquals(concurrentRequests - 1, conflictCount, "All other requests should return 409 Conflict")
        
        val response = client.get("/products?country=Sweden")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val products = Json.decodeFromString<List<ProductResponse>>(response.bodyAsText())
        val product = products.first { it.id == productId }
        
        assertEquals(1, product.discounts.size, "Product should have exactly one discount")
        assertEquals(discountId, product.discounts[0].discountId)
        assertEquals(10.0, product.discounts[0].percent)
        
        assertEquals(112.5, product.finalPrice, 0.01)
        
        println("Concurrency Test: $successCount succeeded, $conflictCount conflicts - discount applied once")
    }
    
    @Test
    fun `multiple different discounts can be applied concurrently`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val productRepository = ProductRepository()
        val productId = "test-prod-3-${System.currentTimeMillis()}"
        val discounts = listOf(
            "DISCOUNT1" to 5.0,
            "DISCOUNT2" to 10.0,
            "DISCOUNT3" to 15.0
        )
        
        productRepository.createProduct(
            Product(
                id = productId,
                name = "Test Product",
                basePrice = 100.0,
                country = "Germany"
            )
        )
        
        val results = coroutineScope {
            discounts.map { (discountId, percent) ->
                async {
                    client.put("/products/$productId/discount") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"discountId":"$discountId","percent":$percent}""")
                    }
                }
            }.awaitAll()
        }
        
        val successCount = results.count { it.status == HttpStatusCode.OK }
        assertEquals(discounts.size, successCount, "All different discounts should be applied")
        
        val response = client.get("/products?country=Germany")
        val products = Json.decodeFromString<List<ProductResponse>>(response.bodyAsText())
        val product = products.first { it.id == productId }
        
        assertEquals(3, product.discounts.size)
        assertTrue(product.discounts.any { it.discountId == "DISCOUNT1" })
        assertTrue(product.discounts.any { it.discountId == "DISCOUNT2" })
        assertTrue(product.discounts.any { it.discountId == "DISCOUNT3" })
        
        assertEquals(83.3, product.finalPrice, 0.01)
        
        println(" Multiple Discounts Test: All different discounts applied successfully")
    }
    
    @Test
    fun `discount exceeding 100 percent total should be rejected`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val productRepository = ProductRepository()
        val productId = "test-prod-4-${System.currentTimeMillis()}"
        
        productRepository.createProduct(
            Product(
                id = productId,
                name = "Test Product",
                basePrice = 100.0,
                country = "France"
            )
        )
        
        val response1 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"BIG_DISCOUNT","percent":60.0}""")
        }
        assertEquals(HttpStatusCode.OK, response1.status)
        
        val response2 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"ANOTHER_BIG","percent":50.0}""")
        }
        
        assertEquals(HttpStatusCode.BadRequest, response2.status)
        val errorBody = response2.bodyAsText()
        assertTrue(errorBody.contains("Total discount would exceed 100%"))
        
        println("Validation Test: Total discount >100% properly rejected")
    }
    
    @Test
    fun `invalid discount ID format should be rejected`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val productRepository = ProductRepository()
        val productId = "test-prod-5-${System.currentTimeMillis()}"
        
        productRepository.createProduct(
            Product(
                id = productId,
                name = "Test Product",
                basePrice = 100.0,
                country = "Sweden"
            )
        )
        
        val response1 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"","percent":10.0}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response1.status)
        assertTrue(response1.bodyAsText().contains("cannot be blank"))
        
        val response2 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"INVALID DISCOUNT!","percent":10.0}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response2.status)
        assertTrue(response2.bodyAsText().contains("alphanumeric"))
        
        val longId = "A".repeat(101)
        val response3 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"$longId","percent":10.0}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response3.status)
        assertTrue(response3.bodyAsText().contains("100 characters"))
        
        println("Validation Test: Invalid discount IDs properly rejected")
    }
    
    @Test
    fun `invalid discount percentage should be rejected`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val productRepository = ProductRepository()
        val productId = "test-prod-6-${System.currentTimeMillis()}"
        
        productRepository.createProduct(
            Product(
                id = productId,
                name = "Test Product",
                basePrice = 100.0,
                country = "Sweden"
            )
        )
        
        val response1 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"ZERO","percent":0.0}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response1.status)
        assertTrue(response1.bodyAsText().contains("must be greater than 0"))
        
        val response2 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"NEGATIVE","percent":-10.0}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response2.status)
        
        val response3 = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"OVER","percent":150.0}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response3.status)
        assertTrue(response3.bodyAsText().contains("cannot exceed 100"))
        
        println("Validation Test: Invalid percentages properly rejected")
    }
    
    @Test
    fun `product not found should return 404`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val response = client.put("/products/non-existent/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"TEST","percent":10.0}""")
        }
        
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Product not found"))
        
        println(" Error Handling Test: Non-existent product returns 404")
    }
    
    @Test
    fun `country parameter case insensitive`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val productRepository = ProductRepository()
        val productId = "test-prod-7-${System.currentTimeMillis()}"
        
        productRepository.createProduct(
            Product(
                id = productId,
                name = "Swedish Product",
                basePrice = 100.0,
                country = "Sweden"
            )
        )
        
        val responses = listOf("sweden", "Sweden", "SWEDEN", "sWeDeN").map { country ->
            client.get("/products?country=$country")
        }
        
        responses.forEach { response ->
            assertEquals(HttpStatusCode.OK, response.status)
            val products = Json.decodeFromString<List<ProductResponse>>(response.bodyAsText())
            assertTrue(products.isNotEmpty())
            assertTrue(products.any { it.id == productId })
        }
        
        println("Case Sensitivity Test: Country names handled correctly")
    }
    
    @Test
    fun `unsupported country should return 400`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val response = client.get("/products?country=Australia")
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorBody = response.bodyAsText()
        assertTrue(errorBody.contains("Unsupported country"))
        assertTrue(errorBody.contains("Sweden") || errorBody.contains("Germany") || errorBody.contains("France"))
        
        println("Validation Test: Unsupported country returns 400 with supported list")
    }
    
    @Test
    fun `missing country parameter should return 400`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val response = client.get("/products")
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Country parameter is required"))
        
        println("Validation Test: Missing country parameter handled")
    }
    
    @Test
    fun `maximum discounts per product limit enforced`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val productRepository = ProductRepository()
        val productId = "test-prod-8-${System.currentTimeMillis()}"
        val maxDiscounts = 20
        
        productRepository.createProduct(
            Product(
                id = productId,
                name = "Test Product",
                basePrice = 100.0,
                country = "Sweden"
            )
        )
        
        val results = (1..maxDiscounts).map { i ->
            client.put("/products/$productId/discount") {
                contentType(ContentType.Application.Json)
                setBody("""{"discountId":"DISCOUNT_$i","percent":1.0}""")
            }
        }
        
        assertEquals(maxDiscounts, results.count { it.status == HttpStatusCode.OK })
        
        val extraResponse = client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody("""{"discountId":"EXTRA","percent":1.0}""")
        }
        
        assertEquals(HttpStatusCode.BadRequest, extraResponse.status)
        assertTrue(extraResponse.bodyAsText().contains("Maximum") || extraResponse.bodyAsText().contains("20"))
        
        println(" Limit Test: Maximum 20 discounts per product enforced")
    }
    
    @Test
    fun `get products performance test with multiple discounts`() = testApplication {
        application {
            DatabaseConfig.init()
            module()
        }
        
        val productRepository = ProductRepository()
        val productId = "test-prod-9-${System.currentTimeMillis()}"
        
        productRepository.createProduct(
            Product(
                id = productId,
                name = "Test Product",
                basePrice = 100.0,
                country = "Sweden"
            )
        )
        
        val discountRequests = listOf(
            "PERF_TEST_1" to 5.0,
            "PERF_TEST_2" to 3.0,
            "PERF_TEST_3" to 2.0
        )
        
        discountRequests.forEach { (discountId, percent) ->
            client.put("/products/$productId/discount") {
                contentType(ContentType.Application.Json)
                setBody("""{"discountId":"$discountId","percent":$percent}""")
            }
        }
        
        val startTime = System.currentTimeMillis()
        val response = client.get("/products?country=Sweden")
        val duration = System.currentTimeMillis() - startTime
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val products = Json.decodeFromString<List<ProductResponse>>(response.bodyAsText())
        val product = products.first { it.id == productId }
        
        assertEquals(3, product.discounts.size)
        assertTrue(product.discounts.any { it.discountId == "PERF_TEST_1" })
        assertTrue(product.discounts.any { it.discountId == "PERF_TEST_2" })
        assertTrue(product.discounts.any { it.discountId == "PERF_TEST_3" })
        
        val expectedTotal = 5.0 + 3.0 + 2.0
        val expectedPrice = 100.0 * (1 - expectedTotal/100.0) * 1.25
        assertEquals(expectedPrice, product.finalPrice, 0.01)
        
        println(" Performance Test: GET /products completed in ${duration}ms")
    }
}
