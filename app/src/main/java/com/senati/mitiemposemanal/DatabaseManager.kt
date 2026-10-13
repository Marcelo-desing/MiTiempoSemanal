package com.senati.mitiemposemanal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException

class DatabaseManager(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    // ==========================================
    //                 CATEGORÍAS
    // ==========================================

    fun obtenerCategorias(): List<Categoria> {
        val lista = mutableListOf<Categoria>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_CATEGORIES}", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_NAME))
                lista.add(Categoria(id, nombre))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun agregarCategoria(nombre: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CATEGORY_NAME, nombre)
        }
        return try {
            // insertOrThrow lanza un error si el nombre ya existe (UNIQUE)
            db.insertOrThrow(DatabaseHelper.TABLE_CATEGORIES, null, values)
            true
        } catch (e: SQLiteConstraintException) {
            false // Devuelve falso para que le mostremos el mensaje al usuario
        }
    }

    fun editarCategoria(id: Int, nuevoNombre: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CATEGORY_NAME, nuevoNombre)
        }
        return try {
            db.update(DatabaseHelper.TABLE_CATEGORIES, values, "${DatabaseHelper.COLUMN_CATEGORY_ID} = ?", arrayOf(id.toString())) > 0
        } catch (e: SQLiteConstraintException) {
            false
        }
    }

    fun eliminarCategoria(id: Int): Boolean {
        val db = dbHelper.writableDatabase
        return try {
            // Esto fallará automáticamente si la categoría tiene tareas asociadas
            db.delete(DatabaseHelper.TABLE_CATEGORIES, "${DatabaseHelper.COLUMN_CATEGORY_ID} = ?", arrayOf(id.toString())) > 0
        } catch (e: SQLiteConstraintException) {
            false
        }
    }

    // ==========================================
    //                   TAREAS
    // ==========================================

    fun obtenerTareasConCategoria(): List<String> {
        val lista = mutableListOf<String>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT t.${DatabaseHelper.COLUMN_TASK_NAME}, c.${DatabaseHelper.COLUMN_CATEGORY_NAME},
                   t.${DatabaseHelper.COLUMN_TASK_DATE}, t.${DatabaseHelper.COLUMN_TASK_TIME_START}, t.${DatabaseHelper.COLUMN_TASK_TIME_END}
            FROM ${DatabaseHelper.TABLE_TASKS} t 
            INNER JOIN ${DatabaseHelper.TABLE_CATEGORIES} c 
            ON t.${DatabaseHelper.COLUMN_TASK_CATEGORY_ID} = c.${DatabaseHelper.COLUMN_CATEGORY_ID}
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val nombreTarea = cursor.getString(0)
                val nombreCategoria = cursor.getString(1)
                val fecha = cursor.getString(2) ?: ""
                val horaInicio = cursor.getString(3) ?: ""
                val horaFin = cursor.getString(4) ?: ""

                // Si la tarea tiene fecha, la mostramos en la lista. Ej: "Jugar futbol - Juegos (12/10/2026 19:00)"
                val infoExtra = if (fecha.isNotEmpty()) "\n📅 $fecha | ⏰ $horaInicio a $horaFin" else ""
                lista.add("$nombreTarea - $nombreCategoria$infoExtra")
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun agregarTarea(nombre: String, categoriaId: Int, fecha: String, horaInicio: String, horaFin: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_TASK_NAME, nombre)
            put(DatabaseHelper.COLUMN_TASK_CATEGORY_ID, categoriaId)
            put(DatabaseHelper.COLUMN_TASK_DATE, fecha)
            put(DatabaseHelper.COLUMN_TASK_TIME_START, horaInicio)
            put(DatabaseHelper.COLUMN_TASK_TIME_END, horaFin)
        }
        return try {
            db.insertOrThrow(DatabaseHelper.TABLE_TASKS, null, values)
            true
        } catch (e: SQLiteConstraintException) {
            false
        }
    }

    fun editarTarea(nombreActual: String, nuevoNombre: String, nuevaCategoriaId: Int, fecha: String, horaInicio: String, horaFin: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_TASK_NAME, nuevoNombre)
            put(DatabaseHelper.COLUMN_TASK_CATEGORY_ID, nuevaCategoriaId)
            put(DatabaseHelper.COLUMN_TASK_DATE, fecha)
            put(DatabaseHelper.COLUMN_TASK_TIME_START, horaInicio)
            put(DatabaseHelper.COLUMN_TASK_TIME_END, horaFin)
        }
        return try {
            db.update(DatabaseHelper.TABLE_TASKS, values, "${DatabaseHelper.COLUMN_TASK_NAME} = ?", arrayOf(nombreActual)) > 0
        } catch (e: SQLiteConstraintException) {
            false
        }
    }

    fun eliminarTarea(nombre: String): Boolean {
        val db = dbHelper.writableDatabase
        return db.delete(DatabaseHelper.TABLE_TASKS, "${DatabaseHelper.COLUMN_TASK_NAME} = ?", arrayOf(nombre)) > 0
    }
    // ==========================================
    //        EVENTOS Y BÚSQUEDA (SPRINT 2)
    // ==========================================

    // HU-03: Buscar tareas por coincidencia de texto
    fun buscarTareasPorNombre(texto: String): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_TASKS} WHERE ${DatabaseHelper.COLUMN_TASK_NAME} LIKE ? ORDER BY ${DatabaseHelper.COLUMN_TASK_NAME} ASC",
            arrayOf("%$texto%")
        )
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_NAME))
                val catId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_CATEGORY_ID))
                lista.add(Tarea(id = id, nombre = nombre, categoriaId = catId))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    // HU-04: Buscar tarea exacta para saber si ya existe antes de crearla
    fun obtenerTareaPorNombreExacto(nombre: String): Tarea? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_TASKS} WHERE ${DatabaseHelper.COLUMN_TASK_NAME} = ?",
            arrayOf(nombre)
        )
        var tarea: Tarea? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_ID))
            val catId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TASK_CATEGORY_ID))
            tarea = Tarea(id = id, nombre = nombre, categoriaId = catId)
        }
        cursor.close()
        return tarea
    }

    // HU-05: Iniciar un nuevo evento
    fun iniciarEvento(tareaId: Int, inicio: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_EVENT_TASK_ID, tareaId)
            put(DatabaseHelper.COLUMN_EVENT_START, inicio)
            // 'fin' y 'duracion' se quedan nulos automáticamente hasta que se finalice
        }
        return db.insert(DatabaseHelper.TABLE_EVENTS, null, values) != -1L
    }

    // HU-05: Saber si hay un evento activo (donde la fecha de fin es nula)
    fun obtenerEventoEnCurso(): Evento? {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT e.${DatabaseHelper.COLUMN_EVENT_ID}, e.${DatabaseHelper.COLUMN_EVENT_TASK_ID}, 
                   t.${DatabaseHelper.COLUMN_TASK_NAME}, c.${DatabaseHelper.COLUMN_CATEGORY_NAME}, 
                   e.${DatabaseHelper.COLUMN_EVENT_START}
            FROM ${DatabaseHelper.TABLE_EVENTS} e
            INNER JOIN ${DatabaseHelper.TABLE_TASKS} t ON e.${DatabaseHelper.COLUMN_EVENT_TASK_ID} = t.${DatabaseHelper.COLUMN_TASK_ID}
            INNER JOIN ${DatabaseHelper.TABLE_CATEGORIES} c ON t.${DatabaseHelper.COLUMN_TASK_CATEGORY_ID} = c.${DatabaseHelper.COLUMN_CATEGORY_ID}
            WHERE e.${DatabaseHelper.COLUMN_EVENT_END} IS NULL
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        var evento: Evento? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(0)
            val tareaId = cursor.getInt(1)
            val nombreTarea = cursor.getString(2)
            val nombreCategoria = cursor.getString(3)
            val inicio = cursor.getString(4)
            evento = Evento(id = id, tareaId = tareaId, nombreTarea = nombreTarea, categoriaTarea = nombreCategoria, inicio = inicio)
        }
        cursor.close()
        return evento
    }

    // HU-05: Finalizar el evento calculando y guardando su fin y duración
    fun finalizarEvento(eventoId: Int, fin: String, duracion: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_EVENT_END, fin)
            put(DatabaseHelper.COLUMN_EVENT_DURATION, duracion)
        }
        return db.update(DatabaseHelper.TABLE_EVENTS, values, "${DatabaseHelper.COLUMN_EVENT_ID} = ?", arrayOf(eventoId.toString())) > 0
    }

    // Mostrar la lista del historial de eventos completados
    fun obtenerHistorialEventos(): List<String> {
        val lista = mutableListOf<String>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT t.${DatabaseHelper.COLUMN_TASK_NAME}, c.${DatabaseHelper.COLUMN_CATEGORY_NAME}, 
                   e.${DatabaseHelper.COLUMN_EVENT_START}, e.${DatabaseHelper.COLUMN_EVENT_END}, e.${DatabaseHelper.COLUMN_EVENT_DURATION}
            FROM ${DatabaseHelper.TABLE_EVENTS} e
            INNER JOIN ${DatabaseHelper.TABLE_TASKS} t ON e.${DatabaseHelper.COLUMN_EVENT_TASK_ID} = t.${DatabaseHelper.COLUMN_TASK_ID}
            INNER JOIN ${DatabaseHelper.TABLE_CATEGORIES} c ON t.${DatabaseHelper.COLUMN_TASK_CATEGORY_ID} = c.${DatabaseHelper.COLUMN_CATEGORY_ID}
            WHERE e.${DatabaseHelper.COLUMN_EVENT_END} IS NOT NULL
            ORDER BY e.${DatabaseHelper.COLUMN_EVENT_ID} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val tarea = cursor.getString(0)
                val categoria = cursor.getString(1)
                val inicio = cursor.getString(2)
                val fin = cursor.getString(3)
                val dur = cursor.getString(4)
                lista.add("$tarea ($categoria)\n▶ $inicio  |  ⏹ $fin\n⏱ Duración: $dur")
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }
}