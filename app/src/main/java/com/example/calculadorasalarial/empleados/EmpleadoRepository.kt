package com.example.calculadorasalarial.empleados

import androidx.lifecycle.LiveData

// El repositorio es el único que habla con el DAO. El ViewModel le pide cosas a él.
// Acá también viven las reglas de los datos, por ejemplo que no haya dos empleados con el mismo DNI.
class EmpleadoRepository(private val dao: EmpleadoDao) {

    val todos: LiveData<List<Empleado>> = dao.obtenerTodos()

    // Devuelve false si ya existe otro empleado con ese DNI (en ese caso no guarda nada).
    suspend fun agregar(empleado: Empleado): Boolean {
        if (dao.contarPorDni(empleado.dni, 0) > 0) return false
        dao.insertar(empleado)
        return true
    }

    // Igual que agregar, pero ignora al propio empleado al buscar un DNI repetido.
    suspend fun actualizar(empleado: Empleado): Boolean {
        if (dao.contarPorDni(empleado.dni, empleado.id) > 0) return false
        dao.actualizar(empleado)
        return true
    }

    suspend fun eliminar(empleado: Empleado) = dao.eliminar(empleado)

    suspend fun eliminarTodos() = dao.eliminarTodos()
}
