package com.example.validation

import com.example.models.VatRules

object CountryValidator {
    private val supportedCountries = setOf("sweden", "germany", "france")
    
    /**
     * Validates and normalizes country name (case-insensitive)
     * Returns normalized country name if valid, null otherwise
     */
    fun normalizeCountry(country: String?): String? {
        if (country.isNullOrBlank()) {
            return null
        }
        
        val normalized = country.trim().lowercase()
        if (normalized in supportedCountries) {
            // Return proper case version
            return when (normalized) {
                "sweden" -> "Sweden"
                "germany" -> "Germany"
                "france" -> "France"
                else -> normalized
            }
        }
        
        return null
    }
    
    /**
     * Checks if country is supported
     */
    fun isSupported(country: String?): Boolean {
        return normalizeCountry(country) != null
    }
    
    /**
     * Gets list of supported countries
     */
    fun getSupportedCountries(): List<String> {
        return listOf("Sweden", "Germany", "France")
    }
}