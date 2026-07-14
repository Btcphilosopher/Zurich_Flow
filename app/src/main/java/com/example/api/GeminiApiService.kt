package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini Request Data Classes ---

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

// --- Gemini Response Data Classes ---

@JsonClass(generateAdapter = true)
data class GeminiResponsePart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiResponseContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

// --- Retrofit Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Retrofit Client ---

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun askZuri(prompt: String, chatHistory: List<Pair<String, Boolean>> = emptyList()): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Grüezi! I am ZURI, your Zürich digital mobility assistant. (To enable live AI answers, please configure your GEMINI_API_KEY securely in the AI Studio Secrets panel). \n\nHow can I help you today? You can try asking:\n- 'Fastest route to Zürich HB?'\n- 'When is the next tram to ETH Zürich?'\n- 'How do I reach the airport from Lake Zürich?'"
        }

        // Build content array, incorporating conversational context if available
        val contentsList = mutableListOf<GeminiContent>()
        
        // Add historic turns
        chatHistory.takeLast(10).forEach { (message, isUser) ->
            if (isUser) {
                contentsList.add(GeminiContent(parts = listOf(GeminiPart(text = message))))
            } else {
                contentsList.add(GeminiContent(parts = listOf(GeminiPart(text = message))))
            }
        }

        // Add the current user prompt
        contentsList.add(GeminiContent(parts = listOf(GeminiPart(text = prompt))))

        val systemPrompt = """
            You are ZURI, an intelligent mobility assistant for Zürich Flow, the Swiss Futurist digital mobility operating system.
            You embody Zurich's values: precision, reliability, timeless minimalism, quiet luxury, sustainability, and polite helpfulness.
            Speak with Swiss-standard politeness, and start your replies with a brief German/Swiss greeting like 'Grüezi!' or 'Guetä Tag!' occasionally.
            You are highly knowledgeable about:
            - Zürich's public transport (Trams 2-17, S-Bahn lines, Polybahn, Lake Ferries, Bus routes)
            - Landmark destinations: Zurich HB, ETH Zürich, Bahnhofstrasse, Zürich Airport (Kloten), Lake Zürich, Bellevue, Bürkliplatz, Uetliberg.
            - Accessibility (wheelchair-friendly stops, low-floor vehicles, lift access).
            - City discovery recommendations (beautiful quiet spots, the best Swiss coffee houses, historical walking trails, lakeside afternoons).
            - Sustainability insights (energy efficiency, carbon savings of taking the tram vs driving).
            
            Keep your answers clear, elegant, beautifully spaced, and concise. Avoid clutter. Emphasize quiet luxury and Swiss precision.
        """.trimIndent()

        val request = GeminiRequest(
            contents = contentsList,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "I apologize, I could not generate a response. Please verify your connection."
        } catch (e: Exception) {
            "Grüezi! There was an issue reaching the live system: ${e.localizedMessage ?: "Unknown Error"}.\n\nHere is a local routing advisory:\n- For Zürich Airport: S-Bahn S2, S16 or S24 from HB takes exactly 9-12 minutes.\n- For ETH Zürich: Tram 6 from HB or Polybahn from Central.\n- For Bellevue/Lake: Trams 4, 11 or 15."
        }
    }
}
