package com.example.juntavecinal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.juntavecinal.model.dao.UsuarioDao
import com.example.juntavecinal.model.entity.Usuario

@Database(entities = [Usuario::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "junta_vecinal.db"
                ).build().also { INSTANCE = it }
            }
    }
}