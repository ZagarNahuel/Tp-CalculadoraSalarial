package com.example.calculadorasalarial

import java.util.Calendar

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
    val antiguedadPct: Double,
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

    // Escalas por categoría (coinciden exactamente con tu strings.xml)
    private val VALORES_DICIEMBRE = mapOf(
        "CATEGORIA INICIAL" to EscalaCategoria(3873.0, 34187.0, 35710.0, 50409.0, 2276.0, 1652.0),
        "CATEGORIA 1" to EscalaCategoria(4314.0, 35397.0, 36926.0, 52266.0, 2276.0, 1652.0),
        "CATEGORIA 2" to EscalaCategoria(4506.0, 36267.0, 40075.0, 54291.0, 2276.0, 1652.0),
        "CATEGORIA 3" to EscalaCategoria(4715.0, 37078.0, 40984.0, 55792.0, 2276.0, 1652.0),
        "CATEGORIA 4" to EscalaCategoria(4920.0, 37943.0, 42433.0, 57405.0, 2276.0, 1652.0)
    )

    // Porcentajes de descuentos de ley y sindicales
    private const val DESC_JUBILACION = 0.109961
    private const val DESC_19032 = 0.029985
    private const val DESC_23660 = 0.029985
    private const val DESC_TURISMO = 0.009985
    private const val DESC_SOLIDARIO = 0.019970

    // Feriados nacionales (Mes 1..12, Día)
    private val FERIADOS = setOf(
        Pair(1, 1), Pair(3, 24), Pair(4, 2), Pair(4, 3),
        Pair(5, 1), Pair(5, 25), Pair(6, 17), Pair(6, 20),
        Pair(7, 9), Pair(8, 17), Pair(9, 22),
        Pair(10, 12), Pair(11, 20),
        Pair(12, 8), Pair(12, 25)
    )

    // Regla de antigüedad: 0 años = 0% | 1 a 4 años = 4% | >4 años = 1% adicional por año
    private fun obtenerPorcentajeAntiguedad(anios: Int): Double {
        return when {
            anios <= 0 -> 0.0
            anios in 1..4 -> 0.04
            else -> anios * 0.01
        }
    }

    private fun obtenerDiaSemana(dia: Int, mes: Int, anio: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(anio, mes - 1, dia) // En Calendar de Android, Enero es 0
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    private fun esDomingo(dia: Int, mes: Int, anio: Int): Boolean {
        return obtenerDiaSemana(dia, mes, anio) == Calendar.SUNDAY
    }

    private fun esSabado(dia: Int, mes: Int, anio: Int): Boolean {
        return obtenerDiaSemana(dia, mes, anio) == Calendar.SATURDAY
    }

    private fun esFeriadoLaboral(dia: Int, mes: Int, anio: Int): Boolean {
        return FERIADOS.contains(Pair(mes, dia)) && !esDomingo(dia, mes, anio)
    }

    fun calcularMes(
        categoria: String,
        antiguedadAnios: Int,
        mes: Int, // 1 a 12
        anio: Int,
        estadoDias: IntArray, // 0 = Blanco (Trabajado), 1 = Rojo (Justificada), 2 = Negro (Injustificada)
        horasExtraQ1: Int,
        horasExtraQ2: Int,
        apercibimientoQ1: Boolean,
        apercibimientoQ2: Boolean
    ): Pair<ResultadoQuincena, ResultadoQuincena> {

        val escala = VALORES_DICIEMBRE[categoria] ?: VALORES_DICIEMBRE["CATEGORIA INICIAL"]!!
        val pctAntiguedad = obtenerPorcentajeAntiguedad(antiguedadAnios)

        val cal = Calendar.getInstance()
        cal.set(anio, mes - 1, 1)
        val ultimoDiaMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Evaluamos faltas injustificadas (estado == 2) en todo el mes para el Premio Asistencia Perfecta
        val faltasInjustQ1 = (1..15).count { !esDomingo(it, mes, anio) && estadoDias[it] == 2 }
        val faltasInjustQ2 = (16..ultimoDiaMes).count { !esDomingo(it, mes, anio) && estadoDias[it] == 2 }

        val ganaPremioPerfectoMes = (faltasInjustQ1 == 0 && faltasInjustQ2 == 0 && !apercibimientoQ1 && !apercibimientoQ2)

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

    private fun calcularQuincena(
        rangoDias: IntRange,
        mes: Int,
        anio: Int,
        escala: EscalaCategoria,
        pctAntiguedad: Double,
        estadoDias: IntArray,
        horasExtraManuales: Int,
        tuvoApercibimiento: Boolean,
        correspondePremioPerfecto: Boolean
    ): ResultadoQuincena {

        val diasLaborales = rangoDias.filter { !esDomingo(it, mes, anio) }

        // Días efectivamente trabajados (Blanco = estado 0)
        val diasEfectivos = diasLaborales.filter { estadoDias[it] == 0 }

        // Faltas Injustificadas (Negro = estado 2)
        val faltasInjustificadas = diasLaborales.count { estadoDias[it] == 2 }

        var horasNormales = 0
        var horasSabado100 = 0
        var horasFeriado = 0

        for (dia in diasEfectivos) {
            when {
                esFeriadoLaboral(dia, mes, anio) -> {
                    // Se descuenta de Horas Normales (porque no entra al 'else')
                    // Va a Horas Feriado y también suma a Horas 100%
                    horasFeriado += 8
                }
                esSabado(dia, mes, anio) -> {
                    horasNormales += 6
                    horasSabado100 += 2
                }
                else -> {
                    horasNormales += 8
                }
            }
        }

        // Horas 100% agrupa: 2 hs por sábado trabajado + horas extra de la quincena + las horas del feriado trabajado
        val totalHoras100 = horasSabado100 + horasExtraManuales + horasFeriado

        // CÁLCULO MONETARIO (Forma A):
        // - Horas normales: valor simple
        // - Horas feriado: valor simple (pago base del día feriado)
        // - Horas 100%: sábado y horas extra al doble (2.0), y las del feriado suman el 100% restante (1.0)
        val valorHorasNormales = horasNormales * escala.valorHora
        val valorHorasFeriado = horasFeriado * escala.valorHora
        val valorHoras100 = ((horasSabado100 + horasExtraManuales) * (escala.valorHora * 2.0)) + (horasFeriado * escala.valorHora)

        // Premios: Falta justificada (Rojo) NO quita premios. Apercibimiento quita Puntualidad y Perfecto.
        val premioAsistencia = if (faltasInjustificadas == 0) escala.premioAsistencia else 0.0
        val premioPuntualidad = if (faltasInjustificadas == 0 && !tuvoApercibimiento) escala.premioPuntualidad else 0.0
        val premioPerfecto = if (correspondePremioPerfecto) escala.premioPerfecto else 0.0

        // Antigüedad sobre retribución básica (Horas normales + Horas 100% + Horas feriado)
        val retribucionBasica = valorHorasNormales + valorHoras100 + valorHorasFeriado
        val antiguedadValor = retribucionBasica * pctAntiguedad

        // Total Remuneraciones Sujetas a retención
        val remSujetas = retribucionBasica + premioAsistencia + premioPuntualidad + premioPerfecto + antiguedadValor

        // Conceptos No Remunerativos (Exentos): por días efectivos trabajados
        val cantDiasEfectivos = diasEfectivos.size
        val viaticosValor = cantDiasEfectivos * escala.viaticoDia
        val refrigerioValor = cantDiasEfectivos * escala.refrigerioDia
        val remExentas = viaticosValor + refrigerioValor

        // Descuentos de ley y sindicales
        val descJub = remSujetas * DESC_JUBILACION
        val desc19032 = remSujetas * DESC_19032
        val desc23660 = remSujetas * DESC_23660
        val descTur = remSujetas * DESC_TURISMO
        val descSol = remSujetas * DESC_SOLIDARIO
        val totalDescuentos = descJub + desc19032 + desc23660 + descTur + descSol

        val totalNeto = remSujetas + remExentas - totalDescuentos

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
            antiguedadPct = pctAntiguedad * 100,
            antiguedadValor = antiguedadValor,
            viaticosDias = cantDiasEfectivos,
            viaticosValor = viaticosValor,
            refrigerioDias = cantDiasEfectivos,
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