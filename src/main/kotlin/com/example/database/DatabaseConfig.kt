package com.example.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseConfig {
    private val dbUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/productdb"
    private val dbUser = System.getenv("DATABASE_USER") ?: "postgres"
    private val dbPassword = System.getenv("DATABASE_PASSWORD") ?: "postgres"
    
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            isAutoCommit = false
        }
        
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        
        // Create tables
        transaction {
            SchemaUtils.create(ProductsTable, DiscountsTable)
        }
    }
}