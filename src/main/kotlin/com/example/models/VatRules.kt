package com.example.models

object VatRules {
    private val vatRates = mapOf(
        "Sweden" to 0.25,
        "Germany" to 0.19,
        "France" to 0.20
    )
    
    fun getVatRate(country: String): Double {
        return vatRates[country] ?: 0.0
    }
    
    fun calculateFinalPrice(basePrice: Double, totalDiscountPercent: Double, country: String): Double {
        val vatRate = getVatRate(country)
        return basePrice * (1 - totalDiscountPercent / 100.0) * (1 + vatRate)
    }
}