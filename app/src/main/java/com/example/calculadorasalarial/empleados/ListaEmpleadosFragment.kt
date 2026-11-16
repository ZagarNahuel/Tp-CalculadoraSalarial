package com.example.calculadorasalarial.empleados

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calculadorasalarial.databinding.FragmentListaEmpleadosBinding

// Primera pantalla de Empleados: muestra todos los empleados guardados en la base.
class ListaEmpleadosFragment : Fragment() {

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

        adaptador = EmpleadoAdapter()
        binding.recyclerEmpleados.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEmpleados.adapter = adaptador

        // Observer: cada vez que cambia la tabla en Room, llega la lista nueva y se redibuja.
        viewModel.empleados.observe(viewLifecycleOwner) { lista ->
            adaptador.submitList(lista)
            binding.tvListaVacia.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
