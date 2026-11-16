package com.example.calculadorasalarial.empleados

import androidx.room.Entity
import androidx.room.PrimaryKey

// Una fila de la tabla "empleados". Room arma la tabla a partir de esta clase.
// La categoría se guarda con el mismo texto que usa MotorLiquidacion (por ejemplo "CATEGORIA 2"),
// así se puede liquidar el sueldo del empleado sin convertir nada.
@Entity(tableName = "empleados")
data class Empleado(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val categoria: String,
    val antiguedad: Int
) {
    fun nombreCompleto(): String = "$nombre $apellido"
}
