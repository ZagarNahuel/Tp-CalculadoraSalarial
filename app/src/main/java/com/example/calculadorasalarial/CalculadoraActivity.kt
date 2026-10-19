package com.example.calculadorasalarial

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

/**
 * Pantalla principal (Home). Los datos del empleado (categoría y antigüedad) salen del
 * registro guardado en SharedPreferences; acá solo se marcan los días del mes.
 *
 *  - Tocar un día:            blanco -> justificada (J) -> injustificada (I) -> blanco
 *  - Mantener presionado:     feriado trabajado (F, se paga x2). Mantener de nuevo lo saca.
 */
class CalculadoraActivity : AppCompatActivity() {

    companion object {
        private const val TAG_DOMINGO = "domingo"
        private const val ANIO_BASE = 2020   // las páginas de años empiezan en 2020, 2032, 2008...
        private const val INICIO_MIN = 2008
        private const val INICIO_MAX = 2092

        // Claves para conservar el calendario si Android recrea la pantalla
        private const val GUARDADO_ESTADO = "estado_dias"
        private const val GUARDADO_MES = "mes"
        private const val GUARDADO_ANIO = "anio"
    }

    private val repositorio by lazy { RepositorioUsuarios(this) }

    // 0 = Trabajado, 1 = Justificada, 2 = Injustificada, 3 = Feriado (ver MotorLiquidacion.ESTADO_*)
    private val estadoDias = IntArray(32)
    private var ultimoDiaMes = 31

    // Mes (1..12) y año del calendario que se está mostrando: si cambian, se reinician las marcas
    private var mesMostrado = -1
    private var anioMostrado = -1

    private lateinit var tvDatosEmpleado: TextView
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
    private lateinit var etHorasExtraQ1: EditText
    private lateinit var etHorasExtraQ2: EditText
    private lateinit var cbApercibimientoQ1: CheckBox
    private lateinit var cbApercibimientoQ2: CheckBox
    private lateinit var tvUltimoNeto: TextView

    // El Recibo nos devuelve el neto calculado o pide empezar una consulta nueva.
    private val lanzadorRecibo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            val datos = resultado.data
            if (resultado.resultCode != RESULT_OK || datos == null) return@registerForActivityResult

            if (datos.getStringExtra(ReciboActivity.EXTRA_ACCION_RECIBO) == ReciboActivity.ACCION_NUEVA_CONSULTA) {
                reiniciarFormulario()
            } else if (datos.hasExtra(ReciboActivity.EXTRA_RESULTADO_NETO)) {
                mostrarUltimoNeto(datos.getDoubleExtra(ReciboActivity.EXTRA_RESULTADO_NETO, 0.0))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sin una cuenta iniciada no hay categoría con la que calcular: se vuelve al login.
        if (repositorio.usuarioActual() == null) {
            irAlLogin()
            return
        }

        // La orientación vertical se fija en el AndroidManifest (screenOrientation="portrait").
        setContentView(R.layout.activity_calculadora)
        ajustarBordesDelSistema()

        // 1. Vincular elementos del XML
        tvDatosEmpleado = findViewById(R.id.tvDatosEmpleado)
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
        etHorasExtraQ1 = findViewById(R.id.etHorasExtraQ1)
        etHorasExtraQ2 = findViewById(R.id.etHorasExtraQ2)
        cbApercibimientoQ1 = findViewById(R.id.cbApercibimientoQ1)
        cbApercibimientoQ2 = findViewById(R.id.cbApercibimientoQ2)
        tvUltimoNeto = findViewById(R.id.tvUltimoNeto)
        val btnCalcularRecibo = findViewById<Button>(R.id.btnCalcularRecibo)

        mostrarDatosEmpleado()

        // "Atrás" (el botón del encabezado y el del sistema) vuelve al login y cierra la sesión
        findViewById<Button>(R.id.btnAtras).setOnClickListener { volverAlLogin() }
        onBackPressedDispatcher.addCallback(this) { volverAlLogin() }

        // 2. Spinner de meses con texto blanco (igual que el año)
        spMes.adapter = adaptadorSpinnerBlanco(resources.getStringArray(R.array.lista_meses))

        // 3. Mes y año: los de hoy, o los que había antes de que Android recreara la pantalla
        if (savedInstanceState == null) {
            val hoy = Calendar.getInstance()
            spMes.setSelection(hoy.get(Calendar.MONTH))
            anioSeleccionado = hoy.get(Calendar.YEAR)
        } else {
            val guardado = savedInstanceState.getIntArray(GUARDADO_ESTADO)
            if (guardado != null) {
                guardado.copyInto(estadoDias, 0, 0, minOf(guardado.size, estadoDias.size))
            }
            val mesGuardado = savedInstanceState.getInt(GUARDADO_MES, 0)
            anioSeleccionado = savedInstanceState.getInt(GUARDADO_ANIO, anioSeleccionado)
            spMes.setSelection(mesGuardado)
            mesMostrado = mesGuardado + 1
            anioMostrado = anioSeleccionado
        }
        tvAnio.text = anioSeleccionado.toString()

        // 4. Al cambiar el mes o el año, el calendario se vuelve a armar
        spMes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                reconstruirCalendarios()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        tvAnio.setOnClickListener { mostrarSelectorAnio() }

        // 5. Habilitar / deshabilitar quincenas
        cbHabilitarQ1.setOnCheckedChangeListener { _, marcado ->
            cambiarEstadoSeccion(layoutContenedorQ1, marcado)
        }
        cbHabilitarQ2.setOnCheckedChangeListener { _, marcado ->
            cambiarEstadoSeccion(layoutContenedorQ2, marcado)
        }

        reconstruirCalendarios()

        // 6. Calcular recibo
        btnCalcularRecibo.setOnClickListener { calcularRecibo() }
    }

