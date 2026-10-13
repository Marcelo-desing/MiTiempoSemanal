package com.senati.mitiemposemanal

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ReportesActivity : AppCompatActivity() {

    private lateinit var dbManager: DatabaseManager
    private lateinit var spinnerSemanas: Spinner
    private lateinit var tvSinReportes: TextView
    private lateinit var llPanelEstadisticas: LinearLayout
    private lateinit var lvEventosReporte: ListView
    private lateinit var cvPanelReporte: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        dbManager = DatabaseManager(this)
        spinnerSemanas = findViewById(R.id.spinnerSemanas)
        tvSinReportes = findViewById(R.id.tvSinReportes)
        llPanelEstadisticas = findViewById(R.id.llPanelEstadisticas)
        lvEventosReporte = findViewById(R.id.lvEventosReporte)
        cvPanelReporte = findViewById(R.id.cvPanelReporte)

        configurarPantalla()
    }

    private fun configurarPantalla() {
        val semanas = dbManager.obtenerSemanasConReportes()

        if (semanas.isEmpty()) {
            // HU-08: Si no hay reportes, mostramos el texto rojo y ocultamos lo demás
            tvSinReportes.visibility = View.VISIBLE
            spinnerSemanas.visibility = View.GONE
            cvPanelReporte.visibility = View.GONE
            lvEventosReporte.visibility = View.GONE
        } else {
            tvSinReportes.visibility = View.GONE
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, semanas)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSemanas.adapter = adapter

            // Al seleccionar una semana, cargamos sus datos dinámicos
            spinnerSemanas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    cargarDatosDeSemana(semanas[position])
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private fun cargarDatosDeSemana(semana: String) {
        llPanelEstadisticas.removeAllViews()
        val reporte = dbManager.obtenerReporteDeSemana(semana)

        var horasRegistradasTotales = 0.0
        val colores = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"), Color.parseColor("#FF9800"), Color.parseColor("#9C27B0"), Color.parseColor("#E91E63"))

        reporte.forEachIndexed { index, fila ->
            horasRegistradasTotales += fila.horas
            crearFilaGrafica(fila.categoria, fila.horas, fila.porcentaje, colores[index % colores.size])
        }

        // HU-08: Calculamos las horas sin registrar basándonos en el límite de 168 horas
        val horasSinReg = 168.0 - horasRegistradasTotales
        val pctSinReg = (horasSinReg / 168.0) * 100.0
        crearFilaGrafica("Sin registrar", horasSinReg, pctSinReg, Color.parseColor("#B0BEC5"))

        // Cargamos la lista de eventos que ocurrieron solo en esa semana
        val eventos = dbManager.obtenerEventosPorSemanaExacta(semana)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, eventos)
        lvEventosReporte.adapter = adapter
    }

    private fun crearFilaGrafica(nombre: String, horas: Double, porcentaje: Double, color: Int) {
        val tv = TextView(this).apply {
            text = String.format(Locale.US, "%s: %.1fh (%.1f%%)", nombre, horas, porcentaje)
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 8, 0, 4)
        }
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000 // Multiplicado por 10 para soportar decimales (ej: 12.5% = 125)
            progress = (porcentaje * 10).toInt()
            progressTintList = ColorStateList.valueOf(color)
        }
        llPanelEstadisticas.addView(tv)
        llPanelEstadisticas.addView(progressBar)
    }
}