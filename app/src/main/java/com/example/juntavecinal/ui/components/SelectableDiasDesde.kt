package com.example.juntavecinal.ui.components

import androidx.compose.material3.SelectableDates
import com.example.juntavecinal.model.repository.CalendarioFeriados
import com.example.juntavecinal.util.Fechas

class SelectableDiasDesde(
    private val minimo: Long,
    private val calendario: CalendarioFeriados
) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis >= minimo && calendario.permite(Fechas.desdePicker(utcTimeMillis))

    override fun isSelectableYear(year: Int): Boolean = calendario.cubre(year)
}
