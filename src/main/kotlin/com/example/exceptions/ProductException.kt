package com.example.exceptions

/**
 * Base exception for product-related errors
 */
sealed class ProductException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when a product is not found
 */
class ProductNotFoundException(productId: String) : ProductException("Product with ID '$productId' does not exist")

/**
 * Thrown when validation fails
 */
class ValidationException(message: String) : ProductException(message)

/**
 * Thrown when a business rule is violated
 */
class BusinessRuleException(message: String) : ProductException(message)
