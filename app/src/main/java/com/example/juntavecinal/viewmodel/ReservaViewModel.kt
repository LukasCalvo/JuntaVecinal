package com.example.juntavecinal.viewmodel

data class ReservaErrores(
    val nombre: String? = null,
    val fecha: Long? = null,
    val horaInicio: Int? = null,                 // Int (hora 0-23)
    val horaFin: Int? = null,                    // Int (hora 0-23)
    val asistentes: Int? = null,                 // Int
    val observaciones: String = "",
    val aceptaReglamento: String? = null,
)

class ReservaViewModel {
}