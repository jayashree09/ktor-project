package com.example.routes

import com.example.models.ApplyDiscountRequest
import com.example.models.Discount
import com.example.models.DiscountResult
import com.example.models.ProductResponse
import com.example.service.ProductService
import com.example.validation.CountryValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ErrorResponse(
    val error: String,
    val details: String? = null
)

@Serializable
data class SuccessResponse(
    val message: String,
    val product: ProductResponse
)

fun Application.productRoutes() {
    val logger = LoggerFactory.getLogger("ProductRoutes")
    val productService = ProductService(com.example.repository.ProductRepository())
    
    routing {
        get("/products") {
            val countryParam = call.request.queryParameters["country"]
            
            if (countryParam.isNullOrBlank()) {
                logger.warn("GET /products called without country parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = "Country parameter is required",
                        details = "Supported countries: ${CountryValidator.getSupportedCountries().joinToString(", ")}"
                    )
                )
                return@get
            }
            
            val normalizedCountry = CountryValidator.normalizeCountry(countryParam)
            if (normalizedCountry == null) {
                logger.warn("GET /products called with unsupported country: $countryParam")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = "Unsupported country: $countryParam",
                        details = "Supported countries: ${CountryValidator.getSupportedCountries().joinToString(", ")}"
                    )
                )
                return@get
            }
            
            try {
                val products = productService.getAllProductsByCountry(normalizedCountry)
                val productResponses = products.map { product ->
                    ProductResponse(
                        id = product.id,
                        name = product.name,
                        basePrice = product.basePrice,
                        country = product.country,
                        discounts = product.discounts,
                        finalPrice = product.finalPrice
                    )
                }
                
                logger.debug("Returning ${productResponses.size} products for country: $normalizedCountry")
                call.respond(productResponses)
            } catch (e: Exception) {
                logger.error("Error fetching products for country $normalizedCountry", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        error = "Internal server error",
                        details = e.message
                    )
                )
            }
        }
        
        put("/products/{id}/discount") {
            val productId = call.parameters["id"]
                ?: run {
                    logger.warn("PUT /products/{id}/discount called without product ID")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(error = "Product ID is required")
                    )
                    return@put
                }
            
            try {
                val request = call.receive<ApplyDiscountRequest>()
                
                val discount = Discount(
                    discountId = request.discountId,
                    percent = request.percent
                )
                
                val result = productService.applyDiscount(productId, discount)
                
                when (result) {
                    is DiscountResult.Success -> {
                        call.respond(
                            HttpStatusCode.OK,
                            ProductResponse(
                                id = result.product.id,
                                name = result.product.name,
                                basePrice = result.product.basePrice,
                                country = result.product.country,
                                discounts = result.product.discounts,
                                finalPrice = result.product.finalPrice
                            )
                        )
                    }
                    is DiscountResult.AlreadyApplied -> {
                        call.respond(
                            HttpStatusCode.Conflict,
                            ErrorResponse(
                                error = "Discount already applied",
                                details = "Discount '${discount.discountId}' is already applied to product '$productId'"
                            )
                        )
                    }
                    is DiscountResult.ProductNotFound -> {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(
                                error = "Product not found",
                                details = "Product with ID '${result.productId}' does not exist"
                            )
                        )
                    }
                    is DiscountResult.ValidationError -> {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "Validation error",
                                details = result.message
                            )
                        )
                    }
                    is DiscountResult.DatabaseError -> {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            ErrorResponse(
                                error = "Service unavailable",
                                details = result.message
                            )
                        )
                    }
                    is DiscountResult.BusinessRuleError -> {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "Business rule violation",
                                details = result.message
                            )
                        )
                    }
                }
            } catch (e: io.ktor.serialization.SerializationException) {
                logger.warn("Invalid JSON in discount request for product $productId: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = "Invalid request body",
                        details = "Failed to parse JSON: ${e.message}"
                    )
                )
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid discount data for product $productId: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = "Validation error",
                        details = e.message
                    )
                )
            } catch (e: Exception) {
                logger.error("Unexpected error applying discount to product $productId", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        error = "Internal server error",
                        details = e.message
                    )
                )
            }
        }
    }
}