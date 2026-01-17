package com.example.models

/**
 * Sealed class representing the result of applying a discount.
 * Provides type-safe error handling with explicit result types.
 */
sealed class DiscountResult {
    data class Success(val product: Product) : DiscountResult()
    data class AlreadyApplied(val product: Product) : DiscountResult()
    data class ProductNotFound(val productId: String) : DiscountResult()
    data class ValidationError(val message: String) : DiscountResult()
    data class DatabaseError(val message: String) : DiscountResult()
    data class BusinessRuleError(val message: String) : DiscountResult()
}