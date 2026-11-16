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

// Pantalla de alta: formulario vacío para cargar un empleado nuevo.
class AltaEmpleadoFragment : Fragment() {

    private var _binding: FragmentFormularioEmpleadoBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EmpleadoViewModel
    private lateinit var formulario: FormularioEmpleado

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
        formulario = FormularioEmpleado(binding)
        binding.btnGuardar.setText(R.string.emp_guardar)

        binding.btnGuardar.setOnClickListener {
            // Si hay datos mal escritos, el formulario ya marcó el error y no se guarda nada.
            val empleado = formulario.leer() ?: return@setOnClickListener

            binding.btnGuardar.isEnabled = false
            viewModel.agregar(empleado) { guardado ->
                // Si el usuario ya salió de la pantalla, no hay nada que actualizar.
                if (_binding == null) return@agregar

                if (guardado) {
                    Toast.makeText(requireContext(), R.string.emp_alta_ok, Toast.LENGTH_SHORT).show()
                    // Vuelve a la lista, que se actualiza sola porque observa la base.
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
