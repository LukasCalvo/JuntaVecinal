package com.example.juntavecinal.model.entity

enum class Dependencia(val displayName: String, val capacidad: Int) {
    SALON_PRINCIPAL("Salón principal", 120),
    QUINCHO("Quincho", 40),
    CANCHA("Cancha multiuso", 30),
    SALA_REUNIONES("Sala de reuniones", 20)
}