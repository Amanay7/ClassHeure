package com.example.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Gestionnaire du dossier de stockage dédié "ClassHeure".
 * Crée et maintient le dossier "Documents/ClassHeure" compatible avec Android 8 à 15+.
 * Enregistre proprement tous les fichiers générés et exportés (PDF, sauvegardes, etc.)
 * via MediaStore sur Android 11+ et stockage direct avec MediaScanner sur les versions antérieures.
 */
object AppStorageHelper {

    private const val TAG = "AppStorageHelper"
    private const val FOLDER_NAME = "ClassHeure"
    private const val RELATIVE_DOCUMENTS_PATH = "Documents/ClassHeure"

    /**
     * Sauvegarde un tableau d'octets directement dans "Documents/ClassHeure".
     * Garantit que le fichier créé est complet, non vide, et lisible par
     * les gestionnaires de fichiers et visionneuses PDF externes.
     */
    fun saveFileToClassHeure(
        context: Context,
        fileName: String,
        mimeType: String = "application/pdf",
        data: ByteArray
    ): Uri? {
        if (data.isEmpty()) {
            Log.e(TAG, "Tentative de sauvegarde d'un fichier vide pour $fileName - Annulation")
            return null
        }

        val cleanName = fileName.replace("[/\\\\?%*:|\"<>]".toRegex(), "_")

        // 1. Essai d'écriture directe par fichier (Android <= 10 ou permissions directes)
        try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appFolder = File(publicDocs, FOLDER_NAME)
            if (!appFolder.exists()) {
                appFolder.mkdirs()
            }
            if (appFolder.exists() && appFolder.canWrite()) {
                val targetFile = File(appFolder, cleanName)
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                FileOutputStream(targetFile).use { fos ->
                    fos.write(data)
                    fos.flush()
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                    Log.d(TAG, "Fichier sauvegardé avec succès par File direct : ${targetFile.absolutePath} (${targetFile.length()} octets)")
                    return Uri.fromFile(targetFile)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Écriture directe File non disponible (${e.message}), passage à MediaStore...")
        }

        // 2. Écriture via MediaStore (Android 10 à 15+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveViaMediaStore(context, cleanName, mimeType, data)
        }

        return null
    }

    /** Alias de compatibilité */
    fun saveFileToMaClasse(context: Context, fileName: String, mimeType: String = "application/pdf", data: ByteArray): Uri? =
        saveFileToClassHeure(context, fileName, mimeType, data)

    /**
     * Sauvegarde un flux de données dans "Documents/ClassHeure".
     */
    fun saveStreamToClassHeure(
        context: Context,
        fileName: String,
        mimeType: String = "application/pdf",
        streamBlock: (OutputStream) -> Unit
    ): Uri? {
        val baos = ByteArrayOutputStream()
        streamBlock(baos)
        baos.flush()
        return saveFileToClassHeure(context, fileName, mimeType, baos.toByteArray())
    }

    /** Alias de compatibilité */
    fun saveStreamToMaClasse(context: Context, fileName: String, mimeType: String = "application/pdf", streamBlock: (OutputStream) -> Unit): Uri? =
        saveStreamToClassHeure(context, fileName, mimeType, streamBlock)

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(
        context: Context,
        fileName: String,
        mimeType: String,
        data: ByteArray
    ): Uri? {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        // 1. Supprimer toute entrée MediaStore existante portant ce nom dans Documents/MaClasse
        // pour éviter les doublons avec suffixe (1) et garantir l'écrasement propre
        try {
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf(fileName, "%$RELATIVE_DOCUMENTS_PATH%")
            resolver.delete(collection, selection, selectionArgs)
        } catch (e: Exception) {
            Log.w(TAG, "Suppression ancienne entrée MediaStore ignorée : ${e.message}")
        }

        // Supprimer également le fichier physique si résiduel
        try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val physicalFile = File(File(publicDocs, FOLDER_NAME), fileName)
            if (physicalFile.exists()) {
                physicalFile.delete()
            }
        } catch (e: Exception) {
            // Ignorer si Scoped Storage empêche la suppression directe
        }

