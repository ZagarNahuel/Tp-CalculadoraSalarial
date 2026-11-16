package com.example.calculadorasalarial.empleados

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.example.calculadorasalarial.R
import com.example.calculadorasalarial.databinding.FragmentFormularioEmpleadoBinding

// Pantalla de modificación: el mismo formulario, pero cargado con los datos del empleado elegido.
class ModificarEmpleadoFragment : Fragment() {

    private var _binding: FragmentFormularioEmpleadoBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EmpleadoViewModel
    private lateinit var formulario: FormularioEmpleado

    // El empleado original, que llegó desde la lista dentro del Bundle.
    private lateinit var empleadoOriginal: Empleado

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormularioEmpleadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[EmpleadoViewModel::class.java]
        empleadoOriginal = ArgumentosEmpleado.desdeBundle(requireArguments())
        formulario = FormularioEmpleado(binding)
        binding.btnGuardar.setText(R.string.emp_guardar_cambios)

        // Solo se completan los campos la primera vez; si Android recrea la pantalla,
        // los campos recuerdan lo que el usuario había escrito.
        if (savedInstanceState == null) {
            formulario.cargar(empleadoOriginal)
        }

        binding.btnGuardar.setOnClickListener {
            // Se conserva el id original: así Room sabe cuál fila reemplazar.
            val empleado = formulario.leer(empleadoOriginal.id) ?: return@setOnClickListener

            binding.btnGuardar.isEnabled = false
            viewModel.actualizar(empleado) { guardado ->
                if (_binding == null) return@actualizar

                if (guardado) {
                    Toast.makeText(requireContext(), R.string.emp_modificacion_ok, Toast.LENGTH_SHORT).show()
                    NavHostFragment.findNavController(this).popBackStack()
                } else {
                    binding.btnGuardar.isEnabled = true
                    formulario.mostrarDniRepetido()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
