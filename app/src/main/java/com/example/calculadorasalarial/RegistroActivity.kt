package com.example.calculadorasalarial

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla de Registro: valida los datos y los guarda con [RepositorioUsuarios]
 * (SharedPreferences). La categoría y la antigüedad que se eligen acá son las que
 * después usan la Calculadora y el Recibo.
 */
class RegistroActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMAIL_REGISTRADO = "EMAIL_REGISTRADO"
        private const val LARGO_MINIMO_PASSWORD = 8
        private const val ANTIGUEDAD_MAXIMA = 60
        private val REGEX_USUARIO = Regex("^[A-Za-z0-9._-]{3,20}$")
    }

    private lateinit var etNombre: EditText
    private lateinit var etEmail: EditText
    private lateinit var etUsuario: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmar: EditText
    private lateinit var spCategoria: Spinner
    private lateinit var etAntiguedad: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        ajustarBordesDelSistema(findViewById(R.id.raizRegistro), findViewById(R.id.bandaSuperior))

        val repositorio = RepositorioUsuarios(this)

        etNombre = findViewById(R.id.etNombreRegistro)
        etEmail = findViewById(R.id.etEmailRegistro)
        etUsuario = findViewById(R.id.etUsuarioRegistro)
        etPassword = findViewById(R.id.etPasswordRegistro)
        etConfirmar = findViewById(R.id.etConfirmPasswordRegistro)
        spCategoria = findViewById(R.id.spCategoriaRegistro)
        etAntiguedad = findViewById(R.id.etAntiguedadRegistro)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnVolverALogin = findViewById<Button>(R.id.tvVolverALogin)

        // Selector de categoría con el mismo formato celeste que el de meses de la Calculadora
        spCategoria.adapter = adaptadorSpinnerBlanco(resources.getStringArray(R.array.lista_categorias))

        // Cada contraseña tiene su ojo (dentro del campo) para mostrarla u ocultarla
        configurarOjoContrasena(etPassword, findViewById(R.id.ivOjoPassword))
        configurarOjoContrasena(etConfirmar, findViewById(R.id.ivOjoConfirmar))

        btnRegistrar.setOnClickListener {
            if (!datosValidos()) return@setOnClickListener

            val email = etEmail.text.toString().trim()
            val error = repositorio.registrar(
                nombre = etNombre.text.toString(),
                email = email,
                usuario = etUsuario.text.toString(),
                password = etPassword.text.toString(),
                categoria = spCategoria.selectedItem.toString(),
                antiguedad = etAntiguedad.text.toString().trim().toInt()
            )

            when (error) {
                ErrorRegistro.EMAIL_REPETIDO -> marcarError(etEmail, R.string.reg_error_email_repetido)
                ErrorRegistro.USUARIO_REPETIDO -> marcarError(etUsuario, R.string.reg_error_usuario_repetido)
                null -> {
                    // Volvemos al Login con el e-mail ya escrito: ahí se valida lo que se guardó.
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_EMAIL_REGISTRADO, email))
                    finish()
                }
            }
        }

        btnVolverALogin.setOnClickListener { finish() }
    }

    /** Valida todos los campos; marca el primero con error y le da el foco. */
    private fun datosValidos(): Boolean {
        var primerError: EditText? = null
        fun fallo(campo: EditText, mensaje: Int) {
            campo.error = getString(mensaje)
            if (primerError == null) primerError = campo
        }

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val antiguedad = etAntiguedad.text.toString().trim().toIntOrNull()

        if (etNombre.text.toString().isBlank()) fallo(etNombre, R.string.reg_error_nombre)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) fallo(etEmail, R.string.reg_error_email)
        if (!REGEX_USUARIO.matches(etUsuario.text.toString().trim())) fallo(etUsuario, R.string.reg_error_usuario)
        if (password.length < LARGO_MINIMO_PASSWORD) fallo(etPassword, R.string.reg_error_password)
        if (etConfirmar.text.toString() != password) fallo(etConfirmar, R.string.reg_error_confirmar)
        if (antiguedad == null || antiguedad !in 0..ANTIGUEDAD_MAXIMA) fallo(etAntiguedad, R.string.reg_error_antiguedad)

        val categoriaValida = spCategoria.selectedItemPosition > 0 &&
            MotorLiquidacion.escalaPara(spCategoria.selectedItem.toString()) != null
        if (!categoriaValida) {
            Toast.makeText(this, R.string.reg_error_categoria, Toast.LENGTH_SHORT).show()
        }

        primerError?.requestFocus()
        return primerError == null && categoriaValida
    }

    private fun marcarError(campo: EditText, mensaje: Int) {
        campo.error = getString(mensaje)
        campo.requestFocus()
    }
}
