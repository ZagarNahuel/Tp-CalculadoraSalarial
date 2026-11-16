package com.example.calculadorasalarial.empleados

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// El DAO dice qué se puede hacer con la tabla. Room escribe el código de cada función.
// Las funciones 'suspend' se llaman desde una corrutina, para no frenar la pantalla.
@Dao
interface EmpleadoDao {

    // Alta
    @Insert
    suspend fun insertar(empleado: Empleado)

    // Modificación: busca la fila por el id y reemplaza el resto de los datos.
    @Update
    suspend fun actualizar(empleado: Empleado)

    // Baja de un solo empleado.
    @Delete
    suspend fun eliminar(empleado: Empleado)

    // Baja de todos los empleados.
    @Query("DELETE FROM empleados")
    suspend fun eliminarTodos()

    // Listado. Con LiveData, la lista se vuelve a pedir sola cada vez que cambia la tabla.
    @Query("SELECT * FROM empleados ORDER BY apellido COLLATE NOCASE, nombre COLLATE NOCASE")
    fun obtenerTodos(): LiveData<List<Empleado>>

    // Cuántos empleados tienen ese DNI, sin contar al que tiene el id 'idExcluido'.
    // Sirve para no cargar dos veces el mismo DNI (al dar de alta se pasa 0 porque ningún id vale 0).
    @Query("SELECT COUNT(*) FROM empleados WHERE dni = :dni AND id != :idExcluido")
    suspend fun contarPorDni(dni: String, idExcluido: Int): Int
}
