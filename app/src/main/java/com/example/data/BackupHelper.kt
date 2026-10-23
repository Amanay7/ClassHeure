package com.example.data

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object CryptoHelper {
    private const val FIXED_SALT = "AzmamUselmadSalt"
    private const val V2_PREFIX = "V2:"

    private fun deriveKeyPBKDF2(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 15000, 256)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }

    fun encrypt(plainText: String, password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val ivBytes = ByteArray(16).also { random.nextBytes(it) }
        val key = deriveKeyPBKDF2(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(ivBytes))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return V2_PREFIX + Base64.encodeToString(salt + ivBytes + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encryptedText: String, password: String): String {
        if (encryptedText.startsWith(V2_PREFIX)) {
            val raw = Base64.decode(encryptedText.removePrefix(V2_PREFIX), Base64.NO_WRAP)
            val salt = raw.copyOfRange(0, 16)
            val ivBytes = raw.copyOfRange(16, 32)
            val cipherBytes = raw.copyOfRange(32, raw.size)
            val key = deriveKeyPBKDF2(password, salt)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(ivBytes))
            return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }
        // Rétrocompatibilité V1 pour les anciennes sauvegardes
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val key = SecretKeySpec(digest.digest((password + FIXED_SALT).toByteArray()).copyOf(16), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(FIXED_SALT.toByteArray()))
        return String(cipher.doFinal(Base64.decode(encryptedText, Base64.NO_WRAP)), Charsets.UTF_8)
    }
}

object BackupHelper {

