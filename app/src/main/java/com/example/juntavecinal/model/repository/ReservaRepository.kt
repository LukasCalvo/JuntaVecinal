package com.example.juntavecinal.model.repository

import android.content.Context
import com.example.juntavecinal.data.AppDatabase
import com.example.juntavecinal.data.remote.Api
import com.example.juntavecinal.data.remote.HorarioReserva
import com.example.juntavecinal.data.remote.ReservaMovil
import com.example.juntavecinal.data.remote.ReservasApi
import com.example.juntavecinal.data.remote.Sesion
import com.example.juntavecinal.data.remote.SolicitudReserva
import com.example.juntavecinal.model.dao.ReservaDao
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.model.entity.ReservaCache
import com.example.juntavecinal.util.Fechas
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReservaRepository(private val dao: ReservaDao, private val api: ReservasApi) {
    private val operaciones = Mutex()

    fun observar(cuentaId: Long): Flow<List<Reserva>> =
        dao.observar(cuentaId).map { lista -> lista.map(ReservaCache::reserva) }

    suspend fun actualizar(sesion: Sesion) = operaciones.withLock {
        val id = sesion.usuario.id
        val reservas = api.listar(sesion.autorizacion, id)
        dao.reemplazar(id, reservas.map { ReservaCache(id, it.aReserva()) })
    }

    suspend fun crear(sesion: Sesion, datos: SolicitudReserva) = operaciones.withLock {
        val reserva = api.crear(sesion.autorizacion, datos).aReserva()
        dao.guardar(listOf(ReservaCache(sesion.usuario.id, reserva)))
    }

    suspend fun editar(sesion: Sesion, original: Reserva, fecha: String, inicio: String, fin: String) =
        operaciones.withLock {
            val auth = sesion.autorizacion
            val espacioId = api.detalle(auth, original.id).espacioId
            val datos = HorarioReserva(sesion.usuario.id, espacioId, fecha, inicio, fin)
            val editada = api.editar(auth, original.id, datos)
            val reserva = original.copy(
                fecha = editada.fecha,
                horaInicio = editada.horaInicio,
                horaFin = editada.horaFin,
                estado = editada.estado
            )
            dao.guardar(listOf(ReservaCache(sesion.usuario.id, reserva)))
        }

    suspend fun cancelar(sesion: Sesion, id: Long) = operaciones.withLock {
        api.cancelar(sesion.autorizacion, id)
        dao.marcarCancelada(sesion.usuario.id, id)
    }

    private fun ReservaMovil.aReserva() = Reserva(
        id, usuarioId, nombre ?: "Reserva", Fechas.desdePicker(fecha).toString(),
        LocalTime.ofSecondOfDay(horaInicioMin * 60L).toString(),
        LocalTime.ofSecondOfDay(horaFinMin * 60L).toString(),
        asistentes ?: 0, observaciones, requiereProyector, aceptaReglamento, estado
    )

    companion object {
        @Volatile
        private var instancia: ReservaRepository? = null

        fun getInstance(context: Context): ReservaRepository = instancia ?: synchronized(this) {
            instancia ?: ReservaRepository(AppDatabase.getInstance(context).reservaDao(), Api.reservas)
                .also { instancia = it }
        }
    }
}
