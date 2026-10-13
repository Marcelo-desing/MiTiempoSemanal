package com.senati.mitiemposemanal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MiTiempoSemanal.db"
        // ¡Subimos la versión a 3 para agregar Eventos sin borrar lo anterior!
        private const val DATABASE_VERSION = 3

        const val TABLE_CATEGORIES = "categorias"
        const val COLUMN_CATEGORY_ID = "id"
        const val COLUMN_CATEGORY_NAME = "nombre"

        const val TABLE_TASKS = "tareas"
        const val COLUMN_TASK_ID = "id"
        const val COLUMN_TASK_NAME = "nombre"
        const val COLUMN_TASK_CATEGORY_ID = "categoria_id"
        const val COLUMN_TASK_DATE = "fecha"
        const val COLUMN_TASK_TIME_START = "hora_inicio"
        const val COLUMN_TASK_TIME_END = "hora_fin"

        // NUEVA TABLA: Eventos
        const val TABLE_EVENTS = "eventos"
        const val COLUMN_EVENT_ID = "id"
        const val COLUMN_EVENT_TASK_ID = "tarea_id"
        const val COLUMN_EVENT_START = "inicio"
        const val COLUMN_EVENT_END = "fin"
        const val COLUMN_EVENT_DURATION = "duracion"
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

        val createTasksTable = ("CREATE TABLE $TABLE_TASKS ("
                + "$COLUMN_TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_TASK_NAME TEXT UNIQUE NOT NULL, "
                + "$COLUMN_TASK_CATEGORY_ID INTEGER NOT NULL, "
                + "$COLUMN_TASK_DATE TEXT, "
                + "$COLUMN_TASK_TIME_START TEXT, "
                + "$COLUMN_TASK_TIME_END TEXT, "
                + "FOREIGN KEY($COLUMN_TASK_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_CATEGORY_ID))")
        db.execSQL(createTasksTable)

        // Creamos la tabla de eventos por si alguien instala la app desde cero
        crearTablaEventos(db)

        insertarCategoriasPorDefecto(db)
    }

    private fun crearTablaEventos(db: SQLiteDatabase) {
        val createEventsTable = ("CREATE TABLE IF NOT EXISTS $TABLE_EVENTS ("
                + "$COLUMN_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_EVENT_TASK_ID INTEGER NOT NULL, "
                + "$COLUMN_EVENT_START TEXT, "
                + "$COLUMN_EVENT_END TEXT, "
                + "$COLUMN_EVENT_DURATION TEXT, "
                + "FOREIGN KEY($COLUMN_EVENT_TASK_ID) REFERENCES $TABLE_TASKS($COLUMN_TASK_ID))")
        db.execSQL(createEventsTable)
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
        // Si actualizamos desde la versión 2 a la 3, solo creamos la tabla Eventos
        // Así no perdemos los datos del Sprint 1
        if (oldVersion < 3) {
            crearTablaEventos(db)
        }
    }
}