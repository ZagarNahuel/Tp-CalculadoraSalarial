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
[15:42, 19/10/2026] +54 9 11 2388-8341: <?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:app="http://schemas.android.com/apk/res-auto"
xmlns:tools="http://schemas.android.com/tools"
android:id="@+id/RegistroActivity"
android:layout_width="match_parent"
android:layout_height="match_parent"
android:gravity="center_horizontal"
android:orientation="vertical"
tools:context=".RegistroActivity">

<!--  Franja azul superior decorativa  -->
<View
android:layout_width="match_parent"
android:layout_height="80dp"
android:background="#005CE6" />


<TextView
android:id="@+id/tv.Registro"
android:layout_width="wrap_content"
android:layout_height="wrap_co…
[15:42, 19/10/2026] +54 9 11 2388-8341: xml
[15:50, 19/10/2026] +54 9 11 2388-8341: class RegistroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.RegistroActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

            val etNombreyapellido = findViewById<EditText>(R.id.etNombreyapellido)
            val etEmail = findViewById<EditText>(R.id.etEmail)
            val etUsuario = findViewById<EditText>(R.id.etUsuario)
            val etContrasena = findViewById<EditText>(R.id.etContrasena)
            val etRepetirContrasena = findViewById<EditText>(R.id.etRepetirContrasena)
            val spLista_Categoria = findViewById<Spinner>(R.id.spLista_Categoria)
            val btnCrearUsuario = findViewById<Button>(R.id.btnCrearUsuario)
            val spAntiguedad = findViewById<Spinner>(R.id.spAntiguedad)
            val tvVolverALogin = findViewById<Button>(R.id.tvVolverALogin)

            //opciones de la lista desplegable
            val Lista_Categoria = arrayOf(
                "Seleccionar Categoría", "CATEGORIA INICIAL", "CATEGORIA 1", "CATEGORIA 2",
                "CATEGORIA 3", "CATEGORIA 4"
            )

            val adaptador = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                Lista_Categoria

            )

            adaptador.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )

            Lista_Categoria.adapter = adaptador

            //opciones de la lista desplegable
            val lista_meses = arrayOf(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio","Agosto",
                "Septiembre", "Octubre", "Noviembre", "Diciembre"
            )

            val adaptador2 = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                lista_meses

            )

            adaptador.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )

            spAntiguedad.adapter = adaptador

            // Acción del botón "Crear Usuario"
            btnCrearUsuario.setOnClickListener {
                val nombre = etNombreyapellido.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val usuario = etUsuario.text.toString().trim()
                val contrasena = etContrasena.text.toString()
                val repetirContrasena = etRepetirContrasena.text.toString()

                when {
                    nombre.isEmpty() -> {
                        etNombreyapellido.error = "Ingresá tu nombre y apellido"
                        etNombreyapellido.requestFocus()
                    }

                    email.isEmpty() ||
                            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        etEmail.error = "Ingresá un e-mail válido"
                        etEmail.requestFocus()
                    }

                    usuario.isEmpty() -> {
                        etUsuario.error = "Ingresá un nombre de usuario"
                        etUsuario.requestFocus()
                    }

                    contrasena.length < 8 -> {
                        etContrasena.error =
                            "La contraseña debe tener al menos 8 caracteres"
                        etContrasena.requestFocus()
                    }

                    contrasena != repetirContrasena -> {
                        etRepetirContrasena.error =
                            "Las contraseñas no coinciden"
                        etRepetirContrasena.requestFocus()
                    }

                    spLista_Categoria.selectedItemPosition == 0 -> {
                        Toast.makeText(
                            this,
                            "Seleccioná una categoría",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    antiguedadTex.isEmpty() -> {
                        etAntiguedad.error =
                            "Ingresá la antigüedad en años"
                        etAntiguedad.requestFocus()
                    }

                    else -> {
                        val categoria = spLista_Categoria.selectedItem.toString()
                        val antiguedad = antiguedadTexto.toIntOrNull()

                        if (antiguedad == null) {
                            etAntiguedad.error =
                                "Ingresá una cantidad válida de años"

                        } else {
                            Toast.makeText(
                                this,
                                "Formulario validado correctamente",
                                Toast.LENGTH_LONG
                            ).show()

                            // Acá podés guardar el usuario
                            // en una base de datos.
                        }
                    }
                }
            }
        }
    }

// 2. Al tocar "Crear cuenta", pasamos a la Calculadora enviando la categoría elegida
    btnRegistrar.setOnClickListener
    {
        val categoriaElegida = spCategoria.selectedItem.toString()
        val intent = Intent(this, CalculadoraActivity::class.java)
        intent.putExtra("CATEGORIA", categoriaElegida)
        startActivity(intent)
    }

// 3. Volver al Login
    tvVolverALogin.setOnClickListener
    {
        finish()
    }
}