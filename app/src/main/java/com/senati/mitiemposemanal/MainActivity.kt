package com.senati.mitiemposemanal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnIrCategorias = findViewById<Button>(R.id.btnIrCategorias)
        val btnIrTareas = findViewById<Button>(R.id.btnIrTareas)
        val btnIrEventos = findViewById<Button>(R.id.btnIrEventos) // Nuevo botón

        btnIrCategorias.setOnClickListener {
            val intent = Intent(this, CategoriasActivity::class.java)
            startActivity(intent)
        }

        btnIrTareas.setOnClickListener {
            val intent = Intent(this, TareasActivity::class.java)
            startActivity(intent)
        }

        // Lógica para abrir la pantalla de eventos
        btnIrEventos.setOnClickListener {
            val intent = Intent(this, EventosActivity::class.java)
            startActivity(intent)
        }
    }
}