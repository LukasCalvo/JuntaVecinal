package com.example.juntavecinal.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reservas",
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("usuarioId")]
)
data class Reserva(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val usuarioId: Int,
    val nombre: String,                  // String
    val fecha: Long,                     // Date (milis a medianoche UTC)
    val horaInicioMin: Int,              // Int  minutos desde medianoche (14:30 = 870)
    val horaFinMin: Int,                 // Int  minutos desde medianoche (16:00 = 960)
    val asistentes: Int,                 // Int
    val observaciones: String = "",
    val requiereProyector: Boolean = false,
    val aceptaReglamento: Boolean = false,
    val fotoPath: String? = null,
    val estado: EstadoReserva = EstadoReserva.PENDIENTE,
    val createdAt: Long = System.currentTimeMillis()
)