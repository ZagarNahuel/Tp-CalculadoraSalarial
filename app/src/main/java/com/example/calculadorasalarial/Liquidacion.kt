package com.example.calculadorasalarial

import kotlin.math.roundToLong

data class EscalaCategoria(
    val valorHora: Double,
    val premioAsistencia: Double,
    val premioPuntualidad: Double,
    val premioPerfecto: Double,
    val viaticoDia: Double,
    val refrigerioDia: Double
)

data class ResultadoQuincena(
    val horasNormales: Int,
    val valorHorasNormales: Double,
    val horas100: Int,
    val valorHoras100: Double,
    val horasFeriado: Int,
    val valorHorasFeriado: Double,
    val premioPuntualidad: Double,
    val premioAsistencia: Double,
    val premioPerfecto: Double,
    val antiguedadPct: Int,
    val antiguedadValor: Double,
    val viaticosDias: Int,
    val viaticosValor: Double,
    val refrigerioDias: Int,
    val refrigerioValor: Double,
    val descJubilacion: Double,
    val descLey19032: Double,
    val descLey23660: Double,
    val descTurismo: Double,
    val descSolidario: Double,
    val remSujetas: Double,
    val remExentas: Double,
    val totalDescuentos: Double,
    val totalNeto: Double
)

object MotorLiquidacion {

    // Estado de cada día del calendario (así se guarda en estadoDias[día])
    const val ESTADO_TRABAJADO = 0      // blanco: día trabajado
    const val ESTADO_JUSTIFICADA = 1    // rojo: falta justificada (se paga, no lleva viático ni refrigerio)
    const val ESTADO_INJUSTIFICADA = 2  // negro: falta injustificada (no se paga y pierde premios)
    const val ESTADO_FERIADO = 3        // verde: feriado trabajado, lo marca el usuario manteniendo presionado (pago x2)
    const val ESTADO_MAXIMO = ESTADO_FERIADO

    private const val HORAS_JORNADA = 8
    private const val HORAS_SABADO_NORMALES = 6   // el sábado trabajado son 6 hs simples + 2 hs al 100%
    private const val HORAS_SABADO_AL_100 = 2

    // Escalas vigentes: última columna de la tabla ("desde el 1º de noviembre de 2026").
    // Cada categoría usa siempre su escala más actual, sin importar el mes que se liquide.
    //                                   valor hora | (1) asistencia | (2) puntualidad | (3) perfecta | viático | refrigerio
    private val ESCALAS = linkedMapOf(
        "CATEGORIA INICIAL" to EscalaCategoria(5379.0, 47332.0, 49174.0, 71015.0, 3123.0, 2366.0),
        "CATEGORIA 1" to EscalaCategoria(5888.0, 48793.0, 50642.0, 73258.0, 3123.0, 2366.0),
        "CATEGORIA 2" to EscalaCategoria(6120.0, 49847.0, 54450.0, 75708.0, 3123.0, 2366.0),
        "CATEGORIA 3" to EscalaCategoria(6374.0, 50826.0, 55548.0, 77521.0, 3123.0, 2366.0),
        "CATEGORIA 4" to EscalaCategoria(6620.0, 51871.0, 57300.0, 79470.0, 3123.0, 2366.0)
    )

    /** Nombres de categoría válidos (son los mismos que muestra el Spinner del registro). */
    val categorias: List<String> get() = ESCALAS.keys.toList()

    fun escalaPara(categoria: String): EscalaCategoria? = ESCALAS[categoria]

    // Porcentajes de descuentos de ley y sindicales (sobre las remuneraciones sujetas)
    const val DESC_JUBILACION = 0.109961
    const val DESC_19032 = 0.029985
    const val DESC_23660 = 0.029985
    const val DESC_TURISMO = 0.009985
    const val DESC_SOLIDARIO = 0.019970

    /**
     * Regla de antigüedad (años COMPLETOS):
     *  - 0 años (primer año de trabajo) = no se paga antigüedad
     *  - de 1 a 4 años = 4 %
     *  - 5 años o más = 1 % por cada año
     */
    private fun obtenerPorcentajeAntiguedad(anios: Int): Int = when {
        anios <= 0 -> 0
        anios in 1..4 -> 4
        else -> anios
    }

