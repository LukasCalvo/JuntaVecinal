package com.example.juntavecinal.model.repository

import com.example.juntavecinal.data.remote.Api
import com.example.juntavecinal.data.remote.FeriadosApi
import com.example.juntavecinal.data.remote.peticion
import com.example.juntavecinal.util.Fechas
import com.google.gson.JsonParseException
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CalendarioFeriados(val nacionales: Map<LocalDate, String>) {
    private val anios = nacionales.keys.map { it.year }.toSet()

    fun cubre(anio: Int): Boolean = anio in anios
    fun permite(fecha: LocalDate): Boolean = cubre(fecha.year) && fecha !in nacionales

    fun proximo(desde: LocalDate): Map.Entry<LocalDate, String>? =
        nacionales.entries.filter { it.key >= desde }.minByOrNull { it.key }
}

class FeriadosRepository(private val api: FeriadosApi) {
    private val consulta = Mutex()
    private var calendario: CalendarioFeriados? = null

    suspend fun obtener(): Result<CalendarioFeriados> = consulta.withLock {
        calendario?.let { return@withLock Result.success(it) }

        peticion {
            val datos = api.listar().data ?: throw JsonParseException("Calendario de feriados incompleto")
            val nacionales = datos.filter { it.scope == "Nacional" }.associate { feriado ->
                val fecha = feriado.date?.let(Fechas::leerIso)
                    ?: throw JsonParseException("Fecha de feriado inválida")
                val nombre = feriado.title?.takeIf(String::isNotBlank)
                    ?: throw JsonParseException("Feriado sin nombre")
                fecha to nombre
            }
            if (nacionales.isEmpty()) throw JsonParseException("Calendario sin feriados nacionales")
            CalendarioFeriados(nacionales)
        }.onSuccess { calendario = it }
    }

    companion object {
        val compartido by lazy { FeriadosRepository(Api.feriados) }
    }
}
