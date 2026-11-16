package com.example.calculadorasalarial.empleados

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calculadorasalarial.CalculadoraActivity
import com.example.calculadorasalarial.R
import com.example.calculadorasalarial.databinding.FragmentListaEmpleadosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Primera pantalla de Empleados: muestra todos los empleados guardados en la base.
// Implementa OnEmpleadoClickListener para enterarse de lo que se toca en cada fila.
class ListaEmpleadosFragment : Fragment(), OnEmpleadoClickListener {

    // El binding solo vale mientras existe la vista del fragment, por eso se vacía en onDestroyView.
    private var _binding: FragmentListaEmpleadosBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EmpleadoViewModel
    private lateinit var adaptador: EmpleadoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaEmpleadosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // El ViewModel es de la Activity, así todos los fragments comparten el mismo.
        viewModel = ViewModelProvider(requireActivity())[EmpleadoViewModel::class.java]

        adaptador = EmpleadoAdapter(this)
        binding.recyclerEmpleados.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEmpleados.adapter = adaptador

        // Observer: cada vez que cambia la tabla en Room, llega la lista nueva y se redibuja.
        viewModel.empleados.observe(viewLifecycleOwner) { lista ->
            adaptador.submitList(lista)
            binding.tvListaVacia.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }

        // Botón Agregar: navega a la pantalla de alta.
        binding.fabAgregar.setOnClickListener {
            NavHostFragment.findNavController(this).navigate(R.id.action_lista_to_alta)
        }
    }

    // Tocar una fila: abre la modificación y le pasa los datos del empleado en un Bundle.
    override fun onEmpleadoClick(empleado: Empleado) {
        NavHostFragment.findNavController(this)
            .navigate(R.id.action_lista_to_modificar, ArgumentosEmpleado.aBundle(empleado))
    }

    // Tocar el tacho: pide confirmación antes de borrar.
    override fun onEliminarClick(empleado: Empleado) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.emp_eliminar_titulo)
            .setMessage(getString(R.string.emp_eliminar_mensaje, empleado.nombreCompleto()))
            .setNegativeButton(R.string.emp_cancelar, null)
            .setPositiveButton(R.string.emp_eliminar) { _, _ ->
                viewModel.eliminar(empleado)
                Toast.makeText(requireContext(), R.string.emp_eliminado, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // Tocar "Liquidar sueldo": abre la calculadora y le pasa los datos del empleado en el Intent.
    override fun onLiquidarClick(empleado: Empleado) {
        val intent = Intent(requireContext(), CalculadoraActivity::class.java)
            .putExtra(CalculadoraActivity.EXTRA_EMPLEADO_NOMBRE, empleado.nombreCompleto())
            .putExtra(CalculadoraActivity.EXTRA_EMPLEADO_CATEGORIA, empleado.categoria)
            .putExtra(CalculadoraActivity.EXTRA_EMPLEADO_ANTIGUEDAD, empleado.antiguedad)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
