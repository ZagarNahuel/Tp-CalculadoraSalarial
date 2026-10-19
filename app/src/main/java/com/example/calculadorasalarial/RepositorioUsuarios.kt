package com.example.calculadorasalarial

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Los datos de una cuenta que necesitan las pantallas. La contraseña nunca sale de este archivo.
data class Usuario(
    val nombre: String,
    val email: String,
    val usuario: String,
    val categoria: String,
    val antiguedad: Int
)

enum class ErrorRegistro { EMAIL_REPETIDO, USUARIO_REPETIDO }

// Guarda y lee las cuentas en el celular (SharedPreferences).
// El Registro guarda, el Login comprueba, y las demás pantallas piden de acá la categoría y la antigüedad.
class RepositorioUsuarios(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(NOMBRE_PREFS, Context.MODE_PRIVATE)

    companion object {
        private const val NOMBRE_PREFS = "calculadora_pref"
        private const val SESION_EMAIL = "sesion_email"
        private const val MANTENER_SESION = "mantener_sesion"
        private const val ITERACIONES = 20_000
        private const val LONGITUD_HASH_BITS = 256
        private const val LONGITUD_SAL_BYTES = 16
    }

    private fun clave(email: String, campo: String) = "usr|$email|$campo"
    private fun claveAlias(usuario: String) = "alias|$usuario"

    private fun normalizarEmail(email: String) = email.trim().lowercase(Locale.ROOT)
    private fun normalizarUsuario(usuario: String) = usuario.trim().lowercase(Locale.ROOT)

    private fun existeEmail(email: String) = prefs.contains(clave(email, "hash"))

    // Guarda una cuenta nueva. Devuelve null si salió bien, o el motivo si no se pudo.
    fun registrar(
        nombre: String,
        email: String,
        usuario: String,
        password: String,
        categoria: String,
        antiguedad: Int
    ): ErrorRegistro? {
        val correo = normalizarEmail(email)
        val alias = normalizarUsuario(usuario)

        if (existeEmail(correo)) return ErrorRegistro.EMAIL_REPETIDO
        if (prefs.contains(claveAlias(alias))) return ErrorRegistro.USUARIO_REPETIDO

        // La contraseña no se guarda como se escribe: se guarda una huella calculada con un valor al azar (la sal).
        val sal = ByteArray(LONGITUD_SAL_BYTES).also { SecureRandom().nextBytes(it) }

        prefs.edit()
            .putString(clave(correo, "nombre"), nombre.trim())
            .putString(clave(correo, "usuario"), usuario.trim())
            .putString(clave(correo, "categoria"), categoria)
            .putInt(clave(correo, "antiguedad"), antiguedad)
            .putString(clave(correo, "sal"), aTexto(sal))
            .putString(clave(correo, "hash"), aTexto(hash(password, sal)))
            .putString(claveAlias(alias), correo)
            .apply()
        return null
    }

    // Se puede entrar con el e-mail o con el nombre de usuario.
    fun iniciarSesion(identificador: String, password: String, mantenerSesion: Boolean): Boolean {
        val correo = emailDe(identificador) ?: return false
        val sal = prefs.getString(clave(correo, "sal"), null)?.let { deTexto(it) } ?: return false
        val esperado = prefs.getString(clave(correo, "hash"), null)?.let { deTexto(it) } ?: return false

        if (!MessageDigest.isEqual(esperado, hash(password, sal))) return false

        prefs.edit()
            .putString(SESION_EMAIL, correo)
            .putBoolean(MANTENER_SESION, mantenerSesion)
            .apply()
        return true
    }

    // Es verdadero si el usuario eligió 'Mantener sesión'.
    fun haySesionGuardada(): Boolean =
        prefs.getBoolean(MANTENER_SESION, false) && usuarioActual() != null

    fun usuarioActual(): Usuario? {
        val correo = prefs.getString(SESION_EMAIL, null) ?: return null
        val nombre = prefs.getString(clave(correo, "nombre"), null) ?: return null
        val categoria = prefs.getString(clave(correo, "categoria"), null) ?: return null
        return Usuario(
            nombre = nombre,
            email = correo,
            usuario = prefs.getString(clave(correo, "usuario"), "") ?: "",
            categoria = categoria,
            antiguedad = prefs.getInt(clave(correo, "antiguedad"), 0)
        )
    }

    fun cerrarSesion() {
        prefs.edit()
            .remove(SESION_EMAIL)
            .putBoolean(MANTENER_SESION, false)
            .apply()
    }

    fun actualizarCategoria(categoria: String) {
        val correo = prefs.getString(SESION_EMAIL, null) ?: return
        prefs.edit().putString(clave(correo, "categoria"), categoria).apply()
    }

    private fun emailDe(identificador: String): String? {
        val dato = identificador.trim().lowercase(Locale.ROOT)
        if (dato.isEmpty()) return null
        return if (dato.contains("@")) {
            dato.takeIf { existeEmail(it) }
        } else {
            prefs.getString(claveAlias(dato), null)
        }
    }

    // Convierte la contraseña en una huella que no se puede deshacer.
    // Para una app real habría que sumar un almacenamiento cifrado; para el TP alcanza con no guardarla tal cual.
    private fun hash(password: String, sal: ByteArray): ByteArray {
        val especificacion = PBEKeySpec(password.toCharArray(), sal, ITERACIONES, LONGITUD_HASH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(especificacion).encoded
    }

    private fun aTexto(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun deTexto(texto: String): ByteArray = Base64.decode(texto, Base64.NO_WRAP)
}
