package com.example.calculadorasalarial

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

/** Filas del recibo que se pueden tocar para ver cómo se calcularon. */
enum class ConceptoRecibo {
    HORAS_NORMALES, HORAS_100, HORAS_FERIADO,
    PREMIO_PUNTUALIDAD, PREMIO_ASISTENCIA, PREMIO_PERFECTO,
    ANTIGUEDAD, VIATICOS, REFRIGERIO,
    JUBILACION, LEY_19032, LEY_23660, FONDO_TURISMO, APORTE_SOLIDARIO,
    TOTALES
}

/** Datos de la consulta que hacen falta para explicar una quincena. */
data class DatosExplicacion(
    val escala: EscalaCategoria,
    val antiguedad: Int,
    val quincena: Int,          // 1 o 2
    val horasExtra: Int,
    val apercibimiento: Boolean,
    val justificadas: Int,
    val injustificadas: Int
)

/**
 * Arma los textos que muestra [ExplicacionConceptoFragment]. Los números salen del mismo
 * [ResultadoQuincena] que usa el recibo, así que la explicación nunca se contradice con la tabla.
 */
object ExplicacionRecibo {

    fun titulo(contexto: Context, concepto: ConceptoRecibo): String = contexto.getString(
        when (concepto) {
            ConceptoRecibo.HORAS_NORMALES -> R.string.recibo_horas_normales
            ConceptoRecibo.HORAS_100 -> R.string.recibo_horas_100
            ConceptoRecibo.HORAS_FERIADO -> R.string.recibo_horas_feriado
            ConceptoRecibo.PREMIO_PUNTUALIDAD -> R.string.recibo_premio_puntualidad
            ConceptoRecibo.PREMIO_ASISTENCIA -> R.string.recibo_premio_asistencia
            ConceptoRecibo.PREMIO_PERFECTO -> R.string.recibo_premio_perfecto
            ConceptoRecibo.ANTIGUEDAD -> R.string.recibo_antiguedad
            ConceptoRecibo.VIATICOS -> R.string.recibo_viaticos
            ConceptoRecibo.REFRIGERIO -> R.string.recibo_refrigerio
            ConceptoRecibo.JUBILACION -> R.string.recibo_jubilacion
            ConceptoRecibo.LEY_19032 -> R.string.recibo_ley_19032
            ConceptoRecibo.LEY_23660 -> R.string.recibo_ley_23660
            ConceptoRecibo.FONDO_TURISMO -> R.string.recibo_fondo_turismo
            ConceptoRecibo.APORTE_SOLIDARIO -> R.string.recibo_aporte_solidario
            ConceptoRecibo.TOTALES -> R.string.recibo_fila_totales
        }
    )

