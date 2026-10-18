package com.example.juntavecinal.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.juntavecinal.model.entity.Usuario

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(usuario: Usuario): Long

    @Query("SELECT * FROM usuarios WHERE rut = :rut LIMIT 1")
    suspend fun buscarPorRut(rut: String): Usuario?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int
}