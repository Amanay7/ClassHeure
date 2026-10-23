package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service pour appeler l'API Gemini (Modèle gemini-3.5-flash) et générer un résumé hebdomadaire
 * automatique des entrées du Cahier Journal.
 */
object GeminiSummaryHelper {

    private const val TAG = "GeminiSummaryHelper"

    // Client HTTP avec timeouts de 60s recommandés pour l'IA
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateWeeklySummary(journalEntries: List<JournalEntryEntity>, lang: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext if (lang == "ar") {
                "خطأ: مفتاح API لـ Gemini غير مكوّن. يرجى تهيئته في لوحة Secrets."
            } else {
                "Erreur : Clé API Gemini non configurée dans le panneau Secrets."
            }
        }

        if (journalEntries.isEmpty()) {
            return@withContext if (lang == "ar") {
                "لا توجد مدخلات في الكراس اليومي لتوليد الملخص."
            } else {
                "Aucune entrée de journal disponible pour générer un résumé."
            }
        }

        // Construction du prompt pour l'IA
        val promptBuilder = StringBuilder()
        if (lang == "ar") {
            promptBuilder.append("أنت مساعد بيداغوجي ذكي للأستاذ. قم بتلخيص الأنشطة التعليمية والدروس التالية للأسبوع المنصرم بشكل احترافي ومنظم، واذكر الأهداف والملحوظات الهامة:\n\n")
        } else {
            promptBuilder.append("Tu es un conseiller pédagogique expert. Rédige un résumé hebdomadaire synthétique et professionnel des cours et activités pédagogiques suivants pour le cahier journal de l'enseignant. Souligne les objectifs atteints et formule des recommandations :\n\n")
        }

        for (entry in journalEntries) {
            promptBuilder.append("- Date: ${entry.date} (${entry.timeSlot})\n")
            promptBuilder.append("  Matière: ${entry.courseSubject}\n")
            promptBuilder.append("  Objectifs: ${entry.objectives}\n")
            promptBuilder.append("  Remarques: ${entry.observations}\n\n")
        }

        val prompt = promptBuilder.toString()

        try {
            // URL officielle pour le modèle gemini-3.5-flash
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            // Construction manuelle robuste du JSON pour éviter les soucis de bibliothèques
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()

            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Erreur API Gemini: Code ${response.code} | $errBody")
                    return@withContext if (lang == "ar") {
                        "خطأ في الاتصال بخدمة Gemini (رمز الخطأ: ${response.code})"
                    } else {
                        "Erreur de communication avec l'API Gemini (Code: ${response.code})"
                    }
                }

                val responseBody = response.body?.string() ?: return@withContext "Réponse vide."
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text")
                    }
                }

                "Impossible de lire le résumé généré."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la génération du résumé", e)
            if (lang == "ar") {
                "حدث خطأ غير متوقع: ${e.localizedMessage}"
            } else {
                "Une erreur inattendue est survenue : ${e.localizedMessage}"
            }
        }
    }
}
