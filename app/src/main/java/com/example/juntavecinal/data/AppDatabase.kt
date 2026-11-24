package com.example.juntavecinal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.juntavecinal.model.dao.ReservaDao
import com.example.juntavecinal.model.entity.ReservaCache
import com.example.juntavecinal.model.entity.ReservaLocal
import com.example.juntavecinal.model.entity.Usuario

@Database(entities = [Usuario::class, ReservaLocal::class, ReservaCache::class], version = 12, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reservaDao(): ReservaDao

    companion object {
        private fun migracionReservasGateway(desde: Int) = object : Migration(desde, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE reservas_gateway (
                        cuentaId INTEGER NOT NULL, id INTEGER NOT NULL, usuarioId INTEGER NOT NULL,
                        nombre TEXT NOT NULL, fecha TEXT NOT NULL,
                        horaInicio TEXT NOT NULL, horaFin TEXT NOT NULL, asistentes INTEGER NOT NULL,
                        observaciones TEXT, requiereProyector INTEGER NOT NULL, aceptaReglamento INTEGER NOT NULL,
                        estado TEXT NOT NULL, PRIMARY KEY(cuentaId, id)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "junta_vecinal.db"
                )
                    .addMigrations(migracionReservasGateway(10), migracionReservasGateway(11))
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
