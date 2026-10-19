package com.example.calculadorasalarial

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class CalculadoraActivity : AppCompatActivity() {

    companion object {
        private const val PREFS = "calculadora_pref"
        private const val PREF_CATEGORIA = "reg_categoria"     // lo guarda RegistroActivity
        private const val PREF_ANTIGUEDAD = "reg_antiguedad"   // lo guarda RegistroActivity
        private const val TAG_DOMINGO = "domingo"
        private const val ANIO_BASE = 2020   // las páginas de años empiezan en 2020, 2032, 2008...
        private const val INICIO_MIN = 2008
        private const val INICIO_MAX = 2092
    }

    // 0 = Trabajado (blanco), 1 = Falta justificada (rojo), 2 = Falta injustificada (negro)
    private val estadoDias = IntArray(32) { 0 }
    private var ultimoDiaMes = 31

    private lateinit var spMes: Spinner
    private lateinit var tvAnio: TextView
    private var anioSeleccionado = Calendar.getInstance().get(Calendar.YEAR)
    private lateinit var gridDiasQ1: GridLayout
    private lateinit var gridDiasQ2: GridLayout
    private lateinit var tvResumenQ1: TextView
    private lateinit var tvResumenQ2: TextView
    private lateinit var cbHabilitarQ1: CheckBox
    private lateinit var cbHabilitarQ2: CheckBox
    private lateinit var layoutContenedorQ1: LinearLayout
    private lateinit var layoutContenedorQ2: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Solo vertical: evita que al girar el celular se pierdan los días marcados
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_calculadora)
        ajustarBordesDelSistema()

        val pref = getSharedPreferences(PREFS, MODE_PRIVATE)

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            pref.edit().putBoolean("mantener_sesion", false).apply()
            irAlLogin()
        }

        // 1. Categoría y antigüedad: primero lo que llega del Registro, si no lo guardado al registrarse
        val categoria = intent.getStringExtra("CATEGORIA")?.takeIf { it.startsWith("CATEGORIA") }
            ?: pref.getString(PREF_CATEGORIA, null)?.takeIf { it.startsWith("CATEGORIA") }

        if (categoria == null) {
            // El registro es obligatorio: sin categoría no se puede calcular.
            // Se desactiva "mantener sesión" para que el login no vuelva a mandar acá en bucle.
            pref.edit().putBoolean("mantener_sesion", false).apply()
            Toast.makeText(
                this,
                "Primero regístrate y elige tu categoría para poder calcular.",
                Toast.LENGTH_LONG
            ).show()
            irAlLogin()
            return
        }

        val antiguedad = if (intent.hasExtra("ANTIGUEDAD")) {
            intent.getIntExtra("ANTIGUEDAD", 0)
        } else {
            pref.getInt(PREF_ANTIGUEDAD, 0)
        }

        findViewById<TextView>(R.id.tvDatosEmpleado).text =
            "Categoría: $categoria | Antigüedad: $antiguedad años"

        // 2. Vincular elementos del XML
        spMes = findViewById(R.id.spMes)
        tvAnio = findViewById(R.id.tvAnio)
        gridDiasQ1 = findViewById(R.id.gridDiasQ1)
        gridDiasQ2 = findViewById(R.id.gridDiasQ2)
        tvResumenQ1 = findViewById(R.id.tvResumenQ1)
        tvResumenQ2 = findViewById(R.id.tvResumenQ2)
        cbHabilitarQ1 = findViewById(R.id.cbHabilitarQ1)
        cbHabilitarQ2 = findViewById(R.id.cbHabilitarQ2)
        layoutContenedorQ1 = findViewById(R.id.layoutContenedorQ1)
        layoutContenedorQ2 = findViewById(R.id.layoutContenedorQ2)

        val etHorasExtraQ1 = findViewById<EditText>(R.id.etHorasExtraQ1)
        val etHorasExtraQ2 = findViewById<EditText>(R.id.etHorasExtraQ2)
        val cbApercibimientoQ1 = findViewById<CheckBox>(R.id.cbApercibimientoQ1)
        val cbApercibimientoQ2 = findViewById<CheckBox>(R.id.cbApercibimientoQ2)
        val btnCalcularRecibo = findViewById<Button>(R.id.btnCalcularRecibo)

        // 3. Spinner de meses con texto blanco (igual que el año)
        val meses = resources.getStringArray(R.array.lista_meses)
        val adaptador = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, meses) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val vista = super.getView(position, convertView, parent) as TextView
                vista.setTextColor(Color.WHITE)
                return vista
            }
        }
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spMes.adapter = adaptador

        // 4. Mes y año actuales por defecto
        val hoy = Calendar.getInstance()
        spMes.setSelection(hoy.get(Calendar.MONTH))
        anioSeleccionado = hoy.get(Calendar.YEAR)
        tvAnio.text = anioSeleccionado.toString()

        // 5. Al cambiar el mes o el año, el calendario se vuelve a armar
        spMes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                reconstruirCalendarios()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        tvAnio.setOnClickListener { mostrarSelectorAnio() }

        // 6. Habilitar / deshabilitar quincenas
        cbHabilitarQ1.setOnCheckedChangeListener { _, marcado ->
            cambiarEstadoSeccion(layoutContenedorQ1, marcado)
        }
        cbHabilitarQ2.setOnCheckedChangeListener { _, marcado ->
            cambiarEstadoSeccion(layoutContenedorQ2, marcado)
        }

        reconstruirCalendarios()

        // 7. Calcular recibo
        btnCalcularRecibo.setOnClickListener {
            if (!cbHabilitarQ1.isChecked && !cbHabilitarQ2.isChecked) {
                Toast.makeText(this, "Habilita al menos una quincena para calcular.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Una quincena deshabilitada se envía sin faltas, apercibimientos ni horas extra.
            // Así, si solo se calcula la 2°, se asume que en la 1° cobró todos los premios
            // y el premio perfecto depende únicamente de la 2° quincena.
            val dias = estadoDias.copyOf()
            if (!cbHabilitarQ1.isChecked) for (d in 1..15) dias[d] = 0
            if (!cbHabilitarQ2.isChecked) for (d in 16..31) dias[d] = 0

            val horasQ1 = if (cbHabilitarQ1.isChecked) etHorasExtraQ1.text.toString().toIntOrNull() ?: 0 else 0
            val horasQ2 = if (cbHabilitarQ2.isChecked) etHorasExtraQ2.text.toString().toIntOrNull() ?: 0 else 0
            val apercQ1 = cbHabilitarQ1.isChecked && cbApercibimientoQ1.isChecked
            val apercQ2 = cbHabilitarQ2.isChecked && cbApercibimientoQ2.isChecked

            val intentRecibo = Intent(this, ReciboActivity::class.java)
            intentRecibo.putExtra("CATEGORIA", categoria)
            intentRecibo.putExtra("ANTIGUEDAD", antiguedad)
            intentRecibo.putExtra("MES_NOMBRE", spMes.selectedItem.toString())
            intentRecibo.putExtra("MES_NUMERO", spMes.selectedItemPosition + 1) // 1 = Enero ... 12 = Diciembre
            intentRecibo.putExtra("ANIO", anioSeleccionado)
            intentRecibo.putExtra("ESTADO_DIAS", dias)
            intentRecibo.putExtra("HORAS_EXTRA_Q1", horasQ1)
            intentRecibo.putExtra("HORAS_EXTRA_Q2", horasQ2)
            intentRecibo.putExtra("APERCIBIMIENTO_Q1", apercQ1)
            intentRecibo.putExtra("APERCIBIMIENTO_Q2", apercQ2)
            intentRecibo.putExtra("MOSTRAR_Q1", cbHabilitarQ1.isChecked)
            intentRecibo.putExtra("MOSTRAR_Q2", cbHabilitarQ2.isChecked)
            startActivity(intentRecibo)
        }
    }

    // ------------------------------------------------------------------
    // Navegación
    // ------------------------------------------------------------------

    /**
     * Abre el login borrando todas las pantallas anteriores de la pila,
     * así el botón "Atrás" no vuelve a Registro ni a la Calculadora.
     */
    private fun irAlLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    // ------------------------------------------------------------------
    // Calendario
    // ------------------------------------------------------------------

    /** Muestra una grilla de 12 años con flechas para cambiar de página. */
    private fun mostrarSelectorAnio() {
        val vista = layoutInflater.inflate(R.layout.dialogo_selector_anio, null)
        val tvRango = vista.findViewById<TextView>(R.id.tvRangoAnios)
        val btnAnterior = vista.findViewById<Button>(R.id.btnAniosAnterior)
        val btnSiguiente = vista.findViewById<Button>(R.id.btnAniosSiguiente)
        val gridAnios = vista.findViewById<GridLayout>(R.id.gridAnios)
        val dialogo = MaterialAlertDialogBuilder(this).setView(vista).create()

        // Primer año de la página que contiene al año elegido
        var inicio = anioSeleccionado - (anioSeleccionado - ANIO_BASE).mod(12)
        inicio = inicio.coerceIn(INICIO_MIN, INICIO_MAX)

        fun dibujarPagina() {
            tvRango.text = "$inicio - ${inicio + 11}"
            btnAnterior.isEnabled = inicio > INICIO_MIN
            btnSiguiente.isEnabled = inicio < INICIO_MAX
            gridAnios.removeAllViews()

            for (anio in inicio..inicio + 11) {
                val celda = TextView(this)
                celda.text = anio.toString()
                celda.gravity = Gravity.CENTER
                celda.textSize = 15f
                celda.layoutParams = paramsCelda(dp(44))
                if (anio == anioSeleccionado) {
                    celda.setTextColor(Color.WHITE)
                    celda.background = GradientDrawable().apply {
                        cornerRadius = dp(22).toFloat()
                        setColor(color(R.color.calc_azul))
                    }
                } else {
                    celda.setTextColor(color(R.color.calc_negro))
                }
                celda.setOnClickListener {
                    anioSeleccionado = anio
                    tvAnio.text = anio.toString()
                    dialogo.dismiss()
                    reconstruirCalendarios()
                }
                gridAnios.addView(celda)
            }
        }

        btnAnterior.setOnClickListener {
            inicio -= 12
            dibujarPagina()
        }
        btnSiguiente.setOnClickListener {
            inicio += 12
            dibujarPagina()
        }

        dibujarPagina()
        dialogo.show()
    }

    /** Vuelve a armar las dos quincenas según el mes y el año elegidos. */
    private fun reconstruirCalendarios() {
        val anio = anioSeleccionado
        val mes = spMes.selectedItemPosition // 0 = Enero ... 11 = Diciembre
        if (mes < 0) return

        estadoDias.fill(0) // al cambiar de mes o año se reinician las marcas
        ultimoDiaMes = Calendar.getInstance().apply {
            clear()
            set(anio, mes, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        construirCalendario(gridDiasQ1, 1, 15, mes, anio)
        construirCalendario(gridDiasQ2, 16, ultimoDiaMes, mes, anio)

        cambiarEstadoSeccion(layoutContenedorQ1, cbHabilitarQ1.isChecked)
        cambiarEstadoSeccion(layoutContenedorQ2, cbHabilitarQ2.isChecked)
        actualizarResumenes()
    }

    /** Dibuja L M M J V S D y los días del rango, alineados con el día real de la semana. */
    private fun construirCalendario(grid: GridLayout, desde: Int, hasta: Int, mes: Int, anio: Int) {
        grid.removeAllViews()

        for (letra in listOf("L", "M", "M", "J", "V", "S", "D")) {
            val encabezado = TextView(this)
            encabezado.text = letra
            encabezado.gravity = Gravity.CENTER
            encabezado.textSize = 12f
            encabezado.setTypeface(null, Typeface.BOLD)
            encabezado.setTextColor(color(R.color.calc_azul))
            encabezado.layoutParams = paramsCelda(dp(22))
            grid.addView(encabezado)
        }

        // Casilleros vacíos hasta llegar al día de la semana en que empieza el rango
        repeat(columnaDeLaSemana(anio, mes, desde)) {
            val vacio = View(this)
            vacio.layoutParams = paramsCelda(dp(48))
            grid.addView(vacio)
        }

        for (dia in desde..hasta) {
            val esDomingo = columnaDeLaSemana(anio, mes, dia) == 6
            grid.addView(crearBotonDia(dia, esDomingo))
        }
    }

    private fun crearBotonDia(dia: Int, esDomingo: Boolean): MaterialButton {
        val boton = MaterialButton(this)
        boton.textSize = 12f
        boton.isAllCaps = false
        boton.includeFontPadding = false
        boton.setPadding(0, 0, 0, 0)
        boton.insetTop = 0
        boton.insetBottom = 0
        boton.minWidth = 0
        boton.minimumWidth = 0
        boton.minHeight = 0
        boton.minimumHeight = 0
        boton.cornerRadius = dp(8)
        boton.layoutParams = paramsCelda(dp(48))

        if (esDomingo) {
            // Los domingos no se pueden marcar: no cuentan como día laboral
            boton.text = dia.toString()
            boton.tag = TAG_DOMINGO
            boton.isEnabled = false
            boton.backgroundTintList = ColorStateList.valueOf(color(R.color.calc_domingo_fondo))
            boton.setTextColor(color(R.color.calc_domingo_texto))
        } else {
            pintarDia(boton, dia, 0)
            boton.setOnClickListener {
                // Rotar estado: 0 (blanco) -> 1 (rojo) -> 2 (negro) -> 0 (blanco)
                estadoDias[dia] = (estadoDias[dia] + 1) % 3
                pintarDia(boton, dia, estadoDias[dia])
                actualizarResumenes()
            }
        }
        return boton
    }

    /** Además del color, agrega una letra (J / I) para no depender solo del color. */
    private fun pintarDia(boton: MaterialButton, dia: Int, estado: Int) {
        when (estado) {
            1 -> {
                boton.text = "$dia\nJ"
                boton.backgroundTintList = ColorStateList.valueOf(color(R.color.calc_rojo))
                boton.setTextColor(Color.WHITE)
            }
            2 -> {
                boton.text = "$dia\nI"
                boton.backgroundTintList = ColorStateList.valueOf(color(R.color.calc_negro))
                boton.setTextColor(Color.WHITE)
            }
            else -> {
                boton.text = dia.toString()
                boton.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                boton.setTextColor(color(R.color.calc_azul))
            }
        }
    }

    private fun actualizarResumenes() {
        tvResumenQ1.text = resumen(1, 15)
        tvResumenQ2.text = resumen(16, ultimoDiaMes)
    }

    private fun resumen(desde: Int, hasta: Int): String {
        val justificadas = (desde..hasta).count { estadoDias[it] == 1 }
        val injustificadas = (desde..hasta).count { estadoDias[it] == 2 }
        return "Justificadas: $justificadas · Injustificadas: $injustificadas"
    }

    /** Lunes = 0 ... Domingo = 6 */
    private fun columnaDeLaSemana(anio: Int, mes: Int, dia: Int): Int {
        val cal = Calendar.getInstance().apply {
            clear()
            set(anio, mes, dia)
        }
        return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    private fun cambiarEstadoSeccion(contenedor: LinearLayout, habilitado: Boolean) {
        contenedor.alpha = if (habilitado) 1.0f else 0.35f
        for (vista in contenedor.children) {
            vista.isEnabled = habilitado
            if (vista is GridLayout) {
                for (celda in vista.children) {
                    // Los domingos siempre quedan deshabilitados
                    celda.isEnabled = habilitado && celda.tag != TAG_DOMINGO
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private fun paramsCelda(alto: Int): GridLayout.LayoutParams {
        val params = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED),
            GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f)
        )
        params.width = 0
        params.height = alto
        params.setMargins(dp(2), dp(2), dp(2), dp(2))
        return params
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    private fun color(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

    /** Evita que el encabezado y la barra inferior queden bajo la barra de estado o de navegación. */
    private fun ajustarBordesDelSistema() {
        val raiz = findViewById<View>(R.id.raizCalculadora)
        val encabezado = findViewById<View>(R.id.encabezadoCalc)
        val barraInferior = findViewById<View>(R.id.barraInferior)
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            encabezado.setPadding(dp(20), barras.top + dp(12), dp(8), dp(12))
            barraInferior.setPadding(dp(20), dp(12), dp(20), barras.bottom + dp(12))
            insets
        }
    }
}