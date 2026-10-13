package com.senati.mitiemposemanal

data class Evento(
    var id: Int = 0,
    var tareaId: Int,
    var nombreTarea: String = "", // Lo guardamos para mostrarlo fácil en la lista
    var categoriaTarea: String = "", // También la categoría
    var inicio: String, // Fecha y hora completa (ej. "2026-10-12 18:30:00")
    var fin: String? = null,
    var duracion: String? = null
)