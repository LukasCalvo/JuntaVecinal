package com.example.juntavecinal.model.repository

import com.example.juntavecinal.data.remote.AuthApi
import com.example.juntavecinal.data.remote.Credenciales
import com.example.juntavecinal.data.remote.Perfil
import com.example.juntavecinal.data.remote.Registro
import com.example.juntavecinal.data.remote.Sesion

class UsuarioRepository(private val api: AuthApi) {
    suspend fun login(rut: String, password: String): Sesion {
        val acceso = api.login(Credenciales(rut, password))
        val nombre = acceso.nombre?.takeIf { it.isNotBlank() } ?: acceso.rut ?: rut
        return Sesion(acceso.accessToken, Perfil(acceso.usuarioId, nombre, acceso.rol))
    }

    suspend fun registrar(datos: Registro): Sesion {
        api.registrar(datos)
        return login(datos.rut, datos.password)
    }
}
