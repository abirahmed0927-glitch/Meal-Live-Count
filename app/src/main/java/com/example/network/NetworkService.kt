package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- Google Sheets Apps Script Data Classes ---

@JsonClass(generateAdapter = true)
data class AppsScriptResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "sheetName") val sheetName: String? = null,
    @Json(name = "date") val date: Int? = null,
    @Json(name = "month") val month: String? = null,
    @Json(name = "column") val column: Int? = null,
    @Json(name = "data") val data: MealData? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class MealData(
    @Json(name = "breakfast") val breakfast: Int,
    @Json(name = "lunch") val lunch: Int,
    @Json(name = "dinner") val dinner: Int
)

// --- Gemini AI Data Classes (from SKILL.md) ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content
)

// --- Service Interfaces ---

interface AppsScriptService {
    @GET
    suspend fun fetchLiveMealStats(@Url url: String): AppsScriptResponse
}

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Retrofit Client Provider ---

object NetworkClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val sheetRetrofit = Retrofit.Builder()
        .baseUrl("https://script.google.com/")
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val geminiRetrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val appsScriptService: AppsScriptService by lazy {
        sheetRetrofit.create(AppsScriptService::class.java)
    }

    val geminiApiService: GeminiApiService by lazy {
        geminiRetrofit.create(GeminiApiService::class.java)
    }
}
