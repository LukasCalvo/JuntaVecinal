package com.example.juntavecinal.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios", indices = [Index(value = ["rut"], unique = true)])
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rut: String,
    val nombre: String,
    val passwordHash: String,
    val salt: String
)
