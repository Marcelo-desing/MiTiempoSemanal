package com.senati.mitiemposemanal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase

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

    // HU-05 y HU-06: Finalizar el evento guardando su semana y minutos para el cálculo
    fun finalizarEvento(eventoId: Int, fin: String, duracionTexto: String, semana: String, duracionMinutos: Int): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_EVENT_END, fin)
            put(DatabaseHelper.COLUMN_EVENT_DURATION, duracionTexto)
            put(DatabaseHelper.COLUMN_EVENT_WEEK, semana)
            put(DatabaseHelper.COLUMN_EVENT_DURATION_MINS, duracionMinutos)
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

    // ==========================================
    //        ESTADÍSTICAS (SPRINT 3)
    // ==========================================

    // HU-06: Obtener la suma total de minutos por categoría en una semana específica
    fun obtenerMinutosPorCategoriaEnSemana(semana: String): Map<String, Int> {
        val resultado = mutableMapOf<String, Int>()
        val db = dbHelper.readableDatabase

        // Consulta SQL con SUM y GROUP BY exigida en la rúbrica
        val query = """
            SELECT c.${DatabaseHelper.COLUMN_CATEGORY_NAME}, SUM(e.${DatabaseHelper.COLUMN_EVENT_DURATION_MINS}) 
            FROM ${DatabaseHelper.TABLE_EVENTS} e
            INNER JOIN ${DatabaseHelper.TABLE_TASKS} t ON e.${DatabaseHelper.COLUMN_EVENT_TASK_ID} = t.${DatabaseHelper.COLUMN_TASK_ID}
            INNER JOIN ${DatabaseHelper.TABLE_CATEGORIES} c ON t.${DatabaseHelper.COLUMN_TASK_CATEGORY_ID} = c.${DatabaseHelper.COLUMN_CATEGORY_ID}
            WHERE e.${DatabaseHelper.COLUMN_EVENT_WEEK} = ? AND e.${DatabaseHelper.COLUMN_EVENT_END} IS NOT NULL
            GROUP BY c.${DatabaseHelper.COLUMN_CATEGORY_NAME}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(semana))
        if (cursor.moveToFirst()) {
            do {
                val categoria = cursor.getString(0)
                val minutosTotales = cursor.getInt(1)
                resultado[categoria] = minutosTotales
            } while (cursor.moveToNext())
        }
        cursor.close()
        return resultado
    }

    // ==========================================
    //      CIERRE Y REPORTES (SPRINT 4)
    // ==========================================

    // Estructura para enviar los datos a la pantalla visual
    data class FilaReporte(val categoria: String, val horas: Double, val porcentaje: Double)

    // HU-07: Cerrar semanas automáticamente
    fun generarReportesPendientes(semanaActual: String) {
        val db = dbHelper.writableDatabase

        val semanasConEventos = mutableListOf<String>()
        // AQUÍ ESTÁ LA CORRECCIÓN: Ignoramos los eventos antiguos con semana nula
        val querySemanas = "SELECT DISTINCT ${DatabaseHelper.COLUMN_EVENT_WEEK} FROM ${DatabaseHelper.TABLE_EVENTS} WHERE ${DatabaseHelper.COLUMN_EVENT_END} IS NOT NULL AND ${DatabaseHelper.COLUMN_EVENT_WEEK} IS NOT NULL"
        val cursorSemanas = db.rawQuery(querySemanas, null)
        if (cursorSemanas.moveToFirst()) {
            do {
                semanasConEventos.add(cursorSemanas.getString(0))
            } while (cursorSemanas.moveToNext())
        }
        cursorSemanas.close()

        for (semana in semanasConEventos) {
            if (semana == semanaActual) continue

            val queryExiste = "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_WEEK} = ?"
            val cursorExiste = db.rawQuery(queryExiste, arrayOf(semana))
            cursorExiste.moveToFirst()
            val yaExiste = cursorExiste.getInt(0) > 0
            cursorExiste.close()

            if (!yaExiste) {
                val datosCategoria = obtenerMinutosPorCategoriaEnSemana(semana)
                val categoriasTodas = obtenerCategorias()

                for (categoria in categoriasTodas) {
                    val minutos = datosCategoria[categoria.nombre] ?: 0
                    if (minutos > 0) {
                        val horas = minutos / 60.0
                        val porcentaje = (minutos / 10080.0) * 100.0

                        val values = ContentValues().apply {
                            put(DatabaseHelper.COLUMN_REPORT_WEEK, semana)
                            put(DatabaseHelper.COLUMN_REPORT_CATEGORY_ID, categoria.id)
                            put(DatabaseHelper.COLUMN_REPORT_HOURS, horas)
                            put(DatabaseHelper.COLUMN_REPORT_PERCENTAGE, porcentaje)
                        }
                        db.insertWithOnConflict(DatabaseHelper.TABLE_REPORTS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                    }
                }
            }
        }
    }

    // HU-08: Obtener la lista de semanas con reporte para el Spinner
    fun obtenerSemanasConReportes(): List<String> {
        val lista = mutableListOf<String>()
        val db = dbHelper.readableDatabase
        // ORDER BY DESC para mostrar de la más reciente a la más antigua
        val query = "SELECT DISTINCT ${DatabaseHelper.COLUMN_REPORT_WEEK} FROM ${DatabaseHelper.TABLE_REPORTS} ORDER BY ${DatabaseHelper.COLUMN_REPORT_ID} DESC"
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                lista.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    // HU-08: Traer las estadísticas de una semana específica
    fun obtenerReporteDeSemana(semana: String): List<FilaReporte> {
        val lista = mutableListOf<FilaReporte>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT c.${DatabaseHelper.COLUMN_CATEGORY_NAME}, r.${DatabaseHelper.COLUMN_REPORT_HOURS}, r.${DatabaseHelper.COLUMN_REPORT_PERCENTAGE}
            FROM ${DatabaseHelper.TABLE_REPORTS} r
            INNER JOIN ${DatabaseHelper.TABLE_CATEGORIES} c ON r.${DatabaseHelper.COLUMN_REPORT_CATEGORY_ID} = c.${DatabaseHelper.COLUMN_CATEGORY_ID}
            WHERE r.${DatabaseHelper.COLUMN_REPORT_WEEK} = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(semana))
        if (cursor.moveToFirst()) {
            do {
                lista.add(FilaReporte(cursor.getString(0), cursor.getDouble(1), cursor.getDouble(2)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    // HU-08: Traer el historial de eventos que pertenecen solo a esa semana
    fun obtenerEventosPorSemanaExacta(semana: String): List<String> {
        val lista = mutableListOf<String>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT t.${DatabaseHelper.COLUMN_TASK_NAME}, c.${DatabaseHelper.COLUMN_CATEGORY_NAME},
                   e.${DatabaseHelper.COLUMN_EVENT_START}, e.${DatabaseHelper.COLUMN_EVENT_END}, e.${DatabaseHelper.COLUMN_EVENT_DURATION}
            FROM ${DatabaseHelper.TABLE_EVENTS} e
            INNER JOIN ${DatabaseHelper.TABLE_TASKS} t ON e.${DatabaseHelper.COLUMN_EVENT_TASK_ID} = t.${DatabaseHelper.COLUMN_TASK_ID}
            INNER JOIN ${DatabaseHelper.TABLE_CATEGORIES} c ON t.${DatabaseHelper.COLUMN_TASK_CATEGORY_ID} = c.${DatabaseHelper.COLUMN_CATEGORY_ID}
            WHERE e.${DatabaseHelper.COLUMN_EVENT_WEEK} = ? AND e.${DatabaseHelper.COLUMN_EVENT_END} IS NOT NULL
            ORDER BY e.${DatabaseHelper.COLUMN_EVENT_ID} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(semana))
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