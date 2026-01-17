package com.example

import com.example.database.DatabaseConfig
import com.example.models.ApplyDiscountRequest
import com.example.models.Product
import com.example.repository.ProductRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpConcurrencyTest {
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    
    private val baseUrl = "http://localhost:8080"
    private val productRepository = ProductRepository()
    
    @BeforeAll
    fun setup() {
        DatabaseConfig.init()
        
        // Create a test product
        productRepository.createProduct(
            Product(
                id = "test-product-1",
                name = "Test Product",
                basePrice = 100.0,
                country = "Sweden"
            )
        )
    }
    
    @Test
    fun `test concurrent discount application - same discount should only be applied once`() = runBlocking {
        val productId = "test-product-1"
        val discountId = "discount-concurrent-test-${System.currentTimeMillis()}"
        val discountPercent = 10.0
        
        // Launch 50 concurrent requests to apply the same discount
        val numberOfRequests = 50
        val deferredResults = (1..numberOfRequests).map { index ->
            async(Dispatchers.IO) {
                try {
                    val response = client.put("$baseUrl/products/$productId/discount") {
                        contentType(ContentType.Application.Json)
                        setBody(ApplyDiscountRequest(discountId, discountPercent))
                    }
                    response.status == HttpStatusCode.OK
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        val results = deferredResults.awaitAll()
        val successfulRequests = results.count { it }
        
        // Wait a bit for all transactions to complete
        delay(1000)
        
        // Verify the discount was applied exactly once by checking the product
        val product = productRepository.getProductById(productId)
        assertTrue(product != null, "Product should exist")
        
        val discountCount = product!!.discounts.count { it.discountId == discountId }
        assertEquals(1, discountCount, "Discount should be applied exactly once, even with $numberOfRequests concurrent requests")
        
        // Verify final price calculation
        val expectedFinalPrice = 100.0 * (1 - 10.0 / 100.0) * (1 + 0.25) // basePrice * (1 - discount%) * (1 + VAT%)
        assertEquals(expectedFinalPrice, product.finalPrice, 0.01, "Final price should be calculated correctly")
        
        println("✓ Concurrency test passed: $successfulRequests/$numberOfRequests requests succeeded, discount applied $discountCount time(s)")
    }
    
    @AfterAll
    fun cleanup() {
        client.close()
    }
}