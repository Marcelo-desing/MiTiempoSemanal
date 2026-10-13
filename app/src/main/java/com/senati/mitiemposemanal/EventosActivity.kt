package com.senati.mitiemposemanal

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EventosActivity : AppCompatActivity() {

    private lateinit var dbManager: DatabaseManager
    private lateinit var actvTareaNombre: AutoCompleteTextView
    private lateinit var tvEventoActual: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnFinalizar: Button
    private lateinit var lvEventos: ListView

    private var eventoEnCurso: Evento? = null
    // Formato exacto de tiempo exigido por la rúbrica para cálculos futuros
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eventos)

        dbManager = DatabaseManager(this)
        actvTareaNombre = findViewById(R.id.actvTareaNombre)
        tvEventoActual = findViewById(R.id.tvEventoActual)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnFinalizar = findViewById(R.id.btnFinalizar)
        lvEventos = findViewById(R.id.lvEventos)

        // Cargamos los datos al abrir la pantalla
        actualizarHistorial()
        verificarEventoActivo() // HU-05: Si cierras y abres la app, esto detecta si hay algo corriendo
        configurarBuscador()

        btnIniciar.setOnClickListener {
            val textoEscrito = actvTareaNombre.text.toString().trim()

            if (textoEscrito.isEmpty()) {
                Toast.makeText(this, "Escribe el nombre de una tarea", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificamos si la tarea ya existe en la base de datos
            val tareaExiste = dbManager.obtenerTareaPorNombreExacto(textoEscrito)

            if (tareaExiste != null) {
                // Si existe, iniciamos el evento directamente
                registrarInicio(tareaExiste.id)
            } else {
                // HU-04: Si no existe, lanzamos la alerta para crearla sin cambiar de pantalla
                mostrarDialogoCrearTarea(textoEscrito)
            }
        }

        btnFinalizar.setOnClickListener {
            finalizarEventoActual()
        }
    }

    private fun configurarBuscador() {
        // HU-03: Alimentamos el buscador con las tareas. El XML ya se encarga de buscar a partir de 2 letras.
        val listaCompleta = dbManager.obtenerTareasConCategoria().map { it.split("\n")[0] }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listaCompleta)
        actvTareaNombre.setAdapter(adapter)

        // HU-03: Al elegir una tarea de la lista, mostramos su categoría en un mensaje Toast
        actvTareaNombre.setOnItemClickListener { _, _, position, _ ->
            val seleccion = adapter.getItem(position) ?: ""
            val partes = seleccion.split(" - ")
            if (partes.size > 1) {
                val nombre = partes[0]
                val categoria = partes[1]

                // Limpiamos el texto para dejar solo el nombre y que el botón Iniciar funcione bien
                actvTareaNombre.setText(nombre)
                actvTareaNombre.setSelection(nombre.length) // Pone el cursor al final
                Toast.makeText(this, "Categoría: $categoria", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoCrearTarea(nombreTarea: String) {
        val categorias = dbManager.obtenerCategorias()
        if (categorias.isEmpty()) {
            Toast.makeText(this, "No hay categorías disponibles. Crea una en la pantalla principal.", Toast.LENGTH_LONG).show()
            return
        }

        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Nueva Tarea")
            .setMessage("La tarea '$nombreTarea' no existe. Elige su categoría para crearla:")
            .setView(spinner)
            .setPositiveButton("Crear e Iniciar") { _, _ ->
                val categoriaSel = spinner.selectedItem as Categoria

                // Creamos la tarea (le pasamos strings vacíos a la fecha/hora del Sprint 1)
                val exito = dbManager.agregarTarea(nombreTarea, categoriaSel.id, "", "", "")
                if (exito) {
                    Toast.makeText(this, "Tarea creada con éxito", Toast.LENGTH_SHORT).show()
                    val nuevaTarea = dbManager.obtenerTareaPorNombreExacto(nombreTarea)
                    nuevaTarea?.let { registrarInicio(it.id) }
                    configurarBuscador() // Refrescamos el buscador con la nueva tarea
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarInicio(tareaId: Int) {
        // HU-05: Guardamos la fecha y hora completa del celular usando LocalDateTime
        val ahora = LocalDateTime.now().format(formatter)
        dbManager.iniciarEvento(tareaId, ahora)
        actvTareaNombre.text.clear()
        verificarEventoActivo() // Bloquea los botones automáticamente
        Toast.makeText(this, "Evento iniciado", Toast.LENGTH_SHORT).show()
    }

    private fun finalizarEventoActual() {
        eventoEnCurso?.let { evento ->
            val ahora = LocalDateTime.now()
            val stringFin = ahora.format(formatter)

            // HU-05: Parseamos el inicio para calcular cuánto tiempo pasó (Duration)
            val inicioDate = LocalDateTime.parse(evento.inicio, formatter)
            val duracionMilis = Duration.between(inicioDate, ahora).toMillis()

            // Matemática clásica para convertir milisegundos en formato Horas:Minutos:Segundos
            val segundosTotales = duracionMilis / 1000
            val horas = segundosTotales / 3600
            val minutos = (segundosTotales % 3600) / 60
            val segundos = segundosTotales % 60
            val stringDuracion = String.format("%02d:%02d:%02d", horas, minutos, segundos)

            dbManager.finalizarEvento(evento.id, stringFin, stringDuracion)
            Toast.makeText(this, "Terminado. Tiempo invertido: $stringDuracion", Toast.LENGTH_LONG).show()

            verificarEventoActivo() // Libera los botones de nuevo
            actualizarHistorial()   // Actualiza la lista de abajo
        }
    }

    private fun verificarEventoActivo() {
        eventoEnCurso = dbManager.obtenerEventoEnCurso()

        // HU-05: Solo puede haber un evento a la vez. Controlamos la interfaz gráfica:
        if (eventoEnCurso != null) {
            tvEventoActual.text = "🔴 EN CURSO: ${eventoEnCurso?.nombreTarea}\n🕒 Inicio: ${eventoEnCurso?.inicio}"
            btnIniciar.isEnabled = false
            btnFinalizar.isEnabled = true
            actvTareaNombre.isEnabled = false // Bloqueamos el buscador mientras trabajas
        } else {
            tvEventoActual.text = "No hay ningún evento en curso."
            btnIniciar.isEnabled = true
            btnFinalizar.isEnabled = false
            actvTareaNombre.isEnabled = true
        }
    }

    private fun actualizarHistorial() {
        val historial = dbManager.obtenerHistorialEventos()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historial)
        lvEventos.adapter = adapter
    }
}