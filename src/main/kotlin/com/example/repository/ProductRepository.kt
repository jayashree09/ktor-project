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
    
    fun getAllProductsByCountry(country: String): List<Product> = transaction {
        val rows = (ProductsTable leftJoin DiscountsTable)
            .select { ProductsTable.country.lowerCase() eq country.lowercase() }
            .orderBy(ProductsTable.id to SortOrder.ASC, DiscountsTable.discountId to SortOrder.ASC)
        
        rows.groupBy { it[ProductsTable.id] }
            .map { (productId, productRows) ->
                val firstRow = productRows.first()
                val discounts = productRows
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
                            null
                        }
                    }
                    .distinctBy { it.discountId }
                
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
    
    fun applyDiscount(productId: String, discount: Discount): DiscountResult = transaction {
        val existingProduct = getProductById(productId)
        if (existingProduct == null) {
            return@transaction DiscountResult.ProductNotFound(productId)
        }
        
        try {
            DiscountsTable.insert {
                it[DiscountsTable.productId] = productId
                it[DiscountsTable.discountId] = discount.discountId
                it[DiscountsTable.percent] = discount.percent
            }
            
            val updatedProduct = getProductById(productId)
                ?: throw IllegalStateException("Product disappeared after discount insertion")
            
            DiscountResult.Success(updatedProduct)
        } catch (e: Exception) {
            when {
                e is PSQLException && e.sqlState == "23505" -> {
                    val currentProduct = getProductById(productId)
                        ?: throw IllegalStateException("Product not found")
                    DiscountResult.AlreadyApplied(currentProduct)
                }
                e.message?.contains("duplicate", ignoreCase = true) == true ||
                e.message?.contains("unique", ignoreCase = true) == true -> {
                    val currentProduct = getProductById(productId)
                        ?: throw IllegalStateException("Product not found")
                    DiscountResult.AlreadyApplied(currentProduct)
                }
                else -> {
                    DiscountResult.DatabaseError("Database error: ${e.message}")
                }
            }
        }
    }
    
}