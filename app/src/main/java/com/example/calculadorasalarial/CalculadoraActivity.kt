package com.example.calculadorasalarial

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import com.example.calculadorasalarial.ReciboActivity
import java.util.Calendar

class CalculadoraActivity : AppCompatActivity() {

    // 0 = Blanco (Trabajado), 1 = Rojo (Falta Justificada), 2 = Negro (Falta Injustificada)
    private val estadoDias = IntArray(32) { 0 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculadora)

        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        btnCerrarSesion.setOnClickListener {
            val pref = getSharedPreferences("calculadora_pref", MODE_PRIVATE)
            pref.edit().putBoolean("mantener_sesion", false).apply()

            val intent = Intent(this, MainActivity::class.java)

            startActivity(intent)
            finish() //cerramos la calculadora
        }

        // 1. Recibir Categoría y Antigüedad desde el Registro
        val categoriaRecibida = intent.getStringExtra("CATEGORIA") ?: "CATEGORIA INICIAL"
        val antiguedadRecibida = intent.getIntExtra("ANTIGUEDAD", 0)

        // Si el usuario eligió "Seleccionar Categoría" sin querer, usamos CATEGORIA INICIAL por defecto
        val categoriaValida = if (categoriaRecibida == "Seleccionar Categoría") {
            "CATEGORIA INICIAL"
        } else {
            categoriaRecibida
        }

        val tvDatosEmpleado = findViewById<TextView>(R.id.tvDatosEmpleado)
        tvDatosEmpleado.text = "Categoría: $categoriaValida | Antigüedad: $antiguedadRecibida años"

        // 2. Vincular elementos del XML
        val spMes = findViewById<Spinner>(R.id.spMes)
        val etAnio = findViewById<EditText>(R.id.etAnio)
        val gridDiasQ1 = findViewById<GridLayout>(R.id.gridDiasQ1)
        val gridDiasQ2 = findViewById<GridLayout>(R.id.gridDiasQ2)

        val cbHabilitarQ1 = findViewById<CheckBox>(R.id.cbHabilitarQ1)
        val layoutContenedorQ1 = findViewById<LinearLayout>(R.id.layoutContenedorQ1)
        val cbHabilitarQ2 = findViewById<CheckBox>(R.id.cbHabilitarQ2)
        val layoutContenedorQ2 = findViewById<LinearLayout>(R.id.layoutContenedorQ2)

        val etHorasExtraQ1 = findViewById<EditText>(R.id.etHorasExtraQ1)
        val etHorasExtraQ2 = findViewById<EditText>(R.id.etHorasExtraQ2)
        val cbApercibimientoQ1 = findViewById<CheckBox>(R.id.cbApercibimientoQ1)
        val cbApercibimientoQ2 = findViewById<CheckBox>(R.id.cbApercibimientoQ2)
        val btnCalcularRecibo = findViewById<Button>(R.id.btnCalcularRecibo)

        // 3. Configurar los toques de los botones de días (Blanco -> Rojo -> Negro)
        configurarToquesGrilla(gridDiasQ1)
        configurarToquesGrilla(gridDiasQ2)

        fun actualizarDiasDelMes() {
            val mesIndex = spMes.selectedItemPosition
            val anio = etAnio.text.toString().toIntOrNull() ?: 2026

            val calendario = Calendar.getInstance()
            calendario.set(anio, mesIndex, 1)
            val maxDias = calendario.getActualMaximum(Calendar.DAY_OF_MONTH)

            // Mostrar u ocultar los días 29, 30 y 31 según el mes elegido
            for (vista in gridDiasQ2.children) {
                if (vista is Button) {
                    val dia = vista.text.toString().toInt()
                    vista.visibility = if (dia <= maxDias) View.VISIBLE else View.GONE
                }
            }
        }

        spMes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                actualizarDiasDelMes()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etAnio.doAfterTextChanged {
            if (it.toString().length == 4) actualizarDiasDelMes()
        }

        // 4. Habilitar / Deshabilitar quincenas en modo "Hint"
        cbHabilitarQ1.setOnCheckedChangeListener { _, isChecked ->
            cambiarEstadoSeccion(layoutContenedorQ1, isChecked)
        }

        cbHabilitarQ2.setOnCheckedChangeListener { _, isChecked ->
            cambiarEstadoSeccion(layoutContenedorQ2, isChecked)
        }

        cambiarEstadoSeccion(layoutContenedorQ2, false)

        // 5. Botón Calcular Recibo -> Envía todo a ReciboActivity
        btnCalcularRecibo.setOnClickListener {
            val intentRecibo = Intent(this, ReciboActivity::class.java)
            intentRecibo.putExtra("CATEGORIA", categoriaValida)
            intentRecibo.putExtra("ANTIGUEDAD", antiguedadRecibida)
            intentRecibo.putExtra("MES_NOMBRE", spMes.selectedItem.toString())
            intentRecibo.putExtra("MES_NUMERO", spMes.selectedItemPosition + 1) // 1 = Enero ... 12 = Diciembre
            intentRecibo.putExtra("ANIO", etAnio.text.toString().toIntOrNull() ?: 2026)
            intentRecibo.putExtra("ESTADO_DIAS", estadoDias)
            intentRecibo.putExtra("HORAS_EXTRA_Q1", etHorasExtraQ1.text.toString().toIntOrNull() ?: 0)
            intentRecibo.putExtra("HORAS_EXTRA_Q2", etHorasExtraQ2.text.toString().toIntOrNull() ?: 0)
            intentRecibo.putExtra("APERCIBIMIENTO_Q1", cbApercibimientoQ1.isChecked)
            intentRecibo.putExtra("APERCIBIMIENTO_Q2", cbApercibimientoQ2.isChecked)
            intentRecibo.putExtra("MOSTRAR_Q1", cbHabilitarQ1.isChecked)
            intentRecibo.putExtra("MOSTRAR_Q2", cbHabilitarQ2.isChecked)
            startActivity(intentRecibo)
        }
    }

    private fun configurarToquesGrilla(grid: GridLayout) {
        for (vista in grid.children) {
            if (vista is Button) {
                val dia = vista.text.toString().toInt()
                vista.setOnClickListener {
                    // Rotar estado: 0 (Blanco) -> 1 (Rojo) -> 2 (Negro) -> 0 (Blanco)
                    estadoDias[dia] = (estadoDias[dia] + 1) % 3

                    when (estadoDias[dia]) {
                        1 -> { // Falta Justificada (Rojo)
                            vista.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E53935"))
                            vista.setTextColor(Color.WHITE)
                        }
                        2 -> { // Falta Injustificada (Negro)
                            vista.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#212121"))
                            vista.setTextColor(Color.WHITE)
                        }
                        else -> { // Día Trabajado (Blanco)
                            vista.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                            vista.setTextColor(Color.parseColor("#005CE6"))
                        }
                    }
                }
            }
        }
    }

    private fun cambiarEstadoSeccion(contenedor: LinearLayout, habilitado: Boolean) {
        contenedor.alpha = if (habilitado) 1.0f else 0.35f
        for (vista in contenedor.children) {
            vista.isEnabled = habilitado
            if (vista is GridLayout) {
                for (boton in vista.children) {
                    boton.isEnabled = habilitado
                }
            }
        }
    }
}