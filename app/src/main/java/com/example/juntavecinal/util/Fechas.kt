package com.example.juntavecinal.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

object Fechas {
    val SANTIAGO: ZoneId = ZoneId.of("America/Santiago")
    private const val DIA_MS = 86_400_000L
    private val pantalla = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

    fun hoy(): LocalDate = LocalDate.now(SANTIAGO)

    fun leerFecha(texto: String): LocalDate? = try {
        LocalDate.parse(texto, pantalla)
    } catch (_: DateTimeParseException) {
        null
    }

    fun leerIso(texto: String): LocalDate? = try {
        LocalDate.parse(texto)
    } catch (_: DateTimeParseException) {
        null
    }

    fun fechaPicker(fecha: LocalDate): Long = fecha.toEpochDay() * DIA_MS

    fun desdePicker(millis: Long): LocalDate = LocalDate.ofEpochDay(Math.floorDiv(millis, DIA_MS))

    fun formatearFecha(fecha: LocalDate): String = fecha.format(pantalla)

    fun formatearFecha(iso: String): String = formatearFecha(LocalDate.parse(iso))

    fun formatearHora(hora: String): String = hora.take(5)
}
