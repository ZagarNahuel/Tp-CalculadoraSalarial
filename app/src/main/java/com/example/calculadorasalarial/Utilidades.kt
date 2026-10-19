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

// Cuentas con fechas: día de la semana y cantidad de días del mes.
// El mes va de 1 (enero) a 12 (diciembre).
object Fechas {
    private fun calendario(anio: Int, mes: Int, dia: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(anio, mes - 1, dia)
        }

    fun ultimoDiaDelMes(anio: Int, mes: Int): Int =
        calendario(anio, mes, 1).getActualMaximum(Calendar.DAY_OF_MONTH)

    fun columnaDeLaSemana(anio: Int, mes: Int, dia: Int): Int =
        (calendario(anio, mes, dia).get(Calendar.DAY_OF_WEEK) + 5) % 7

    fun esSabado(anio: Int, mes: Int, dia: Int): Boolean = columnaDeLaSemana(anio, mes, dia) == 5

    fun esDomingo(anio: Int, mes: Int, dia: Int): Boolean = columnaDeLaSemana(anio, mes, dia) == 6
}

// Muestra el dinero como en Argentina: $ 1.234,56
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

// La pantalla se dibuja hasta los bordes del celular. Esto evita que la barra de arriba, la de
// abajo o el teclado tapen el contenido. La 'banda' es la franja azul de arriba: se agranda lo
// que mide la barra de estado para que el azul llegue hasta el borde.
fun Activity.ajustarBordesDelSistema(raiz: View, banda: View? = null) {
    val alturaBanda = banda?.layoutParams?.height ?: 0
    // Con franja azul arriba, los íconos de la barra se ven claros; con fondo claro, oscuros.
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

// Ojo para ver u ocultar la contraseña: tachado es oculta, abierto es visible.
fun configurarOjoContrasena(campo: EditText, ojo: ImageView) {
    fun aplicar(visible: Boolean) {
        campo.transformationMethod =
            if (visible) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
        campo.setSelection(campo.text.length)
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

// Lista desplegable que muestra lo elegido en blanco, para que se lea sobre el fondo celeste.
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

// Escribe '1 año' o '2 años' según la cantidad.
fun Context.textoAnios(cantidad: Int): String = resources.getQuantityString(R.plurals.anios, cantidad, cantidad)
