package com.example.healthconnect.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ImgBbResponse(
    @SerialName("data") val data: ImgBbData? = null,
    @SerialName("success") val success: Boolean,
    @SerialName("error") val error: ImgBbError? = null
)

@Serializable
data class ImgBbData(
    @SerialName("url") val url: String,
    @SerialName("display_url") val displayUrl: String? = null
)

@Serializable
data class ImgBbError(
    @SerialName("message") val message: String? = null
)

class ImageUploadService(private val context: Context) {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

  
    private val apiKey = "9f2374aeb028270aab9a5df60e039f19"

    suspend fun uploadImage(imageUri: Uri): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val imageBytes = inputStream?.readBytes()
            inputStream?.close()
            
            if (imageBytes == null) {
                Log.e("ImageUploadService", "Failed to read image bytes")
                return null
            }
            
            // ImgBB API requires base64 encoded image
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            
            // ImgBB API endpoint expects form data with 'image' parameter containing base64 string
            val response: ImgBbResponse = client.submitFormWithBinaryData(
                url = "https://api.imgbb.com/1/upload",
                formData = formData {
                    append("key", apiKey)
                    append("image", base64Image)
                }
            ).body()
            
            if (response.success && response.data != null) {
                // Return the display_url if available, otherwise use url
                val imageUrl = response.data.displayUrl ?: response.data.url
                Log.d("ImageUploadService", "Image uploaded successfully: $imageUrl")
                return imageUrl
            } else {
                val errorMsg = response.error?.message ?: "Unknown error"
                Log.e("ImageUploadService", "ImgBB API error: $errorMsg")
                return null
            }
        } catch (e: Exception) {
            Log.e("ImageUploadService", "Error uploading image: ${e.message}", e)
            return null
        }
    }
}