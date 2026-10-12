package com.senati.mitiemposemanal

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class CategoriasActivity : AppCompatActivity() {

    private lateinit var dbManager: DatabaseManager
    private lateinit var etCategoriaNombre: EditText
    private lateinit var lvCategorias: ListView
    private var categoriaSeleccionada: Categoria? = null
    private lateinit var adapter: ArrayAdapter<Categoria>
    private val listaCategorias = mutableListOf<Categoria>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categorias)

        dbManager = DatabaseManager(this)
        etCategoriaNombre = findViewById(R.id.etCategoriaNombre)
        lvCategorias = findViewById(R.id.lvCategorias)

        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnEditar = findViewById<Button>(R.id.btnEditar)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)

        // Configurar la lista
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaCategorias)
        lvCategorias.adapter = adapter
        actualizarLista()

        // Seleccionar un elemento de la lista al tocarlo
        lvCategorias.setOnItemClickListener { _, _, position, _ ->
            categoriaSeleccionada = listaCategorias[position]
            etCategoriaNombre.setText(categoriaSeleccionada?.nombre)
        }

        // Botón Guardar
        btnGuardar.setOnClickListener {
            val nombre = etCategoriaNombre.text.toString().trim()
            if (nombre.isEmpty()) {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val exito = dbManager.agregarCategoria(nombre)
            if (exito) {
                Toast.makeText(this, "Categoría guardada", Toast.LENGTH_SHORT).show()
                etCategoriaNombre.text.clear()
                actualizarLista()
            } else {
                Toast.makeText(this, "Error: Esa categoría ya existe", Toast.LENGTH_LONG).show()
            }
        }

        // Botón Editar
        btnEditar.setOnClickListener {
            val categoria = categoriaSeleccionada
            val nuevoNombre = etCategoriaNombre.text.toString().trim()

            if (categoria == null) {
                Toast.makeText(this, "Selecciona una categoría de la lista primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (nuevoNombre.isEmpty()) {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val exito = dbManager.editarCategoria(categoria.id, nuevoNombre)
            if (exito) {
                Toast.makeText(this, "Categoría actualizada", Toast.LENGTH_SHORT).show()
                etCategoriaNombre.text.clear()
                categoriaSeleccionada = null
                actualizarLista()
            } else {
                Toast.makeText(this, "Error al editar (posible nombre repetido)", Toast.LENGTH_LONG).show()
            }
        }

        // Botón Eliminar con confirmación
        btnEliminar.setOnClickListener {
            val categoria = categoriaSeleccionada
            if (categoria == null) {
                Toast.makeText(this, "Selecciona una categoría de la lista primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar la categoría '${categoria.nombre}'?")
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    val exito = dbManager.eliminarCategoria(categoria.id)
                    if (exito) {
                        Toast.makeText(this, "Categoría eliminada", Toast.LENGTH_SHORT).show()
                        etCategoriaNombre.text.clear()
                        categoriaSeleccionada = null
                        actualizarLista()
                    } else {
                        Toast.makeText(this, "No se puede eliminar: La categoría tiene tareas asignadas", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun actualizarLista() {
        listaCategorias.clear()
        listaCategorias.addAll(dbManager.obtenerCategorias())
        adapter.notifyDataSetChanged()
    }
}