    fun texto(
        contexto: Context,
        concepto: ConceptoRecibo,
        d: ResultadoQuincena,
        datos: DatosExplicacion
    ): String {
        val e = datos.escala
        return when (concepto) {
            ConceptoRecibo.HORAS_NORMALES -> contexto.getString(
                R.string.exp_horas_normales,
                dinero(e.valorHora), d.horasNormales.toString(), dinero(d.valorHorasNormales),
                datos.justificadas.toString(), datos.injustificadas.toString()
            )

            ConceptoRecibo.HORAS_100 -> {
                val horasSabado = d.horas100 - datos.horasExtra - d.horasFeriado
                contexto.getString(
                    R.string.exp_horas_100,
                    dinero(e.valorHora), dinero(e.valorHora * 2), d.horas100.toString(),
                    horasSabado.toString(), datos.horasExtra.toString(), d.horasFeriado.toString(),
                    dinero(d.valorHoras100)
                )
            }

            ConceptoRecibo.HORAS_FERIADO -> contexto.getString(
                R.string.exp_horas_feriado,
                (d.horasFeriado / 8).toString(), d.horasFeriado.toString(),
                dinero(e.valorHora), dinero(d.valorHorasFeriado)
            )

            ConceptoRecibo.PREMIO_PUNTUALIDAD -> contexto.getString(
                R.string.exp_premio_puntualidad,
                dinero(e.premioPuntualidad),
                when {
                    datos.injustificadas > 0 -> contexto.getString(R.string.exp_motivo_injustificadas, datos.injustificadas.toString())
                    datos.apercibimiento -> contexto.getString(R.string.exp_motivo_apercibimiento)
                    else -> contexto.getString(R.string.exp_motivo_se_pago)
                }
            )

            ConceptoRecibo.PREMIO_ASISTENCIA -> contexto.getString(
                R.string.exp_premio_asistencia,
                dinero(e.premioAsistencia),
                if (datos.injustificadas > 0) {
                    contexto.getString(R.string.exp_motivo_injustificadas, datos.injustificadas.toString())
                } else {
                    contexto.getString(R.string.exp_motivo_se_pago)
                }
            )

            ConceptoRecibo.PREMIO_PERFECTO -> contexto.getString(
                R.string.exp_premio_perfecto,
                dinero(e.premioPerfecto),
                when {
                    datos.quincena == 1 -> contexto.getString(R.string.exp_motivo_perfecto_primera)
                    d.premioPerfecto > 0 -> contexto.getString(R.string.exp_motivo_perfecto_pagado)
                    else -> contexto.getString(R.string.exp_motivo_perfecto_perdido)
                }
            )

            ConceptoRecibo.ANTIGUEDAD -> {
                val basica = d.valorHorasNormales + d.valorHoras100 + d.valorHorasFeriado
                contexto.getString(
                    R.string.exp_antiguedad,
                    dinero(basica), datos.antiguedad.toString(), d.antiguedadPct.toString(), dinero(d.antiguedadValor)
                )
            }

            ConceptoRecibo.VIATICOS -> contexto.getString(
                R.string.exp_viaticos,
                dinero(e.viaticoDia), d.viaticosDias.toString(), dinero(d.viaticosValor)
            )

            ConceptoRecibo.REFRIGERIO -> contexto.getString(
                R.string.exp_refrigerio,
                dinero(e.refrigerioDia), d.refrigerioDias.toString(), dinero(d.refrigerioValor)
            )

            ConceptoRecibo.JUBILACION ->
                descuento(contexto, MotorLiquidacion.DESC_JUBILACION, d.remSujetas, d.descJubilacion)
            ConceptoRecibo.LEY_19032 ->
                descuento(contexto, MotorLiquidacion.DESC_19032, d.remSujetas, d.descLey19032)
            ConceptoRecibo.LEY_23660 ->
                descuento(contexto, MotorLiquidacion.DESC_23660, d.remSujetas, d.descLey23660)
            ConceptoRecibo.FONDO_TURISMO ->
                descuento(contexto, MotorLiquidacion.DESC_TURISMO, d.remSujetas, d.descTurismo)
            ConceptoRecibo.APORTE_SOLIDARIO ->
                descuento(contexto, MotorLiquidacion.DESC_SOLIDARIO, d.remSujetas, d.descSolidario)

            ConceptoRecibo.TOTALES -> {
                val total = MotorLiquidacion.DESC_JUBILACION + MotorLiquidacion.DESC_19032 +
                    MotorLiquidacion.DESC_23660 + MotorLiquidacion.DESC_TURISMO + MotorLiquidacion.DESC_SOLIDARIO
                contexto.getString(
                    R.string.exp_totales,
                    dinero(d.remSujetas), dinero(d.remExentas), dinero(d.totalDescuentos),
                    porcentaje(total), dinero(d.totalNeto)
                )
            }
        }
    }

    private fun descuento(contexto: Context, tasa: Double, base: Double, importe: Double): String =
        contexto.getString(R.string.exp_descuento, dinero(base), porcentaje(tasa), dinero(importe))

    private fun dinero(valor: Double): String = Formato.dinero(valor)

    /** 0.109961 -> "10,9961" */
    private fun porcentaje(tasa: Double): String =
        NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 4
        }.format(tasa * 100)
}
