package com.example.juntavecinal.data.remote

import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.io.IOException
import retrofit2.HttpException

class ErrorApi(val codigo: Int, message: String) : Exception(message)

internal suspend fun <T> peticion(accion: suspend () -> T): Result<T> = try {
    Result.success(accion())
} catch (error: HttpException) {
    val codigo = error.code()
    val detalle = runCatching {
        error.response()?.errorBody()?.use { cuerpo ->
            val json = JsonParser.parseString(cuerpo.string()).asJsonObject
            sequenceOf("message", "detail").mapNotNull { campo ->
                json.get(campo)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf(String::isNotBlank)
            }.firstOrNull()
        }
    }.getOrNull()
    Result.failure(ErrorApi(codigo, mensajeHttp(codigo, detalle)))
} catch (_: IOException) {
    Result.failure(ErrorApi(0, "No se pudo conectar. Revisa la conexión e inténtalo de nuevo."))
} catch (_: JsonParseException) {
    Result.failure(ErrorApi(0, "El servicio devolvió una respuesta inválida."))
}

internal fun mensajeHttp(codigo: Int, detalle: String?): String = when (codigo) {
    401 -> "Tu sesión venció. Vuelve a iniciar sesión."
    403 -> "Tu cuenta no tiene permiso para esta acción."
    404 -> detalle ?: "El registro ya no está disponible."
    409 -> detalle ?: "La operación entra en conflicto con un registro existente."
    in 500..599 -> "El servicio no está disponible en este momento."
    else -> detalle ?: "Revisa los datos e inténtalo de nuevo."
}
