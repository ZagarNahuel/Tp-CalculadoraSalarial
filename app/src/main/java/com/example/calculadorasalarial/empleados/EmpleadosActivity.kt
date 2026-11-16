package com.example.calculadorasalarial.empleados

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.calculadorasalarial.R
import com.example.calculadorasalarial.databinding.ActivityEmpleadosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Aloja la sección Empleados: una barra arriba (Toolbar) y, debajo, un contenedor donde
// Navigation Component cambia entre la lista, el alta y la modificación.
class EmpleadosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmpleadosBinding
    private lateinit var navController: NavController
    private lateinit var viewModel: EmpleadoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmpleadosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBordesDelSistema()

        setSupportActionBar(binding.toolbar)

        // Es el mismo ViewModel que usan los fragments, porque todos lo piden a esta Activity.
        viewModel = ViewModelProvider(this)[EmpleadoViewModel::class.java]

        // El NavHostFragment es el contenedor que muestra la pantalla actual del nav_empleados.
        val navHost = supportFragmentManager.findFragmentById(R.id.contenedorEmpleados) as NavHostFragment
        navController = navHost.navController

        // La barra muestra el título de cada pantalla y la flecha para volver.
        NavigationUI.setupActionBarWithNavController(this, navController)

        // Cada vez que cambia la pantalla se vuelve a armar el menú (ver onPrepareOptionsMenu).
        navController.addOnDestinationChangedListener { _, _, _ -> invalidateOptionsMenu() }
    }

    // Flecha de volver de la barra: vuelve a la pantalla anterior de la navegación.
    override fun onSupportNavigateUp(): Boolean = navController.navigateUp() || super.onSupportNavigateUp()

    // Arma el menú de los tres puntitos a partir de res/menu/menu_empleados.xml.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_empleados, menu)
        return true
    }

    // "Eliminar todos" solo se ofrece en el listado; en el alta o la modificación no tiene sentido.
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val enElListado = navController.currentDestination?.id == R.id.listaEmpleadosFragment
        menu.findItem(R.id.accion_eliminar_todos)?.isVisible = enElListado
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.accion_eliminar_todos -> {
            confirmarEliminarTodos()
            true
        }
        R.id.accion_volver_calculadora -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun confirmarEliminarTodos() {
        // Si ya se sabe que la lista está vacía, no hace falta preguntar nada.
        if (viewModel.empleados.value?.isEmpty() == true) {
            Toast.makeText(this, R.string.emp_no_hay_empleados, Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.emp_eliminar_todos_titulo)
            .setMessage(R.string.emp_eliminar_todos_mensaje)
            .setNegativeButton(R.string.emp_cancelar, null)
            .setPositiveButton(R.string.emp_eliminar_todos_confirmar) { _, _ ->
                viewModel.eliminarTodos()
                Toast.makeText(this, R.string.emp_eliminados_todos, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // Evita que la barra de estado, la de abajo o el teclado tapen el contenido.
    private fun ajustarBordesDelSistema() {
        WindowCompat.getInsetsController(window, binding.raizEmpleados).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.raizEmpleados) { _, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.encabezadoEmpleados.setPadding(barras.left, barras.top, barras.right, 0)
            binding.contenedorEmpleados.setPadding(
                barras.left, 0, barras.right, maxOf(barras.bottom, teclado.bottom)
            )
            insets
        }
    }
}
