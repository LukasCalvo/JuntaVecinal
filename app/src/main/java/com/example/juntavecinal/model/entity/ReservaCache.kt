package com.example.juntavecinal.model.entity

import androidx.room.Embedded
import androidx.room.Entity

@Entity(tableName = "reservas_gateway", primaryKeys = ["cuentaId", "id"])
data class ReservaCache(val cuentaId: Long, @Embedded val reserva: Reserva)
