package com.senati.mitiemposemanal

data class Categoria(
    var id: Int = 0,
    var nombre: String
) {
    // Este truco hace que cuando mostremos la categoría en una lista desplegable,
    // solo se lea el nombre (ej. "Estudio") en lugar de un código raro.
    override fun toString(): String {
        return nombre
    }
}