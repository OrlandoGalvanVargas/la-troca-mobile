package com.troca.latroca.domain.models

import android.net.Uri
import java.io.File

data class RegistrationData(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val bio: String = "",
    val ubicacion: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageFile: File? = null,
    val imageUri: Uri? = null,

) {
    val isStep1Valid: Boolean
        get() = nombre.isNotBlank() && email.isNotBlank() && password.isNotBlank()

    val isStep2Valid: Boolean
        get() = bio.isNotBlank() && ubicacion.isNotBlank()
}