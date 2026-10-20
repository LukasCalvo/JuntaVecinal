package com.example.juntavecinal.model.repository

import com.example.juntavecinal.util.Rut
import com.example.juntavecinal.model.dao.UsuarioDao
import com.example.juntavecinal.model.entity.Usuario
import java.security.MessageDigest
import java.security.SecureRandom

class UsuarioRepository(private val dao: UsuarioDao) {
    /** Devuelve el usuario si RUT y clave son correctos, o null si no. */
    suspend fun login(rut: String, password: String): Usuario? {
        val usuario = dao.buscarPorRut(normalizarRut(rut)) ?: return null
        val hashIngresado = hash(password, usuario.salt)
        return if (MessageDigest.isEqual(
                hashIngresado.toByteArray(),
                usuario.passwordHash.toByteArray()
            )
        ) usuario else null
    }

    /** Crea un usuario con la clave cifrada. Devuelve false si el RUT ya existe. */
    suspend fun crearUsuario(
        rut: String,
        nombre: String,
        telefono: String,
        region: String,
        comuna: String,
        password: String
    ): Boolean {
        val salt = generarSalt()
        val usuario = Usuario(
            rut = normalizarRut(rut),
            nombre = nombre,
            telefono = telefono,
            region = region,
            comuna = comuna,
            passwordHash = hash(password, salt),
            salt = salt
        )
        return dao.insertar(usuario) != -1L
    }

    suspend fun sembrarUsuariosDePrueba() {
        if (dao.contar() == 0) {
            crearUsuario(
                "11.111.111-1", "Presidente Junta", "9 1111 1111",
                "Región Metropolitana", "Santiago", "admin123"
            )
            crearUsuario(
                "22.222.222-2", "Vecino Demo", "9 2222 2222",
                "Región Metropolitana", "Ñuñoa", "vecino123"
            )
        }
    }

    private fun normalizarRut(rut: String): String = Rut.normalizar(rut)

    private fun generarSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    private fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((salt + password).toByteArray()).toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}