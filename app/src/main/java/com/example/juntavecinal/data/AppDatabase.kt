package com.example.juntavecinal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.juntavecinal.model.dao.ReservaDao
import com.example.juntavecinal.model.dao.UsuarioDao
import com.example.juntavecinal.model.entity.Usuario
import com.example.juntavecinal.model.entity.Reserva
    /*
    Si se modifica algun dato relacionado con la base de datos (cualquier model)
    es necesario cambiar la version a una superior para que reinicie las base de datos
     */
@Database(entities = [Usuario::class, Reserva::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun reservaDao(): ReservaDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "junta_vecinal.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}