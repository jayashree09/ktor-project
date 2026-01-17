package com.example.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Column

object ProductsTable : Table("products") {
    val id: Column<String> = varchar("id", 255).uniqueIndex()
    val name: Column<String> = varchar("name", 255)
    val basePrice: Column<Double> = double("base_price")
    val country: Column<String> = varchar("country", 100)
}

object DiscountsTable : Table("discounts") {
    val productId: Column<String> = varchar("product_id", 255).references(ProductsTable.id, onDelete = org.jetbrains.exposed.sql.ForeignKeyConstraint.Cascade)
    val discountId: Column<String> = varchar("discount_id", 255)
    val percent: Column<Double> = double("percent")
    
    init {
        uniqueIndex(productId, discountId)
    }
}