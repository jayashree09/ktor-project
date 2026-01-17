package com.example.repository

import com.example.database.DiscountsTable
import com.example.database.ProductsTable
import com.example.models.Discount
import com.example.models.DiscountResult
import com.example.models.Product
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PSQLException

class ProductRepository {
    
    /**
     * Optimized: Uses single LEFT JOIN query instead of N+1 pattern.
     * Fetches products and their discounts in one database round trip.
     */
    fun getAllProductsByCountry(country: String): List<Product> = transaction {
        // Single query using LEFT JOIN - optimized to avoid N+1 queries
        val rows = (ProductsTable leftJoin DiscountsTable)
            .select { ProductsTable.country.lowerCase() eq country.lowercase() }
            .orderBy(ProductsTable.id to SortOrder.ASC, DiscountsTable.discountId to SortOrder.ASC)
        
        // Group by product ID and aggregate discounts in memory
        rows.groupBy { it[ProductsTable.id] }
            .map { (productId, productRows) ->
                val firstRow = productRows.first()
                val discounts = productRows
                    .mapNotNull { row ->
                        // Extract discount if it exists (discount_id will be null for products without discounts)
                        try {
                            val discountId = row[DiscountsTable.discountId]
                            if (discountId.isNotBlank()) {
                                Discount(
                                    discountId = discountId,
                                    percent = row[DiscountsTable.percent]
                                )
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            // Column is null (no discount) - skip
                            null
                        }
                    }
                    .distinctBy { it.discountId } // Remove duplicates if any
                
                Product(
                    id = productId,
                    name = firstRow[ProductsTable.name],
                    basePrice = firstRow[ProductsTable.basePrice],
                    country = firstRow[ProductsTable.country],
                    discounts = discounts
                )
            }
    }
    
    fun getProductById(id: String): Product? = transaction {
        // Use LEFT JOIN for single query
        val rows = (ProductsTable leftJoin DiscountsTable)
            .select { ProductsTable.id eq id }
            .orderBy(DiscountsTable.discountId to SortOrder.ASC)
        
        val firstRow = rows.firstOrNull() ?: return@transaction null
        
        val discounts = rows
            .mapNotNull { row ->
                try {
                    val discountId = row[DiscountsTable.discountId]
                    if (discountId.isNotBlank()) {
                        Discount(
                            discountId = discountId,
                            percent = row[DiscountsTable.percent]
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    // Column is null (no discount) - skip
                    null
                }
            }
            .distinctBy { it.discountId }
        
        Product(
            id = firstRow[ProductsTable.id],
            name = firstRow[ProductsTable.name],
            basePrice = firstRow[ProductsTable.basePrice],
            country = firstRow[ProductsTable.country],
            discounts = discounts
        )
    }
    
    fun createProduct(product: Product): Unit = transaction {
        ProductsTable.insert {
            it[id] = product.id
            it[name] = product.name
            it[basePrice] = product.basePrice
            it[country] = product.country
        }
    }
    
    /**
     * Applies a discount to a product in an idempotent and concurrency-safe way.
     * Uses database unique constraint to ensure the same discount cannot be applied twice.
     * Returns a DiscountResult for better error handling.
     */
    fun applyDiscount(productId: String, discount: Discount): DiscountResult = transaction {
        // Get product and existing discounts first (for validation)
        val existingProduct = getProductById(productId)
        if (existingProduct == null) {
            return@transaction DiscountResult.ProductNotFound(productId)
        }
        
        // Try to insert the discount. The unique constraint on (product_id, discount_id)
        // will prevent duplicates even under concurrent load.
        try {
            DiscountsTable.insert {
                it[DiscountsTable.productId] = productId
                it[DiscountsTable.discountId] = discount.discountId
                it[DiscountsTable.percent] = discount.percent
            }
            
            // Fetch updated product
            val updatedProduct = getProductById(productId)
                ?: throw IllegalStateException("Product disappeared after discount insertion")
            
            DiscountResult.Success(updatedProduct)
        } catch (e: Exception) {
            // Check for unique constraint violation (PostgreSQL error code 23505)
            when {
                e is PSQLException && e.sqlState == "23505" -> {
                    // Unique constraint violation - discount already exists
                    val currentProduct = getProductById(productId)
                        ?: throw IllegalStateException("Product not found")
                    DiscountResult.AlreadyApplied(currentProduct)
                }
                e.message?.contains("duplicate", ignoreCase = true) == true ||
                e.message?.contains("unique", ignoreCase = true) == true -> {
                    // Fallback: catch unique constraint by message
                    val currentProduct = getProductById(productId)
                        ?: throw IllegalStateException("Product not found")
                    DiscountResult.AlreadyApplied(currentProduct)
                }
                else -> {
                    // Other database errors
                    DiscountResult.DatabaseError("Database error: ${e.message}")
                }
            }
        }
    }
    
}