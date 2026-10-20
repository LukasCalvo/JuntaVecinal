package com.example.juntavecinal.model

data class ReservaForm(
    val nombre: String = "",
    val fechaTexto: String = "",
    val horaInicioTexto: String = "",
    val horaFinTexto: String = "",
    val asistentes: String = "",
    val observaciones: String = "",
    val requiereProyector: Boolean = false,
    val aceptaReglamento: Boolean = false
)