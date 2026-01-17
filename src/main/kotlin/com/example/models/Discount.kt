package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Discount(
    val discountId: String,
    val percent: Double
) {
    init {
        require(percent > 0 && percent < 100) {
            "Discount percentage must be between 0 and 100 (exclusive)"
        }
    }
}