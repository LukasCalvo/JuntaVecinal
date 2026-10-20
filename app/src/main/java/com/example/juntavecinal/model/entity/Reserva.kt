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
    val nombre: String,
    val fecha: Long,
    val horaInicioMin: Int,
    val horaFinMin: Int,
    val asistentes: Int,
    val observaciones: String = "",
    val requiereProyector: Boolean = false,
    val aceptaReglamento: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)