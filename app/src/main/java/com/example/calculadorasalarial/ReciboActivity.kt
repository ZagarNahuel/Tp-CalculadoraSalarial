package com.example.calculadorasalarial
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.calculadorasalarial.MotorLiquidacion
import com.example.calculadorasalarial.ResultadoQuincena
import java.util.Locale

class ReciboActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recibo)

        val tvHeader = findViewById<TextView>(R.id.tvHeaderRecibo)
        val tvCambiarCategoria = findViewById<TextView>(R.id.tvCambiarCategoria)
        val btnNuevaConsulta = findViewById<Button>(R.id.btnNuevaConsulta)

        val seccionQ1 = findViewById<LinearLayout>(R.id.seccionQuincenaA)
        val seccionQ2 = findViewById<LinearLayout>(R.id.seccionQuincenaB)
        val tablaQ1 = findViewById<TableLayout>(R.id.tablaQuincenaA)
        val tablaQ2 = findViewById<TableLayout>(R.id.tablaQuincenaB)

        // 1. Recibir todos los datos enviados desde CalculadoraActivity
        val categoria = intent.getStringExtra("CATEGORIA") ?: "CATEGORIA INICIAL"
        val antiguedad = intent.getIntExtra("ANTIGUEDAD", 0)
        val mesNombre = intent.getStringExtra("MES_NOMBRE") ?: "Enero"
        val mesNumero = intent.getIntExtra("MES_NUMERO", 1)
        val anio = intent.getIntExtra("ANIO", 2026)
        val estadoDias = intent.getIntArrayExtra("ESTADO_DIAS") ?: IntArray(32) { 0 }
        val horasExtraQ1 = intent.getIntExtra("HORAS_EXTRA_Q1", 0)
        val horasExtraQ2 = intent.getIntExtra("HORAS_EXTRA_Q2", 0)
        val apercibimientoQ1 = intent.getBooleanExtra("APERCIBIMIENTO_Q1", false)
        val apercibimientoQ2 = intent.getBooleanExtra("APERCIBIMIENTO_Q2", false)
        val mostrarQ1 = intent.getBooleanExtra("MOSTRAR_Q1", true)
        val mostrarQ2 = intent.getBooleanExtra("MOSTRAR_Q2", true)

        tvHeader.text = "Recibo — Mes $mesNombre $anio — $categoria"

        // 2. Ejecutar el cálculo con MotorLiquidacion
        val (datosQ1, datosQ2) = MotorLiquidacion.calcularMes(
            categoria = categoria,
            antiguedadAnios = antiguedad,
            mes = mesNumero,
            anio = anio,
            estadoDias = estadoDias,
            horasExtraQ1 = horasExtraQ1,
            horasExtraQ2 = horasExtraQ2,
            apercibimientoQ1 = apercibimientoQ1,
            apercibimientoQ2 = apercibimientoQ2
        )

        // 3. Mostrar las quincenas habilitadas y poblar las tablas
        seccionQ1.visibility = if (mostrarQ1) View.VISIBLE else View.GONE
        seccionQ2.visibility = if (mostrarQ2) View.VISIBLE else View.GONE

        if (mostrarQ1) poblarTablaQuincena(tablaQ1, datosQ1)
        if (mostrarQ2) poblarTablaQuincena(tablaQ2, datosQ2)

        tvCambiarCategoria.setOnClickListener { finish() }
        btnNuevaConsulta.setOnClickListener { finish() }
    }

    private fun fmt(valor: Double): String {
        return String.format(Locale.US, "%,.2f", valor)
    }

    private fun poblarTablaQuincena(tabla: TableLayout, d: ResultadoQuincena) {
        tabla.removeAllViews()

        agregarFilaTabla(
            tabla,
            "Concepto", "Unidades", "Rem. suj.", "Rem. ex.", "Desc.",
            esEncabezado = true
        )

        agregarFilaTabla(tabla, "Horas normales", "${d.horasNormales} h", fmt(d.valorHorasNormales), "-", "-")
        agregarFilaTabla(tabla, "Horas 100%", "${d.horas100} h", fmt(d.valorHoras100), "-", "-")
        agregarFilaTabla(tabla, "Horas feriado", "${d.horasFeriado} h", fmt(d.valorHorasFeriado), "-", "-")
        agregarFilaTabla(tabla, "Premio puntualidad", "-", fmt(d.premioPuntualidad), "-", "-")
        agregarFilaTabla(tabla, "Premio asistencia", "-", fmt(d.premioAsistencia), "-", "-")
        agregarFilaTabla(tabla, "Premio perfecta", "-", fmt(d.premioPerfecto), "-", "-")
        agregarFilaTabla(tabla, "Antigüedad", "${d.antiguedadPct.toInt()}%", fmt(d.antiguedadValor), "-", "-")
        agregarFilaTabla(tabla, "Viáticos", "${d.viaticosDias} días", "-", fmt(d.viaticosValor), "-")
        agregarFilaTabla(tabla, "Refrigerio", "${d.refrigerioDias} días", "-", fmt(d.refrigerioValor), "-")
        agregarFilaTabla(tabla, "Jubilación", "-", "-", "-", fmt(d.descJubilacion))
        agregarFilaTabla(tabla, "Ley 19032", "-", "-", "-", fmt(d.descLey19032))
        agregarFilaTabla(tabla, "Ley 23660", "-", "-", "-", fmt(d.descLey23660))
        agregarFilaTabla(tabla, "Fondo Turismo", "-", "-", "-", fmt(d.descTurismo))
        agregarFilaTabla(tabla, "Aporte Solidario", "-", "-", "-", fmt(d.descSolidario))

        agregarFilaTabla(
            tabla,
            "Total", "-", fmt(d.remSujetas), fmt(d.remExentas), fmt(d.totalDescuentos),
            esTotal = true
        )
        agregarFilaTabla(
            tabla,
            "Total neto", "-", "-", "-", fmt(d.totalNeto),
            esTotal = true
        )
    }

    private fun agregarFilaTabla(
        tabla: TableLayout,
        c1: String, c2: String, c3: String, c4: String, c5: String,
        esEncabezado: Boolean = false,
        esTotal: Boolean = false
    ) {
        val fila = TableRow(this).apply {
            setPadding(4, 10, 4, 10)
            if (esEncabezado) setBackgroundColor(Color.parseColor("#F8F9FA"))
            if (esTotal) setBackgroundColor(Color.parseColor("#EEF4FF"))
        }

        val textos = arrayOf(c1, c2, c3, c4, c5)
        val pesos = floatArrayOf(0.30f, 0.14f, 0.19f, 0.19f, 0.18f)
        val alineaciones = intArrayOf(
            Gravity.START, Gravity.CENTER, Gravity.END, Gravity.END, Gravity.END
        )

        for (i in 0..4) {
            val tv = TextView(this).apply {
                text = textos[i]
                textSize = if (esEncabezado) 10.5f else 11f
                gravity = alineaciones[i]
                setTextColor(Color.parseColor(if (esEncabezado || esTotal) "#1A2B4C" else "#333333"))
                if (esEncabezado || esTotal) {
                    setTypeface(null, Typeface.BOLD)
                }
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, pesos[i])
            }
            fila.addView(tv)
        }

        tabla.addView(fila)

        val linea = View(this).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.parseColor("#E5E9F2"))
        }
        tabla.addView(linea)
    }
}
