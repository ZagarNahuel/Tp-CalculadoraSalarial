package com.example.calculadorasalarial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ExplicacionConceptoFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "explicacion_concepto"
        private const val ARG_TITULO = "titulo"
        private const val ARG_TEXTO = "texto"

        fun nuevo(titulo: String, texto: String): ExplicacionConceptoFragment =
            ExplicacionConceptoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITULO, titulo)
                    putString(ARG_TEXTO, texto)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vista = inflater.inflate(R.layout.fragment_explicacion_concepto, container, false)
        vista.findViewById<TextView>(R.id.tvTituloExplicacion).text = requireArguments().getString(ARG_TITULO)
        vista.findViewById<TextView>(R.id.tvTextoExplicacion).text = requireArguments().getString(ARG_TEXTO)
        vista.findViewById<Button>(R.id.btnCerrarExplicacion).setOnClickListener { dismiss() }
        return vista
    }
}
