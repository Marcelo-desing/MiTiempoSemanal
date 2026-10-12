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
}