    /** Redondea a centavos para que los renglones del recibo sumen exactamente el total. */
    private fun centavos(valor: Double): Double = (valor * 100).roundToLong() / 100.0

    fun calcularMes(
        categoria: String,
        antiguedadAnios: Int,
        mes: Int, // 1 a 12
        anio: Int,
        estadoDias: IntArray, // índice = día del mes; valores ESTADO_*
        horasExtraQ1: Int,
        horasExtraQ2: Int,
        apercibimientoQ1: Boolean,
        apercibimientoQ2: Boolean
    ): Pair<ResultadoQuincena, ResultadoQuincena> {

        val escala = requireNotNull(ESCALAS[categoria]) { "Categoría desconocida: $categoria" }
        val pctAntiguedad = obtenerPorcentajeAntiguedad(antiguedadAnios)

        val ultimoDiaMes = Fechas.ultimoDiaDelMes(anio, mes)
        require(estadoDias.size > ultimoDiaMes) { "estadoDias debe tener un lugar por cada día del mes" }

        // Premio Asistencia Perfecta (se cobra en la 2° quincena): ninguna falta injustificada ni apercibimiento en el mes
        val faltasInjustQ1 = contarInjustificadas(1..15, mes, anio, estadoDias)
        val faltasInjustQ2 = contarInjustificadas(16..ultimoDiaMes, mes, anio, estadoDias)
        val ganaPremioPerfectoMes =
            faltasInjustQ1 == 0 && faltasInjustQ2 == 0 && !apercibimientoQ1 && !apercibimientoQ2

        val resQ1 = calcularQuincena(
            rangoDias = 1..15,
            mes = mes,
            anio = anio,
            escala = escala,
            pctAntiguedad = pctAntiguedad,
            estadoDias = estadoDias,
            horasExtraManuales = horasExtraQ1,
            tuvoApercibimiento = apercibimientoQ1,
            correspondePremioPerfecto = false // En 1° Quincena siempre es 0.00
        )

        val resQ2 = calcularQuincena(
            rangoDias = 16..ultimoDiaMes,
            mes = mes,
            anio = anio,
            escala = escala,
            pctAntiguedad = pctAntiguedad,
            estadoDias = estadoDias,
            horasExtraManuales = horasExtraQ2,
            tuvoApercibimiento = apercibimientoQ2,
            correspondePremioPerfecto = ganaPremioPerfectoMes
        )

        return Pair(resQ1, resQ2)
    }

    private fun contarInjustificadas(rango: IntRange, mes: Int, anio: Int, estadoDias: IntArray): Int =
        rango.count { !Fechas.esDomingo(anio, mes, it) && estadoDias[it] == ESTADO_INJUSTIFICADA }

