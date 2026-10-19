package com.example.calculadorasalarial

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.calculadorasalarial.CalculadoraActivity
import com.example.calculadorasalarial.R

class RegistroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // 1. Vincular elementos del XML
        val spCategoria = findViewById<Spinner>(R.id.spCategoriaRegistro)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val tvVolverALogin = findViewById<Button>(R.id.tvVolverALogin)

        // 2. Al tocar "Crear cuenta", pasamos a la Calculadora enviando la categoría elegida
        btnRegistrar.setOnClickListener {
            val categoriaElegida = spCategoria.selectedItem.toString()
            val intent = Intent(this, CalculadoraActivity::class.java)
            intent.putExtra("CATEGORIA", categoriaElegida)
            startActivity(intent)
        }

        // 3. Volver al Login
        tvVolverALogin.setOnClickListener {
            finish()
        }
    }
}