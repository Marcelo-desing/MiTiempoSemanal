package com.senati.mitiemposemanal

data class Tarea(
    var id: Int = 0,
    var nombre: String,
    var categoriaId: Int,
    var fecha: String = "",       // Ej: 12/10/2026
    var horaInicio: String = "",  // Ej: 19:00
    var horaFin: String = ""      // Ej: 21:00
)