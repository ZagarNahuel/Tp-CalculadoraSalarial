package com.example.calculadorasalarial

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.util.TypedValue
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * RECIBO — Pantalla de presentación de la liquidación.
 *
 * El motor de los importes vive en Liquidacion.kt (MotorLiquidacion), NO acá.
 * Acá recibimos los datos del formulario, mostramos Q1/Q2, guardamos el último
 * resultado si lo pide el usuario y preparamos el neto para devolverlo.
 *
 * Nota para el grupo: si se cambia una clave de Intent en una sola pantalla,
 * la otra no la va a encontrar. Hay que actualizar ambas.
 *
 * "Cambiar categoría": abre un selector, guarda la nueva categoría en SharedPreferences
 * (RepositorioUsuarios) y vuelve a liquidar el recibo en el momento.
 */
class ReciboActivity : AppCompatActivity() {

    companion object {
        // Contrato de SALIDA hacia CalculadoraActivity (mantener nombres y tipos).
        const val EXTRA_RESULTADO_NETO = "RESULTADO_NETO"   // Double
        const val EXTRA_ACCION_RECIBO = "ACCION_RECIBO"     // String
        const val ACCION_NUEVA_CONSULTA = "NUEVA_CONSULTA"

        private const val PREFS_RECIBO = "recibo_ultima_consulta_v1"
    }

    private val formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    // Datos de la consulta (llegan desde CalculadoraActivity por Intent)
    private var categoria = ""
    private var antiguedad = 0
    private var mes = -1
    private var anio = -1
    private var estadoOriginal: IntArray? = null
    private var mostrarQ1 = true
    private var mostrarQ2 = true
    private var horasQ1 = 0
    private var horasQ2 = 0
    private var apercibimientoQ1 = false
    private var apercibimientoQ2 = false
    private var estadoLiquidado = IntArray(32)

