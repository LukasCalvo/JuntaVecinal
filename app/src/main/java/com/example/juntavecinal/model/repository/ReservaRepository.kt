package com.example.juntavecinal.model.repository

import com.example.juntavecinal.model.dao.ReservaDao
import com.example.juntavecinal.model.entity.Reserva
import kotlinx.coroutines.flow.Flow

class ReservaRepository(private val dao: ReservaDao) {

    fun todas(): Flow<List<Reserva>> = dao.observeAll()

    fun delUsuario(usuarioId: Int): Flow<List<Reserva>> = dao.observeByUser(usuarioId)

    suspend fun guardar(reserva: Reserva): String? {
        val delDia = dao.buscarPorDia(reserva.fecha)
        val solapada = delDia.any {
            reserva.horaInicioMin < it.horaFinMin &&
            it.horaInicioMin < reserva.horaFinMin
        }
        if (solapada) return "Ese horario ya está reservado."
        dao.insert(reserva)
        return null
    }

    suspend fun eliminar(reserva: Reserva) = dao.delete(reserva)
}