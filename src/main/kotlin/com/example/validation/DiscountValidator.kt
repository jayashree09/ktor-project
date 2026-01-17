package com.example.validation

import com.example.models.Discount

object DiscountValidator {
    private const val MAX_DISCOUNT_ID_LENGTH = 100
    private const val MAX_DISCOUNTS_PER_PRODUCT = 20
    private const val MAX_TOTAL_DISCOUNT_PERCENT = 100.0
    private val DISCOUNT_ID_PATTERN = Regex("^[A-Za-z0-9_-]+$")
    
    /**
     * Validates discount ID format and length
     */
    fun validateDiscountId(discountId: String): ValidationResult {
        if (discountId.isBlank()) {
            return ValidationResult.Error("Discount ID cannot be blank or empty")
        }
        
        if (discountId.length > MAX_DISCOUNT_ID_LENGTH) {
            return ValidationResult.Error("Discount ID must be at most $MAX_DISCOUNT_ID_LENGTH characters")
        }
        
        if (!discountId.matches(DISCOUNT_ID_PATTERN)) {
            return ValidationResult.Error("Discount ID can only contain alphanumeric characters, hyphens, and underscores")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Validates discount percentage range
     */
    fun validateDiscountPercent(percent: Double): ValidationResult {
        if (percent <= 0) {
            return ValidationResult.Error("Discount percentage must be greater than 0")
        }
        
        if (percent > MAX_TOTAL_DISCOUNT_PERCENT) {
            return ValidationResult.Error("Discount percentage cannot exceed $MAX_TOTAL_DISCOUNT_PERCENT%")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Validates that the cumulative discount doesn't exceed maximum
     */
    fun validateCumulativeDiscount(existingDiscounts: List<Discount>, newDiscountPercent: Double): ValidationResult {
        val totalDiscount = existingDiscounts.sumOf { it.percent } + newDiscountPercent
        
        if (totalDiscount > MAX_TOTAL_DISCOUNT_PERCENT) {
            val currentTotal = existingDiscounts.sumOf { it.percent }
            return ValidationResult.Error(
                "Total discount would exceed $MAX_TOTAL_DISCOUNT_PERCENT%. " +
                "Current total: ${currentTotal}%, Adding: $newDiscountPercent%"
            )
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Validates that product doesn't exceed maximum number of discounts
     */
    fun validateMaxDiscounts(existingDiscounts: List<Discount>): ValidationResult {
        if (existingDiscounts.size >= MAX_DISCOUNTS_PER_PRODUCT) {
            return ValidationResult.Error(
                "Maximum of $MAX_DISCOUNTS_PER_PRODUCT discounts allowed per product. " +
                "Current count: ${existingDiscounts.size}"
            )
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Comprehensive validation of a new discount
     */
    fun validateNewDiscount(discount: Discount, existingDiscounts: List<Discount>): ValidationResult {
        // Validate discount ID
        validateDiscountId(discount.discountId).let { result ->
            if (result is ValidationResult.Error) return result
        }
        
        // Validate discount percentage
        validateDiscountPercent(discount.percent).let { result ->
            if (result is ValidationResult.Error) return result
        }
        
        // Check if discount already exists
        if (existingDiscounts.any { it.discountId == discount.discountId }) {
            return ValidationResult.Error("Discount '${discount.discountId}' already applied to this product")
        }
        
        // Validate max discounts limit
        validateMaxDiscounts(existingDiscounts).let { result ->
            if (result is ValidationResult.Error) return result
        }
        
        // Validate cumulative discount
        validateCumulativeDiscount(existingDiscounts, discount.percent).let { result ->
            if (result is ValidationResult.Error) return result
        }
        
        return ValidationResult.Success
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}