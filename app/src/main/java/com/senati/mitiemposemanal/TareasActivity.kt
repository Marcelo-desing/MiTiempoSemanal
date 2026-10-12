package com.senati.mitiemposemanal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class TareasActivity : AppCompatActivity() {

    private lateinit var dbManager: DatabaseManager
    private lateinit var etTareaNombre: EditText
    private lateinit var spinnerCategorias: Spinner
    private lateinit var lvTareas: ListView

    // Declaramos los nuevos botones
    private lateinit var btnSeleccionarFecha: Button
    private lateinit var btnHoraInicio: Button
    private lateinit var btnHoraFin: Button

    private var tareaSeleccionadaNombre: String? = null
    private val listaTareas = mutableListOf<String>()
    private lateinit var adapterTareas: ArrayAdapter<String>
    private lateinit var adapterSpinner: ArrayAdapter<Categoria>

    // Variables para guardar la información seleccionada
    private var fechaSeleccionada = ""
    private var horaInicioSeleccionada = ""
    private var horaFinSeleccionada = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tareas)

        dbManager = DatabaseManager(this)
        etTareaNombre = findViewById(R.id.etTareaNombre)
        spinnerCategorias = findViewById(R.id.spinnerCategorias)
        lvTareas = findViewById(R.id.lvTareas)

        val btnGuardar = findViewById<Button>(R.id.btnGuardarTarea)
        val btnEditar = findViewById<Button>(R.id.btnEditarTarea)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarTarea)

        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha)
        btnHoraInicio = findViewById(R.id.btnHoraInicio)
        btnHoraFin = findViewById(R.id.btnHoraFin)

        val categorias = dbManager.obtenerCategorias()
        adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategorias.adapter = adapterSpinner

        adapterTareas = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaTareas)
        lvTareas.adapter = adapterTareas
        actualizarListaTareas()

        // Lógica del Calendario
        btnSeleccionarFecha.setOnClickListener {
            val calendario = Calendar.getInstance()
            val year = calendario.get(Calendar.YEAR)
            val month = calendario.get(Calendar.MONTH)
            val day = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, anio, mes, dia ->
                val mesReal = mes + 1 // Los meses en Java empiezan en 0
                fechaSeleccionada = String.format("%02d/%02d/%d", dia, mesReal, anio)
                btnSeleccionarFecha.text = fechaSeleccionada
            }, year, month, day)
            datePicker.show()
        }

        // Lógica del Reloj (Inicio)
        btnHoraInicio.setOnClickListener {
            val calendario = Calendar.getInstance()
            val hour = calendario.get(Calendar.HOUR_OF_DAY)
            val minute = calendario.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(this, { _, hora, minuto ->
                horaInicioSeleccionada = String.format("%02d:%02d", hora, minuto)
                btnHoraInicio.text = horaInicioSeleccionada
            }, hour, minute, true) // "true" es para formato 24h
            timePicker.show()
        }

        // Lógica del Reloj (Fin)
        btnHoraFin.setOnClickListener {
            val calendario = Calendar.getInstance()
            val hour = calendario.get(Calendar.HOUR_OF_DAY)
            val minute = calendario.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(this, { _, hora, minuto ->
                horaFinSeleccionada = String.format("%02d:%02d", hora, minuto)
                btnHoraFin.text = horaFinSeleccionada
            }, hour, minute, true)
            timePicker.show()
        }

        // Seleccionar de la lista
        lvTareas.setOnItemClickListener { _, _, position, _ ->
            val itemCompleto = listaTareas[position]
            tareaSeleccionadaNombre = itemCompleto.split(" - ")[0]
            etTareaNombre.setText(tareaSeleccionadaNombre)

            // Reseteamos las selecciones previas para evitar confusiones
            fechaSeleccionada = ""
            horaInicioSeleccionada = ""
            horaFinSeleccionada = ""
            btnSeleccionarFecha.text = "Fecha"
            btnHoraInicio.text = "Inicio"
            btnHoraFin.text = "Fin"
        }

        // Guardar Tarea (con los nuevos datos)
        btnGuardar.setOnClickListener {
            val nombre = etTareaNombre.text.toString().trim()
            val categoriaSeleccionada = spinnerCategorias.selectedItem as Categoria?

            if (nombre.isEmpty() || categoriaSeleccionada == null) {
                Toast.makeText(this, "Nombre y categoría son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val exito = dbManager.agregarTarea(nombre, categoriaSeleccionada.id, fechaSeleccionada, horaInicioSeleccionada, horaFinSeleccionada)
            if (exito) {
                Toast.makeText(this, "Tarea guardada", Toast.LENGTH_SHORT).show()
                limpiarCampos()
                actualizarListaTareas()
            } else {
                Toast.makeText(this, "Esa tarea ya existe", Toast.LENGTH_SHORT).show()
            }
        }

        // Editar Tarea
        btnEditar.setOnClickListener {
            val nombreActual = tareaSeleccionadaNombre
            val nuevoNombre = etTareaNombre.text.toString().trim()
            val categoriaSeleccionada = spinnerCategorias.selectedItem as Categoria?

            if (nombreActual == null || nuevoNombre.isEmpty() || categoriaSeleccionada == null) {
                Toast.makeText(this, "Selecciona una tarea válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val exito = dbManager.editarTarea(nombreActual, nuevoNombre, categoriaSeleccionada.id, fechaSeleccionada, horaInicioSeleccionada, horaFinSeleccionada)
            if (exito) {
                Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                limpiarCampos()
                actualizarListaTareas()
            } else {
                Toast.makeText(this, "Error al editar", Toast.LENGTH_SHORT).show()
            }
        }

        // Eliminar Tarea
        btnEliminar.setOnClickListener {
            val nombreActual = tareaSeleccionadaNombre
            if (nombreActual == null) {
                Toast.makeText(this, "Selecciona una tarea de la lista primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val exito = dbManager.eliminarTarea(nombreActual)
            if (exito) {
                Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                limpiarCampos()
                actualizarListaTareas()
            }
        }
    }

    private fun actualizarListaTareas() {
        listaTareas.clear()
        listaTareas.addAll(dbManager.obtenerTareasConCategoria())
        adapterTareas.notifyDataSetChanged()
    }

    private fun limpiarCampos() {
        etTareaNombre.text.clear()
        tareaSeleccionadaNombre = null
        fechaSeleccionada = ""
        horaInicioSeleccionada = ""
        horaFinSeleccionada = ""
        btnSeleccionarFecha.text = "Fecha"
        btnHoraInicio.text = "Inicio"
        btnHoraFin.text = "Fin"
    }
    private fun programarAlarma(fecha: String, horaInicio: String, nombreTarea: String) {
        if (fecha.isEmpty() || horaInicio.isEmpty()) return

        try {
            // Separamos la fecha (DD/MM/YYYY) y la hora (HH:MM)
            val partesFecha = fecha.split("/")
            val partesHora = horaInicio.split(":")

            if (partesFecha.size == 3 && partesHora.size == 2) {
                val calendario = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, partesFecha[0].toInt())
                    set(java.util.Calendar.MONTH, partesFecha[1].toInt() - 1) // Los meses van de 0 a 11
                    set(java.util.Calendar.YEAR, partesFecha[2].toInt())
                    set(java.util.Calendar.HOUR_OF_DAY, partesHora[0].toInt())
                    set(java.util.Calendar.MINUTE, partesHora[1].toInt())
                    set(java.util.Calendar.SECOND, 0)
                }

                // Si la hora programada ya pasó, no hacemos nada
                if (calendario.timeInMillis <= System.currentTimeMillis()) return

                val intent = android.content.Intent(this, AlarmReceiver::class.java).apply {
                    putExtra("EXTRA_TAREA_NOMBRE", nombreTarea)
                }

                // Creamos el PendingIntent que se ejecutará en el futuro
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    this,
                    nombreTarea.hashCode(), // ID único basado en el nombre de la tarea
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager

                // Programamos la alarma exacta
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendario.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}