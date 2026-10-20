package com.example.juntavecinal.viewmodel

class Sesion {
    companion object {
        var usuarioId: Int = 0
        var nombre: String = ""

        fun iniciar(id: Int, nombreUsuario: String) {
            usuarioId = id
            nombre = nombreUsuario
        }

        fun cerrar() {
            usuarioId = 0
            nombre = ""
        }
    }
}
