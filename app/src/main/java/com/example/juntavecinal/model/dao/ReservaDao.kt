package com.example.juntavecinal.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.juntavecinal.model.entity.Reserva
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservaDao {
    @Query("SELECT * FROM reservas ORDER BY fecha DESC, horaInicio ASC")
    fun observeAll(): Flow<List<Reserva>>

    @Query("SELECT * FROM reservas WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    fun observeByUser(usuarioId: Int): Flow<List<Reserva>>

    @Query("SELECT * FROM reservas WHERE dependencia = :dependencia AND fecha = :fecha")
    suspend fun buscarPorDia(dependencia: String, fecha: Long): List<Reserva>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reserva: Reserva)

    @Delete
    suspend fun delete(reserva: Reserva)

    @Query("UPDATE reservas SET estado = :estado WHERE id = :id")
    suspend fun updateEstado(id: Int, estado: String)
}