package com.example.juntavecinal.model.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.juntavecinal.model.entity.ReservaCache
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservaDao {
    @Query("SELECT * FROM reservas_gateway WHERE cuentaId = :cuentaId ORDER BY fecha DESC, horaInicio ASC, id DESC")
    fun observar(cuentaId: Long): Flow<List<ReservaCache>>

    @Query("DELETE FROM reservas_gateway WHERE cuentaId = :cuentaId")
    suspend fun borrarCache(cuentaId: Long)

    @Upsert
    suspend fun guardar(reservas: List<ReservaCache>)

    @Query("UPDATE reservas_gateway SET estado = 'CANCELADA' WHERE cuentaId = :cuentaId AND id = :id")
    suspend fun marcarCancelada(cuentaId: Long, id: Long)

    @Transaction
    suspend fun reemplazar(cuentaId: Long, reservas: List<ReservaCache>) {
        borrarCache(cuentaId)
        guardar(reservas)
    }
}
