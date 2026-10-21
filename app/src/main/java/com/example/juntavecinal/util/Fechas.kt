package com.example.juntavecinal.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Fechas {
    private val CHILE = TimeZone.getTimeZone("America/Santiago")
    private val UTC = TimeZone.getTimeZone("UTC")
    const val DIA_MS = 86_400_000L

    /** Medianoche UTC del día actual en Chile (mismo formato que Reserva.fecha). */
    fun inicioDeHoy(): Long {
        val chile = Calendar.getInstance(CHILE)
        return Calendar.getInstance(UTC).apply {
            clear()
            set(
                chile.get(Calendar.YEAR),
                chile.get(Calendar.MONTH),
                chile.get(Calendar.DAY_OF_MONTH)
            )
        }.timeInMillis
    }

    fun minutosActuales(): Int {
        val ahora = Calendar.getInstance(CHILE)
        return ahora.get(Calendar.HOUR_OF_DAY) * 60 + ahora.get(Calendar.MINUTE)
    }

    fun formatearFecha(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = UTC }.format(Date(millis))

    fun formatearHora(minutos: Int): String = "%02d:%02d".format(minutos / 60, minutos % 60)
}