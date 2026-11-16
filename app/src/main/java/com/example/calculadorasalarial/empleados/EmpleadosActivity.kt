package com.example.calculadorasalarial.empleados

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.calculadorasalarial.R
import com.example.calculadorasalarial.databinding.ActivityEmpleadosBinding

// Aloja la sección Empleados: una barra arriba (Toolbar) y, debajo, un contenedor donde
// Navigation Component cambia entre la lista, el alta y la modificación.
class EmpleadosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmpleadosBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmpleadosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBordesDelSistema()

        setSupportActionBar(binding.toolbar)

        // El NavHostFragment es el contenedor que muestra la pantalla actual del nav_empleados.
        val navHost = supportFragmentManager.findFragmentById(R.id.contenedorEmpleados) as NavHostFragment
        navController = navHost.navController

        // La barra muestra el título de cada pantalla y la flecha para volver.
        NavigationUI.setupActionBarWithNavController(this, navController)
    }

    // Flecha de volver de la barra: vuelve a la pantalla anterior de la navegación.
    override fun onSupportNavigateUp(): Boolean = navController.navigateUp() || super.onSupportNavigateUp()

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