    private fun calcularQuincena(
        rangoDias: IntRange,
        mes: Int,
        anio: Int,
        escala: EscalaCategoria,
        pctAntiguedad: Int,
        estadoDias: IntArray,
        horasExtraManuales: Int,
        tuvoApercibimiento: Boolean,
        correspondePremioPerfecto: Boolean
    ): ResultadoQuincena {

        // Los domingos no se marcan ni se liquidan
        val diasLaborales = rangoDias.filter { !Fechas.esDomingo(anio, mes, it) }

        var horasNormales = 0
        var horasSabado100 = 0
        var horasFeriado = 0
        var diasConAsistencia = 0 // días en los que realmente se fue a trabajar (llevan viático y refrigerio)
        var faltasInjustificadas = 0

        for (dia in diasLaborales) {
            val sabado = Fechas.esSabado(anio, mes, dia)
            when (estadoDias[dia]) {
                ESTADO_FERIADO -> {
                    // Feriado trabajado: 8 hs al valor simple (horasFeriado) + 8 hs adicionales al 100 % (se suman en valorHoras100)
                    horasFeriado += HORAS_JORNADA
                    diasConAsistencia++
                }
                ESTADO_JUSTIFICADA -> {
                    // Falta justificada: se paga la jornada simple, sin el 100 % del sábado ni viáticos
                    horasNormales += if (sabado) HORAS_SABADO_NORMALES else HORAS_JORNADA
                }
                ESTADO_INJUSTIFICADA -> faltasInjustificadas++
                else -> {
                    diasConAsistencia++
                    if (sabado) {
                        horasNormales += HORAS_SABADO_NORMALES
                        horasSabado100 += HORAS_SABADO_AL_100
                    } else {
                        horasNormales += HORAS_JORNADA
                    }
                }
            }
        }

        // Horas 100 % agrupa: 2 hs por sábado trabajado + horas extra de la quincena + las horas del feriado trabajado
        val totalHoras100 = horasSabado100 + horasExtraManuales + horasFeriado

        // CÁLCULO MONETARIO:
        // - Horas normales: valor simple
        // - Horas feriado: valor simple (pago base del día feriado)
        // - Horas 100 %: sábado y horas extra al doble (x2) + el 100 % adicional de las horas del feriado
        val valorHorasNormales = horasNormales * escala.valorHora
        val valorHorasFeriado = horasFeriado * escala.valorHora
        val valorHoras100 =
            ((horasSabado100 + horasExtraManuales) * (escala.valorHora * 2.0)) + (horasFeriado * escala.valorHora)

        // Premios: la falta justificada NO quita premios. El apercibimiento quita Puntualidad y Perfecto.
        val premioAsistencia = if (faltasInjustificadas == 0) escala.premioAsistencia else 0.0
        val premioPuntualidad =
            if (faltasInjustificadas == 0 && !tuvoApercibimiento) escala.premioPuntualidad else 0.0
        val premioPerfecto = if (correspondePremioPerfecto) escala.premioPerfecto else 0.0

        // Antigüedad sobre retribución básica (horas normales + horas 100 % + horas feriado)
        val retribucionBasica = valorHorasNormales + valorHoras100 + valorHorasFeriado
        val antiguedadValor = centavos(retribucionBasica * (pctAntiguedad / 100.0))

        // Total remuneraciones sujetas a retención
        val remSujetas = centavos(
            retribucionBasica + premioAsistencia + premioPuntualidad + premioPerfecto + antiguedadValor
        )

        // Conceptos no remunerativos (exentos): por cada día que se fue a trabajar
        val viaticosValor = diasConAsistencia * escala.viaticoDia
        val refrigerioValor = diasConAsistencia * escala.refrigerioDia
        val remExentas = centavos(viaticosValor + refrigerioValor)

        // Descuentos de ley y sindicales (cada uno redondeado, así el total es la suma de lo que se ve)
        val descJub = centavos(remSujetas * DESC_JUBILACION)
        val desc19032 = centavos(remSujetas * DESC_19032)
        val desc23660 = centavos(remSujetas * DESC_23660)
        val descTur = centavos(remSujetas * DESC_TURISMO)
        val descSol = centavos(remSujetas * DESC_SOLIDARIO)
        val totalDescuentos = centavos(descJub + desc19032 + desc23660 + descTur + descSol)

        val totalNeto = centavos(remSujetas + remExentas - totalDescuentos)

        return ResultadoQuincena(
            horasNormales = horasNormales,
            valorHorasNormales = valorHorasNormales,
            horas100 = totalHoras100,
            valorHoras100 = valorHoras100,
            horasFeriado = horasFeriado,
            valorHorasFeriado = valorHorasFeriado,
            premioPuntualidad = premioPuntualidad,
            premioAsistencia = premioAsistencia,
            premioPerfecto = premioPerfecto,
            antiguedadPct = pctAntiguedad,
            antiguedadValor = antiguedadValor,
            viaticosDias = diasConAsistencia,
            viaticosValor = viaticosValor,
            refrigerioDias = diasConAsistencia,
            refrigerioValor = refrigerioValor,
            descJubilacion = descJub,
            descLey19032 = desc19032,
            descLey23660 = desc23660,
            descTurismo = descTur,
            descSolidario = descSol,
            remSujetas = remSujetas,
            remExentas = remExentas,
            totalDescuentos = totalDescuentos,
            totalNeto = totalNeto
        )
    }
}
