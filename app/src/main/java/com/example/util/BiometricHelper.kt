package com.example.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    enum class BiometricStatus {
        AVAILABLE,
        NOT_ENROLLED,
        NO_HARDWARE,
        UNAVAILABLE
    }

    /**
     * Déballe récursivement les ContextWrappers pour retrouver l'instance réelle de FragmentActivity.
     * Indispensable car LocalContext.current peut être un ContextThemeWrapper ou ContextImpl (createConfigurationContext).
     */
    fun findActivity(context: Context?): FragmentActivity? {
        var ctx: Context? = context
        while (ctx != null) {
            if (ctx is FragmentActivity) return ctx
            if (ctx is ContextWrapper) {
                ctx = ctx.baseContext
            } else {
                break
            }
        }
        return null
    }

    fun checkBiometricStatus(context: Context): BiometricStatus {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.UNAVAILABLE
                else -> {
                    @Suppress("DEPRECATION")
                    when (biometricManager.canAuthenticate()) {
                        BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                        else -> BiometricStatus.UNAVAILABLE
                    }
                }
            }
        } catch (e: Exception) {
            try {
                @Suppress("DEPRECATION")
                when (BiometricManager.from(context).canAuthenticate()) {
                    BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                    else -> BiometricStatus.UNAVAILABLE
                }
            } catch (e2: Exception) {
                BiometricStatus.UNAVAILABLE
            }
        }
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return checkBiometricStatus(context) == BiometricStatus.AVAILABLE
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Déverrouillage ClassHeure",
        subtitle: String? = null,
        negativeButtonText: String = "Saisir le code PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Empreinte non reconnue. Réessayez ou utilisez votre code PIN.")
            }
        }

        try {
            val builder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            if (!subtitle.isNullOrBlank()) {
                builder.setSubtitle(subtitle)
            }
            val promptInfo = builder.build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Fallback pour les appareils où setAllowedAuthenticators avec negativeButton n'est pas permis
            try {
                val builder = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setNegativeButtonText(negativeButtonText)
                if (!subtitle.isNullOrBlank()) {
                    builder.setSubtitle(subtitle)
                }
                val promptInfo = builder.build()

                val biometricPrompt = BiometricPrompt(activity, executor, callback)
                biometricPrompt.authenticate(promptInfo)
            } catch (fallbackError: Exception) {
                onError("Erreur biométrie : ${fallbackError.localizedMessage ?: "Module biométrique inaccessible"}")
            }
        }
    }
}

