package com.example.calculadorasalarial

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Datos del usuario registrado que necesitan las pantallas. La contraseña nunca sale del repositorio. */
data class Usuario(
    val nombre: String,
    val email: String,
    val usuario: String,
    val categoria: String,
    val antiguedad: Int
)

enum class ErrorRegistro { EMAIL_REPETIDO, USUARIO_REPETIDO }

/**
 * Único punto de acceso a SharedPreferences para cuentas y sesión.
 *
 *  - Registro  -> [registrar]            guarda nombre, e-mail, usuario, categoría, antigüedad y
 *                                        la contraseña (con sal + hash, nunca en texto plano).
 *  - Login     -> [iniciarSesion]        vuelve a leer lo guardado y valida que coincida.
 *  - Pantallas -> [usuarioActual]        categoría y antigüedad salen de acá.
 *  - Recibo    -> [actualizarCategoria]  cambia la categoría y la deja persistida.
 *
 * Soporta varias cuentas en el mismo celular (la clave de cada una es su e-mail).
 */
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

    // ---------------------------------------------------------------- claves
    private fun clave(email: String, campo: String) = "usr|$email|$campo"
    private fun claveAlias(usuario: String) = "alias|$usuario"

    private fun normalizarEmail(email: String) = email.trim().lowercase(Locale.ROOT)
    private fun normalizarUsuario(usuario: String) = usuario.trim().lowercase(Locale.ROOT)

    private fun existeEmail(email: String) = prefs.contains(clave(email, "hash"))

    // ---------------------------------------------------------------- registro
    /** Devuelve null si se registró bien, o el motivo por el que no se pudo. */
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

    // ---------------------------------------------------------------- sesión
    /** [identificador] puede ser el e-mail o el nombre de usuario. */
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

    /** True si hay una sesión iniciada con "Mantener sesión" tildado. */
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

    // ---------------------------------------------------------------- datos de la cuenta
    fun actualizarCategoria(categoria: String) {
        val correo = prefs.getString(SESION_EMAIL, null) ?: return
        prefs.edit().putString(clave(correo, "categoria"), categoria).apply()
    }

    // ---------------------------------------------------------------- internos
    private fun emailDe(identificador: String): String? {
        val dato = identificador.trim().lowercase(Locale.ROOT)
        if (dato.isEmpty()) return null
        return if (dato.contains("@")) {
            dato.takeIf { existeEmail(it) }
        } else {
            prefs.getString(claveAlias(dato), null)
        }
    }

    // PBKDF2 con sal aleatoria. Para una app real conviene además guardar los datos en
    // EncryptedSharedPreferences / Keystore; para el TP alcanza con no guardar la clave en texto plano.
    private fun hash(password: String, sal: ByteArray): ByteArray {
        val especificacion = PBEKeySpec(password.toCharArray(), sal, ITERACIONES, LONGITUD_HASH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(especificacion).encoded
    }

    private fun aTexto(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun deTexto(texto: String): ByteArray = Base64.decode(texto, Base64.NO_WRAP)
}
