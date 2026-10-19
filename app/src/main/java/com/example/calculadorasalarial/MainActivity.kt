package com.example.calculadorasalarial

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculadorasalarial.CalculadoraActivity
import com.example.calculadorasalarial.R

class MainActivity : AppCompatActivity() {

    private val EMAIL_PRUEBA = "prueba@mail.com"
    private val PASSWORD_PRUEBA = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pref = getSharedPreferences("calculadora_pref", MODE_PRIVATE)
        if (pref.getBoolean("mantener_sesion",false)) {
            irCalculadora()
            return
        }

        val etEmail = findViewById<EditText>(R.id.etEmailLogin)
        val etPass = findViewById<EditText>(R.id.etPasswordLogin)
        val cbMantener = findViewById<CheckBox>(R.id.cbMantenerSesion)

        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnIrARegistro = findViewById<Button>(R.id.btnIrARegistro)

        // 1. Al tocar Ingresar, va a la Calculadora
        btnIngresar.setOnClickListener {
            val intent = Intent(this, CalculadoraActivity::class.java)
            //startActivity(intent)

            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (email == EMAIL_PRUEBA && pass == PASSWORD_PRUEBA) {
                pref.edit().putBoolean("mantener_sesion", cbMantener.isChecked).apply()
                irCalculadora()
            } else {
                Toast.makeText(this, "Email o contraseña incorrecto", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Al tocar Regístrate, abre la pantalla de Registro
        btnIrARegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }
    private fun irCalculadora(){
        val intent = Intent(this, CalculadoraActivity::class.java)
        intent.putExtra("CATEGORIA", "COMPLETAR_CON_UNA_CATEGORIA_VALIDA")
        startActivity(intent)
        finish()
    }
}