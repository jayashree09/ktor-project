package com.example.service

import com.example.models.Discount
import com.example.models.DiscountResult
import com.example.models.Product
import com.example.repository.ProductRepository
import com.example.validation.DiscountValidator
import org.slf4j.LoggerFactory

class ProductService(private val productRepository: ProductRepository) {
    private val logger = LoggerFactory.getLogger(ProductService::class.java)
    
    fun getAllProductsByCountry(country: String): List<Product> {
        logger.debug("Fetching products for country: $country")
        return productRepository.getAllProductsByCountry(country)
    }
    
    fun getProductById(id: String): Product? {
        logger.debug("Fetching product with ID: $id")
        return productRepository.getProductById(id)
    }
    
    fun applyDiscount(productId: String, discount: Discount): DiscountResult {
        logger.info("Applying discount '${discount.discountId}' (${discount.percent}%) to product '$productId'")
        
        val idValidation = DiscountValidator.validateDiscountId(discount.discountId)
        if (idValidation is com.example.validation.ValidationResult.Error) {
            logger.warn("Invalid discount ID format: ${idValidation.message}")
            return DiscountResult.ValidationError(idValidation.message)
        }
        
        val percentValidation = DiscountValidator.validateDiscountPercent(discount.percent)
        if (percentValidation is com.example.validation.ValidationResult.Error) {
            logger.warn("Invalid discount percentage: ${percentValidation.message}")
            return DiscountResult.ValidationError(percentValidation.message)
        }
        
        val result = productRepository.applyDiscount(productId, discount)
        
        when (result) {
            is DiscountResult.Success -> {
                logger.info("Discount '${discount.discountId}' successfully applied to product '$productId'")
            }
            is DiscountResult.AlreadyApplied -> {
                logger.info("Discount '${discount.discountId}' already applied to product '$productId' (database constraint)")
            }
            is DiscountResult.ProductNotFound -> {
                logger.warn("Product not found: $productId")
            }
            is DiscountResult.DatabaseError -> {
                logger.error("Database error applying discount to product '$productId': ${result.message}")
            }
            else -> {
                logger.warn("Unexpected result when applying discount: $result")
            }
        }
        
        return result
    }
    
    fun createProduct(product: Product) {
        logger.info("Creating product: ${product.id}")
        productRepository.createProduct(product)
    }
}
