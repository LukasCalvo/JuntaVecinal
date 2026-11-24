package com.example.juntavecinal.data.remote

import java.math.BigDecimal
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

enum class Rol {
    VECINO, DIRECTIVA, TESORERO, SECRETARIO;

    val consultaFinanzas: Boolean get() = this != VECINO
    val gestionaFinanzas: Boolean get() = this == DIRECTIVA || this == TESORERO
}

data class Perfil(val id: Long, val nombre: String, val rol: Rol)

data class Sesion(val token: String, val usuario: Perfil) {
    val autorizacion: String get() = "Bearer $token"
}

data class Credenciales(val rut: String, val password: String)

data class Registro(
    val rut: String,
    val nombre: String,
    val telefono: String,
    val region: String,
    val comuna: String,
    val password: String
)

data class Acceso(val usuarioId: Long, val rut: String?, val nombre: String?, val rol: Rol, val accessToken: String)

interface AuthApi {
    @POST("auth/register")
    suspend fun registrar(@Body datos: Registro)

    @POST("auth/login")
    suspend fun login(@Body datos: Credenciales): Acceso
}

data class SolicitudReserva(
    val usuarioId: Long,
    val nombre: String,
    val fecha: Long,
    val horaInicioMin: Int,
    val horaFinMin: Int,
    val asistentes: Int,
    val observaciones: String,
    val requiereProyector: Boolean,
    val aceptaReglamento: Boolean
)

data class ReservaMovil(
    val id: Long,
    val usuarioId: Long,
    val nombre: String?,
    val fecha: Long,
    val horaInicioMin: Int,
    val horaFinMin: Int,
    val asistentes: Int?,
    val observaciones: String?,
    val requiereProyector: Boolean,
    val aceptaReglamento: Boolean,
    val estado: String
)

data class HorarioReserva(
    val vecinoId: Long,
    val espacioId: Long,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String
)

data class DetalleReserva(
    val espacioId: Long,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: String
)

interface ReservasApi {
    @GET("reserva/reservas/app")
    suspend fun listar(@Header("Authorization") auth: String, @Query("usuarioId") usuarioId: Long): List<ReservaMovil>

    @POST("reserva/reservas/app")
    suspend fun crear(@Header("Authorization") auth: String, @Body datos: SolicitudReserva): ReservaMovil

    @GET("reserva/reservas/{id}")
    suspend fun detalle(@Header("Authorization") auth: String, @Path("id") id: Long): DetalleReserva

    @PUT("reserva/reservas/{id}")
    suspend fun editar(
        @Header("Authorization") auth: String, @Path("id") id: Long, @Body datos: HorarioReserva
    ): DetalleReserva

    @DELETE("reserva/reservas/{id}")
    suspend fun cancelar(@Header("Authorization") auth: String, @Path("id") id: Long)
}

data class Resumen(val ingresos: BigDecimal, val egresos: BigDecimal, val saldo: BigDecimal)
enum class TipoMovimiento(val etiqueta: String) { INGRESO("Ingreso"), EGRESO("Egreso") }

data class Movimiento(
    val id: Long,
    val tipo: TipoMovimiento,
    val categoria: String,
    val descripcion: String,
    val monto: BigDecimal,
    val fecha: String
)

data class DatosMovimiento(
    val tipo: TipoMovimiento,
    val categoria: String,
    val descripcion: String,
    val monto: BigDecimal,
    val fecha: String
)

data class CuotaPendiente(
    val id: Long,
    val vecinoId: Long,
    val cuotaNombre: String,
    val saldoPendiente: BigDecimal
)

data class DatosPago(val cuotaVecinoId: Long, val monto: BigDecimal, val metodoPago: String)
data class Pagina<T>(val contenido: List<T>, val totalPaginas: Int)

interface FinanzasApi {
    @GET("finanzas/reportes")
    suspend fun resumen(
        @Header("Authorization") auth: String, @Query("desde") desde: String, @Query("hasta") hasta: String
    ): Resumen

    @GET("finanzas/movimientos?size=50")
    suspend fun movimientos(
        @Header("Authorization") auth: String,
        @Query("desde") desde: String,
        @Query("hasta") hasta: String,
        @Query("page") pagina: Int
    ): Pagina<Movimiento>

    @POST("finanzas/movimientos")
    suspend fun crear(@Header("Authorization") auth: String, @Body datos: DatosMovimiento)

    @GET("finanzas/cuotas/asignaciones?estado=PENDIENTE&size=50")
    suspend fun cuotasPendientes(@Header("Authorization") auth: String, @Query("page") pagina: Int): Pagina<CuotaPendiente>

    @POST("finanzas/pagos")
    suspend fun registrarPago(@Header("Authorization") auth: String, @Body datos: DatosPago)
}

data class RespuestaFeriados(val data: List<FeriadoRemoto>?)
data class FeriadoRemoto(val date: String?, val title: String?, val scope: String?)

interface FeriadosApi {
    @GET("feriados.json")
    suspend fun listar(): RespuestaFeriados
}

object Api {
    private val gateway by lazy { conectar("http://10.0.2.2:8080/api/") }
    val auth: AuthApi by lazy { gateway.create(AuthApi::class.java) }
    val reservas: ReservasApi by lazy { gateway.create(ReservasApi::class.java) }
    val finanzas: FinanzasApi by lazy { gateway.create(FinanzasApi::class.java) }
    val feriados: FeriadosApi by lazy { conectar("https://api.roison.cl/").create(FeriadosApi::class.java) }

    private fun conectar(url: String): Retrofit = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