    override fun onResume() {
        super.onResume()
        // Si en el Recibo se cambió la categoría, el encabezado tiene que reflejarlo.
        if (::tvDatosEmpleado.isInitialized) mostrarDatosEmpleado()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::spMes.isInitialized) {
            outState.putIntArray(GUARDADO_ESTADO, estadoDias)
            outState.putInt(GUARDADO_MES, spMes.selectedItemPosition)
            outState.putInt(GUARDADO_ANIO, anioSeleccionado)
        }
    }

    // ------------------------------------------------------------------
    // Datos y recibo
    // ------------------------------------------------------------------

    private fun mostrarDatosEmpleado() {
        val usuario = repositorio.usuarioActual() ?: return
        tvDatosEmpleado.text = getString(
            R.string.calc_datos_empleado, usuario.nombre, usuario.categoria, usuario.antiguedad
        )
    }

    private fun calcularRecibo() {
        val usuario = repositorio.usuarioActual()
        if (usuario == null) {
            irAlLogin()
            return
        }
        if (!cbHabilitarQ1.isChecked && !cbHabilitarQ2.isChecked) {
            Toast.makeText(this, R.string.calc_error_sin_quincena, Toast.LENGTH_SHORT).show()
            return
        }

        // Una quincena deshabilitada se envía sin faltas, apercibimientos ni horas extra.
        // Así, si solo se calcula la 2°, se asume que en la 1° cobró todos los premios
        // y el premio perfecto depende únicamente de la 2° quincena.
        val dias = estadoDias.copyOf()
        if (!cbHabilitarQ1.isChecked) for (d in 1..15) dias[d] = MotorLiquidacion.ESTADO_TRABAJADO
        if (!cbHabilitarQ2.isChecked) for (d in 16..31) dias[d] = MotorLiquidacion.ESTADO_TRABAJADO

        val horasQ1 = if (cbHabilitarQ1.isChecked) etHorasExtraQ1.text.toString().toIntOrNull() ?: 0 else 0
        val horasQ2 = if (cbHabilitarQ2.isChecked) etHorasExtraQ2.text.toString().toIntOrNull() ?: 0 else 0
        val apercQ1 = cbHabilitarQ1.isChecked && cbApercibimientoQ1.isChecked
        val apercQ2 = cbHabilitarQ2.isChecked && cbApercibimientoQ2.isChecked

        val intentRecibo = Intent(this, ReciboActivity::class.java)
        intentRecibo.putExtra("CATEGORIA", usuario.categoria)
        intentRecibo.putExtra("ANTIGUEDAD", usuario.antiguedad)
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
        lanzadorRecibo.launch(intentRecibo)
    }

    /** "Nueva consulta" desde el Recibo: deja el formulario como recién abierto. */
    private fun reiniciarFormulario() {
        estadoDias.fill(0)
        etHorasExtraQ1.text.clear()
        etHorasExtraQ2.text.clear()
        cbApercibimientoQ1.isChecked = false
        cbApercibimientoQ2.isChecked = false
        cbHabilitarQ1.isChecked = true
        cbHabilitarQ2.isChecked = false
        tvUltimoNeto.visibility = View.GONE
        reconstruirCalendarios()
    }

    private fun mostrarUltimoNeto(neto: Double) {
        tvUltimoNeto.text = getString(R.string.calc_ultimo_neto, Formato.dinero(neto))
        tvUltimoNeto.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------------
    // Navegación
    // ------------------------------------------------------------------

    /** Vuelve al login cerrando la sesión: así "Mantener sesión" no salta el login al volver a entrar. */
    private fun volverAlLogin() {
        repositorio.cerrarSesion()
        irAlLogin()
    }

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
        val mes = spMes.selectedItemPosition + 1 // 1 = Enero ... 12 = Diciembre
        if (mes < 1) return

        // Las marcas solo se reinician si realmente cambió el mes o el año
        if (mes != mesMostrado || anio != anioMostrado) {
            estadoDias.fill(0)
            mesMostrado = mes
            anioMostrado = anio
        }

        ultimoDiaMes = Fechas.ultimoDiaDelMes(anio, mes)

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
        repeat(Fechas.columnaDeLaSemana(anio, mes, desde)) {
            val vacio = View(this)
            vacio.layoutParams = paramsCelda(dp(48))
            grid.addView(vacio)
        }

        for (dia in desde..hasta) {
            grid.addView(crearBotonDia(dia, Fechas.esDomingo(anio, mes, dia)))
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
            pintarDia(boton, dia, estadoDias[dia])

            boton.setOnClickListener {
                // Rotar estado: blanco -> justificada (rojo) -> injustificada (negro) -> blanco.
                // Si el día estaba como feriado, un toque lo vuelve a blanco.
                estadoDias[dia] = if (estadoDias[dia] == MotorLiquidacion.ESTADO_FERIADO) {
                    MotorLiquidacion.ESTADO_TRABAJADO
                } else {
                    (estadoDias[dia] + 1) % 3
                }
                pintarDia(boton, dia, estadoDias[dia])
                actualizarResumenes()
            }

            boton.setOnLongClickListener {
                // Mantener presionado: marca / desmarca el día como feriado trabajado (pago x2)
                estadoDias[dia] = if (estadoDias[dia] == MotorLiquidacion.ESTADO_FERIADO) {
                    MotorLiquidacion.ESTADO_TRABAJADO
                } else {
                    MotorLiquidacion.ESTADO_FERIADO
                }
                pintarDia(boton, dia, estadoDias[dia])
                actualizarResumenes()
                true
            }
        }
        return boton
    }

    /** Además del color, agrega una letra (J / I / F) para no depender solo del color. */
    private fun pintarDia(boton: MaterialButton, dia: Int, estado: Int) {
        when (estado) {
            MotorLiquidacion.ESTADO_JUSTIFICADA -> {
                boton.text = "$dia\nJ"
                boton.backgroundTintList = ColorStateList.valueOf(color(R.color.calc_rojo))
                boton.setTextColor(Color.WHITE)
            }
            MotorLiquidacion.ESTADO_INJUSTIFICADA -> {
                boton.text = "$dia\nI"
                boton.backgroundTintList = ColorStateList.valueOf(color(R.color.calc_negro))
                boton.setTextColor(Color.WHITE)
            }
            MotorLiquidacion.ESTADO_FERIADO -> {
                boton.text = "$dia\nF"
                boton.backgroundTintList = ColorStateList.valueOf(color(R.color.calc_feriado))
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
        val justificadas = (desde..hasta).count { estadoDias[it] == MotorLiquidacion.ESTADO_JUSTIFICADA }
        val injustificadas = (desde..hasta).count { estadoDias[it] == MotorLiquidacion.ESTADO_INJUSTIFICADA }
        val feriados = (desde..hasta).count { estadoDias[it] == MotorLiquidacion.ESTADO_FERIADO }
        return getString(R.string.calc_resumen_quincena, justificadas, injustificadas, feriados)
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

    /**
     * Evita que el encabezado y la barra inferior queden bajo la barra de estado, la de
     * navegación o el teclado (la pantalla se dibuja de borde a borde).
     */
    private fun ajustarBordesDelSistema() {
        val raiz = findViewById<View>(R.id.raizCalculadora)
        val encabezado = findViewById<View>(R.id.encabezadoCalc)
        val barraInferior = findViewById<View>(R.id.barraInferior)
        WindowCompat.getInsetsController(window, raiz).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            encabezado.setPadding(dp(20) + barras.left, barras.top + dp(12), dp(8) + barras.right, dp(12))
            barraInferior.setPadding(
                dp(20) + barras.left, dp(12), dp(20) + barras.right,
                dp(12) + maxOf(barras.bottom, teclado.bottom)
            )
            insets
        }
    }
}
