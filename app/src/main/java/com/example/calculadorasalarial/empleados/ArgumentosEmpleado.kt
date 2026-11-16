package com.example.calculadorasalarial.empleados

import android.os.Bundle

// Un Bundle es una "mochila" de datos que viaja de una pantalla a otra.
// La lista arma la mochila con los datos del empleado y la pantalla de modificación la abre.
object ArgumentosEmpleado {
    private const val ID = "empleado_id"
    private const val NOMBRE = "empleado_nombre"
    private const val APELLIDO = "empleado_apellido"
    private const val DNI = "empleado_dni"
    private const val CATEGORIA = "empleado_categoria"
    private const val ANTIGUEDAD = "empleado_antiguedad"

    fun aBundle(empleado: Empleado): Bundle = Bundle().apply {
        putInt(ID, empleado.id)
        putString(NOMBRE, empleado.nombre)
        putString(APELLIDO, empleado.apellido)
        putString(DNI, empleado.dni)
        putString(CATEGORIA, empleado.categoria)
        putInt(ANTIGUEDAD, empleado.antiguedad)
    }

    fun desdeBundle(bundle: Bundle): Empleado = Empleado(
        id = bundle.getInt(ID),
        nombre = bundle.getString(NOMBRE).orEmpty(),
        apellido = bundle.getString(APELLIDO).orEmpty(),
        dni = bundle.getString(DNI).orEmpty(),
        categoria = bundle.getString(CATEGORIA).orEmpty(),
        antiguedad = bundle.getInt(ANTIGUEDAD)
    )
}
