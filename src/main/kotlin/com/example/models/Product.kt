package com.example.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.productRoutes() {
    routing {
        get("/health") {
            call.respondText("Ktor is alive 🚀")
        }
    }
}