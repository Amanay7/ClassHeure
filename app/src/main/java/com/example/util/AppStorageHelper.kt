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

        val appContext = context.applicationContext ?: context
        val cleanName = fileName.replace("[/\\\\?%*:|\"<>]".toRegex(), "_")

        // 0. Copie de sécurité garantie dans le stockage externe propre à l'application
        var appExternalUri: Uri? = null
        try {
            val extDocs = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (extDocs != null) {
                val privateFolder = File(extDocs, FOLDER_NAME)
                if (!privateFolder.exists()) privateFolder.mkdirs()
                val backupFile = File(privateFolder, cleanName)
                FileOutputStream(backupFile).use { fos ->
                    fos.write(data)
                    fos.flush()
                    try { fos.fd.sync() } catch (_: Exception) {}
                }
                if (backupFile.exists() && backupFile.length() > 0) {
                    appExternalUri = Uri.fromFile(backupFile)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sauvegarde dans externalFilesDir ignorée : ${e.message}")
        }

        // 1. Essai d'écriture directe par fichier (Android <= 9 ou si accessible sans conflit SAF)
        try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appFolder = File(publicDocs, FOLDER_NAME)
            if (!appFolder.exists()) {
                appFolder.mkdirs()
            }
            val targetFile = File(appFolder, cleanName)
            // Si le fichier vient tout juste d'être écrit par SAF (CreateDocument) avec la bonne taille, ne jamais l'écraser ni le supprimer !
            if (targetFile.exists() && targetFile.length() == data.size.toLong() &&
                (System.currentTimeMillis() - targetFile.lastModified()) < 20_000L
            ) {
                MediaScannerConnection.scanFile(
                    appContext,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
                return Uri.fromFile(targetFile)
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && appFolder.exists() && appFolder.canWrite()) {
                FileOutputStream(targetFile).use { fos ->
                    fos.write(data)
                    fos.flush()
                    try { fos.fd.sync() } catch (_: Exception) {}
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    MediaScannerConnection.scanFile(
                        appContext,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                    return Uri.fromFile(targetFile)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Écriture directe File non disponible (${e.message}), passage à MediaStore...")
        }

        // 2. Écriture via MediaStore (Android 10 à 15+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveViaMediaStore(appContext, cleanName, mimeType, data) ?: appExternalUri
        }

        return appExternalUri
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

        // Vérifier si le fichier existe déjà dans Documents/ClassHeure avec des données valides (ex: créé via SAF CreateDocument)
        try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val physicalFile = File(File(publicDocs, FOLDER_NAME), fileName)
            if (physicalFile.exists() && physicalFile.length() >= data.size.toLong()) {
                return Uri.fromFile(physicalFile)
            }
        } catch (_: Exception) {}

        // Vérifier dans MediaStore si une entrée non-vide récente existe déjà
        try {
            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf(fileName, "%$RELATIVE_DOCUMENTS_PATH%")
            resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    val existingSize = cursor.getLong(1)
                    val existingUri = android.content.ContentUris.withAppendedId(collection, id)
                    if (existingSize == data.size.toLong()) {
                        return existingUri
                    }
                }
            }
        } catch (_: Exception) {}

        // Préparation des métadonnées sans IS_PENDING bloquant
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$RELATIVE_DOCUMENTS_PATH/")
        }

        var targetUri: Uri? = null
        try {
            targetUri = resolver.insert(collection, contentValues)
        } catch (e: Exception) {
            Log.w(TAG, "Erreur insertion MediaStore.Files (${e.message})")
        }

        // Fallback spécifique Android 10 (Q) : MediaStore.Downloads
        if (targetUri == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val dlCollection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val dlValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ClassHeure/")
                }
                targetUri = resolver.insert(dlCollection, dlValues)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur fallback Downloads : ${e.message}")
            }
        }

        if (targetUri == null) {
            return null
        }

        // Écriture intégrale des données dans le flux avec synchronisation matérielle
        var writeSuccess = false
        try {
            resolver.openFileDescriptor(targetUri, "w")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { fos ->
                    fos.write(data)
                    fos.flush()
                    try { fos.fd.sync() } catch (_: Exception) {}
                    writeSuccess = true
                }
            }
        } catch (e: Exception) {
            try {
                resolver.openOutputStream(targetUri)?.use { os ->
                    os.write(data)
                    os.flush()
                    if (os is FileOutputStream) {
                        try { os.fd.sync() } catch (_: Exception) {}
                    }
                    writeSuccess = true
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Erreur lors de l'écriture des données MediaStore pour $fileName : ${e2.message}", e2)
            }
        }

        if (writeSuccess) {
            try {
                val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val targetFile = File(File(publicDocs, FOLDER_NAME), fileName)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            } catch (_: Exception) {}

            return targetUri
        } else {
            try {
                resolver.delete(targetUri, null, null)
            } catch (_: Exception) {}
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
