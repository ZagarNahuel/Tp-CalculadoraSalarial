package com.example.calculadorasalarial

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/** Pantalla de Login: valida lo que se guardó en el Registro (SharedPreferences). */
class MainActivity : AppCompatActivity() {

    private lateinit var repositorio: RepositorioUsuarios
    private lateinit var etUsuario: EditText
    private lateinit var etPassword: EditText

    // El Registro devuelve el e-mail con el que se creó la cuenta para dejarlo escrito acá.
    private val lanzadorRegistro =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            if (resultado.resultCode == RESULT_OK) {
                val email = resultado.data?.getStringExtra(RegistroActivity.EXTRA_EMAIL_REGISTRADO).orEmpty()
                etUsuario.setText(email)
                etPassword.text.clear()
                etPassword.requestFocus()
                Toast.makeText(this, R.string.login_cuenta_creada, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repositorio = RepositorioUsuarios(this)

        // Si el usuario eligió "Mantener sesión", se salta el login.
        if (repositorio.haySesionGuardada()) {
            irCalculadora()
            return
        }
        // Sin "Mantener sesión" cada vez que se abre la app se empieza desde el login:
        // se borra cualquier sesión que haya quedado de la vez anterior.
        repositorio.cerrarSesion()

        setContentView(R.layout.activity_main)
        ajustarBordesDelSistema(findViewById(R.id.raizLogin), findViewById(R.id.bandaSuperior))

        etUsuario = findViewById(R.id.etEmailLogin)
        etPassword = findViewById(R.id.etPasswordLogin)
        val cbMantener = findViewById<CheckBox>(R.id.cbMantenerSesion)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnIrARegistro = findViewById<Button>(R.id.btnIrARegistro)

        configurarOjoContrasena(etPassword, findViewById(R.id.ivOjoPassword))

        btnIngresar.setOnClickListener {
            val identificador = etUsuario.text.toString().trim()
            val password = etPassword.text.toString()

            when {
                identificador.isEmpty() || password.isEmpty() ->
                    Toast.makeText(this, R.string.login_campos_vacios, Toast.LENGTH_SHORT).show()

                repositorio.iniciarSesion(identificador, password, cbMantener.isChecked) -> irCalculadora()

                else -> Toast.makeText(this, R.string.login_credenciales_invalidas, Toast.LENGTH_SHORT).show()
            }
        }

        btnIrARegistro.setOnClickListener {
            lanzadorRegistro.launch(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun irCalculadora() {
        startActivity(Intent(this, CalculadoraActivity::class.java))
        finish()
    }
}
