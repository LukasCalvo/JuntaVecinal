package com.example.juntavecinal.model.entity

import com.example.juntavecinal.data.remote.Perfil

data class Reserva(
    val id: Long,
    val usuarioId: Long,
    val nombre: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val asistentes: Int,
    val observaciones: String?,
    val requiereProyector: Boolean,
    val aceptaReglamento: Boolean,
    val estado: String
) {
    fun modificablePor(usuario: Perfil): Boolean = usuario.id == usuarioId && estado != "CANCELADA"

    val estadoVisible: String
        get() = when (estado) {
            "PENDIENTE" -> "Pendiente"
            "CONFIRMADA" -> "Confirmada"
            "CANCELADA" -> "Cancelada"
            else -> estado
        }
}
