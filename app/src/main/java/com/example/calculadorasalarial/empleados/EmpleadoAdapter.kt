package com.example.calculadorasalarial.empleados

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.calculadorasalarial.MotorLiquidacion
import com.example.calculadorasalarial.R
import com.example.calculadorasalarial.databinding.ItemEmpleadoBinding
import com.example.calculadorasalarial.textoAnios

// Cómo se entera la pantalla de lo que se toca en cada fila. El adaptador no sabe qué hacer
// con el toque: solo avisa, y el fragment que lo implementa decide (modificar, eliminar...).
interface OnEmpleadoClickListener {
    fun onEmpleadoClick(empleado: Empleado)

    fun onEliminarClick(empleado: Empleado)
}

// El adaptador es el puente entre la lista de empleados y el RecyclerView.
// El RecyclerView solo crea las filas que se ven en pantalla y las reutiliza al hacer scroll.
// ListAdapter usa DiffUtil: compara la lista vieja con la nueva y solo redibuja lo que cambió.
class EmpleadoAdapter(
    private val listener: OnEmpleadoClickListener
) : ListAdapter<Empleado, EmpleadoAdapter.EmpleadoViewHolder>(DiferenciasEmpleado) {

    // El ViewHolder guarda las vistas de una fila para no buscarlas cada vez.
    class EmpleadoViewHolder(val binding: ItemEmpleadoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpleadoViewHolder {
        val binding = ItemEmpleadoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmpleadoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmpleadoViewHolder, position: Int) {
        val empleado = getItem(position)
        val contexto = holder.binding.root.context

        holder.binding.tvNombreItem.text = empleado.nombreCompleto()
        holder.binding.tvDetalleItem.text = contexto.getString(
            R.string.emp_detalle,
            empleado.dni,
            MotorLiquidacion.nombreVisible(empleado.categoria),
            contexto.textoAnios(empleado.antiguedad)
        )

        holder.binding.root.setOnClickListener { listener.onEmpleadoClick(empleado) }
        holder.binding.btnEliminar.setOnClickListener { listener.onEliminarClick(empleado) }
    }
}

// Cómo saber si dos empleados son el mismo (mismo id) y si cambió algún dato.
private object DiferenciasEmpleado : DiffUtil.ItemCallback<Empleado>() {
    override fun areItemsTheSame(anterior: Empleado, nuevo: Empleado): Boolean = anterior.id == nuevo.id

    override fun areContentsTheSame(anterior: Empleado, nuevo: Empleado): Boolean = anterior == nuevo
}
