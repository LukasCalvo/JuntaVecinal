package com.example.juntavecinal.ui.components

import androidx.compose.material3.SelectableDates

class SelectableDiasDesde(private val minimo: Long) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minimo
}