    suspend fun exportBackup(repository: AppRepository, password: String, context: Context? = null): String {
        val json = JSONObject()

        // 1. Settings
        val settings = repository.getSettings()
        val settingsObj = JSONObject().apply {
            put("teacherName", settings.teacherName)
            put("schoolName", settings.schoolName)
            put("activeLanguage", settings.activeLanguage)
            put("activeTheme", settings.activeTheme)
            put("activeFont", settings.activeFont)
            put("fontSizeScale", settings.fontSizeScale)
            put("pinCode", settings.pinCode ?: JSONObject.NULL)
            put("isBiometricEnabled", settings.isBiometricEnabled)
            put("gradeCoefficientsJson", settings.gradeCoefficientsJson)
            put("currentSchoolYear", settings.currentSchoolYear)
            put("schoolYearsJson", settings.schoolYearsJson)
            put("archivedSchoolYearsJson", settings.archivedSchoolYearsJson)
            put("taughtSubject", settings.taughtSubject)
            put("profilePictureUri", settings.profilePictureUri ?: JSONObject.NULL)
            put("subjectCoefficient", settings.subjectCoefficient)
            put("teacherFreeDay", settings.teacherFreeDay ?: JSONObject.NULL)
            put("isFontBold", settings.isFontBold)
            put("themeColor", settings.themeColor)
            put("educationLevel", settings.educationLevel)
            put("hasCompletedOnboarding", settings.hasCompletedOnboarding)
            put("teacherTitle", settings.teacherTitle)
        }
        json.put("settings", settingsObj)

        // 2. Classes
        val classes = repository.allClasses.firstOrNull() ?: emptyList()
        val classesArray = JSONArray()
        for (c in classes) {
            classesArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("schoolYear", c.schoolYear)
                put("subjectCoefficient", c.subjectCoefficient)
            })
        }
        json.put("classes", classesArray)

        // 3. Students
        val students = repository.allStudents.firstOrNull() ?: emptyList()
        val studentsArray = JSONArray()
        for (s in students) {
            studentsArray.put(JSONObject().apply {
                put("id", s.id)
                put("classId", s.classId)
                put("lastName", s.lastName)
                put("firstName", s.firstName)
                put("birthDate", s.birthDate)
                put("gender", s.gender)
                put("observations", s.observations)
                put("birthPlace", s.birthPlace)
                put("goodToKnow", s.goodToKnow)
                put("seatIndex", s.seatIndex ?: JSONObject.NULL)
                put("repeatedYears", s.repeatedYears)
                put("groupName", s.groupName)
                put("orderIndex", s.orderIndex)
            })
        }
        json.put("students", studentsArray)

        // 4. Grades
        val gradesArray = JSONArray()
        // We'll gather grades for all classes and all terms
        for (c in classes) {
            for (term in 1..3) {
                val gradesList = repository.getStudentsWithGradesForTermList(c.id, term)
                for (g in gradesList) {
                    if (g.gradeId != null) {
                        gradesArray.put(JSONObject().apply {
                            put("studentId", g.studentId)
                            put("term", term)
                            put("participation", g.participation ?: JSONObject.NULL)
                            put("discipline", g.discipline ?: JSONObject.NULL)
                            put("assiduity", g.assiduity ?: JSONObject.NULL)
                            put("notebook", g.notebook ?: JSONObject.NULL)
                            put("project", g.project ?: JSONObject.NULL)
                            put("exam1", g.exam1 ?: JSONObject.NULL)
                            put("exam2", g.exam2 ?: JSONObject.NULL)
                            put("exam3", g.exam3 ?: JSONObject.NULL)
                            put("finalExam", g.finalExam ?: JSONObject.NULL)
                        })
                    }
                }
            }
        }
        json.put("grades", gradesArray)

        // 5. Journal Entries
        val journalEntries = repository.allJournalEntries.firstOrNull() ?: emptyList()
        val journalArray = JSONArray()
        for (j in journalEntries) {
            journalArray.put(JSONObject().apply {
                put("id", j.id)
                put("date", j.date)
                put("timeSlot", j.timeSlot)
                put("courseSubject", j.courseSubject)
                put("classId", j.classId)
                put("objectives", j.objectives)
                put("observations", j.observations)
                put("photoPath", j.photoPath ?: JSONObject.NULL)
                put("audioPath", j.audioPath ?: JSONObject.NULL)
                put("activity", j.activity)
                put("lessonTitle", j.lessonTitle)
            })
        }
        json.put("journal_entries", journalArray)

        // 6. Timetable Slots
        val timetableSlots = repository.allTimetableSlots.firstOrNull() ?: emptyList()
        val timetableArray = JSONArray()
        for (t in timetableSlots) {
            timetableArray.put(JSONObject().apply {
                put("id", t.id)
                put("classId", t.classId ?: JSONObject.NULL)
                put("dayOfWeek", t.dayOfWeek)
                put("timeSlot", t.timeSlot)
                put("subject", t.subject)
                put("classroom", t.classroom)
                put("colorHex", t.colorHex)
            })
        }
        json.put("timetable_slots", timetableArray)

        // 7. Reminders
        val reminders = repository.getAllRemindersList()
        val remindersArray = JSONArray()
        for (r in reminders) {
            remindersArray.put(JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("description", r.description)
                put("date", r.date)
                put("time", r.time)
                put("isNotificationEnabled", r.isNotificationEnabled)
                put("isCompleted", r.isCompleted)
            })
        }
        json.put("reminders", remindersArray)

        // 8. Notes
        val notes = repository.getAllNotesList()
        val notesArray = JSONArray()
        for (n in notes) {
            notesArray.put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("createdAt", n.createdAt)
            })
        }
        json.put("notes", notesArray)

        // 9. Document Categories
        val categories = repository.getAllCategoriesList()
        val categoriesArray = JSONArray()
        for (cat in categories) {
            categoriesArray.put(JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("parentCategoryId", cat.parentCategoryId ?: JSONObject.NULL)
            })
        }
        json.put("document_categories", categoriesArray)

        // 10. Documents
        val docs = repository.getAllDocumentsList()
        val docsArray = JSONArray()
        for (d in docs) {
            val docObj = JSONObject().apply {
                put("id", d.id)
                put("categoryId", d.categoryId)
                put("title", d.title)
                put("fileUri", d.fileUri)
                put("createdAt", d.createdAt)
            }
            try {
                var file = File(d.fileUri)
                if (!file.exists() && context != null) {
                    val fallback = File(context.filesDir, "copied_documents/${File(d.fileUri).name}")
                    if (fallback.exists()) file = fallback
                }
                if (file.exists() && file.isFile && file.length() in 1..(35 * 1024 * 1024L)) {
                    val bytes = file.readBytes()
                    docObj.put("fileData", Base64.encodeToString(bytes, Base64.NO_WRAP))
                    docObj.put("fileName", file.name)
                }
            } catch (_: Exception) {}
            docsArray.put(docObj)
        }
        json.put("documents", docsArray)

        // 11. Attendances
        val attendances = repository.allAttendances.firstOrNull() ?: emptyList()
        val attendancesArray = JSONArray()
        for (a in attendances) {
            attendancesArray.put(JSONObject().apply {
                put("id", a.id)
                put("studentId", a.studentId)
                put("classId", a.classId)
                put("date", a.date)
                put("timeSlot", a.timeSlot)
                put("subject", a.subject)
                put("status", a.status)
                put("isJustified", a.isJustified)
                put("delayMinutes", a.delayMinutes)
                put("reason", a.reason)
                put("recordedAt", a.recordedAt)
            })
        }
        json.put("attendances", attendancesArray)

        val plainText = json.toString()
        return CryptoHelper.encrypt(plainText, password)
    }

    suspend fun importBackup(repository: AppRepository, encryptedText: String, password: String, context: Context? = null): Boolean {
        return try {
            val trimmed = encryptedText.trim()
            val plainText = CryptoHelper.decrypt(trimmed, password)
            val json = JSONObject(plainText)

            // Validate structure
            if (!json.has("settings") || !json.has("classes") || !json.has("students")) {
                return false
            }

            suspend fun runImport() {
                // 1. Restore Settings
                val settingsObj = json.getJSONObject("settings")
            val settings = SettingsEntity(
                id = 1,
                teacherName = settingsObj.optString("teacherName", "Enseignant"),
                schoolName = settingsObj.optString("schoolName", "Établissement"),
                activeLanguage = settingsObj.optString("activeLanguage", "fr"),
                activeTheme = settingsObj.optString("activeTheme", "system"),
                activeFont = settingsObj.optString("activeFont", "Times New Roman"),
                fontSizeScale = settingsObj.optString("fontSizeScale", "medium"),
                pinCode = if (settingsObj.isNull("pinCode")) null else settingsObj.optString("pinCode", null),
                isBiometricEnabled = settingsObj.optBoolean("isBiometricEnabled", false),
                gradeCoefficientsJson = settingsObj.optString("gradeCoefficientsJson", "{\"participation\":1,\"discipline\":1,\"assiduity\":1,\"notebook\":1,\"exams\":2,\"final_exam\":3}"),
                currentSchoolYear = settingsObj.optString("currentSchoolYear", ""),
                schoolYearsJson = settingsObj.optString("schoolYearsJson", "[]"),
                archivedSchoolYearsJson = settingsObj.optString("archivedSchoolYearsJson", "[]"),
                taughtSubject = settingsObj.optString("taughtSubject", "Mathématiques"),
                profilePictureUri = if (settingsObj.isNull("profilePictureUri")) null else settingsObj.optString("profilePictureUri", null),
                subjectCoefficient = settingsObj.optInt("subjectCoefficient", 3),
                teacherFreeDay = if (settingsObj.isNull("teacherFreeDay")) null else settingsObj.optInt("teacherFreeDay"),
                isFontBold = settingsObj.optBoolean("isFontBold", false),
                themeColor = settingsObj.optString("themeColor", "default"),
                educationLevel = settingsObj.optString("educationLevel", "CEM_LYCEE"),
                hasCompletedOnboarding = settingsObj.optBoolean("hasCompletedOnboarding", false),
                teacherTitle = settingsObj.optString("teacherTitle", "M.")
            )
            repository.saveSettings(settings)

            // To avoid constraints failing, we delete in order (foreign keys are cascade, but let's clear cleanly)
            // Actually, we'll clear everything and insert classes first, then students, then grades, etc.
            // Since we're inserting specific IDs, we'll replace the existing ones.
            // Let's delete existing classes (which cascades to students, grades, journal_entries, etc.)
            val existingClasses = repository.allClasses.firstOrNull() ?: emptyList()
            for (c in existingClasses) {
                repository.deleteClass(c)
            }

            repository.clearAllReminders()
            repository.clearAllNotes()
            repository.clearAllCategories()
            repository.clearAllDocuments()
            repository.clearAllAttendances()

            // 2. Insert Classes
            val classesArray = json.getJSONArray("classes")
            for (i in 0 until classesArray.length()) {
                val obj = classesArray.getJSONObject(i)
                repository.insertClass(
                    ClassEntity(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        schoolYear = obj.optString("schoolYear", "2023 / 2024"),
                        subjectCoefficient = obj.optInt("subjectCoefficient", 3)
                    )
                )
            }

            // 3. Insert Students
            val studentsArray = json.getJSONArray("students")
            for (i in 0 until studentsArray.length()) {
                val obj = studentsArray.getJSONObject(i)
                repository.insertStudent(
                    StudentEntity(
                        id = obj.getInt("id"),
                        classId = obj.getInt("classId"),
                        lastName = obj.getString("lastName"),
                        firstName = obj.getString("firstName"),
                        birthDate = obj.getString("birthDate"),
                        gender = obj.getString("gender"),
                        observations = obj.optString("observations", ""),
                        birthPlace = obj.optString("birthPlace", ""),
                        goodToKnow = obj.optString("goodToKnow", ""),
                        seatIndex = if (obj.isNull("seatIndex")) null else obj.getInt("seatIndex"),
                        repeatedYears = obj.optString("repeatedYears", ""),
                        groupName = obj.optString("groupName", ""),
                        orderIndex = obj.optInt("orderIndex", 0)
                    )
                )
            }

            // 4. Insert Grades
            val gradesArray = json.getJSONArray("grades")
            for (i in 0 until gradesArray.length()) {
                val obj = gradesArray.getJSONObject(i)
                repository.saveGrade(
                    GradeEntity(
                        studentId = obj.getInt("studentId"),
                        term = obj.getInt("term"),
                        participation = if (obj.isNull("participation")) null else obj.getDouble("participation").toFloat(),
                        discipline = if (obj.isNull("discipline")) null else obj.getDouble("discipline").toFloat(),
                        assiduity = if (obj.isNull("assiduity")) null else obj.getDouble("assiduity").toFloat(),
                        notebook = if (obj.isNull("notebook")) null else obj.getDouble("notebook").toFloat(),
                        project = if (obj.has("project") && !obj.isNull("project")) obj.getDouble("project").toFloat() else null,
                        exam1 = if (obj.isNull("exam1")) null else obj.getDouble("exam1").toFloat(),
                        exam2 = if (obj.isNull("exam2")) null else obj.getDouble("exam2").toFloat(),
                        exam3 = if (obj.isNull("exam3")) null else obj.getDouble("exam3").toFloat(),
                        finalExam = if (obj.isNull("finalExam")) null else obj.getDouble("finalExam").toFloat()
                    )
                )
            }

            // 5. Insert Journal Entries
            val journalArray = json.getJSONArray("journal_entries")
            for (i in 0 until journalArray.length()) {
                val obj = journalArray.getJSONObject(i)
                repository.insertJournalEntry(
                    JournalEntryEntity(
                        id = obj.getInt("id"),
                        date = obj.getString("date"),
                        timeSlot = obj.getString("timeSlot"),
                        courseSubject = obj.getString("courseSubject"),
                        classId = obj.getInt("classId"),
                        objectives = obj.getString("objectives"),
                        observations = obj.getString("observations"),
                        photoPath = if (obj.isNull("photoPath")) null else obj.getString("photoPath"),
                        audioPath = if (obj.isNull("audioPath")) null else obj.getString("audioPath"),
                        activity = obj.optString("activity", ""),
                        lessonTitle = obj.optString("lessonTitle", "")
                    )
                )
            }

            // 6. Insert Timetable Slots
            val timetableArray = json.getJSONArray("timetable_slots")
            for (i in 0 until timetableArray.length()) {
                val obj = timetableArray.getJSONObject(i)
                repository.insertTimetableSlot(
                    TimetableSlotEntity(
                        id = obj.getInt("id"),
                        classId = if (obj.isNull("classId")) null else obj.getInt("classId"),
                        dayOfWeek = obj.getInt("dayOfWeek"),
                        timeSlot = obj.getString("timeSlot"),
                        subject = obj.getString("subject"),
                        classroom = obj.getString("classroom"),
                        colorHex = obj.getString("colorHex")
                    )
                )
            }

            // 7. Insert Reminders
            if (json.has("reminders")) {
                val remindersArray = json.getJSONArray("reminders")
                for (i in 0 until remindersArray.length()) {
                    val obj = remindersArray.getJSONObject(i)
                    repository.insertReminder(
                        ReminderEntity(
                            id = obj.getInt("id"),
                            title = obj.getString("title"),
                            description = obj.getString("description"),
                            date = obj.getString("date"),
                            time = obj.getString("time"),
                            isNotificationEnabled = obj.optBoolean("isNotificationEnabled", true),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
            }

            // 8. Insert Notes
            if (json.has("notes")) {
                val notesArray = json.getJSONArray("notes")
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.getJSONObject(i)
                    repository.insertNote(
                        NoteEntity(
                            id = obj.getInt("id"),
                            title = obj.getString("title"),
                            content = obj.getString("content"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 9. Insert Document Categories
            if (json.has("document_categories")) {
                val categoriesArray = json.getJSONArray("document_categories")
                for (i in 0 until categoriesArray.length()) {
                    val obj = categoriesArray.getJSONObject(i)
                    repository.insertCategory(
                        DocumentCategoryEntity(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            parentCategoryId = if (obj.isNull("parentCategoryId")) null else obj.getInt("parentCategoryId")
                        )
                    )
                }
            }

            // 10. Insert Documents
            if (json.has("documents")) {
                val docsArray = json.getJSONArray("documents")
                val copiedDocsDir = context?.let {
                    File(it.filesDir, "copied_documents").apply { if (!exists()) mkdirs() }
                }

                for (i in 0 until docsArray.length()) {
                    val obj = docsArray.getJSONObject(i)
                    var targetFileUri = obj.getString("fileUri")

                    if (obj.has("fileData") && copiedDocsDir != null) {
                        try {
                            val fileDataB64 = obj.getString("fileData")
                            val fileName = obj.optString("fileName", "doc_${System.currentTimeMillis()}_$i.bin")
                            val targetFile = File(copiedDocsDir, fileName)
                            val bytes = Base64.decode(fileDataB64, Base64.NO_WRAP)
                            targetFile.writeBytes(bytes)
                            targetFileUri = targetFile.absolutePath
                        } catch (_: Exception) {}
                    } else if (copiedDocsDir != null) {
                        try {
                            val localCandidate = File(copiedDocsDir, File(targetFileUri).name)
                            if (localCandidate.exists()) {
                                targetFileUri = localCandidate.absolutePath
                            }
                        } catch (_: Exception) {}
                    }

                    repository.insertDocument(
                        DocumentEntity(
                            id = obj.getInt("id"),
                            categoryId = obj.getInt("categoryId"),
                            title = obj.getString("title"),
                            fileUri = targetFileUri,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 11. Insert Attendances
            if (json.has("attendances")) {
                val attArray = json.getJSONArray("attendances")
                val attList = mutableListOf<AttendanceEntity>()
                for (i in 0 until attArray.length()) {
                    val obj = attArray.getJSONObject(i)
                    attList.add(
                        AttendanceEntity(
                            id = obj.getInt("id"),
                            studentId = obj.getInt("studentId"),
                            classId = obj.getInt("classId"),
                            date = obj.getString("date"),
                            timeSlot = obj.getString("timeSlot"),
                            subject = obj.optString("subject", ""),
                            status = obj.optString("status", "ABSENT"),
                            isJustified = obj.optBoolean("isJustified", false),
                            delayMinutes = obj.optInt("delayMinutes", 0),
                            reason = obj.optString("reason", ""),
                            recordedAt = obj.optLong("recordedAt", System.currentTimeMillis())
                        )
                    )
                }
                repository.insertAttendances(attList)
            }
        }

            if (context != null) {
                AppDatabase.getDatabase(context).withTransaction {
                    runImport()
                }
            } else {
                runImport()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
