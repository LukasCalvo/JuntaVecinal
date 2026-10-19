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
    val dependencia: Dependencia,
    val fecha: Long,                     // Date (millis del DatePicker)
    val horaInicio: Int,                 // Int (hora 0-23)
    val horaFin: Int,                    // Int (hora 0-23)
    val asistentes: Int,                 // Int
    val observaciones: String = "",
    val requiereMontaje: Boolean = false,
    val aceptaReglamento: Boolean = false,
    val fotoPath: String? = null,        // foto de evidencia (cámara/galería)
    val estado: EstadoReserva = EstadoReserva.PENDIENTE,
    val createdAt: Long = System.currentTimeMillis()
)