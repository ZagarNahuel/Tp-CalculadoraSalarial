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

// Acá están todas las reglas del sueldo. Si el convenio cambia, se modifica solo este archivo.
object MotorLiquidacion {
    const val ESTADO_TRABAJADO = 0 // día trabajado (blanco)
    const val ESTADO_JUSTIFICADA = 1 // falta justificada (rojo): se paga, pero sin viático
    const val ESTADO_INJUSTIFICADA = 2 // falta injustificada (negro): no se paga
    const val ESTADO_FERIADO = 3 // feriado trabajado (verde): se paga doble. Lo marca el usuario manteniendo presionado el día
    const val ESTADO_MAXIMO = ESTADO_FERIADO

    private const val HORAS_JORNADA = 8
    private const val HORAS_SABADO_NORMALES = 6 // el sábado son 6 horas simples y 2 al 100%
    private const val HORAS_SABADO_AL_100 = 2

    // Escalas de cada categoría, vigentes desde el 1° de noviembre de 2026.
    // Se usa siempre la más reciente, sin importar el mes que se liquide.
    // Orden: valor hora, premio asistencia, premio puntualidad, asistencia perfecta, viático y refrigerio.
    private val ESCALAS = linkedMapOf(
        "CATEGORIA INICIAL" to EscalaCategoria(5379.0, 47332.0, 49174.0, 71015.0, 3123.0, 2366.0),
        "CATEGORIA 1" to EscalaCategoria(5888.0, 48793.0, 50642.0, 73258.0, 3123.0, 2366.0),
        "CATEGORIA 2" to EscalaCategoria(6120.0, 49847.0, 54450.0, 75708.0, 3123.0, 2366.0),
        "CATEGORIA 3" to EscalaCategoria(6374.0, 50826.0, 55548.0, 77521.0, 3123.0, 2366.0),
        "CATEGORIA 4" to EscalaCategoria(6620.0, 51871.0, 57300.0, 79470.0, 3123.0, 2366.0)
    )

    val categorias: List<String> get() = ESCALAS.keys.toList()

    fun escalaPara(categoria: String): EscalaCategoria? = ESCALAS[categoria]

    fun nombreVisible(categoria: String): String =
        categoria.lowercase().replaceFirstChar { it.uppercase() }.replace("Categoria", "Categoría")

    // Porcentajes que se descuentan de lo remunerativo.
    const val DESC_JUBILACION = 0.109961
    const val DESC_19032 = 0.029985
    const val DESC_23660 = 0.029985
    const val DESC_TURISMO = 0.009985
    const val DESC_SOLIDARIO = 0.019970

    // Antigüedad (años completos): menos de 1 año no paga, de 1 a 4 años es 4%, desde 5 años es 1% por año.
    private fun obtenerPorcentajeAntiguedad(anios: Int): Int = when {
        anios <= 0 -> 0
        anios in 1..4 -> 4
        else -> anios
    }

    // Redondea a centavos para que los renglones del recibo sumen bien.
    private fun centavos(valor: Double): Double = (valor * 100).roundToLong() / 100.0

    // Calcula las dos quincenas de un mes. En 'estadoDias' la posición es el día del mes.
    fun calcularMes(
        categoria: String,
        antiguedadAnios: Int,
        mes: Int,
        anio: Int,
        estadoDias: IntArray,
        horasExtraQ1: Int,
        horasExtraQ2: Int,
        apercibimientoQ1: Boolean,
        apercibimientoQ2: Boolean
    ): Pair<ResultadoQuincena, ResultadoQuincena> {
        val escala = requireNotNull(ESCALAS[categoria]) { "Categoría desconocida: $categoria" }
        val pctAntiguedad = obtenerPorcentajeAntiguedad(antiguedadAnios)

        val ultimoDiaMes = Fechas.ultimoDiaDelMes(anio, mes)
        require(estadoDias.size > ultimoDiaMes) { "estadoDias debe tener un lugar por cada día del mes" }

        // La asistencia perfecta se cobra en la 2° quincena si en todo el mes no hubo
        // faltas injustificadas ni apercibimientos.
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
            correspondePremioPerfecto = false // en la 1° quincena nunca se cobra
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

    // Calcula una quincena: recorre los días, suma las horas y después arma premios, antigüedad y descuentos.
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
        // Los domingos no se marcan ni se pagan.
        val diasLaborales = rangoDias.filter { !Fechas.esDomingo(anio, mes, it) }

        var horasNormales = 0
        var horasSabado100 = 0
        var horasFeriado = 0
        var diasConAsistencia = 0
        var faltasInjustificadas = 0

        for (dia in diasLaborales) {
            val sabado = Fechas.esSabado(anio, mes, dia)
            when (estadoDias[dia]) {
                // Feriado trabajado: 8 horas al valor simple y otras 8 al 100%, por eso se paga doble.
                ESTADO_FERIADO -> {
                    horasFeriado += HORAS_JORNADA
                    diasConAsistencia++
                }
                // Falta justificada: se paga la jornada simple, sin el 100% del sábado ni viáticos.
                ESTADO_JUSTIFICADA -> {
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

        // En 'horas al 100%' entran: 2 horas por cada sábado, las horas extra y las horas del feriado.
        val totalHoras100 = horasSabado100 + horasExtraManuales + horasFeriado

        // Las horas normales y las de feriado se pagan al valor simple; las del 100% al doble.
        val valorHorasNormales = horasNormales * escala.valorHora
        val valorHorasFeriado = horasFeriado * escala.valorHora
        val valorHoras100 =
            ((horasSabado100 + horasExtraManuales) * (escala.valorHora * 2.0)) + (horasFeriado * escala.valorHora)

        // La falta justificada no quita premios. El apercibimiento quita puntualidad y asistencia perfecta.
        val premioAsistencia = if (faltasInjustificadas == 0) escala.premioAsistencia else 0.0
        val premioPuntualidad =
            if (faltasInjustificadas == 0 && !tuvoApercibimiento) escala.premioPuntualidad else 0.0
        val premioPerfecto = if (correspondePremioPerfecto) escala.premioPerfecto else 0.0

        // La antigüedad se calcula sobre las horas normales, las del 100% y las de feriado.
        val retribucionBasica = valorHorasNormales + valorHoras100 + valorHorasFeriado
        val antiguedadValor = centavos(retribucionBasica * (pctAntiguedad / 100.0))

        val remSujetas = centavos(
            retribucionBasica + premioAsistencia + premioPuntualidad + premioPerfecto + antiguedadValor
        )

        // Viáticos y refrigerio: un importe por cada día que se fue a trabajar. No llevan descuentos.
        val viaticosValor = diasConAsistencia * escala.viaticoDia
        val refrigerioValor = diasConAsistencia * escala.refrigerioDia
        val remExentas = centavos(viaticosValor + refrigerioValor)

        // Los descuentos se calculan solo sobre lo remunerativo y cada uno se redondea.
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
