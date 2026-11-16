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

// El adaptador es el puente entre la lista de empleados y el RecyclerView.
// El RecyclerView solo crea las filas que se ven en pantalla y las reutiliza al hacer scroll.
// ListAdapter usa DiffUtil: compara la lista vieja con la nueva y solo redibuja lo que cambió.
class EmpleadoAdapter : ListAdapter<Empleado, EmpleadoAdapter.EmpleadoViewHolder>(DiferenciasEmpleado) {

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
    }
}

// Cómo saber si dos empleados son el mismo (mismo id) y si cambió algún dato.
private object DiferenciasEmpleado : DiffUtil.ItemCallback<Empleado>() {
    override fun areItemsTheSame(anterior: Empleado, nuevo: Empleado): Boolean = anterior.id == nuevo.id

    override fun areContentsTheSame(anterior: Empleado, nuevo: Empleado): Boolean = anterior == nuevo
}
