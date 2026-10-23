package com.example.util

import android.content.Context
import org.json.JSONObject
import java.io.InputStreamReader

/**
 * Gestionnaire d'internationalisation et de localisation (i18n)
 * Supporte le français (FR) et l'arabe standard moderne (AR - الفصحى).
 * Gère le fallback automatique vers le français et les 6 formes de pluriel arabe.
 */
object I18n {
    private var frJson: JSONObject? = null
    private var arJson: JSONObject? = null
    private var currentLang: String = "fr"

    fun init(context: Context) {
        if (frJson == null) {
            frJson = loadJsonFromAsset(context, "i18n/fr.json")
        }
        if (arJson == null) {
            arJson = loadJsonFromAsset(context, "i18n/ar.json")
        }
    }

    fun setLanguage(lang: String) {
        currentLang = if (lang == "ar") "ar" else "fr"
    }

    fun getLanguage(): String = currentLang

    fun isRtl(): Boolean = currentLang == "ar"

    private fun loadJsonFromAsset(context: Context, filename: String): JSONObject? {
        return try {
            context.assets.open(filename).use { inputStream ->
                val reader = InputStreamReader(inputStream, Charsets.UTF_8)
                val content = reader.readText()
                JSONObject(content)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Traduit une clé au format dot-notation (ex: "nav.home", "actions.save")
     * Si la traduction arabe n'existe pas, utilise le fallback français.
     */
    fun t(key: String, vararg args: Any): String {
        val targetJson = if (currentLang == "ar") arJson else frJson
        var result = resolveKey(targetJson, key)

        // Fallback vers le français si non trouvé dans la langue cible
        if (result == null && currentLang != "fr") {
            result = resolveKey(frJson, key)
        }

        if (result == null) {
            return key
        }

        return if (args.isNotEmpty()) {
            try {
                String.format(result, *args)
            } catch (e: Exception) {
                result
            }
        } else {
            result
        }
    }

    private fun resolveKey(root: JSONObject?, dotKey: String): String? {
        if (root == null) return null
        val parts = dotKey.split(".")
        var current: Any = root
        for (part in parts) {
            if (current is JSONObject && current.has(part)) {
                current = current.get(part)
            } else {
                return null
            }
        }
        return (current as? String)
    }

    /**
     * Gestion des 6 formes de pluriel arabe :
     * 1. zero (0)
     * 2. one (1)
     * 3. two (2 - المثنى)
     * 4. few (3 à 10 - جمع القلة)
     * 5. many (11 à 99 - جمع الكثرة)
     * 6. other (100+)
     */
    fun plural(key: String, count: Int): String {
        val targetJson = if (currentLang == "ar") arJson else frJson
        val parts = key.split(".")
        var current: Any? = targetJson

        for (part in parts) {
            if (current is JSONObject && current.has(part)) {
                current = current.get(part)
            } else {
                current = null
                break
            }
        }

        if (current !is JSONObject && currentLang != "fr") {
            // Fallback FR
            current = frJson
            for (part in parts) {
                if (current is JSONObject && current.has(part)) {
                    current = current.get(part)
                } else {
                    current = null
                    break
                }
            }
        }

        if (current !is JSONObject) {
            return "$count $key"
        }

        val pluralObj = current
        val template = if (currentLang == "ar") {
            when {
                count == 0 && pluralObj.has("zero") -> pluralObj.getString("zero")
                count == 1 && pluralObj.has("one") -> pluralObj.getString("one")
                count == 2 && pluralObj.has("two") -> pluralObj.getString("two")
                count in 3..10 && pluralObj.has("few") -> pluralObj.getString("few")
                count in 11..99 && pluralObj.has("many") -> pluralObj.getString("many")
                pluralObj.has("other") -> pluralObj.getString("other")
                pluralObj.has("few") -> pluralObj.getString("few")
                else -> "%d"
            }
        } else {
            if (count <= 1 && pluralObj.has("one")) {
                pluralObj.getString("one")
            } else if (pluralObj.has("other")) {
                pluralObj.getString("other")
            } else {
                "%d"
            }
        }

        return try {
            String.format(template, count)
        } catch (e: Exception) {
            template.replace("%d", count.toString())
        }
    }
}
