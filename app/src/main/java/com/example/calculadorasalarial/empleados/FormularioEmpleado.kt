package com.example.calculadorasalarial.empleados

import android.view.View
import android.widget.ArrayAdapter
import com.example.calculadorasalarial.MotorLiquidacion
import com.example.calculadorasalarial.R
import com.example.calculadorasalarial.databinding.FragmentFormularioEmpleadoBinding

// Maneja el formulario que comparten el alta y la modificación: llena la lista de categorías,
// carga los datos de un empleado, y valida lo escrito antes de armar el Empleado.
class FormularioEmpleado(private val binding: FragmentFormularioEmpleadoBinding) {

    private val contexto = binding.root.context

    init {
        // La primera opción es un aviso ("Seleccionar categoría"); las demás son las categorías reales.
        val opciones = listOf(contexto.getString(R.string.emp_categoria_elegir)) +
            MotorLiquidacion.categorias.map { MotorLiquidacion.nombreVisible(it) }
        val adaptador = ArrayAdapter(contexto, android.R.layout.simple_spinner_item, opciones)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spCategoria.adapter = adaptador
    }

    // Completa los campos con los datos de un empleado (para modificarlo).
    fun cargar(empleado: Empleado) {
        binding.etNombre.setText(empleado.nombre)
        binding.etApellido.setText(empleado.apellido)
        binding.etDni.setText(empleado.dni)
        binding.etAntiguedad.setText(empleado.antiguedad.toString())
        // +1 porque la posición 0 es el aviso. Si no se encuentra, indexOf da -1 y queda en el aviso.
        binding.spCategoria.setSelection(MotorLiquidacion.categorias.indexOf(empleado.categoria) + 1)
    }

    // Lee los campos. Si algo está mal, marca el error en pantalla y devuelve null.
    // 'id' es el del empleado que se modifica; en un alta queda en 0 y Room le pone uno nuevo.
    fun leer(id: Int = 0): Empleado? {
        val nombre = binding.etNombre.text?.toString().orEmpty().trim()
        val apellido = binding.etApellido.text?.toString().orEmpty().trim()
        val dni = binding.etDni.text?.toString().orEmpty().trim()
        val antiguedad = binding.etAntiguedad.text?.toString().orEmpty().trim().toIntOrNull()
        val posicionCategoria = binding.spCategoria.selectedItemPosition

        val nombreOk = nombre.length >= 2
        val apellidoOk = apellido.length >= 2
        val dniOk = dni.length in 7..8 && dni.all { it.isDigit() }
        val categoriaOk = posicionCategoria > 0
        val antiguedadOk = antiguedad != null && antiguedad in 0..60

        binding.tilNombre.error = if (nombreOk) null else contexto.getString(R.string.emp_error_nombre)
        binding.tilApellido.error = if (apellidoOk) null else contexto.getString(R.string.emp_error_apellido)
        binding.tilDni.error = if (dniOk) null else contexto.getString(R.string.emp_error_dni)
        binding.tvErrorCategoria.visibility = if (categoriaOk) View.GONE else View.VISIBLE
        binding.tilAntiguedad.error = if (antiguedadOk) null else contexto.getString(R.string.emp_error_antiguedad)

        // Si algún dato está mal no se arma el empleado (los errores ya quedaron marcados arriba).
        val anios = antiguedad ?: return null
        if (!(nombreOk && apellidoOk && dniOk && categoriaOk && antiguedadOk)) return null

        return Empleado(
            id = id,
            nombre = nombre,
            apellido = apellido,
            dni = dni,
            categoria = MotorLiquidacion.categorias[posicionCategoria - 1],
            antiguedad = anios
        )
    }

    // Lo llama el alta o la modificación cuando el repositorio avisa que ese DNI ya existe.
    fun mostrarDniRepetido() {
        binding.tilDni.error = contexto.getString(R.string.emp_error_dni_repetido)
    }
}
