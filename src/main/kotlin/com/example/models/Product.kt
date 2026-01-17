package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val basePrice: Double,
    val country: String,
    val discounts: List<Discount> = emptyList()
) {
    val finalPrice: Double
        get() {
            val totalDiscountPercent = discounts.sumOf { it.percent }
            return VatRules.calculateFinalPrice(basePrice, totalDiscountPercent, country)
        }
}

@Serializable
data class ProductResponse(
    val id: String,
    val name: String,
    val basePrice: Double,
    val country: String,
    val discounts: List<Discount>,
    val finalPrice: Double
)

@Serializable
data class ApplyDiscountRequest(
    val discountId: String,
    val percent: Double
)