        // 2. Préparation des métadonnées avec IS_PENDING = 1
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$RELATIVE_DOCUMENTS_PATH/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        var targetUri: Uri? = null
        try {
            targetUri = resolver.insert(collection, contentValues)
        } catch (e: Exception) {
            Log.w(TAG, "Erreur insertion MediaStore.Files (${e.message})")
        }

        // Fallback spécifique Android 10 (Q) : si Documents/ n'est pas autorisé dans MediaStore.Files,
        // enregistrer dans MediaStore.Downloads
        if (targetUri == null && Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            try {
                val dlCollection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val dlValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ClassHeure/")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                targetUri = resolver.insert(dlCollection, dlValues)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur fallback Downloads sur Android Q : ${e.message}")
            }
        }

        if (targetUri == null) {
            Log.e(TAG, "Impossible de créer l'entrée MediaStore pour $fileName")
            return null
        }

        // 3. Écriture intégrale des données dans le flux
        var writeSuccess = false
        try {
            resolver.openOutputStream(targetUri, "w")?.use { os ->
                os.write(data)
                os.flush()
                writeSuccess = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'écriture des données MediaStore pour $fileName : ${e.message}", e)
        }

        // 4. Finalisation : marquer IS_PENDING = 0 pour rendre le fichier immédiatement visible et lisible
        if (writeSuccess) {
            try {
                val updateValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(targetUri, updateValues, null, null)
            } catch (e: Exception) {
                Log.w(TAG, "Mise à jour IS_PENDING=0 ignorée : ${e.message}")
            }

            // Scanner le fichier physique pour garantir la détection immédiate dans les gestionnaires de fichiers
            try {
                val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val targetFile = File(File(publicDocs, FOLDER_NAME), fileName)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            } catch (e: Exception) {
                // Ignorer
            }

            Log.d(TAG, "Fichier enregistré via MediaStore avec succès : $fileName ($targetUri, ${data.size} octets)")
            return targetUri
        } else {
            // Nettoyage en cas d'échec pour éviter de laisser un fichier de 0 octet
            try {
                resolver.delete(targetUri, null, null)
            } catch (e: Exception) {}
            return null
        }
    }

    /**
     * Initialise le dossier à l'installation / au démarrage de l'application.
     */
    fun ensureAppDirectory(context: Context) {
        try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val folder = File(publicDocs, FOLDER_NAME)
            if (!folder.exists()) {
                folder.mkdirs()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur ensureAppDirectory : ${e.message}")
        }
    }

    /**
     * Rétrocompatibilité : Enregistre automatiquement une copie d'un fichier exporté.
     */
    fun autoSaveExportedFile(context: Context, fileName: String, sourceStreamBlock: (OutputStream) -> Unit): Uri? {
        val mime = when {
            fileName.endsWith(".json", true) -> "application/json"
            fileName.endsWith(".txt", true) -> "text/plain"
            else -> "application/pdf"
        }
        return saveStreamToMaClasse(context, fileName, mimeType = mime, streamBlock = sourceStreamBlock)
    }

    /**
     * Copie un flux entrant vers Documents/MaClasse.
     */
    fun copyToAppFolder(context: Context, fileName: String, inputStream: InputStream): Uri? {
        val data = inputStream.readBytes()
        val mime = when {
            fileName.endsWith(".json", true) -> "application/json"
            fileName.endsWith(".txt", true) -> "text/plain"
            else -> "application/pdf"
        }
        return saveFileToMaClasse(context, fileName, mime, data)
    }

    /**
     * Copie un Uri vers Documents/ClassHeure.
     */
    fun copyUriToAppFolder(context: Context, uri: Uri, fileName: String): Uri? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                copyToAppFolder(context, fileName, inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur copie Uri vers ClassHeure : ${e.message}")
            null
        }
    }
}
