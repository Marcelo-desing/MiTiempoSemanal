package com.senati.mitiemposemanal

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class EventosActivity : AppCompatActivity() {

    private lateinit var dbManager: DatabaseManager
    private lateinit var actvTareaNombre: AutoCompleteTextView
    private lateinit var tvEventoActual: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnFinalizar: Button
    private lateinit var lvEventos: ListView

    // Contenedores del Panel del Sprint 3
    private lateinit var tvRangoSemana: TextView
    private lateinit var llPanelEstadisticas: LinearLayout

    private var eventoEnCurso: Evento? = null
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateSoloFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eventos)

        // 1. PRIMERO debe estar esta línea obligatoriamente:
        dbManager = DatabaseManager(this)

        // 2. DESPUÉS pegas el código nuevo:
        val hoy = LocalDate.now()
        dbManager.generarReportesPendientes(obtenerRangoSemana(hoy))

        // 3. Y luego sigue lo que ya tenías:
        actvTareaNombre = findViewById(R.id.actvTareaNombre)
        tvEventoActual = findViewById(R.id.tvEventoActual)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnFinalizar = findViewById(R.id.btnFinalizar)
        lvEventos = findViewById(R.id.lvEventos)
        tvRangoSemana = findViewById(R.id.tvRangoSemana)
        llPanelEstadisticas = findViewById(R.id.llPanelEstadisticas)

        actualizarHistorial()
        verificarEventoActivo()
        configurarBuscador()
        actualizarPanelEstadisticas() // HU-06: Cargamos el panel al iniciar

        btnIniciar.setOnClickListener {
            val textoEscrito = actvTareaNombre.text.toString().trim()
            if (textoEscrito.isEmpty()) return@setOnClickListener

            val tareaExiste = dbManager.obtenerTareaPorNombreExacto(textoEscrito)
            if (tareaExiste != null) {
                registrarInicio(tareaExiste.id)
            } else {
                mostrarDialogoCrearTarea(textoEscrito)
            }
        }

        btnFinalizar.setOnClickListener {
            finalizarEventoActual()
        }
    }

    // ==========================================
    //           PANEL SPRINT 3 (HU-06)
    // ==========================================

    private fun obtenerRangoSemana(fecha: LocalDate): String {
        // Regla: La semana va de lunes a domingo
        val lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val domingo = fecha.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return "${lunes.format(dateSoloFormatter)}-${domingo.format(dateSoloFormatter)}"
    }

    private fun actualizarPanelEstadisticas() {
        llPanelEstadisticas.removeAllViews() // Vaciamos el contenedor

        val hoy = LocalDate.now()
        val semanaStr = obtenerRangoSemana(hoy)
        tvRangoSemana.text = "Semana $semanaStr"

        // Obtenemos los minutos agrupados desde SQL
        val datosSemana = dbManager.obtenerMinutosPorCategoriaEnSemana(semanaStr)
        val categoriasTodas = dbManager.obtenerCategorias()

        var minutosRegistradosTotales = 0

        val colores = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"), Color.parseColor("#FF9800"), Color.parseColor("#9C27B0"), Color.parseColor("#E91E63"))

        // Creamos una fila por CADA categoría que exista (incluso si está en 0)
        categoriasTodas.forEachIndexed { index, categoria ->
            val minsCategoria = datosSemana[categoria.nombre] ?: 0
            minutosRegistradosTotales += minsCategoria

            val horasDec = minsCategoria / 60.0
            val pct = (minsCategoria / 10080.0) * 100.0 // Fórmula de la rúbrica

            crearFilaGrafica(categoria.nombre, horasDec, pct, colores[index % colores.size])
        }

        // Fila adicional "Sin registrar" exigida en la rúbrica
        val minutosSinRegistrar = 10080 - minutosRegistradosTotales
        val horasSinReg = minutosSinRegistrar / 60.0
        val pctSinReg = (minutosSinRegistrar / 10080.0) * 100.0

        crearFilaGrafica("Sin registrar", horasSinReg, pctSinReg, Color.parseColor("#B0BEC5"))
    }

    private fun crearFilaGrafica(nombre: String, horas: Double, porcentaje: Double, color: Int) {
        // Creamos el texto de la fila (Ej: "Estudio: 21.0h (12.5%)")
        val tv = TextView(this).apply {
            text = String.format(Locale.US, "%s: %.1fh (%.1f%%)", nombre, horas, porcentaje)
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 8, 0, 4)
        }

        // Creamos la barra de progreso
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000 // Multiplicamos por 10 para soportar un decimal en la barra (12.5% = 125)
            progress = (porcentaje * 10).toInt()
            progressTintList = ColorStateList.valueOf(color)
        }

        llPanelEstadisticas.addView(tv)
        llPanelEstadisticas.addView(progressBar)
    }

    // ==========================================
    //           EVENTOS (SPRINT 2 y 3)
    // ==========================================

    private fun configurarBuscador() {
        val listaCompleta = dbManager.obtenerTareasConCategoria().map { it.split("\n")[0] }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listaCompleta)
        actvTareaNombre.setAdapter(adapter)

        actvTareaNombre.setOnItemClickListener { _, _, position, _ ->
            val seleccion = adapter.getItem(position) ?: ""
            val partes = seleccion.split(" - ")
            if (partes.size > 1) {
                actvTareaNombre.setText(partes[0])
                actvTareaNombre.setSelection(partes[0].length)
                Toast.makeText(this, "Categoría: ${partes[1]}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoCrearTarea(nombreTarea: String) {
        val categorias = dbManager.obtenerCategorias()
        if (categorias.isEmpty()) return

        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Nueva Tarea")
            .setMessage("La tarea '$nombreTarea' no existe. Elige su categoría:")
            .setView(spinner)
            .setPositiveButton("Crear e Iniciar") { _, _ ->
                val categoriaSel = spinner.selectedItem as Categoria
                if (dbManager.agregarTarea(nombreTarea, categoriaSel.id, "", "", "")) {
                    dbManager.obtenerTareaPorNombreExacto(nombreTarea)?.let { registrarInicio(it.id) }
                    configurarBuscador()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarInicio(tareaId: Int) {
        val ahora = LocalDateTime.now().format(formatter)
        dbManager.iniciarEvento(tareaId, ahora)
        actvTareaNombre.text.clear()
        verificarEventoActivo()
    }

    private fun finalizarEventoActual() {
        eventoEnCurso?.let { evento ->
            val ahora = LocalDateTime.now()
            val stringFin = ahora.format(formatter)

            val inicioDate = LocalDateTime.parse(evento.inicio, formatter)
            val duracionMilis = Duration.between(inicioDate, ahora).toMillis()
            val duracionMinutosReales = (duracionMilis / 60000).toInt() // Para SQL y el Sprint 3

            val segundosTotales = duracionMilis / 1000
            val horas = segundosTotales / 3600
            val minutos = (segundosTotales % 3600) / 60
            val segundos = segundosTotales % 60
            val stringDuracion = String.format(Locale.US, "%02d:%02d:%02d", horas, minutos, segundos)

            // Guardamos el evento usando la semana del INICIO (como dice la rúbrica)
            val semanaDelEvento = obtenerRangoSemana(inicioDate.toLocalDate())

            dbManager.finalizarEvento(evento.id, stringFin, stringDuracion, semanaDelEvento, duracionMinutosReales)

            verificarEventoActivo()
            actualizarHistorial()
            actualizarPanelEstadisticas() // HU-06: El panel se actualiza de inmediato
        }
    }

    private fun verificarEventoActivo() {
        eventoEnCurso = dbManager.obtenerEventoEnCurso()
        if (eventoEnCurso != null) {
            tvEventoActual.text = "🔴 EN CURSO: ${eventoEnCurso?.nombreTarea}\n🕒 Inicio: ${eventoEnCurso?.inicio}"
            btnIniciar.isEnabled = false
            btnFinalizar.isEnabled = true
            actvTareaNombre.isEnabled = false
        } else {
            tvEventoActual.text = "No hay ningún evento en curso."
            btnIniciar.isEnabled = true
            btnFinalizar.isEnabled = false
            actvTareaNombre.isEnabled = true
        }
    }

    private fun actualizarHistorial() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dbManager.obtenerHistorialEventos())
        lvEventos.adapter = adapter
    }
}