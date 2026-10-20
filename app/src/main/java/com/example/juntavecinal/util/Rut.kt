package com.example.juntavecinal.util

class Rut {

    companion object {
        fun normalizar(rut: String): String {
            return rut.filter { it.isDigit() || it.uppercaseChar() == 'K' }.uppercase()
        }

        fun formatear(valor: String): String {
            val limpio = normalizar(valor).take(9)
            if (limpio.length <= 1) return limpio

            val cuerpo = limpio.dropLast(1).reversed().chunked(3).joinToString(".").reversed()
            return cuerpo + "-" + limpio.last()
        }


        fun esValido(rut: String): Boolean {
            val limpio = normalizar(rut)
            if (limpio.length < 8) return true
            if (!Regex("^\\d{7,8}[\\dK]$").matches(limpio)) return false

            val runSinDV = limpio.dropLast(1)
            val dv = limpio.last()
            val runInvertido = runSinDV.reversed()
            val serie = listOf(2, 3, 4, 5, 6, 7)
            val runLista = runInvertido.map { it.digitToInt() }

            var suma = 0
            for (i in runLista.indices) {
                val factor = serie[i % serie.size]
                suma += runLista[i] * factor
            }

            val resultado = 11 - (suma % 11)
            val dvEsperado = when (resultado) {
                11 -> '0'
                10 -> 'K'
                else -> resultado.digitToChar()
            }

            return dvEsperado == dv
        }
    }
}