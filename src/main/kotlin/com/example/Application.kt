package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import com.example.routes.productRoutes
import com.example.database.DatabaseConfig
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    logger.info("Initializing database connection...")
    DatabaseConfig.init()
    logger.info("Database initialized successfully")
    
    logger.info("Starting Ktor server on port 8080...")
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    logger.info("Configuring application module...")
    
    install(ContentNegotiation) {
        json()
    }
    
    productRoutes()
    
    logger.info("Application module configured successfully")
}