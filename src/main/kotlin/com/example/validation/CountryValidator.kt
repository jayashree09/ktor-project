package com.example.validation

import com.example.models.VatRules

object CountryValidator {
    private val supportedCountries = setOf("sweden", "germany", "france")
    
    fun normalizeCountry(country: String?): String? {
        if (country.isNullOrBlank()) {
            return null
        }
        
        val normalized = country.trim().lowercase()
        if (normalized in supportedCountries) {
            return when (normalized) {
                "sweden" -> "Sweden"
                "germany" -> "Germany"
                "france" -> "France"
                else -> normalized
            }
        }
        
        return null
    }
    
    fun isSupported(country: String?): Boolean {
        return normalizeCountry(country) != null
    }
    
    fun getSupportedCountries(): List<String> {
        return listOf("Sweden", "Germany", "France")
    }
}