    private lateinit var tvEncabezado: TextView
    private lateinit var tvPistaConceptos: TextView
    private lateinit var seccionQ1: LinearLayout
    private lateinit var seccionQ2: LinearLayout
    private lateinit var tablaQ1: TableLayout
    private lateinit var tablaQ2: TableLayout
    private lateinit var detalleQ1: View
    private lateinit var detalleQ2: View
    private lateinit var tvNetoQ1: TextView
    private lateinit var tvNetoQ2: TextView
    private lateinit var tvTituloTotal: TextView
    private lateinit var tvNetoTotal: TextView
    private lateinit var tvDetalleTotal: TextView
    private lateinit var tvResumenRemunerativo: TextView
    private lateinit var tvResumenNoRemunerativo: TextView
    private lateinit var tvResumenDescuentos: TextView
    private lateinit var tvResumenNeto: TextView
    private lateinit var resumenCard: View
    private lateinit var controlesCard: View
    private lateinit var checkMostrarDetalle: CheckBox
    private lateinit var btnGuardar: Button
    private lateinit var btnVerGuardado: Button
    private lateinit var btnCompartir: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recibo)
        ajustarBordesDelSistema(findViewById(R.id.raizRecibo))

        tvEncabezado = findViewById(R.id.tvHeaderRecibo)
        val tvCambiarCategoria = findViewById<TextView>(R.id.tvCambiarCategoria)
        val btnNuevaConsulta = findViewById<Button>(R.id.btnNuevaConsulta)

        seccionQ1 = findViewById(R.id.seccionQuincenaA)
        seccionQ2 = findViewById(R.id.seccionQuincenaB)
        tablaQ1 = findViewById(R.id.tablaQuincenaA)
        tablaQ2 = findViewById(R.id.tablaQuincenaB)
        detalleQ1 = findViewById(R.id.detalleTablaQ1)
        detalleQ2 = findViewById(R.id.detalleTablaQ2)
        tvNetoQ1 = findViewById(R.id.tvNetoQ1)
        tvNetoQ2 = findViewById(R.id.tvNetoQ2)
        tvTituloTotal = findViewById(R.id.tvTituloTotal)
        tvNetoTotal = findViewById(R.id.tvNetoTotal)
        tvDetalleTotal = findViewById(R.id.tvDetalleTotal)
        tvResumenRemunerativo = findViewById(R.id.tvResumenRemunerativo)
        tvResumenNoRemunerativo = findViewById(R.id.tvResumenNoRemunerativo)
        tvResumenDescuentos = findViewById(R.id.tvResumenDescuentos)
        tvResumenNeto = findViewById(R.id.tvResumenNeto)
        resumenCard = findViewById(R.id.cardResumenRecibo)
        controlesCard = findViewById(R.id.cardHerramientasRecibo)
        checkMostrarDetalle = findViewById(R.id.cbMostrarDetalleRecibo)
        btnGuardar = findViewById(R.id.btnGuardarRecibo)
        btnVerGuardado = findViewById(R.id.btnVerGuardadoRecibo)
        btnCompartir = findViewById(R.id.btnCompartirRecibo)
        tvPistaConceptos = findViewById(R.id.tvPistaConceptos)

        // Cambiar la categoría es lo único que se modifica desde acá; el resto se corrige volviendo (Atrás).
        tvCambiarCategoria.setOnClickListener { mostrarSelectorCategoria() }
        btnNuevaConsulta.setOnClickListener {
            // No abrimos una segunda Calculadora: el receptor decide limpiar la actual, eso es todo.
            setResult(RESULT_OK, Intent().putExtra(EXTRA_ACCION_RECIBO, ACCION_NUEVA_CONSULTA))
            finish()
        }
        setResult(RESULT_CANCELED) // Seguro por defecto: no se devuelve ningún neto inventado.

        // Claves de ENTRADA que manda CalculadoraActivity.
        // Si faltara la categoría, se toma la de la cuenta iniciada.
        categoria = intent.getStringExtra("CATEGORIA")?.takeIf { it.isNotBlank() }
            ?: RepositorioUsuarios(this).usuarioActual()?.categoria
            ?: ""
        antiguedad = intent.getIntExtra("ANTIGUEDAD", 0)
        mes = intent.getIntExtra("MES_NUMERO", -1)
        anio = intent.getIntExtra("ANIO", -1)
        estadoOriginal = intent.getIntArrayExtra("ESTADO_DIAS")
        mostrarQ1 = intent.getBooleanExtra("MOSTRAR_Q1", true)
        mostrarQ2 = intent.getBooleanExtra("MOSTRAR_Q2", true)
        horasQ1 = intent.getIntExtra("HORAS_EXTRA_Q1", 0)
        horasQ2 = intent.getIntExtra("HORAS_EXTRA_Q2", 0)
        apercibimientoQ1 = intent.getBooleanExtra("APERCIBIMIENTO_Q1", false)
        apercibimientoQ2 = intent.getBooleanExtra("APERCIBIMIENTO_Q2", false)

        validarYMostrar()
    }

    /** Valida los datos recibidos, liquida con MotorLiquidacion y dibuja el recibo. */
    private fun validarYMostrar() {
        // Se vuelve a mostrar todo: puede haber quedado oculto por un error anterior.
        resumenCard.visibility = View.VISIBLE
        controlesCard.visibility = View.VISIBLE
        checkMostrarDetalle.visibility = View.VISIBLE
        tvPistaConceptos.visibility = View.VISIBLE

        val estadoRecibido = estadoOriginal
        val datosValidos = MotorLiquidacion.escalaPara(categoria) != null &&
                mes in 1..12 && anio in 1900..2100 &&
                antiguedad in 0..100 && horasQ1 in 0..300 && horasQ2 in 0..300 &&
                estadoRecibido != null && estadoRecibido.size >= 32 &&
                estadoRecibido.all { it in 0..MotorLiquidacion.ESTADO_MAXIMO } && (mostrarQ1 || mostrarQ2)

        if (!datosValidos || estadoRecibido == null) {
            mostrarError(getString(R.string.recibo_error_datos))
            return
        }

        // Los valores se validaron arriba. Mes y año permiten conocer el último día.
        val ultimoDia = Fechas.ultimoDiaDelMes(anio, mes)
        val estadoDias = estadoRecibido.copyOf()
        // Si cambiaron de marzo a febrero, los viejos días 29/30/31 no deben contar.
        for (dia in ultimoDia + 1..31) estadoDias[dia] = 0
        estadoLiquidado = estadoDias

        val periodo = "${nombreMes(mes)} $anio"
        tvEncabezado.text = getString(R.string.recibo_cabecera_datos, periodo, categoria, antiguedad)

        val resultado = try {
            MotorLiquidacion.calcularMes(
                categoria = categoria,
                antiguedadAnios = antiguedad,
                mes = mes,
                anio = anio,
                estadoDias = estadoDias,
                horasExtraQ1 = horasQ1,
                horasExtraQ2 = horasQ2,
                apercibimientoQ1 = apercibimientoQ1,
                apercibimientoQ2 = apercibimientoQ2
            )
        } catch (error: Exception) {
            // Fallar claramente es preferible a mostrar un sueldo falso de $ 0.
            Log.e("ReciboActivity", "Error al calcular el sueldo", error)
            mostrarError(getString(R.string.recibo_error_calculo))
            return
        }

        val (q1, q2) = resultado
        val seleccionadas = listOfNotNull(q1.takeIf { mostrarQ1 }, q2.takeIf { mostrarQ2 })
        if (seleccionadas.any { !cuadraLiquidacion(it) }) {
            mostrarError(getString(R.string.recibo_error_cuadre))
            return
        }

        seccionQ1.visibility = if (mostrarQ1) View.VISIBLE else View.GONE
        seccionQ2.visibility = if (mostrarQ2) View.VISIBLE else View.GONE
        if (mostrarQ1) {
            poblarTabla(tablaQ1, q1, 1)
            tvNetoQ1.text = getString(R.string.recibo_neto_quincena, dinero(q1.totalNeto))
        }
        if (mostrarQ2) {
            poblarTabla(tablaQ2, q2, 2)
            tvNetoQ2.text = getString(R.string.recibo_neto_quincena, dinero(q2.totalNeto))
        }

        val remunerativo = seleccionadas.sumOf { it.remSujetas }
        val noRemunerativo = seleccionadas.sumOf { it.remExentas }
        val descuentos = seleccionadas.sumOf { it.totalDescuentos }
        val neto = seleccionadas.sumOf { it.totalNeto }
        if (!neto.isFinite()) {
            mostrarError(getString(R.string.recibo_error_calculo))
            return
        }

        // Resumen y tablas usan EXACTAMENTE la misma fuente: MotorLiquidacion.
        tvResumenRemunerativo.text = getString(R.string.recibo_resumen_remunerativo, dinero(remunerativo))
        tvResumenNoRemunerativo.text = getString(R.string.recibo_resumen_no_remunerativo, dinero(noRemunerativo))
        tvResumenDescuentos.text = getString(R.string.recibo_resumen_descuentos, dinero(descuentos))
        tvResumenNeto.text = getString(R.string.recibo_resumen_neto, dinero(neto))

        tvTituloTotal.setText(if (mostrarQ1 && mostrarQ2) R.string.recibo_total_mes else R.string.recibo_total_consulta)
        tvNetoTotal.text = dinero(neto)
        tvDetalleTotal.setText(if (mostrarQ1 && mostrarQ2) R.string.recibo_detalle_ambas else R.string.recibo_detalle_una)

        checkMostrarDetalle.setOnCheckedChangeListener { _, visible ->
            detalleQ1.visibility = if (visible) View.VISIBLE else View.GONE
            detalleQ2.visibility = if (visible) View.VISIBLE else View.GONE
        }
        checkMostrarDetalle.isChecked = true

        val quincenas = when {
            mostrarQ1 && mostrarQ2 -> getString(R.string.recibo_quincenas_ambas)
            mostrarQ1 -> getString(R.string.recibo_quincenas_primera)
            else -> getString(R.string.recibo_quincenas_segunda)
        }

        btnGuardar.setOnClickListener {
            val prefs = getSharedPreferences(PREFS_RECIBO, MODE_PRIVATE)
            // commit() confirma si se guardó en disco; se usa sólo al pulsar Guardar.
            val guardado = prefs.edit()
                .putString("neto", neto.toString())
                .putString("periodo", periodo)
                .putString("categoria", categoria)
                .putString("quincenas", quincenas)
                .commit()
            val recuperado = prefs.getString("neto", null)?.toDoubleOrNull()
            val correcto = guardado && recuperado != null && abs(recuperado - neto) < 0.005
            Toast.makeText(this, if (correcto) R.string.recibo_guardado_ok else R.string.recibo_guardado_error,
                Toast.LENGTH_LONG).show()
        }
        // Consultar también cuando todavía no hay nada guardado.
        btnVerGuardado.setOnClickListener { mostrarUltimoGuardado() }
        btnCompartir.setOnClickListener {
            val resumen = buildString {
                appendLine(getString(R.string.recibo_compartir_titulo, periodo))
                appendLine(getString(R.string.recibo_compartir_categoria, categoria))
                appendLine(getString(R.string.recibo_compartir_quincenas, quincenas))
                appendLine(getString(R.string.recibo_resumen_remunerativo, dinero(remunerativo)))
                appendLine(getString(R.string.recibo_resumen_no_remunerativo, dinero(noRemunerativo)))
                appendLine(getString(R.string.recibo_resumen_descuentos, dinero(descuentos)))
                appendLine(getString(R.string.recibo_resumen_neto, dinero(neto)))
                append(getString(R.string.recibo_advertencia))
            }
            val enviar = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, resumen)
            }
            try {
                startActivity(Intent.createChooser(enviar, getString(R.string.recibo_compartir_selector)))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, R.string.recibo_compartir_sin_apps, Toast.LENGTH_SHORT).show()
            }
        }
        // Devuelve el neto tanto con «Nueva consulta» como con "Atrás".
        // CalculadoraActivity lo recibe con un ActivityResultLauncher.
        setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULTADO_NETO, neto))
    }

    // ------------------------------------------------------------------
    // Cambiar categoría
    // ------------------------------------------------------------------

    private fun mostrarSelectorCategoria() {
        val opciones = MotorLiquidacion.categorias
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.recibo_cambiar_categoria_titulo)
            .setSingleChoiceItems(opciones.toTypedArray(), opciones.indexOf(categoria)) { dialogo, posicion ->
                dialogo.dismiss()
                val nueva = opciones[posicion]
                if (nueva != categoria) cambiarCategoria(nueva)
            }
            .setNegativeButton(R.string.recibo_cancelar, null)
            .show()
    }

    /** Guarda la categoría elegida (queda para las próximas consultas) y recalcula el recibo. */
    private fun cambiarCategoria(nueva: String) {
        RepositorioUsuarios(this).actualizarCategoria(nueva)
        categoria = nueva
        Toast.makeText(this, getString(R.string.recibo_categoria_actualizada, nueva), Toast.LENGTH_SHORT).show()
        validarYMostrar()
    }

    private fun mostrarError(mensaje: String) {
        seccionQ1.visibility = View.GONE
        seccionQ2.visibility = View.GONE
        resumenCard.visibility = View.GONE
        controlesCard.visibility = View.GONE
        checkMostrarDetalle.visibility = View.GONE
        tvPistaConceptos.visibility = View.GONE
        tvTituloTotal.setText(R.string.recibo_error_titulo)
        tvNetoTotal.text = "—"
        tvDetalleTotal.text = mensaje
        setResult(RESULT_CANCELED)
    }

    private fun mostrarUltimoGuardado() {
        val prefs = getSharedPreferences(PREFS_RECIBO, MODE_PRIVATE)
        val neto = prefs.getString("neto", null)?.toDoubleOrNull()
        val mensaje = if (neto == null || !neto.isFinite()) {
            getString(R.string.recibo_guardado_vacio)
        } else {
            getString(R.string.recibo_guardado_detalle,
                prefs.getString("periodo", "—"),
                prefs.getString("categoria", "—"),
                prefs.getString("quincenas", "—"),
                dinero(neto))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.recibo_guardado_titulo)
            .setMessage(mensaje)
            .setPositiveButton(R.string.recibo_cerrar, null)
            .show()
    }

    private fun cuadraLiquidacion(r: ResultadoQuincena): Boolean {
        val importes = listOf(r.remSujetas, r.remExentas, r.totalDescuentos, r.totalNeto)
        return importes.all { it.isFinite() } &&
                abs((r.remSujetas + r.remExentas - r.totalDescuentos) - r.totalNeto) <= 0.03
    }

    private fun nombreMes(mes: Int): String = resources.getStringArray(R.array.lista_meses)[mes - 1]

    private fun numero(valor: Double): String = formato.format(valor)
    private fun dinero(valor: Double): String = "$ ${numero(valor)}"

    private fun poblarTabla(tabla: TableLayout, d: ResultadoQuincena, quincena: Int) {
        tabla.removeAllViews()
        agregarFila(tabla, getString(R.string.recibo_col_concepto), getString(R.string.recibo_col_cantidad),
            getString(R.string.recibo_col_remunerativo), getString(R.string.recibo_col_no_remunerativo),
            getString(R.string.recibo_col_descuentos), encabezado = true)

        // Al tocar una fila se abre el Fragment que explica cómo se calculó ese concepto.
        fun linea(nombre: Int, concepto: ConceptoRecibo, cantidad: String, remun: Double? = null,
                  noRem: Double? = null, desc: Double? = null) {
            agregarFila(tabla, getString(nombre), cantidad,
                remun?.let { numero(it) } ?: "—", noRem?.let { numero(it) } ?: "—", desc?.let { numero(it) } ?: "—",
                alTocar = { mostrarExplicacion(concepto, d, quincena) })
        }

        linea(R.string.recibo_horas_normales, ConceptoRecibo.HORAS_NORMALES, "${d.horasNormales} h", remun = d.valorHorasNormales)
        linea(R.string.recibo_horas_100, ConceptoRecibo.HORAS_100, "${d.horas100} h", remun = d.valorHoras100)
        linea(R.string.recibo_horas_feriado, ConceptoRecibo.HORAS_FERIADO, "${d.horasFeriado} h", remun = d.valorHorasFeriado)
        linea(R.string.recibo_premio_puntualidad, ConceptoRecibo.PREMIO_PUNTUALIDAD, "—", remun = d.premioPuntualidad)
        linea(R.string.recibo_premio_asistencia, ConceptoRecibo.PREMIO_ASISTENCIA, "—", remun = d.premioAsistencia)
        linea(R.string.recibo_premio_perfecto, ConceptoRecibo.PREMIO_PERFECTO, "—", remun = d.premioPerfecto)
        linea(R.string.recibo_antiguedad, ConceptoRecibo.ANTIGUEDAD, "${d.antiguedadPct}%", remun = d.antiguedadValor)
        linea(R.string.recibo_viaticos, ConceptoRecibo.VIATICOS, "${d.viaticosDias} días", noRem = d.viaticosValor)
        linea(R.string.recibo_refrigerio, ConceptoRecibo.REFRIGERIO, "${d.refrigerioDias} días", noRem = d.refrigerioValor)
        linea(R.string.recibo_jubilacion, ConceptoRecibo.JUBILACION, "—", desc = d.descJubilacion)
        linea(R.string.recibo_ley_19032, ConceptoRecibo.LEY_19032, "—", desc = d.descLey19032)
        linea(R.string.recibo_ley_23660, ConceptoRecibo.LEY_23660, "—", desc = d.descLey23660)
        linea(R.string.recibo_fondo_turismo, ConceptoRecibo.FONDO_TURISMO, "—", desc = d.descTurismo)
        linea(R.string.recibo_aporte_solidario, ConceptoRecibo.APORTE_SOLIDARIO, "—", desc = d.descSolidario)
        agregarFila(tabla, getString(R.string.recibo_fila_totales), "—", numero(d.remSujetas),
            numero(d.remExentas), numero(d.totalDescuentos), total = true,
            alTocar = { mostrarExplicacion(ConceptoRecibo.TOTALES, d, quincena) })
        // El neto NO va en la columna Descuentos: se muestra aparte, debajo de cada tabla.
    }

    /** Abre el panel (Fragment) con la explicación del concepto tocado, con los números de esta quincena. */
    private fun mostrarExplicacion(concepto: ConceptoRecibo, d: ResultadoQuincena, quincena: Int) {
        if (supportFragmentManager.findFragmentByTag(ExplicacionConceptoFragment.TAG) != null) return
        val escala = MotorLiquidacion.escalaPara(categoria) ?: return

        val rango = if (quincena == 1) 1..15 else 16..Fechas.ultimoDiaDelMes(anio, mes)
        fun contar(estado: Int) =
            rango.count { !Fechas.esDomingo(anio, mes, it) && estadoLiquidado[it] == estado }

        val datos = DatosExplicacion(
            escala = escala,
            antiguedad = antiguedad,
            quincena = quincena,
            horasExtra = if (quincena == 1) horasQ1 else horasQ2,
            apercibimiento = if (quincena == 1) apercibimientoQ1 else apercibimientoQ2,
            justificadas = contar(MotorLiquidacion.ESTADO_JUSTIFICADA),
            injustificadas = contar(MotorLiquidacion.ESTADO_INJUSTIFICADA)
        )
        ExplicacionConceptoFragment
            .nuevo(
                ExplicacionRecibo.titulo(this, concepto),
                ExplicacionRecibo.texto(this, concepto, d, datos)
            )
            .show(supportFragmentManager, ExplicacionConceptoFragment.TAG)
    }

    private fun agregarFila(
        tabla: TableLayout,
        concepto: String,
        cantidad: String,
        remunerativo: String,
        noRemunerativo: String,
        descuentos: String,
        encabezado: Boolean = false,
        total: Boolean = false,
        alTocar: (() -> Unit)? = null
    ) {
        val fila = TableRow(this)
        if (alTocar != null) {
            // Fila tocable: con el efecto de "ondita" estándar de Android
            val fondoTocable = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, fondoTocable, true)
            fila.foreground = ContextCompat.getDrawable(this, fondoTocable.resourceId)
            fila.isClickable = true
            fila.isFocusable = true
            fila.setOnClickListener { alTocar() }
        }
        if (encabezado) fila.setBackgroundColor(ContextCompat.getColor(this, R.color.recibo_fondo_cabecera_tabla))
        if (total) fila.setBackgroundColor(ContextCompat.getColor(this, R.color.recibo_fondo_total_tabla))
        val valores = listOf(concepto, cantidad, remunerativo, noRemunerativo, descuentos)
        val anchos = listOf(154, 85, 115, 112, 110)
        val alineaciones = listOf(Gravity.START, Gravity.CENTER, Gravity.END, Gravity.END, Gravity.END)
        valores.forEachIndexed { indice, contenido ->
            fila.addView(TextView(this).apply {
                text = contenido
                textSize = 12f
                gravity = alineaciones[indice] or Gravity.CENTER_VERTICAL
                setPadding(dp(7), dp(10), dp(7), dp(10))
                setTextColor(ContextCompat.getColor(this@ReciboActivity, R.color.recibo_texto))
                if (encabezado || total) setTypeface(null, Typeface.BOLD)
                layoutParams = TableRow.LayoutParams(dp(anchos[indice]), TableRow.LayoutParams.WRAP_CONTENT)
            })
        }
        tabla.addView(fila)
        tabla.addView(View(this).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, dp(1))
            setBackgroundColor(ContextCompat.getColor(this@ReciboActivity, R.color.recibo_separador))
        })
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density + 0.5f).toInt()
}
