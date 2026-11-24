package com.example.juntavecinal.model.repository

import com.example.juntavecinal.data.remote.CuotaPendiente
import com.example.juntavecinal.data.remote.DatosMovimiento
import com.example.juntavecinal.data.remote.DatosPago
import com.example.juntavecinal.data.remote.FinanzasApi
import com.example.juntavecinal.data.remote.Movimiento
import com.example.juntavecinal.data.remote.Pagina
import com.example.juntavecinal.data.remote.Resumen
import com.example.juntavecinal.data.remote.Sesion
import java.time.YearMonth

data class DatosFinanzas(
    val resumen: Resumen,
    val movimientos: List<Movimiento>,
    val cuotasPendientes: List<CuotaPendiente>
)

class FinanzasRepository(private val api: FinanzasApi) {
    suspend fun consultar(sesion: Sesion, mes: YearMonth): DatosFinanzas {
        val auth = sesion.autorizacion
        val desde = mes.atDay(1).toString()
        val hasta = mes.atEndOfMonth().toString()
        val resumen = api.resumen(auth, desde, hasta)
        val movimientos = paginas { api.movimientos(auth, desde, hasta, it) }
        val cuotas = if (sesion.usuario.rol.gestionaFinanzas) paginas { api.cuotasPendientes(auth, it) } else emptyList()
        return DatosFinanzas(resumen, movimientos, cuotas)
    }

    suspend fun guardar(sesion: Sesion, datos: DatosMovimiento) = api.crear(sesion.autorizacion, datos)

    suspend fun registrarPago(sesion: Sesion, datos: DatosPago) = api.registrarPago(sesion.autorizacion, datos)

    private suspend fun <T> paginas(cargar: suspend (Int) -> Pagina<T>): List<T> = buildList {
        var numero = 0
        do {
            val pagina = cargar(numero++)
            addAll(pagina.contenido)
        } while (numero < pagina.totalPaginas)
    }
}
