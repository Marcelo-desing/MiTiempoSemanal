package com.senati.mitiemposemanal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MiTiempoSemanal.db"
        // ¡Subimos la versión a 2 para que Android aplique los cambios!
        private const val DATABASE_VERSION = 2

        const val TABLE_CATEGORIES = "categorias"
        const val COLUMN_CATEGORY_ID = "id"
        const val COLUMN_CATEGORY_NAME = "nombre"

        const val TABLE_TASKS = "tareas"
        const val COLUMN_TASK_ID = "id"
        const val COLUMN_TASK_NAME = "nombre"
        const val COLUMN_TASK_CATEGORY_ID = "categoria_id"
        // Nuevas columnas para fecha y hora
        const val COLUMN_TASK_DATE = "fecha"
        const val COLUMN_TASK_TIME_START = "hora_inicio"
        const val COLUMN_TASK_TIME_END = "hora_fin"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createCategoriesTable = ("CREATE TABLE $TABLE_CATEGORIES ("
                + "$COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_CATEGORY_NAME TEXT UNIQUE NOT NULL)")
        db.execSQL(createCategoriesTable)

        // Tabla de tareas actualizada con fecha y horas
        val createTasksTable = ("CREATE TABLE $TABLE_TASKS ("
                + "$COLUMN_TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_TASK_NAME TEXT UNIQUE NOT NULL, "
                + "$COLUMN_TASK_CATEGORY_ID INTEGER NOT NULL, "
                + "$COLUMN_TASK_DATE TEXT, "
                + "$COLUMN_TASK_TIME_START TEXT, "
                + "$COLUMN_TASK_TIME_END TEXT, "
                + "FOREIGN KEY($COLUMN_TASK_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_CATEGORY_ID))")
        db.execSQL(createTasksTable)

        insertarCategoriasPorDefecto(db)
    }

    private fun insertarCategoriasPorDefecto(db: SQLiteDatabase) {
        val categoriasDefecto = arrayOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (categoria in categoriasDefecto) {
            val values = ContentValues()
            values.put(COLUMN_CATEGORY_NAME, categoria)
            db.insert(TABLE_CATEGORIES, null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Al detectar que la versión pasó de 1 a 2, borra las tablas viejas y crea las nuevas
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        onCreate(db)
    }
}