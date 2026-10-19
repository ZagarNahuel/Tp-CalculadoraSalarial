package com.example.calculadorasalarial

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Cálculos de calendario en un único lugar (antes estaban duplicados en la
 * Calculadora y en el motor de liquidación).
 * Convención: el mes va de 1 (enero) a 12 (diciembre).
 */
object Fechas {

    private fun calendario(anio: Int, mes: Int, dia: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(anio, mes - 1, dia)
        }

    fun ultimoDiaDelMes(anio: Int, mes: Int): Int =
        calendario(anio, mes, 1).getActualMaximum(Calendar.DAY_OF_MONTH)

    /** Lunes = 0 ... Sábado = 5, Domingo = 6. */
    fun columnaDeLaSemana(anio: Int, mes: Int, dia: Int): Int =
        (calendario(anio, mes, dia).get(Calendar.DAY_OF_WEEK) + 5) % 7

    fun esSabado(anio: Int, mes: Int, dia: Int): Boolean = columnaDeLaSemana(anio, mes, dia) == 5

    fun esDomingo(anio: Int, mes: Int, dia: Int): Boolean = columnaDeLaSemana(anio, mes, dia) == 6
}

/** Formato de dinero argentino: $ 1.234,56 */
object Formato {
    private val localeAr: Locale = Locale.forLanguageTag("es-AR")

    fun dinero(valor: Double): String {
        val formato = NumberFormat.getNumberInstance(localeAr).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$ " + formato.format(valor)
    }
}

/**
 * Con targetSdk 35+ las pantallas se dibujan "de borde a borde": la barra de estado,
 * la de navegación y el teclado pueden tapar el contenido si no se compensan.
 *
 * - [raiz]: vista raíz de la pantalla.
 * - [banda]: franja azul superior de 80dp. Si existe, crece lo que mide la barra de
 *   estado (así el azul llega hasta arriba). Si es null, el padding se aplica a la raíz.
 */
fun Activity.ajustarBordesDelSistema(raiz: View, banda: View? = null) {
    val alturaBanda = banda?.layoutParams?.height ?: 0
    // Banda azul arriba -> íconos de la barra de estado claros; fondo claro -> íconos oscuros.
    WindowCompat.getInsetsController(window, raiz).isAppearanceLightStatusBars = (banda == null)

    ViewCompat.setOnApplyWindowInsetsListener(raiz) { vista, insets ->
        val barras = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
        if (banda != null) {
            banda.updateLayoutParams { height = alturaBanda + barras.top }
            vista.updatePadding(
                left = barras.left,
                right = barras.right,
                bottom = maxOf(barras.bottom, teclado.bottom)
            )
        } else {
            vista.updatePadding(
                left = barras.left,
                top = barras.top,
                right = barras.right,
                bottom = maxOf(barras.bottom, teclado.bottom)
            )
        }
        insets
    }
}

/**
 * Ícono de ojo dentro del campo de contraseña (al final de la caja): al tocarlo se muestra u
 * oculta lo escrito. Empieza oculta: ojo tachado = oculta, ojo abierto = visible.
 */
fun configurarOjoContrasena(campo: EditText, ojo: ImageView) {
    fun aplicar(visible: Boolean) {
        campo.transformationMethod =
            if (visible) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
        campo.setSelection(campo.text.length) // el cursor no salta al principio al cambiar
        ojo.setImageResource(if (visible) R.drawable.ic_ojo else R.drawable.ic_ojo_tachado)
        ojo.contentDescription = ojo.context.getString(
            if (visible) R.string.comun_ocultar_password else R.string.comun_mostrar_password
        )
    }

    var visible = false
    aplicar(visible)
    ojo.setOnClickListener {
        visible = !visible
        aplicar(visible)
    }
}

/** Adaptador para los Spinner con fondo celeste (fondo_spinner_celeste): el elemento elegido se ve en blanco. */
fun Context.adaptadorSpinnerBlanco(opciones: Array<String>): ArrayAdapter<String> {
    val adaptador = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, opciones) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val vista = super.getView(position, convertView, parent) as TextView
            vista.setTextColor(Color.WHITE)
            return vista
        }
    }
    adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    return adaptador
}
