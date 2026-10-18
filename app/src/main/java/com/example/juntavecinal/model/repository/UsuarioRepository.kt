package com.example.juntavecinal.model.repository

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
    suspend fun crearUsuario(rut: String, nombre: String, password: String): Boolean {
        val salt = generarSalt()
        val usuario = Usuario(
            rut = normalizarRut(rut),
            nombre = nombre,
            passwordHash = hash(password, salt),
            salt = salt
        )
        return dao.insertar(usuario) != -1L
    }

    /** Crea usuarios de prueba solo si la tabla está vacía. */
    suspend fun sembrarUsuariosDePrueba() {
        if (dao.contar() == 0) {
            crearUsuario("12345678-5", "Presidente Junta", "admin123")
            crearUsuario("98765432-1", "Vecino Demo", "vecino123")
        }
    }

    private fun normalizarRut(rut: String): String =
        rut.replace(".", "").replace(" ", "").uppercase()

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