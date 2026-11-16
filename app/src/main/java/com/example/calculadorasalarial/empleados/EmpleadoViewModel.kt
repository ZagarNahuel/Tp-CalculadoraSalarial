package com.example.calculadorasalarial.empleados

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// El ViewModel guarda los datos de la pantalla y sobrevive a los giros y recreaciones.
// Las pantallas (fragments) le piden cosas a él y nunca tocan la base de datos directamente.
class EmpleadoViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: EmpleadoRepository

    // La lista de empleados. Las pantallas la "observan" y se redibujan solas cuando cambia.
    val empleados: LiveData<List<Empleado>>

    init {
        val dao = EmpleadoDatabase.obtener(application).empleadoDao()
        repositorio = EmpleadoRepository(dao)
        empleados = repositorio.todos
    }

    // 'alTerminar' recibe true si se guardó y false si el DNI ya estaba cargado.
    // La base se usa en un hilo de fondo (IO) y la respuesta vuelve al hilo principal para tocar la pantalla.
    fun agregar(empleado: Empleado, alTerminar: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val guardado = repositorio.agregar(empleado)
            withContext(Dispatchers.Main) { alTerminar(guardado) }
        }
    }

    fun actualizar(empleado: Empleado, alTerminar: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val guardado = repositorio.actualizar(empleado)
            withContext(Dispatchers.Main) { alTerminar(guardado) }
        }
    }

    fun eliminar(empleado: Empleado) {
        viewModelScope.launch(Dispatchers.IO) { repositorio.eliminar(empleado) }
    }

    fun eliminarTodos() {
        viewModelScope.launch(Dispatchers.IO) { repositorio.eliminarTodos() }
    }
}
