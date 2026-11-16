package com.example.calculadorasalarial.empleados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// La base de datos. Lista las tablas (entities) y da acceso al DAO.
// Si en algún momento se agrega una columna, hay que subir 'version'.
@Database(entities = [Empleado::class], version = 1, exportSchema = false)
abstract class EmpleadoDatabase : RoomDatabase() {

    abstract fun empleadoDao(): EmpleadoDao

    companion object {
        // Una sola base para toda la app (singleton): abrirla varias veces sería lento y podría fallar.
        @Volatile
        private var instancia: EmpleadoDatabase? = null

        fun obtener(context: Context): EmpleadoDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    EmpleadoDatabase::class.java,
                    "empleados_db"
                ).build().also { instancia = it }
            }
    }
}
