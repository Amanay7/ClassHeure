package com.example.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

/**
 * Utilitaire d'export PDF personnalisé pour l'application "ClassHeure".
 * Permet de dessiner des rapports académiques de qualité (A4) avec en-têtes personnalisés,
 * bulletins de notes, grilles d'emplois du temps et résumés de cahiers-journal.
 */
object PdfExporter {

    fun getCustomTypeface(context: Context?, fontName: String?, isBold: Boolean = false, isItalic: Boolean = false): Typeface {
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val isAr = (com.example.util.I18n.getLanguage() == "ar") || (com.example.util.Tr.forcedLanguage == "ar")
        return try {
            when (fontName) {
                "Cairo" -> {
                    if (context != null) {
                        val tf = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.cairo)
                        if (tf != null) Typeface.create(tf, style) else Typeface.create(Typeface.SANS_SERIF, style)
                    } else Typeface.create(Typeface.SANS_SERIF, style)
                }
                "Tajawal" -> {
                    if (context != null) {
                        val tf = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.tajawal)
                        if (tf != null) Typeface.create(tf, style) else Typeface.create(Typeface.SANS_SERIF, style)
                    } else Typeface.create(Typeface.SANS_SERIF, style)
                }
                "Almarai" -> {
                    if (context != null) {
                        val tf = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.almarai)
                        if (tf != null) Typeface.create(tf, style) else Typeface.create(Typeface.SANS_SERIF, style)
                    } else Typeface.create(Typeface.SANS_SERIF, style)
                }
                "Amiri" -> {
                    if (context != null) {
                        val tf = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.amiri)
                        if (tf != null) Typeface.create(tf, style) else Typeface.create(Typeface.SERIF, style)
                    } else Typeface.create(Typeface.SERIF, style)
                }
                "Roboto" -> Typeface.create(Typeface.SANS_SERIF, style)
                "Monospace" -> Typeface.create(Typeface.MONOSPACE, style)
                "Serif" -> Typeface.create(Typeface.SERIF, style)
                "Dancing Script" -> {
                    if (context != null) {
                        val tf = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.dancing_script)
                        if (tf != null) Typeface.create(tf, style) else Typeface.create(Typeface.SERIF, style)
                    } else Typeface.create(Typeface.SERIF, style)
                }
                "Great Vibes" -> {
                    if (context != null) {
                        val tf = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.great_vibes)
                        if (tf != null) Typeface.create(tf, style) else Typeface.create(Typeface.SERIF, style)
                    } else Typeface.create(Typeface.SERIF, style)
                }
                "Lora" -> {
                    if (isAr && context != null) {
                        val tf = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.cairo)
                        if (tf != null) Typeface.create(tf, style) else Typeface.create(Typeface.SERIF, style)
                    } else if (context != null) {
                        val tf = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.lora)
                        if (tf != null) Typeface.create(tf, style) else Typeface.create(Typeface.SERIF, style)
                    } else Typeface.create(Typeface.SERIF, style)
                }
                else -> {
                    if (isAr && context != null) {
                        val tf = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.cairo)
                        if (tf != null) Typeface.create(tf, style) else Typeface.create(Typeface.SERIF, style)
                    } else if (context != null) {
                        val tf = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.lora)
                        if (tf != null) Typeface.create(tf, style) else Typeface.create(Typeface.SERIF, style)
                    } else Typeface.create(Typeface.SERIF, style)
                }
            }
        } catch (e: Exception) {
            Typeface.create(Typeface.DEFAULT, style)
        }
    }

    fun generateDossierPdf(
        context: Context,
        outputStream: java.io.OutputStream,
        teacherName: String,
        schoolName: String,
        taughtSubject: String = "",
        classesList: List<ClassEntity>,
        studentsMap: Map<Int, List<StudentEntity>>,
        gradesMap: Map<Int, Map<Int, List<StudentWithGrade>>>, // classId -> (term -> grades)
        timetableMap: Map<Int, List<TimetableSlotEntity>>, // classId -> slots
        journalMap: Map<Int, List<JournalEntryEntity>>, // classId -> journal entries
        selectedClasses: List<Int>,
        selectedTerms: List<Int>,
        includeGrades: Boolean,
        includeTimetable: Boolean,
        includeJournal: Boolean,
        subjectCoefficient: Int = 3,
        fontName: String = "Lora"
    ): Boolean {
        var pdfDocument: PdfDocument? = null
        return try {
            val isArabic = (com.example.util.I18n.getLanguage() == "ar") || (com.example.util.Tr.forcedLanguage == "ar")
            pdfDocument = PdfDocument()
            var pageNumber = 1

            // --- PAGE DE COUVERTURE ---
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create() // Format A4 standard (595x842pt)
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#1F3A60") // Bleu marine académique
                textSize = 28f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 14f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }

            val contentPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#D4AF37") // Doré d'accentuation
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }

            // Dessin des en-têtes et logos textuels de couverture
            if (isArabic) {
                canvas.drawText("ClassHeure", 535f, 150f, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT })
                canvas.drawText("التقرير التربوي والبيداغوجي الشامل", 535f, 180f, Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT })
            } else {
                canvas.drawText("ClassHeure", 60f, 150f, titlePaint)
                canvas.drawText("Rapport Académique Professionnel", 60f, 180f, subtitlePaint)
            }
            canvas.drawLine(60f, 195f, 535f, 195f, linePaint)

            // Informations de l'enseignant et de l'établissement
            var currentY = 250f
            val sy = if (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) >= java.util.Calendar.AUGUST) "${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}/${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1}" else "${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 1}/${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}"
            val genDateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

            if (isArabic) {
                val rightContentPaint = Paint(contentPaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText("المؤسسة : $schoolName", 535f, currentY, rightContentPaint)
                currentY += 25f
                canvas.drawText("السنة الدراسية : $sy", 535f, currentY, rightContentPaint)
                currentY += 25f
                canvas.drawText("المادة : $taughtSubject", 535f, currentY, rightContentPaint)
                currentY += 25f
                canvas.drawText("الأستاذ(ة) : $teacherName", 535f, currentY, rightContentPaint)
                currentY += 25f
                canvas.drawText("تم التوليد في : $genDateStr", 535f, currentY, rightContentPaint)

                currentY += 60f
                canvas.drawText("المحتويات المضمنة في هذا الملف :", 535f, currentY, Paint(rightContentPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                currentY += 20f
                if (includeGrades) {
                    canvas.drawText("- دفاتر النقط وسجلات التقويم (الفصول : ${selectedTerms.joinToString()})", 515f, currentY, rightContentPaint)
                    currentY += 20f
                }
                if (includeTimetable) {
                    canvas.drawText("- جداول التوقيت الأسبوعية", 515f, currentY, rightContentPaint)
                    currentY += 20f
                }
                if (includeJournal) {
                    canvas.drawText("- بطاقات دفتر النصوص والكراس اليومي", 515f, currentY, rightContentPaint)
                    currentY += 20f
                }
            } else {
                canvas.drawText("Établissement : $schoolName", 60f, currentY, contentPaint)
                currentY += 25f
                canvas.drawText("Enseignant : $teacherName", 60f, currentY, contentPaint)
                currentY += 25f
                canvas.drawText("Matière : $taughtSubject", 60f, currentY, contentPaint)
                currentY += 25f
                canvas.drawText("Année scolaire : $sy", 60f, currentY, contentPaint)
                currentY += 25f
                canvas.drawText("Généré le : $genDateStr", 60f, currentY, contentPaint)

                currentY += 60f
                canvas.drawText("Contenu inclus dans ce dossier :", 60f, currentY, Paint(contentPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                currentY += 20f
                if (includeGrades) {
                    canvas.drawText("- Carnets de Notes (Trimestres : ${selectedTerms.joinToString()})", 80f, currentY, contentPaint)
                    currentY += 20f
                }
                if (includeTimetable) {
                    canvas.drawText("- Emplois du Temps Hebdomadaires", 80f, currentY, contentPaint)
                    currentY += 20f
                }
                if (includeJournal) {
                    canvas.drawText("- Cahiers Journal pédagogiques", 80f, currentY, contentPaint)
                    currentY += 20f
                }
            }

            // Bas de page de couverture
            canvas.drawText(if (isArabic) "صفحة 1" else "Page 1", if (isArabic) 60f else 500f, 800f, subtitlePaint)
            pdfDocument.finishPage(page)

            // --- PAGES PAR CLASSE SÉLECTIONNÉE ---
            for (classId in selectedClasses) {
                val currentClass = classesList.find { it.id == classId } ?: continue

                // 1. Carnet de Notes
                if (includeGrades) {
                    for (term in selectedTerms) {
                        val gradesList = gradesMap[classId]?.get(term) ?: emptyList()
                        if (gradesList.isNotEmpty()) {
                            var detailPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                            var detailPage = pdfDocument.startPage(detailPageInfo)
                            var c = detailPage.canvas

                            // En-tête de page
                            val cd = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                            val cal = java.util.Calendar.getInstance()
                            val sy = if (cal.get(java.util.Calendar.MONTH) >= java.util.Calendar.AUGUST) "${cal.get(java.util.Calendar.YEAR)}/${cal.get(java.util.Calendar.YEAR) + 1}" else "${cal.get(java.util.Calendar.YEAR) - 1}/${cal.get(java.util.Calendar.YEAR)}"

                            var currentYLocal = 50f
                            if (isArabic) {
                                val rightH = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 11f; color = Color.BLACK; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
                                val rightSub = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                                c.drawText(schoolName, 545f, currentYLocal, rightH)
                                c.drawText("السنة الدراسية : $sy", 50f, currentYLocal, subtitlePaint)
                                currentYLocal += 15f
                                c.drawText("الأستاذ(ة) : $teacherName", 545f, currentYLocal, rightSub)
                                currentYLocal += 15f
                                c.drawText("المادة : $taughtSubject", 545f, currentYLocal, rightSub)
                            } else {
                                val rightSub = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                                c.drawText(schoolName, 50f, currentYLocal, Paint(subtitlePaint).apply { textSize = 11f; color = Color.BLACK; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                                c.drawText("Année scolaire : $sy", 545f, currentYLocal, rightSub)
                                currentYLocal += 15f
                                c.drawText("Enseignant : $teacherName", 50f, currentYLocal, subtitlePaint)
                                currentYLocal += 15f
                                c.drawText("Matière : $taughtSubject", 50f, currentYLocal, subtitlePaint)
                            }
                            currentYLocal += 12f
                            c.drawLine(50f, currentYLocal, 545f, currentYLocal, Paint(linePaint).apply { strokeWidth = 1.5f })
                            currentYLocal += 30f

                            val classPalette = com.example.ui.ClassColorHelper.getPaletteForClass(0, currentClass.name)
                            val bigTitlePaint = Paint().apply {
                                color = android.graphics.Color.argb(
                                    (classPalette.primary.alpha * 255).toInt(),
                                    (classPalette.primary.red * 255).toInt(),
                                    (classPalette.primary.green * 255).toInt(),
                                    (classPalette.primary.blue * 255).toInt()
                                )
                                textSize = 24f
                                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                                isAntiAlias = true
                            }
                            val titleText = if (isArabic) "دفتر النقاط : ${currentClass.name}" else "CARNET DE NOTES : ${currentClass.name.uppercase()}"
                            val termStr = if (isArabic) when (term) {
                                1 -> "الفصل الأول"
                                2 -> "الفصل الثاني"
                                3 -> "الفصل الثالث"
                                else -> "الفصل الدراسي : $term"
                            } else when (term) {
                                1 -> "1er Trimestre"
                                2 -> "2ème Trimestre"
                                3 -> "3ème Trimestre"
                                else -> "Trimestre : $term"
                            }

                            val termPaint = Paint(subtitlePaint).apply { textSize = 12f; color = android.graphics.Color.DKGRAY }

                            if (isArabic) {
                                bigTitlePaint.textAlign = Paint.Align.RIGHT
                                termPaint.textAlign = Paint.Align.LEFT
                                c.drawText(titleText, 545f, currentYLocal, bigTitlePaint)
                                c.drawText(termStr, 50f, currentYLocal, termPaint)
                            } else {
                                bigTitlePaint.textAlign = Paint.Align.LEFT
                                termPaint.textAlign = Paint.Align.RIGHT
                                c.drawText(titleText, 50f, currentYLocal, bigTitlePaint)
                                c.drawText(termStr, 545f, currentYLocal, termPaint)
                            }

                            currentYLocal += 30f

                            var tableY = currentYLocal

                            // Table des notes (En-tête de colonne)
                            val colX = if (isArabic) {
                                floatArrayOf(540f, 372.5f, 330f, 290f, 250f, 207.5f, 162.5f, 117.5f, 72.5f)
                            } else {
                                floatArrayOf(50f, 165f, 205f, 245f, 285f, 330f, 380f, 430f, 485f)
                            }
                            val headerPaint = Paint(contentPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 9.5f }

                            if (isArabic) {
                                val rightH = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT }
                                val centerH = Paint(headerPaint).apply { textAlign = Paint.Align.CENTER }
                                c.drawText("اللقب والاسم", colX[0], tableY, rightH)
                                c.drawText("مشاركة", colX[1], tableY, centerH)
                                c.drawText("انضباط", colX[2], tableY, centerH)
                                c.drawText("مواظبة", colX[3], tableY, centerH)
                                c.drawText("كراس", colX[4], tableY, centerH)
                                c.drawText("فروض", colX[5], tableY, centerH)
                                c.drawText("مشروع", colX[6], tableY, centerH)
                                c.drawText("اختبار", colX[7], tableY, centerH)
                                c.drawText("المعدل", colX[8], tableY, centerH)
                            } else {
                                c.drawText("Nom / Prénom", colX[0], tableY, headerPaint)
                                c.drawText("Partic.", colX[1], tableY, headerPaint)
                                c.drawText("Discip.", colX[2], tableY, headerPaint)
                                c.drawText("Assid.", colX[3], tableY, headerPaint)
                                c.drawText("Cahier", colX[4], tableY, headerPaint)
                                c.drawText("Devoirs", colX[5], tableY, headerPaint)
                                c.drawText("Projet", colX[6], tableY, headerPaint)
                                c.drawText("Comp.", colX[7], tableY, headerPaint)
                                c.drawText("Moy.", colX[8], tableY, headerPaint)
                            }

                            c.drawLine(50f, tableY + 5f, 545f, tableY + 5f, Paint().apply { color = Color.GRAY; strokeWidth = 1f })
                            tableY += 20f

                            // Lignes des élèves
                            val rowPaint = Paint(contentPaint).apply { textSize = 9f }
                            val rightRowPaint = Paint(rowPaint).apply { textAlign = Paint.Align.RIGHT }
                            val centerRowPaint = Paint(rowPaint).apply { textAlign = Paint.Align.CENTER }

                            for (grade in gradesList) {
                                if (tableY > 780f) {
                                    c.drawText(if (isArabic) "صفحة ${pageNumber - 1}" else "Page ${pageNumber - 1}", if (isArabic) 50f else 500f, 810f, subtitlePaint)
                                    pdfDocument.finishPage(detailPage)

                                    detailPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                                    detailPage = pdfDocument.startPage(detailPageInfo)
                                    c = detailPage.canvas
                                    if (isArabic) {
                                        c.drawText("دفتر النقط - ${currentClass.name} (تابع)", 545f, 60f, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 16f })
                                    } else {
                                        c.drawText("CARNET DE NOTES - ${currentClass.name.uppercase()} (Suite)", 50f, 60f, Paint(titlePaint).apply { textSize = 16f })
                                    }
                                    c.drawLine(50f, 75f, 545f, 75f, Paint(linePaint).apply { strokeWidth = 1.5f })
                                    tableY = 100f
                                }

                                val fullName = "${grade.lastName} ${grade.firstName}"
                                val displayName = if (fullName.length > 20) fullName.substring(0, 18) + ".." else fullName

                                val ex1 = grade.exam1 ?: 0f
                                val ex2 = grade.exam2 ?: 0f
                                val ex3 = grade.exam3 ?: 0f
                                val devCount = (if (grade.exam1 != null) 1 else 0) + (if (grade.exam2 != null) 1 else 0) + (if (grade.exam3 != null) 1 else 0)
                                val examsStr = if (devCount > 0) {
                                    "%.1f".format((ex1 + ex2 + ex3) / devCount.toFloat())
                                } else "-"

                                val average = calculateRowAverage(grade, currentClass.subjectCoefficient)
                                val averageStr = if (average != null) "%.2f".format(average) else "-"

                                if (isArabic) {
                                    c.drawText(displayName, colX[0], tableY, rightRowPaint)
                                    c.drawText(grade.participation?.toString() ?: "-", colX[1], tableY, centerRowPaint)
                                    c.drawText(grade.discipline?.toString() ?: "-", colX[2], tableY, centerRowPaint)
                                    c.drawText(grade.assiduity?.toString() ?: "-", colX[3], tableY, centerRowPaint)
                                    c.drawText(grade.notebook?.toString() ?: "-", colX[4], tableY, centerRowPaint)
                                    c.drawText(examsStr, colX[5], tableY, centerRowPaint)
                                    c.drawText(grade.project?.toString() ?: "-", colX[6], tableY, centerRowPaint)
                                    c.drawText(grade.finalExam?.toString() ?: "-", colX[7], tableY, centerRowPaint)
                                    c.drawText(averageStr, colX[8], tableY, Paint(centerRowPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                                } else {
                                    c.drawText(displayName, colX[0], tableY, rowPaint)
                                    c.drawText(grade.participation?.toString() ?: "-", colX[1], tableY, rowPaint)
                                    c.drawText(grade.discipline?.toString() ?: "-", colX[2], tableY, rowPaint)
                                    c.drawText(grade.assiduity?.toString() ?: "-", colX[3], tableY, rowPaint)
                                    c.drawText(grade.notebook?.toString() ?: "-", colX[4], tableY, rowPaint)
                                    c.drawText(examsStr, colX[5], tableY, rowPaint)
                                    c.drawText(grade.project?.toString() ?: "-", colX[6], tableY, rowPaint)
                                    c.drawText(grade.finalExam?.toString() ?: "-", colX[7], tableY, rowPaint)
                                    c.drawText(averageStr, colX[8], tableY, Paint(rowPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                                }

                                c.drawLine(50f, tableY + 3f, 545f, tableY + 3f, Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 0.5f })
                                tableY += 18f
                            }

                            if (tableY > 750f) {
                                c.drawText(if (isArabic) "صفحة ${pageNumber - 1}" else "Page ${pageNumber - 1}", if (isArabic) 50f else 500f, 810f, subtitlePaint)
                                pdfDocument.finishPage(detailPage)

                                detailPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                                detailPage = pdfDocument.startPage(detailPageInfo)
                                c = detailPage.canvas
                                if (isArabic) {
                                    c.drawText("دفتر النقط - ${currentClass.name}", 545f, 60f, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 18f })
                                } else {
                                    c.drawText("CARNET DE NOTES - ${currentClass.name.uppercase()}", 50f, 60f, Paint(titlePaint).apply { textSize = 18f })
                                }
                                c.drawLine(50f, 75f, 545f, 75f, Paint(linePaint).apply { strokeWidth = 1.5f })
                                tableY = 100f
                            }

                            c.drawLine(50f, tableY, 545f, tableY, Paint(linePaint).apply { strokeWidth = 1f })
                            tableY += 15f
                            if (isArabic) {
                                c.drawText("المعدل العام للفوج", colX[0], tableY, Paint(rowPaint).apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 10f })
                            } else {
                                c.drawText("MOYENNE GÉNÉRALE DE LA CLASSE", colX[0], tableY, Paint(rowPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 10f })
                            }
                            val allAverages = gradesList.mapNotNull { calculateRowAverage(it, currentClass.subjectCoefficient) }
                            val classAverage = if (allAverages.isNotEmpty()) allAverages.average() else null
                            val classAverageStr = classAverage?.let { "%.2f".format(it) } ?: "N/A"
                            c.drawText(classAverageStr, colX[8], tableY, Paint(rowPaint).apply { if (isArabic) textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 10f; color = Color.parseColor("#1F3A60") })

                            c.drawText(if (isArabic) "صفحة ${pageNumber - 1}" else "Page ${pageNumber - 1}", if (isArabic) 50f else 500f, 810f, subtitlePaint)
                            pdfDocument.finishPage(detailPage)
                        }
                    }
                }

                // 2. Emploi du Temps
                if (includeTimetable) {
                    val slotsList = timetableMap[classId] ?: emptyList()
                    if (slotsList.isNotEmpty()) {
                        val timetablePageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                        val timetablePage = pdfDocument.startPage(timetablePageInfo)
                        val tc = timetablePage.canvas

                            val cd = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                            val cal = java.util.Calendar.getInstance()
                            val sy = if (cal.get(java.util.Calendar.MONTH) >= java.util.Calendar.AUGUST) "${cal.get(java.util.Calendar.YEAR)}/${cal.get(java.util.Calendar.YEAR) + 1}" else "${cal.get(java.util.Calendar.YEAR) - 1}/${cal.get(java.util.Calendar.YEAR)}"

                            var currentYLocal = 50f
                            if (isArabic) {
                                val rightH = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 11f; color = android.graphics.Color.BLACK; typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD) }
                                val rightSub = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                                tc.drawText(schoolName, 545f, currentYLocal, rightH)
                                tc.drawText("السنة الدراسية : $sy", 50f, currentYLocal, subtitlePaint)
                                currentYLocal += 15f
                                tc.drawText("الأستاذ(ة) : $teacherName", 545f, currentYLocal, rightSub)
                                currentYLocal += 15f
                                tc.drawText("المادة : $taughtSubject", 545f, currentYLocal, rightSub)
            }
 else {
                                val rightSub = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                                tc.drawText(schoolName, 50f, currentYLocal, Paint(subtitlePaint).apply { textSize = 11f; color = android.graphics.Color.BLACK; typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD) })
                                tc.drawText("Année scolaire : $sy", 545f, currentYLocal, rightSub)
                                currentYLocal += 15f
                                tc.drawText("Enseignant : $teacherName", 50f, currentYLocal, subtitlePaint)
                                currentYLocal += 15f
                                tc.drawText("Matière : $taughtSubject", 50f, currentYLocal, subtitlePaint)

                            }
            currentYLocal += 12f
                            tc.drawLine(50f, currentYLocal, 545f, currentYLocal, Paint(linePaint).apply { strokeWidth = 1.5f })
                            currentYLocal += 30f

                            if (isArabic) {
                                tc.drawText("جدول التوقيت - ${currentClass.name}", 545f, currentYLocal, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 18f })
                            } else {
                                tc.drawText("EMPLOI DU TEMPS - ${currentClass.name.uppercase()}", 50f, currentYLocal, Paint(titlePaint).apply { textSize = 18f })
                            }
                            currentYLocal += 25f

                        var tY = currentYLocal
                        val days = if (isArabic) listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس") else listOf("Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi")
                        val boldPaint = Paint(contentPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
                        val rightBoldPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
                        val rightEntryPaint = Paint(contentPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 10f }

                        for (dayIdx in 1..5) {
                            val daySlots = slotsList.filter { it.dayOfWeek == dayIdx }
                            if (isArabic) {
                                tc.drawText(days[dayIdx - 1], 545f, tY, rightBoldPaint)
                            } else {
                                tc.drawText(days[dayIdx - 1], 50f, tY, boldPaint)
                            }
                            tY += 15f

                            if (daySlots.isEmpty()) {
                                if (isArabic) {
                                    tc.drawText("لا توجد حصص مبرمجة", 525f, tY, Paint(rightEntryPaint).apply { color = Color.GRAY })
                                } else {
                                    tc.drawText("Aucune leçon planifiée", 70f, tY, Paint(contentPaint).apply { color = Color.GRAY; textSize = 10f })
                                }
                                tY += 15f
                            } else {
                                for (slot in daySlots) {
                                    if (isArabic) {
                                        tc.drawText("${slot.timeSlot} : ${slot.subject} (قاعة ${slot.classroom})", 525f, tY, rightEntryPaint)
                                    } else {
                                        tc.drawText("${slot.timeSlot} : ${slot.subject} (Salle ${slot.classroom})", 70f, tY, Paint(contentPaint).apply { textSize = 10f })
                                    }
                                    tY += 15f
                                }
                            }
                            tY += 10f
                        }

                        timetablePage.canvas.drawText(if (isArabic) "صفحة ${pageNumber - 1}" else "Page ${pageNumber - 1}", if (isArabic) 50f else 500f, 810f, subtitlePaint)
                        pdfDocument.finishPage(timetablePage)
                    }
                }

                // 3. Cahier Journal
                if (includeJournal) {
                    val journalList = journalMap[classId] ?: emptyList()
                    if (journalList.isNotEmpty()) {
                        var journalPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                        var journalPage = pdfDocument.startPage(journalPageInfo)
                        var jc = journalPage.canvas

                        val cal = java.util.Calendar.getInstance()
                        val sy = if (cal.get(java.util.Calendar.MONTH) >= java.util.Calendar.AUGUST) "${cal.get(java.util.Calendar.YEAR)}/${cal.get(java.util.Calendar.YEAR) + 1}" else "${cal.get(java.util.Calendar.YEAR) - 1}/${cal.get(java.util.Calendar.YEAR)}"

                        var currentYLocal = 50f
                        if (isArabic) {
                            val rightH = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 11f; color = android.graphics.Color.BLACK; typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD) }
                            val rightSub = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                            jc.drawText(schoolName, 545f, currentYLocal, rightH)
                            jc.drawText("السنة الدراسية : $sy", 50f, currentYLocal, subtitlePaint)
                            currentYLocal += 15f
                            jc.drawText("الأستاذ(ة) : $teacherName", 545f, currentYLocal, rightSub)
                            currentYLocal += 15f
                            jc.drawText("المادة : $taughtSubject", 545f, currentYLocal, rightSub)
                        } else {
                            val rightSub = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                            jc.drawText(schoolName, 50f, currentYLocal, Paint(subtitlePaint).apply { textSize = 11f; color = android.graphics.Color.BLACK; typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD) })
                            jc.drawText("Année scolaire : $sy", 545f, currentYLocal, rightSub)
                            currentYLocal += 15f
                            jc.drawText("Enseignant : $teacherName", 50f, currentYLocal, subtitlePaint)
                            currentYLocal += 15f
                            jc.drawText("Matière : $taughtSubject", 50f, currentYLocal, subtitlePaint)
                        }
                        currentYLocal += 12f
                        jc.drawLine(50f, currentYLocal, 545f, currentYLocal, Paint(linePaint).apply { strokeWidth = 1.5f })
                        currentYLocal += 30f

                        if (isArabic) {
                            jc.drawText("الكراس اليومي - ${currentClass.name}", 545f, currentYLocal, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 18f })
                        } else {
                            jc.drawText("CAHIER JOURNAL - ${currentClass.name.uppercase()}", 50f, currentYLocal, Paint(titlePaint).apply { textSize = 18f })
                        }
                        currentYLocal += 25f

                        var jY = currentYLocal
                        val entryPaint = Paint(contentPaint).apply { textSize = 10f }
                        val boldLabel = Paint(contentPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 10f }
                        val rightBoldLabel = Paint(boldLabel).apply { textAlign = Paint.Align.RIGHT }
                        val rightEntryPaint = Paint(entryPaint).apply { textAlign = Paint.Align.RIGHT }

                        for (entry in journalList) {
                            if (jY > 750f) {
                                jc.drawText(if (isArabic) "صفحة ${pageNumber - 1}" else "Page ${pageNumber - 1}", if (isArabic) 50f else 500f, 810f, subtitlePaint)
                                pdfDocument.finishPage(journalPage)

                                journalPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                                journalPage = pdfDocument.startPage(journalPageInfo)
                                jc = journalPage.canvas
                                if (isArabic) {
                                    jc.drawText("الكراس اليومي - ${currentClass.name} (تابع)", 545f, 60f, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 16f })
                                } else {
                                    jc.drawText("CAHIER JOURNAL - ${currentClass.name.uppercase()} (Suite)", 50f, 60f, Paint(titlePaint).apply { textSize = 16f })
                                }
                                jc.drawLine(50f, 75f, 545f, 75f, Paint(linePaint).apply { strokeWidth = 1.5f })
                                jY = 100f
                            }

                            if (isArabic) {
                                jc.drawText("التاريخ : ${entry.date} (${entry.timeSlot}) - المادة : ${entry.courseSubject}", 545f, jY, rightBoldLabel)
                                jY += 15f
                                jc.drawText("الأهداف : ${entry.objectives}", 525f, jY, rightEntryPaint)
                                jY += 15f
                                jc.drawText("الملاحظات : ${entry.observations}", 525f, jY, rightEntryPaint)
                                jY += 15f
                            } else {
                                jc.drawText("Date: ${entry.date} (${entry.timeSlot}) - Matière: ${entry.courseSubject}", 50f, jY, boldLabel)
                                jY += 15f
                                jc.drawText("Objectifs: ${entry.objectives}", 70f, jY, entryPaint)
                                jY += 15f
                                jc.drawText("Remarques: ${entry.observations}", 70f, jY, entryPaint)
                                jY += 15f
                            }

                            jc.drawLine(50f, jY, 545f, jY, Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 0.5f })
                            jY += 20f
                        }

                        jc.drawText(if (isArabic) "صفحة ${pageNumber - 1}" else "Page ${pageNumber - 1}", if (isArabic) 50f else 500f, 810f, subtitlePaint)
                        pdfDocument.finishPage(journalPage)
                    }
                }
            }

            // Écriture du fichier final
            pdfDocument?.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (e: Exception) {}
        }
    }

    fun generateSingleTimetablePdf(
        outputStream: java.io.OutputStream,
        schoolName: String,
        taughtSubject: String = "",
        teacherName: String,
        className: String,
        slotsList: List<TimetableSlotEntity>,
        classesList: List<ClassEntity>,
        freeDayIndex: Int? = null,
        lunchBreakLabel: String = "Pause déjeuner",
        deletedTimeSlots: Set<String> = emptySet(),
        timeSlotReplacements: Map<String, String> = emptyMap(),
        customTimeSlots: Set<String> = emptySet(),
        schoolYear: String = "",
        fontName: String = "Lora",
        context: Context? = null
    ): Boolean {
        var pdfDocument: PdfDocument? = null
        return try {
            pdfDocument = PdfDocument()
            val pageWidth = 842
            val pageHeight = 595
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            drawTimetableOnCanvas(
                canvas = page.canvas,
                pageWidth = pageWidth.toFloat(),
                pageHeight = pageHeight.toFloat(),
                schoolName = schoolName,
                teacherName = teacherName,
                taughtSubject = taughtSubject,
                className = className,
                slotsList = slotsList,
                classesList = classesList,
                freeDayIndex = freeDayIndex,
                lunchBreakLabel = lunchBreakLabel,
                deletedTimeSlots = deletedTimeSlots,
                timeSlotReplacements = timeSlotReplacements,
                customTimeSlots = customTimeSlots,
                schoolYear = schoolYear,
                fontName = fontName,
                context = context
            )

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (e: Exception) {}
        }
    }

    fun generateAllTimetablesPdf(
        outputStream: java.io.OutputStream,
        schoolName: String,
        teacherName: String,
        taughtSubject: String = "",
        classesList: List<ClassEntity>,
        slotsList: List<TimetableSlotEntity>,
        teacherFreeDay: Int?,
        lunchBreakLabel: String = "Pause déjeuner",
        deletedTimeSlots: Set<String> = emptySet(),
        timeSlotReplacements: Map<String, String> = emptyMap(),
        customTimeSlots: Set<String> = emptySet(),
        schoolYear: String = "",
        fontName: String = "Lora",
        context: Context? = null
    ): Boolean {
        var pdfDocument: PdfDocument? = null
        return try {
            pdfDocument = PdfDocument()
            val pageWidth = 842
            val pageHeight = 595
            var pageNumber = 1

            // 1. First Page: Teacher schedule
            val teacherPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            val teacherPage = pdfDocument.startPage(teacherPageInfo)
            drawTimetableOnCanvas(
                canvas = teacherPage.canvas,
                pageWidth = pageWidth.toFloat(),
                pageHeight = pageHeight.toFloat(),
                schoolName = schoolName,
                teacherName = teacherName,
                taughtSubject = taughtSubject,
                className = "Enseignant (Vue Consolidée)",
                slotsList = slotsList,
                classesList = classesList,
                freeDayIndex = teacherFreeDay,
                lunchBreakLabel = lunchBreakLabel,
                deletedTimeSlots = deletedTimeSlots,
                timeSlotReplacements = timeSlotReplacements,
                customTimeSlots = customTimeSlots,
                schoolYear = schoolYear,
                fontName = fontName,
                context = context
            )
            pdfDocument.finishPage(teacherPage)

            // 2. Subsequent Pages: Each class schedule
            for (cls in classesList) {
                val classSlots = slotsList.filter { it.classId == cls.id }
                val classPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                val classPage = pdfDocument.startPage(classPageInfo)
                drawTimetableOnCanvas(
                    canvas = classPage.canvas,
                    pageWidth = pageWidth.toFloat(),
                    pageHeight = pageHeight.toFloat(),
                    schoolName = schoolName,
                    teacherName = teacherName,
                    taughtSubject = taughtSubject,
                    className = cls.name,
                    slotsList = classSlots,
                    classesList = classesList,
                    freeDayIndex = null,
                    lunchBreakLabel = lunchBreakLabel,
                    deletedTimeSlots = deletedTimeSlots,
                    timeSlotReplacements = timeSlotReplacements,
                    customTimeSlots = customTimeSlots,
                    schoolYear = cls.schoolYear.ifBlank { schoolYear },
                    fontName = fontName,
                    context = context
                )
                pdfDocument.finishPage(classPage)
            }

            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (e: Exception) {}
        }
    }

    private fun drawTimetableOnCanvas(
        canvas: android.graphics.Canvas,
        pageWidth: Float,
        pageHeight: Float,
        schoolName: String,
        teacherName: String,
        taughtSubject: String,
        className: String,
        slotsList: List<TimetableSlotEntity>,
        classesList: List<ClassEntity>,
        freeDayIndex: Int?,
        lunchBreakLabel: String,
        deletedTimeSlots: Set<String>,
        timeSlotReplacements: Map<String, String> = emptyMap(),
        customTimeSlots: Set<String> = emptySet(),
        schoolYear: String = "",
        fontName: String = "Lora",
        context: Context? = null
    ) {
        val isArabic = (com.example.util.I18n.getLanguage() == "ar") || (com.example.util.Tr.forcedLanguage == "ar")
        val primaryColor = Color.parseColor("#336699")
        val accentColor = Color.parseColor("#E65100")
        val freeDayColor = Color.parseColor("#8E6E53")
        val freeDayBg = Color.parseColor("#F5EBE6")
        val lunchBg = Color.parseColor("#FFF3E0")
        val lunchTextColor = Color.parseColor("#D84315")
        val slotBgColor = Color.WHITE
        val borderColor = Color.parseColor("#CBD5E1")

        val titlePaint = Paint().apply {
            color = primaryColor
            textSize = 17f
            typeface = getCustomTypeface(context, fontName, isBold = true)
            isAntiAlias = true
        }
        val headerInfoPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            typeface = getCustomTypeface(context, fontName, isBold = false)
            isAntiAlias = true
        }
        val headerBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = getCustomTypeface(context, fontName, isBold = true)
            isAntiAlias = true
        }
        val datePaint = Paint().apply {
            color = Color.GRAY
            textSize = 8.5f
            typeface = getCustomTypeface(context, fontName, isBold = false)
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = borderColor
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#E1ECF7")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val headerTextPaint = Paint().apply {
            color = primaryColor
            textSize = 9.5f
            typeface = getCustomTypeface(context, fontName, isBold = true)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val slotBgPaint = Paint().apply {
            color = slotBgColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val freeDayBgPaint = Paint().apply {
            color = freeDayBg
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val lunchBgPaint = Paint().apply {
            color = lunchBg
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cellClassPaint = Paint().apply {
            color = primaryColor
            textSize = 11f
            typeface = getCustomTypeface(context, fontName, isBold = true)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val cellSubjectPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
            typeface = getCustomTypeface(context, fontName, isBold = false)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val cellRoomPaint = Paint().apply {
            color = primaryColor
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val marginX = 35f
        val startY = 32f

        val targetClass = classesList.find { it.name.equals(className, ignoreCase = true) }
        val effectiveSchoolYear = when {
            targetClass != null && targetClass.schoolYear.isNotBlank() -> targetClass.schoolYear
            schoolYear.isNotBlank() -> schoolYear
            classesList.any { it.schoolYear.isNotBlank() } -> classesList.first { it.schoolYear.isNotBlank() }.schoolYear
            else -> {
                val cal = java.util.Calendar.getInstance()
                val y = cal.get(java.util.Calendar.YEAR)
                val m = cal.get(java.util.Calendar.MONTH) + 1
                if (m >= 8) "$y / ${y + 1}" else "${y - 1} / $y"
            }
        }

        if (isArabic) {
            val rightBoldPaint = Paint(headerBoldPaint).apply { textAlign = Paint.Align.RIGHT }
            val rightInfoPaint = Paint(headerInfoPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(schoolName.ifBlank { "المؤسسة التعليمية" }, pageWidth - marginX, startY, rightBoldPaint)
            canvas.drawText("الأستاذ(ة) : ${teacherName.ifBlank { "—" }}", pageWidth - marginX, startY + 14f, rightInfoPaint)
            canvas.drawText("المادة : $taughtSubject", pageWidth - marginX, startY + 28f, rightInfoPaint)

            canvas.drawText("السنة الدراسية : $effectiveSchoolYear", marginX, startY, headerInfoPaint)
        } else {
            val rightInfoPaint = Paint(headerInfoPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(schoolName.ifBlank { "Établissement scolaire" }, marginX, startY, headerBoldPaint)
            canvas.drawText("Enseignant : ${teacherName.ifBlank { "—" }}", marginX, startY + 14f, headerInfoPaint)
            canvas.drawText("Matière : $taughtSubject", marginX, startY + 28f, headerInfoPaint)

            canvas.drawText("Année scolaire : $effectiveSchoolYear", pageWidth - marginX, startY, rightInfoPaint)
        }

        val isTeacherTimetable = className.contains("Enseignant", ignoreCase = true) ||
                                 className.contains("الأستاذ", ignoreCase = true) ||
                                 className.contains("الشامل", ignoreCase = true) ||
                                 className == "جدول التوقيت" ||
                                 className.isBlank()
        val title = if (isTeacherTimetable) (if (isArabic) "جدول التوقيت" else "Emploi du temps") else (if (isArabic) "جدول التوقيت : $className" else "EMPLOI DU TEMPS : ${className.uppercase()}")
        val titleWidth = titlePaint.measureText(title)
        canvas.drawText(title, (pageWidth - titleWidth) / 2f, startY + 14f, titlePaint)

        val days = if (isArabic) listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس") else listOf("Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi")

        // Decompose multi-hour slots into 1-hour units to avoid duplicate/redundant header columns
        fun decomposeSlotToUnitHours(slotStr: String): List<String> {
            val clean = slotStr.trim()
            val parts = clean.split("-", "à", "au").map { it.trim() }
            if (parts.size >= 2) {
                val startH = parts[0].split(":", "h").firstOrNull()?.toIntOrNull()
                val startM = parts[0].split(":", "h").getOrNull(1)?.toIntOrNull() ?: 0
                val endH = parts[1].split(":", "h").firstOrNull()?.toIntOrNull()
                val endM = parts[1].split(":", "h").getOrNull(1)?.toIntOrNull() ?: 0
                if (startH != null && endH != null && (endH * 60 + endM - (startH * 60 + startM)) >= 100) {
                    val list = mutableListOf<String>()
                    val sH: Int = startH
                    val eH: Int = endH
                    var curH = sH
                    while (curH < eH) {
                        val nextH = curH + 1
                        val sStr = String.format(java.util.Locale.US, "%02d:%02d", curH, startM)
                        val eStr = String.format(java.util.Locale.US, "%02d:%02d", nextH, startM)
                        list.add("$sStr - $eStr")
                        curH = nextH
                    }
                    return list
                }
            }
            return listOf(clean)
        }

        val allTimeSlots = com.example.util.TimetableTimeSlotHelper.computeHarmonizedTimeSlots(
            allSlots = slotsList,
            timeSlotReplacements = timeSlotReplacements,
            deletedTimeSlots = deletedTimeSlots,
            customTimeSlots = customTimeSlots
        )

        val morningSlots = allTimeSlots.filter { s ->
            val fWord = s.split("-", " ").firstOrNull { it.contains(":") }
            val h = fWord?.split(":")?.firstOrNull()?.toIntOrNull() ?: 0
            h < 12
        }
        val afternoonSlots = allTimeSlots.filter { s ->
            val fWord = s.split("-", " ").firstOrNull { it.contains(":") }
            val h = fWord?.split(":")?.firstOrNull()?.toIntOrNull() ?: 0
            h >= 12
        }.filter { s ->
            // Filter out 16:30 - 17:30 slot if completely empty across all days
            val clean = s.replace(" ", "").replace("h", ":")
            val is1630Slot = clean.contains("16:30-17:30") || (clean.startsWith("16:30") && clean.endsWith("17:30"))
            if (is1630Slot) {
                slotsList.any { slot ->
                    val slotClean = slot.timeSlot.replace(" ", "").replace("h", ":")
                    (slotClean.contains("16:30-17:30") || (slotClean.startsWith("16:30") && slotClean.endsWith("17:30"))) &&
                    slot.subject.isNotBlank()
                }
            } else {
                true
            }
        }

        val tableTop = startY + 50f
        val tableBottom = pageHeight - 35f
        val tableLeft = marginX
        val tableRight = pageWidth - marginX
        val totalTableWidth = tableRight - tableLeft

        val dayColWidth = 72f
        val hasLunchCol = morningSlots.isNotEmpty() && afternoonSlots.isNotEmpty()
        val lunchColWidth = if (hasLunchCol) 46f else 0f
        val totalTimeColCount = morningSlots.size + afternoonSlots.size
        val remainingWidthForSlots = totalTableWidth - dayColWidth - lunchColWidth
        val slotWidth = if (totalTimeColCount > 0) remainingWidthForSlots / totalTimeColCount else 60f

        val headerRowHeight = 32f
        val dayRowHeight = (tableBottom - tableTop - headerRowHeight) / 5f

        // Draw header grid lines and slot texts (LTR vs RTL)
        val lunchColLeft: Float
        val lunchColRight: Float
        val dayColLeft = if (isArabic) tableRight - dayColWidth else tableLeft
        val dayColRight = if (isArabic) tableRight else tableLeft + dayColWidth

        // Background for slot header columns only (la case 'jour' reste vide/non remplie)
        val headerSlotsLeft = if (isArabic) tableLeft else dayColRight
        val headerSlotsRight = if (isArabic) dayColLeft else tableRight
        canvas.drawRect(headerSlotsLeft, tableTop, headerSlotsRight, tableTop + headerRowHeight, headerBgPaint)

        if (isArabic) {
            // RTL Header: Day column on the right
            canvas.drawLine(dayColLeft, tableTop, dayColLeft, tableBottom, borderPaint)

            var curHeaderX = dayColLeft
            for (slot in morningSlots) {
                val sRight = curHeaderX
                val sLeft = curHeaderX - slotWidth
                val midX = (sLeft + sRight) / 2f
                val (sStart, sEnd) = com.example.util.getSlotHoursWithMinutesTwoLines(slot)
                canvas.drawText(sStart, midX, tableTop + 14f, headerTextPaint)
                canvas.drawText(sEnd, midX, tableTop + 26f, headerTextPaint)
                curHeaderX -= slotWidth
                canvas.drawLine(curHeaderX, tableTop, curHeaderX, tableBottom, borderPaint)
            }

            lunchColRight = curHeaderX
            lunchColLeft = lunchColRight - lunchColWidth
            if (hasLunchCol) {
                canvas.drawRect(lunchColLeft, tableTop, lunchColRight, tableTop + headerRowHeight, lunchBgPaint)
                val lunchHeaderPaint = Paint(headerTextPaint).apply { color = lunchTextColor; textSize = 8f }
                canvas.drawText("12:00", (lunchColLeft + lunchColRight) / 2f, tableTop + 13f, lunchHeaderPaint)
                canvas.drawText("استراحة", (lunchColLeft + lunchColRight) / 2f, tableTop + 25f, lunchHeaderPaint)
                curHeaderX -= lunchColWidth
                canvas.drawLine(curHeaderX, tableTop, curHeaderX, tableBottom, borderPaint)
            }

            for (slot in afternoonSlots) {
                val sRight = curHeaderX
                val sLeft = curHeaderX - slotWidth
                val midX = (sLeft + sRight) / 2f
                val (sStart, sEnd) = com.example.util.getSlotHoursWithMinutesTwoLines(slot)
                canvas.drawText(sStart, midX, tableTop + 14f, headerTextPaint)
                canvas.drawText(sEnd, midX, tableTop + 26f, headerTextPaint)
                curHeaderX -= slotWidth
                if (curHeaderX > tableLeft) {
                    canvas.drawLine(curHeaderX, tableTop, curHeaderX, tableBottom, borderPaint)
                }
            }
        } else {
            // LTR Header: Day column on the left
            canvas.drawLine(dayColRight, tableTop, dayColRight, tableBottom, borderPaint)

            var curHeaderX = dayColRight
            for (slot in morningSlots) {
                val midX = curHeaderX + (slotWidth / 2f)
                val (sStart, sEnd) = com.example.util.getSlotHoursWithMinutesTwoLines(slot)
                canvas.drawText(sStart, midX, tableTop + 14f, headerTextPaint)
                canvas.drawText(sEnd, midX, tableTop + 26f, headerTextPaint)
                curHeaderX += slotWidth
                canvas.drawLine(curHeaderX, tableTop, curHeaderX, tableBottom, borderPaint)
            }

            lunchColLeft = curHeaderX
            lunchColRight = lunchColLeft + lunchColWidth
            if (hasLunchCol) {
                canvas.drawRect(lunchColLeft, tableTop, lunchColRight, tableTop + headerRowHeight, lunchBgPaint)
                val lunchHeaderPaint = Paint(headerTextPaint).apply { color = lunchTextColor; textSize = 8f }
                canvas.drawText("12:00", lunchColLeft + (lunchColWidth / 2f), tableTop + 13f, lunchHeaderPaint)
                canvas.drawText("PAUSE", lunchColLeft + (lunchColWidth / 2f), tableTop + 25f, lunchHeaderPaint)
                curHeaderX += lunchColWidth
                canvas.drawLine(curHeaderX, tableTop, curHeaderX, tableBottom, borderPaint)
            }

            for (slot in afternoonSlots) {
                val midX = curHeaderX + (slotWidth / 2f)
                val (sStart, sEnd) = com.example.util.getSlotHoursWithMinutesTwoLines(slot)
                canvas.drawText(sStart, midX, tableTop + 14f, headerTextPaint)
                canvas.drawText(sEnd, midX, tableTop + 26f, headerTextPaint)
                curHeaderX += slotWidth
                if (curHeaderX < tableRight) {
                    canvas.drawLine(curHeaderX, tableTop, curHeaderX, tableBottom, borderPaint)
                }
            }
        }
        canvas.drawLine(tableLeft, tableTop + headerRowHeight, tableRight, tableTop + headerRowHeight, borderPaint)

        for (dayIdx in 1..5) {
            val rowTop = tableTop + headerRowHeight + (dayIdx - 1) * dayRowHeight
            val rowBottom = rowTop + dayRowHeight
            val isFreeDay = (freeDayIndex == dayIdx)

            if (isFreeDay) {
                canvas.drawRect(dayColLeft, rowTop, dayColRight, rowBottom, freeDayBgPaint)
            }
            val dayPaint = Paint(headerTextPaint).apply {
                color = if (isFreeDay) freeDayColor else primaryColor
                textSize = 10f
            }
            canvas.drawText(days[dayIdx - 1], dayColLeft + (dayColWidth / 2f), rowTop + (dayRowHeight / 2f) - (if (isFreeDay) 4f else -4f), dayPaint)
            if (isFreeDay) {
                val badgePaint = Paint().apply {
                    color = freeDayColor
                    textSize = 8f
                    typeface = getCustomTypeface(context, fontName, isBold = true)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText(if (isArabic) "عطلة" else "Libre", dayColLeft + (dayColWidth / 2f), rowTop + (dayRowHeight / 2f) + 10f, badgePaint)
            }

            if (isFreeDay) {
                val freeAreaLeft = if (isArabic) tableLeft else dayColRight
                val freeAreaRight = if (isArabic) dayColLeft else tableRight
                canvas.drawRect(freeAreaLeft, rowTop, freeAreaRight, rowBottom, freeDayBgPaint)
                val freeTextPaint = Paint().apply {
                    color = freeDayColor
                    textSize = 13f
                    typeface = getCustomTypeface(context, fontName, isBold = true)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText(if (isArabic) "☕ يوم حر / عطلة أسبوعية" else "☕ JOURNÉE LIBRE", (freeAreaLeft + freeAreaRight) / 2f, rowTop + (dayRowHeight / 2f) + 5f, freeTextPaint)
            } else {
                val isClassTimetable = !isTeacherTimetable

                fun normalizeClassKey(name: String): String {
                    return name.lowercase(java.util.Locale.getDefault())
                        .replace(" ", "")
                        .replace("è", "e")
                        .replace("é", "e")
                        .replace("ème", "e")
                        .replace("eme", "e")
                        .replace("er", "e")
                        .replace("ere", "e")
                        .replace("°", "")
                        .replace("-", "")
                        .replace("_", "")
                }

                fun drawSlotCard(slot: TimetableSlotEntity, curX: Float, cardWidth: Float = slotWidth) {
                    val cardMargin = 2.5f
                    val midX = curX + (cardWidth / 2f)
                    val midY = rowTop + (dayRowHeight / 2f)
                    val cleanSubj = slot.subject.trim()
                    val isParentReception = cleanSubj.contains("réception", ignoreCase = true) ||
                            cleanSubj.contains("reception", ignoreCase = true) ||
                            cleanSubj.contains("parent", ignoreCase = true)

                    // Match Class and palette
                    val classObj = if (slot.classId != null && slot.classId > 0) {
                        classesList.find { it.id == slot.classId }
                    } else {
                        val normSubj = normalizeClassKey(cleanSubj)
                        classesList.find { cls ->
                            val normCls = normalizeClassKey(cls.name)
                            cleanSubj.equals(cls.name, ignoreCase = true) ||
                            normSubj == normCls ||
                            (normCls.isNotBlank() && (normSubj.contains(normCls) || normCls.contains(normSubj)))
                        }
                    }

                    val effName = if (isParentReception) {
                        if (isArabic) "استقبال الأولياء" else "Réception Parents"
                    } else {
                        classObj?.name ?: (if (cleanSubj.isNotBlank()) cleanSubj else "Classe")
                    }

                    val (primaryColorInt, containerColorInt) = if (isClassTimetable) {
                        Pair(
                            com.example.ui.SubjectColorHelper.getIntColorForSubject(cleanSubj),
                            com.example.ui.SubjectColorHelper.getContainerIntColorForSubject(cleanSubj)
                        )
                    } else if (isParentReception) {
                        Pair(Color.rgb(217, 119, 6), Color.rgb(254, 243, 199))
                    } else if (effName.contains("2SE", ignoreCase = true) || cleanSubj.contains("2SE", ignoreCase = true) || (classObj?.name?.contains("2SE", ignoreCase = true) == true) || slot.classroom.contains("2SE", ignoreCase = true)) {
                        Pair(
                            com.example.ui.ClassColorHelper.getIntColorForClass(0, "2SE"),
                            com.example.ui.ClassColorHelper.getContainerIntColorForClass(0, "2SE")
                        )
                    } else if (classObj != null) {
                        Pair(
                            com.example.ui.ClassColorHelper.getIntColorForClass(classObj.id, classObj.name),
                            com.example.ui.ClassColorHelper.getContainerIntColorForClass(classObj.id, classObj.name)
                        )
                    } else if (slot.colorHex.isNotBlank() && slot.colorHex.startsWith("#") &&
                        !slot.colorHex.equals("#1F3A60", ignoreCase = true) &&
                        !slot.colorHex.equals("#336699", ignoreCase = true)) {
                        try {
                            val parsedColor = Color.parseColor(slot.colorHex)
                            val r = Color.red(parsedColor)
                            val g = Color.green(parsedColor)
                            val b = Color.blue(parsedColor)
                            val lightContainer = Color.rgb(
                                (r + (255 - r) * 0.82f).toInt(),
                                (g + (255 - g) * 0.82f).toInt(),
                                (b + (255 - b) * 0.82f).toInt()
                            )
                            Pair(parsedColor, lightContainer)
                        } catch (e: Exception) {
                            val p = com.example.ui.ClassColorHelper.getPaletteForClass(0, effName)
                            Pair(
                                Color.rgb((p.primary.red * 255).toInt(), (p.primary.green * 255).toInt(), (p.primary.blue * 255).toInt()),
                                Color.rgb((p.container.red * 255).toInt(), (p.container.green * 255).toInt(), (p.container.blue * 255).toInt())
                            )
                        }
                    } else {
                        val p = com.example.ui.ClassColorHelper.getPaletteForClass(0, effName)
                        Pair(
                            Color.rgb((p.primary.red * 255).toInt(), (p.primary.green * 255).toInt(), (p.primary.blue * 255).toInt()),
                            Color.rgb((p.container.red * 255).toInt(), (p.container.green * 255).toInt(), (p.container.blue * 255).toInt())
                        )
                    }

                    val cellBgPaint = Paint().apply {
                        color = Color.WHITE
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val cellBorderPaint = Paint().apply {
                        color = Color.parseColor("#E2E8F0") // Bordure neutre discrète
                        style = Paint.Style.STROKE
                        strokeWidth = 0.8f
                        isAntiAlias = true
                    }

                    // Background with neutral border
                    canvas.drawRoundRect(curX + cardMargin, rowTop + cardMargin, curX + cardWidth - cardMargin, rowBottom - cardMargin, 4f, 4f, cellBgPaint)
                    canvas.drawRoundRect(curX + cardMargin, rowTop + cardMargin, curX + cardWidth - cardMargin, rowBottom - cardMargin, 4f, 4f, cellBorderPaint)

                    if (isClassTimetable) {
                        val dispSubject = slot.subject.ifBlank { if (isArabic) "المادة" else "Matière" }
                        val profOrRoom = slot.classroom.trim()
                        val hasProfOrRoom = profOrRoom.isNotBlank()

                        val subjectPaint = Paint().apply {
                            color = Color.WHITE
                            textSize = 9.5f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        // Auto shrink if single line is too wide
                        val maxTextWidth = cardWidth - 10f
                        while (subjectPaint.measureText(dispSubject) > maxTextWidth && subjectPaint.textSize > 7f) {
                            subjectPaint.textSize -= 0.5f
                        }

                        val twoLines = if (subjectPaint.measureText(dispSubject) > maxTextWidth || dispSubject.contains(" ") || dispSubject.length > 10) {
                            com.example.ui.SubjectColorHelper.splitSubjectIntoTwoLines(dispSubject)
                        } else null

                        // Cadres colorés avec les couleurs de la matière
                        val subjBoxBgPaint = Paint().apply {
                            color = primaryColorInt
                            style = Paint.Style.FILL
                            isAntiAlias = true
                        }
                        val subjBoxStrokePaint = Paint().apply {
                            color = primaryColorInt
                            style = Paint.Style.STROKE
                            strokeWidth = 1f
                            isAntiAlias = true
                        }

                        if (hasProfOrRoom) {
                            // 1. Cadre matière
                            val sBoxHeight = if (twoLines != null) 22f else 16f
                            val sBoxWidth = (if (twoLines != null) maxOf(subjectPaint.measureText(twoLines.first), subjectPaint.measureText(twoLines.second)) else subjectPaint.measureText(dispSubject)) + 10f
                            val sBoxW = sBoxWidth.coerceIn(36f, cardWidth - 6f)
                            val sBoxLeft = midX - (sBoxW / 2f)
                            val sBoxRight = midX + (sBoxW / 2f)
                            val sBoxTop = midY - sBoxHeight + 1f
                            val sBoxBottom = sBoxTop + sBoxHeight

                            canvas.drawRoundRect(sBoxLeft, sBoxTop, sBoxRight, sBoxBottom, 3.5f, 3.5f, subjBoxBgPaint)
                            canvas.drawRoundRect(sBoxLeft, sBoxTop, sBoxRight, sBoxBottom, 3.5f, 3.5f, subjBoxStrokePaint)

                            if (twoLines != null) {
                                val twoLinePaint = Paint(subjectPaint).apply { textSize = 7.8f }
                                canvas.drawText(twoLines.first, midX, sBoxTop + 8.5f, twoLinePaint)
                                canvas.drawText(twoLines.second, midX, sBoxTop + 18f, twoLinePaint)
                            } else {
                                val fontMetrics = subjectPaint.fontMetrics
                                val textY = sBoxTop + (sBoxHeight / 2f) - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
                                canvas.drawText(dispSubject, midX, textY, subjectPaint)
                            }

                            // 2. Cadre dédié Enseignant (sans préfixe "Salle" ou "قاعة")
                            val teacherClean = profOrRoom.replace(Regex("(?i)^salle\\s*"), "").replace(Regex("^(?i)قاعة\\s*"), "").trim()
                            val displayTeacher = teacherClean.ifBlank { if (isArabic) "الأستاذ" else "Enseignant" }

                            val roomBoxBgPaint = Paint().apply {
                                color = Color.parseColor("#CCFBF1")
                                style = Paint.Style.FILL
                                isAntiAlias = true
                            }
                            val roomBoxStrokePaint = Paint().apply {
                                color = Color.parseColor("#14B8A6")
                                style = Paint.Style.STROKE
                                strokeWidth = 0.8f
                                isAntiAlias = true
                            }
                            val roomTextPaint = Paint().apply {
                                color = Color.parseColor("#0F766E")
                                textSize = 7.5f
                                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            while (roomTextPaint.measureText(displayTeacher) > cardWidth - 14f && roomTextPaint.textSize > 5.5f) {
                                roomTextPaint.textSize -= 0.5f
                            }
                            val pTextWidth = roomTextPaint.measureText(displayTeacher)
                            val pBoxW = (pTextWidth + 10f).coerceIn(34f, cardWidth - 6f)
                            val pBoxHeight = 13f
                            val pBoxLeft = midX - (pBoxW / 2f)
                            val pBoxRight = midX + (pBoxW / 2f)
                            val pBoxTop = midY + 4f
                            val pBoxBottom = pBoxTop + pBoxHeight

                            canvas.drawRoundRect(pBoxLeft, pBoxTop, pBoxRight, pBoxBottom, 3.5f, 3.5f, roomBoxBgPaint)
                            canvas.drawRoundRect(pBoxLeft, pBoxTop, pBoxRight, pBoxBottom, 3.5f, 3.5f, roomBoxStrokePaint)

                            val pFontMetrics = roomTextPaint.fontMetrics
                            val pTextY = pBoxTop + (pBoxHeight / 2f) - ((pFontMetrics.descent + pFontMetrics.ascent) / 2f)
                            canvas.drawText(displayTeacher, midX, pTextY, roomTextPaint)
                        } else {
                            // Matière seule, centrée dans son petit cadre coloré
                            val sBoxHeight = if (twoLines != null) 24f else 18f
                            val sBoxWidth = (if (twoLines != null) maxOf(subjectPaint.measureText(twoLines.first), subjectPaint.measureText(twoLines.second)) else subjectPaint.measureText(dispSubject)) + 12f
                            val sBoxW = sBoxWidth.coerceIn(40f, cardWidth - 8f)
                            val sBoxLeft = midX - (sBoxW / 2f)
                            val sBoxRight = midX + (sBoxW / 2f)
                            val sBoxTop = midY - (sBoxHeight / 2f)
                            val sBoxBottom = sBoxTop + sBoxHeight

                            canvas.drawRoundRect(sBoxLeft, sBoxTop, sBoxRight, sBoxBottom, 4f, 4f, subjBoxBgPaint)
                            canvas.drawRoundRect(sBoxLeft, sBoxTop, sBoxRight, sBoxBottom, 4f, 4f, subjBoxStrokePaint)

                            if (twoLines != null) {
                                val twoLinePaint = Paint(subjectPaint).apply { textSize = 8.5f }
                                canvas.drawText(twoLines.first, midX, sBoxTop + 10f, twoLinePaint)
                                canvas.drawText(twoLines.second, midX, sBoxTop + 20f, twoLinePaint)
                            } else {
                                val fontMetrics = subjectPaint.fontMetrics
                                val textY = sBoxTop + (sBoxHeight / 2f) - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
                                canvas.drawText(dispSubject, midX, textY, subjectPaint)
                            }
                        }
                    } else {
                        // Teacher Timetable : Class name inside a dedicated colored rectangle badge
                        if (isParentReception) {
                            val recBgPaint = Paint().apply {
                                color = containerColorInt
                                style = Paint.Style.FILL
                                isAntiAlias = true
                            }
                            val recStrokePaint = Paint().apply {
                                color = primaryColorInt
                                style = Paint.Style.STROKE
                                strokeWidth = 1.2f
                                isAntiAlias = true
                            }
                            val receptionTextPaint = Paint().apply {
                                color = primaryColorInt
                                textSize = 9.5f
                                typeface = getCustomTypeface(context, fontName, isBold = true)
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            val boxW = (cardWidth - 8f).coerceAtLeast(36f)
                            val boxH = 22f
                            val bLeft = midX - (boxW / 2f)
                            val bRight = midX + (boxW / 2f)
                            val bTop = midY - (boxH / 2f)
                            val bBottom = bTop + boxH

                            canvas.drawRoundRect(bLeft, bTop, bRight, bBottom, 4f, 4f, recBgPaint)
                            canvas.drawRoundRect(bLeft, bTop, bRight, bBottom, 4f, 4f, recStrokePaint)

                            if (isArabic) {
                                canvas.drawText("استقبال", midX, midY - 1f, receptionTextPaint)
                                canvas.drawText("الأولياء", midX, midY + 9f, receptionTextPaint)
                            } else {
                                canvas.drawText("Réception", midX, midY - 1f, receptionTextPaint)
                                canvas.drawText("Parents", midX, midY + 9f, receptionTextPaint)
                            }
                        } else {
                            val hasRoom = slot.classroom.isNotBlank()

                            val classTextPaint = Paint().apply {
                                color = primaryColorInt
                                textSize = 13f
                                typeface = getCustomTypeface(context, fontName, isBold = true)
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            val maxW = cardWidth - 8f
                            while (classTextPaint.measureText(effName) > maxW && classTextPaint.textSize > 8.5f) {
                                classTextPaint.textSize -= 0.5f
                            }

                            val cTextWidth = classTextPaint.measureText(effName)
                            val cBoxW = (cTextWidth + 10f).coerceIn(36f, cardWidth - 6f)
                            val cBoxH = if (hasRoom) 16.5f else 22f
                            val cBoxLeft = midX - (cBoxW / 2f)
                            val cBoxRight = midX + (cBoxW / 2f)
                            val cBoxTop = if (hasRoom) midY - 14.5f else midY - (cBoxH / 2f)
                            val cBoxBottom = cBoxTop + cBoxH

                            val classBoxBgPaint = Paint().apply {
                                color = containerColorInt
                                style = Paint.Style.FILL
                                isAntiAlias = true
                            }
                            val classBoxStrokePaint = Paint().apply {
                                color = primaryColorInt
                                style = Paint.Style.STROKE
                                strokeWidth = 1.2f
                                isAntiAlias = true
                            }

                            canvas.drawRoundRect(cBoxLeft, cBoxTop, cBoxRight, cBoxBottom, 4f, 4f, classBoxBgPaint)
                            canvas.drawRoundRect(cBoxLeft, cBoxTop, cBoxRight, cBoxBottom, 4f, 4f, classBoxStrokePaint)

                            val fontMetrics = classTextPaint.fontMetrics
                            val textY = cBoxTop + (cBoxH / 2f) - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
                            canvas.drawText(effName, midX, textY, classTextPaint)

                            // Encadré de la salle (fond blanc/violet clair + bordure, séparé du nom de classe)
                            if (hasRoom) {
                                val roomClean = slot.classroom.replace(Regex("(?i)^salle\\s*"), "").replace(Regex("(?i)^قاعة\\s*"), "").trim()
                                val displayRoom = if (roomClean.isNotBlank()) (if (isArabic) "قاعة $roomClean" else "Salle $roomClean") else slot.classroom

                                val roomTextPaint = Paint().apply {
                                    color = Color.parseColor("#0F766E")
                                    textSize = 6.8f
                                    typeface = getCustomTypeface(context, fontName, isBold = true)
                                    textAlign = Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                val textWidth = roomTextPaint.measureText(displayRoom)
                                val boxPadX = 5f
                                val boxWidth = (textWidth + boxPadX * 2f).coerceIn(32f, cardWidth - 8f)
                                val boxHeight = 11f
                                val boxLeft = midX - (boxWidth / 2f)
                                val boxRight = midX + (boxWidth / 2f)
                                val boxTop = midY + 4f
                                val boxBottom = boxTop + boxHeight

                                val boxBgPaint = Paint().apply {
                                    color = Color.parseColor("#CCFBF1")
                                    style = Paint.Style.FILL
                                    isAntiAlias = true
                                }
                                val boxStrokePaint = Paint().apply {
                                    color = Color.parseColor("#14B8A6")
                                    style = Paint.Style.STROKE
                                    strokeWidth = 1f
                                    isAntiAlias = true
                                }

                                canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 3.5f, 3.5f, boxBgPaint)
                                canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 3.5f, 3.5f, boxStrokePaint)

                                val pFontMetrics = roomTextPaint.fontMetrics
                                val rTextY = boxTop + (boxHeight / 2f) - ((pFontMetrics.descent + pFontMetrics.ascent) / 2f)
                                canvas.drawText(displayRoom, midX, rTextY, roomTextPaint)
                            }
                        }
                    }
                }

                // Helper to match slot by exact string or by time overlap
                fun findSlotForTimeSlot(slots: List<TimetableSlotEntity>, day: Int, targetTimeSlot: String): TimetableSlotEntity? {
                    val direct = slots.find { it.dayOfWeek == day && it.timeSlot.trim().equals(targetTimeSlot.trim(), ignoreCase = true) }
                    if (direct != null) return direct

                    val targetInterval = com.example.util.TimetableTimeSlotHelper.parseSlotInterval(targetTimeSlot) ?: return null
                    return slots.find { s ->
                        if (s.dayOfWeek != day) return@find false
                        val sInterval = com.example.util.TimetableTimeSlotHelper.parseSlotInterval(s.timeSlot) ?: return@find false
                        sInterval.startMin == targetInterval.startMin || (targetInterval.startMin >= sInterval.startMin && targetInterval.endMin <= sInterval.endMin)
                    }
                }

                if (isArabic) {
                    // RTL Slot Layout: From dayColLeft moving left
                    var curCellX = dayColLeft
                    var mIdx = 0
                    while (mIdx < morningSlots.size) {
                        val slotStr = morningSlots[mIdx]
                        val slot = findSlotForTimeSlot(slotsList, dayIdx, slotStr)
                        val nextSlotStr = morningSlots.getOrNull(mIdx + 1)
                        val nextSlot = if (nextSlotStr != null) findSlotForTimeSlot(slotsList, dayIdx, nextSlotStr) else null

                        val is2HourSpan = slot != null && (
                            (nextSlot != null && slot.id == nextSlot.id) ||
                            (nextSlot != null && slot.subject.isNotBlank() &&
                             slot.subject.trim().equals(nextSlot.subject.trim(), ignoreCase = true) &&
                             slot.classroom.trim().equals(nextSlot.classroom.trim(), ignoreCase = true) &&
                             slot.classId == nextSlot.classId)
                        )

                        if (is2HourSpan) {
                            val cardW = slotWidth * 2f
                            drawSlotCard(slot!!, curCellX - cardW, cardW)
                            curCellX -= cardW
                            mIdx += 2
                        } else {
                            if (slot != null) {
                                drawSlotCard(slot, curCellX - slotWidth, slotWidth)
                            }
                            curCellX -= slotWidth
                            mIdx += 1
                        }
                    }

                    if (hasLunchCol) {
                        canvas.drawRect(lunchColLeft, rowTop, lunchColRight, rowBottom, lunchBgPaint)
                        val lunchCellPaint = Paint().apply {
                            color = lunchTextColor
                            textSize = 8.5f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        canvas.drawText("استراحة", (lunchColLeft + lunchColRight) / 2f, rowTop + (dayRowHeight / 2f) + 4f, lunchCellPaint)
                        curCellX -= lunchColWidth
                    }

                    var aIdx = 0
                    while (aIdx < afternoonSlots.size) {
                        val slotStr = afternoonSlots[aIdx]
                        val slot = findSlotForTimeSlot(slotsList, dayIdx, slotStr)
                        val nextSlotStr = afternoonSlots.getOrNull(aIdx + 1)
                        val nextSlot = if (nextSlotStr != null) findSlotForTimeSlot(slotsList, dayIdx, nextSlotStr) else null

                        val is2HourSpan = slot != null && (
                            (nextSlot != null && slot.id == nextSlot.id) ||
                            (nextSlot != null && slot.subject.isNotBlank() &&
                             slot.subject.trim().equals(nextSlot.subject.trim(), ignoreCase = true) &&
                             slot.classroom.trim().equals(nextSlot.classroom.trim(), ignoreCase = true) &&
                             slot.classId == nextSlot.classId)
                        )

                        if (is2HourSpan) {
                            val cardW = slotWidth * 2f
                            drawSlotCard(slot!!, curCellX - cardW, cardW)
                            curCellX -= cardW
                            aIdx += 2
                        } else {
                            if (slot != null) {
                                drawSlotCard(slot, curCellX - slotWidth, slotWidth)
                            }
                            curCellX -= slotWidth
                            aIdx += 1
                        }
                    }
                } else {
                    // LTR Slot Layout: From dayColRight moving right
                    var curCellX = dayColRight
                    var mIdx = 0
                    while (mIdx < morningSlots.size) {
                        val slotStr = morningSlots[mIdx]
                        val slot = findSlotForTimeSlot(slotsList, dayIdx, slotStr)
                        val nextSlotStr = morningSlots.getOrNull(mIdx + 1)
                        val nextSlot = if (nextSlotStr != null) findSlotForTimeSlot(slotsList, dayIdx, nextSlotStr) else null

                        val is2HourSpan = slot != null && (
                            (nextSlot != null && slot.id == nextSlot.id) ||
                            (nextSlot != null && slot.subject.isNotBlank() &&
                             slot.subject.trim().equals(nextSlot.subject.trim(), ignoreCase = true) &&
                             slot.classroom.trim().equals(nextSlot.classroom.trim(), ignoreCase = true) &&
                             slot.classId == nextSlot.classId)
                        )

                        if (is2HourSpan) {
                            drawSlotCard(slot!!, curCellX, slotWidth * 2f)
                            curCellX += slotWidth * 2f
                            mIdx += 2
                        } else {
                            if (slot != null) {
                                drawSlotCard(slot, curCellX, slotWidth)
                            }
                            curCellX += slotWidth
                            mIdx += 1
                        }
                    }

                    if (hasLunchCol) {
                        canvas.drawRect(lunchColLeft, rowTop, lunchColRight, rowBottom, lunchBgPaint)
                        val lunchCellPaint = Paint().apply {
                            color = lunchTextColor
                            textSize = 8.5f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        val words = lunchBreakLabel.split(" ")
                        if (words.size >= 2) {
                            canvas.drawText(words[0], lunchColLeft + (lunchColWidth / 2f), rowTop + (dayRowHeight / 2f) - 3f, lunchCellPaint)
                            canvas.drawText(words.subList(1, words.size).joinToString(" "), lunchColLeft + (lunchColWidth / 2f), rowTop + (dayRowHeight / 2f) + 9f, lunchCellPaint)
                        } else {
                            canvas.drawText(lunchBreakLabel, lunchColLeft + (lunchColWidth / 2f), rowTop + (dayRowHeight / 2f) + 4f, lunchCellPaint)
                        }
                        curCellX += lunchColWidth
                    }

                    var aIdx = 0
                    while (aIdx < afternoonSlots.size) {
                        val slotStr = afternoonSlots[aIdx]
                        val slot = findSlotForTimeSlot(slotsList, dayIdx, slotStr)
                        val nextSlotStr = afternoonSlots.getOrNull(aIdx + 1)
                        val nextSlot = if (nextSlotStr != null) findSlotForTimeSlot(slotsList, dayIdx, nextSlotStr) else null

                        val is2HourSpan = slot != null && (
                            (nextSlot != null && slot.id == nextSlot.id) ||
                            (nextSlot != null && slot.subject.isNotBlank() &&
                             slot.subject.trim().equals(nextSlot.subject.trim(), ignoreCase = true) &&
                             slot.classroom.trim().equals(nextSlot.classroom.trim(), ignoreCase = true) &&
                             slot.classId == nextSlot.classId)
                        )

                        if (is2HourSpan) {
                            drawSlotCard(slot!!, curCellX, slotWidth * 2f)
                            curCellX += slotWidth * 2f
                            aIdx += 2
                        } else {
                            if (slot != null) {
                                drawSlotCard(slot, curCellX, slotWidth)
                            }
                            curCellX += slotWidth
                            aIdx += 1
                        }
                    }
                }
            }
            canvas.drawLine(tableLeft, rowBottom, tableRight, rowBottom, borderPaint)
        }

        canvas.drawRect(tableLeft, tableTop, tableRight, tableBottom, borderPaint)
        if (isArabic) {
            val genDateDayStr = SimpleDateFormat("EEEE dd/MM/yyyy HH:mm", Locale("ar")).format(Date())
            canvas.drawText("تاريخ ويوم الاستخراج : $genDateDayStr", pageWidth - marginX, pageHeight - 15f, Paint(datePaint).apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText("ClassHeure — جدول التوقيت الأسبوعي • وثيقة رسمية", marginX, pageHeight - 15f, datePaint)
        } else {
            val genDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Généré le : $genDateStr", pageWidth - marginX - 110f, pageHeight - 15f, datePaint)
            canvas.drawText("ClassHeure — Emploi du temps officiel", marginX, pageHeight - 15f, datePaint)
        }
    }

    fun generateSingleGradebookPdf(
        outputStream: java.io.OutputStream,
        schoolName: String,
        taughtSubject: String = "",
        teacherName: String,
        className: String,
        term: Int,
        gradesList: List<StudentWithGrade>,
        subjectCoefficient: Int,
        fontName: String = "Lora",
        context: Context? = null,
        isPrimary: Boolean = false
    ): Boolean {
        var pdfDocument: PdfDocument? = null
        return try {
            val isArabic = (com.example.util.I18n.getLanguage() == "ar") || (com.example.util.Tr.forcedLanguage == "ar")
            pdfDocument = PdfDocument()
            var pageNumber = 1
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Paints
            val titlePaint = Paint().apply {
                color = Color.parseColor("#1F3A60")
                textSize = 18f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }
            val subtitlePaint = Paint().apply {
                color = Color.GRAY
                textSize = 10f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#1F3A60")
                strokeWidth = 1.5f
            }
            val contentPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }

            val cd = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val cal = java.util.Calendar.getInstance()
            val sy = if (cal.get(java.util.Calendar.MONTH) >= java.util.Calendar.AUGUST) "${cal.get(java.util.Calendar.YEAR)}/${cal.get(java.util.Calendar.YEAR) + 1}" else "${cal.get(java.util.Calendar.YEAR) - 1}/${cal.get(java.util.Calendar.YEAR)}"

            // Common header
            var currentY = 32f
            if (isArabic) {
                val rightHeaderPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 11f; color = Color.BLACK; typeface = getCustomTypeface(context, fontName, isBold = true) }
                val rightSubPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(schoolName.ifBlank { "المؤسسة التعليمية" }, 545f, currentY, rightHeaderPaint)
                canvas.drawText("الأستاذ(ة) : ${teacherName.ifBlank { "الأستاذ" }}", 545f, currentY + 15f, rightSubPaint)
                canvas.drawText("المادة : $taughtSubject", 545f, currentY + 30f, rightSubPaint)
                canvas.drawText("السنة الدراسية : $sy", 50f, currentY, subtitlePaint)
            } else {
                val rightSubPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(schoolName.ifBlank { "Établissement Scolaire" }, 50f, currentY, Paint(subtitlePaint).apply { textSize = 11f; color = Color.BLACK; typeface = getCustomTypeface(context, fontName, isBold = true) })
                canvas.drawText("Année scolaire : $sy", 545f, currentY, rightSubPaint)
                canvas.drawText("Enseignant : ${teacherName.ifBlank { "L'enseignant" }}", 50f, currentY + 15f, subtitlePaint)
                canvas.drawText("Matière : $taughtSubject", 50f, currentY + 30f, subtitlePaint)

            }

            currentY += 36f
            canvas.drawLine(50f, currentY, 545f, currentY, Paint().apply { color = Color.parseColor("#1E3A8A"); strokeWidth = 2f })
            canvas.drawLine(50f, currentY + 2.5f, 545f, currentY + 2.5f, Paint().apply { color = Color.parseColor("#D97706"); strokeWidth = 1f })
            currentY += 24f

            // Carnet de notes title
            val classPalette = com.example.ui.ClassColorHelper.getPaletteForClass(0, className)
            val bigTitlePaint = Paint().apply {
                color = android.graphics.Color.argb(
                    (classPalette.primary.alpha * 255).toInt(),
                    (classPalette.primary.red * 255).toInt(),
                    (classPalette.primary.green * 255).toInt(),
                    (classPalette.primary.blue * 255).toInt()
                )
                textSize = 18f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }
            val termPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 13f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }

            val titleText = if (isArabic) "دفتر النقاط : $className" else "CARNET DE NOTES : ${className.uppercase()}"
            val termStr = if (isArabic) when (term) {
                1 -> "الفصل الأول"
                2 -> "الفصل الثاني"
                3 -> "الفصل الثالث"
                else -> "الفصل الدراسي : $term"
            } else when (term) {
                1 -> "1er Trimestre"
                2 -> "2ème Trimestre"
                3 -> "3ème Trimestre"
                else -> "Trimestre : $term"
            }

            if (isArabic) {
                bigTitlePaint.textAlign = Paint.Align.RIGHT
                termPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(titleText, 545f, currentY, bigTitlePaint)
                canvas.drawText(termStr, 50f, currentY, termPaint)
            } else {
                bigTitlePaint.textAlign = Paint.Align.LEFT
                termPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(titleText, 50f, currentY, bigTitlePaint)
                canvas.drawText(termStr, 545f, currentY, termPaint)
            }

            var tableY = currentY + 30f
            val colX = if (isArabic) {
                if (isPrimary) {
                    floatArrayOf(540f, 327.5f, 250f, 170f, 90f)
                } else {
                    floatArrayOf(540f, 375f, 305f, 235f, 165f, 95f)
                }
            } else {
                if (isPrimary) {
                    floatArrayOf(50f, 210f, 310f, 400f, 485f)
                } else {
                    floatArrayOf(50f, 215f, 285f, 355f, 425f, 490f)
                }
            }
            val headerPaint = Paint(contentPaint).apply { typeface = getCustomTypeface(context, fontName, isBold = true); textSize = 9.5f }

            if (isArabic) {
                val rightH = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT }
                val centerH = Paint(headerPaint).apply { textAlign = Paint.Align.CENTER }
                if (isPrimary) {
                    canvas.drawText("اللقب والاسم", colX[0], tableY, rightH)
                    canvas.drawText("فروض/تقويم", colX[1], tableY, centerH)
                    canvas.drawText("المشروع", colX[2], tableY, centerH)
                    canvas.drawText("الاختبار", colX[3], tableY, centerH)
                    canvas.drawText("المعدل (/10)", colX[4], tableY, centerH)
                } else {
                    canvas.drawText("اللقب والاسم", colX[0], tableY, rightH)
                    canvas.drawText("تقويم", colX[1], tableY, centerH)
                    canvas.drawText("فروض", colX[2], tableY, centerH)
                    canvas.drawText("مشروع", colX[3], tableY, centerH)
                    canvas.drawText("اختبار", colX[4], tableY, centerH)
                    canvas.drawText("المعدل (/20)", colX[5], tableY, centerH)
                }
            } else {
                if (isPrimary) {
                    canvas.drawText("Nom / Prénom", colX[0], tableY, headerPaint)
                    canvas.drawText("Devoirs/Tests", colX[1], tableY, headerPaint)
                    canvas.drawText("Projet", colX[2], tableY, headerPaint)
                    canvas.drawText("Composition", colX[3], tableY, headerPaint)
                    canvas.drawText("Moy. (/10)", colX[4], tableY, headerPaint)
                } else {
                    canvas.drawText("Nom / Prénom", colX[0], tableY, headerPaint)
                    canvas.drawText("Évaluation", colX[1], tableY, headerPaint)
                    canvas.drawText("Devoirs", colX[2], tableY, headerPaint)
                    canvas.drawText("Projet", colX[3], tableY, headerPaint)
                    canvas.drawText("Comp.", colX[4], tableY, headerPaint)
                    canvas.drawText("Moy. (/20)", colX[5], tableY, headerPaint)
                }
            }

            canvas.drawLine(50f, tableY + 5f, 545f, tableY + 5f, Paint().apply { color = Color.GRAY; strokeWidth = 1f })
            tableY += 20f

            val rowPaint = Paint(contentPaint).apply { textSize = 9f }
            val rightRowPaint = Paint(rowPaint).apply { textAlign = Paint.Align.RIGHT }
            val centerRowPaint = Paint(rowPaint).apply { textAlign = Paint.Align.CENTER }
            val centerBoldRowPaint = Paint(rowPaint).apply { textAlign = Paint.Align.CENTER; typeface = getCustomTypeface(context, fontName, isBold = true) }
            val allAverages = mutableListOf<Float>()

            for (grade in gradesList) {
                if (tableY > 780f) {
                    canvas.drawText(if (isArabic) "الصفحة ${pageNumber - 1}" else "Page ${pageNumber - 1}", if (isArabic) 50f else 500f, 810f, subtitlePaint)
                    pdfDocument.finishPage(page)
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas

                    if (isArabic) {
                        canvas.drawText("دفتر النقاط - $className (تابع)", 545f, 60f, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 16f })
                    } else {
                        canvas.drawText("CARNET DE NOTES - $className (Suite)", 50f, 60f, Paint(titlePaint).apply { textSize = 16f })
                    }
                    canvas.drawLine(50f, 75f, 545f, 75f, linePaint)
                    tableY = 100f
                }

                val fullName = "${grade.lastName} ${grade.firstName}"
                val displayName = if (fullName.length > (if (isPrimary) 26 else 20)) fullName.substring(0, if (isPrimary) 24 else 18) + ".." else fullName

                val ex1 = grade.exam1 ?: 0f
                val ex2 = grade.exam2 ?: 0f
                val ex3 = grade.exam3 ?: 0f
                val devCount = (if (grade.exam1 != null) 1 else 0) + (if (grade.exam2 != null) 1 else 0) + (if (grade.exam3 != null) 1 else 0)
                val examsStr = if (devCount > 0) {
                    "%.1f".format((ex1 + ex2 + ex3) / devCount.toFloat())
                } else "-"

                val eval = com.example.util.GradeCalculator.computeEvaluationSum(grade.participation, grade.discipline, grade.assiduity, grade.notebook)
                val evalStr = if (eval != null) {
                    if (eval % 1f == 0f) eval.toInt().toString() else "%.1f".format(eval)
                } else "-"

                val average = com.example.util.GradeCalculator.computeStudentAverage(grade, subjectCoefficient, isPrimary)
                if (average != null) {
                    allAverages.add(average)
                }
                val averageStr = if (average != null) "%.2f".format(average) else "-"

                if (isArabic) {
                    if (isPrimary) {
                        canvas.drawText(displayName, colX[0], tableY, rightRowPaint)
                        canvas.drawText(examsStr, colX[1], tableY, centerRowPaint)
                        canvas.drawText(grade.project?.toString() ?: "-", colX[2], tableY, centerRowPaint)
                        canvas.drawText(grade.finalExam?.toString() ?: "-", colX[3], tableY, centerRowPaint)
                        canvas.drawText(averageStr, colX[4], tableY, centerBoldRowPaint)
                    } else {
                        canvas.drawText(displayName, colX[0], tableY, rightRowPaint)
                        canvas.drawText(evalStr, colX[1], tableY, centerRowPaint)
                        canvas.drawText(examsStr, colX[2], tableY, centerRowPaint)
                        canvas.drawText(grade.project?.toString() ?: "-", colX[3], tableY, centerRowPaint)
                        canvas.drawText(grade.finalExam?.toString() ?: "-", colX[4], tableY, centerRowPaint)
                        canvas.drawText(averageStr, colX[5], tableY, centerBoldRowPaint)
                    }
                } else {
                    if (isPrimary) {
                        canvas.drawText(displayName, colX[0], tableY, rowPaint)
                        canvas.drawText(examsStr, colX[1], tableY, rowPaint)
                        canvas.drawText(grade.project?.toString() ?: "-", colX[2], tableY, rowPaint)
                        canvas.drawText(grade.finalExam?.toString() ?: "-", colX[3], tableY, rowPaint)
                        canvas.drawText(averageStr, colX[4], tableY, Paint(rowPaint).apply { typeface = getCustomTypeface(context, fontName, isBold = true) })
                    } else {
                        canvas.drawText(displayName, colX[0], tableY, rowPaint)
                        canvas.drawText(evalStr, colX[1], tableY, rowPaint)
                        canvas.drawText(examsStr, colX[2], tableY, rowPaint)
                        canvas.drawText(grade.project?.toString() ?: "-", colX[3], tableY, rowPaint)
                        canvas.drawText(grade.finalExam?.toString() ?: "-", colX[4], tableY, rowPaint)
                        canvas.drawText(averageStr, colX[5], tableY, Paint(rowPaint).apply { typeface = getCustomTypeface(context, fontName, isBold = true) })
                    }
                }

                canvas.drawLine(50f, tableY + 3f, 545f, tableY + 3f, Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 0.5f })
                tableY += 18f
            }

            // Draw General Average at the bottom
            if (tableY > 750f) {
                if (isArabic) {
                    val genDateDayStr = SimpleDateFormat("EEEE dd/MM/yyyy HH:mm", Locale("ar")).format(Date())
                    canvas.drawText("تاريخ ويوم الاستخراج : $genDateDayStr", 545f, 810f, Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8.5f })
                    canvas.drawText("الصفحة ${pageNumber - 1}", 50f, 810f, subtitlePaint)
                } else {
                    val genDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    canvas.drawText("Page ${pageNumber - 1}", 500f, 810f, subtitlePaint)
                    canvas.drawText("Généré le : $genDateStr", 50f, 810f, Paint(subtitlePaint).apply { textSize = 8.5f; color = Color.GRAY })
                }
                pdfDocument.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                tableY = 100f
            }

            tableY += 10f
            canvas.drawLine(50f, tableY, 545f, tableY, Paint(linePaint).apply { strokeWidth = 1f })
            tableY += 20f
            if (isArabic) {
                canvas.drawText("المعدل العام للفوج", colX[0], tableY, Paint(rowPaint).apply { textAlign = Paint.Align.RIGHT; typeface = getCustomTypeface(context, fontName, isBold = true); textSize = 10f })
            } else {
                canvas.drawText("MOYENNE GÉNÉRALE DE LA CLASSE", colX[0], tableY, Paint(rowPaint).apply { typeface = getCustomTypeface(context, fontName, isBold = true); textSize = 10f })
            }

            val classAverage = if (allAverages.isNotEmpty()) allAverages.average() else null
            val classAverageStr = classAverage?.let { "%.2f".format(it) } ?: "N/A"
            val lastColIdx = if (isPrimary) 4 else 5
            canvas.drawText(classAverageStr, colX[lastColIdx], tableY, Paint(rowPaint).apply { if (isArabic) textAlign = Paint.Align.CENTER; typeface = getCustomTypeface(context, fontName, isBold = true); textSize = 10f; color = Color.parseColor("#1F3A60") })

            if (isArabic) {
                val genDateDayStr = SimpleDateFormat("EEEE dd/MM/yyyy HH:mm", Locale("ar")).format(Date())
                canvas.drawText("تاريخ ويوم الاستخراج : $genDateDayStr", 545f, 810f, Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8.5f })
                canvas.drawText("الصفحة ${pageNumber - 1}", 50f, 810f, subtitlePaint)
            } else {
                val genDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                canvas.drawText("Page ${pageNumber - 1}", 500f, 810f, subtitlePaint)
                canvas.drawText("Généré le : $genDateStr", 50f, 810f, Paint(subtitlePaint).apply { textSize = 8.5f; color = Color.GRAY })
            }
            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (e: Exception) {}
        }
    }

    fun generateSingleSeatingPlanPdf(
        outputStream: java.io.OutputStream,
        schoolName: String,
        taughtSubject: String = "",
        teacherName: String,
        className: String,
        studentsList: List<com.example.data.StudentEntity>,
        fontName: String = "Lora",
        context: Context? = null
    ): Boolean {
        var pdfDocument: PdfDocument? = null
        return try {
            val isArabic = (com.example.util.I18n.getLanguage() == "ar") || (com.example.util.Tr.forcedLanguage == "ar")
            pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Paints
            val titlePaint = Paint().apply {
                color = Color.parseColor("#1F3A60")
                textSize = 18f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }
            val subtitlePaint = Paint().apply {
                color = Color.GRAY
                textSize = 10f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#1F3A60")
                strokeWidth = 1.5f
            }
            val rectPaint = Paint().apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val labelPaint = Paint().apply {
                color = Color.parseColor("#1F3A60")
                textSize = 10f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val cd = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val cal = java.util.Calendar.getInstance()
            val sy = if (cal.get(java.util.Calendar.MONTH) >= java.util.Calendar.AUGUST) "${cal.get(java.util.Calendar.YEAR)}/${cal.get(java.util.Calendar.YEAR) + 1}" else "${cal.get(java.util.Calendar.YEAR) - 1}/${cal.get(java.util.Calendar.YEAR)}"

            var currentY = 32f
            if (isArabic) {
                val rightH = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 11f; color = Color.BLACK; typeface = getCustomTypeface(context, fontName, isBold = true) }
                val rightSub = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(schoolName.ifBlank { "المؤسسة التعليمية" }, 545f, currentY, rightH)
                canvas.drawText("السنة الدراسية : $sy", 50f, currentY, subtitlePaint)
                currentY += 15f
                canvas.drawText("الأستاذ(ة) : ${teacherName.ifBlank { "الأستاذ" }}", 545f, currentY, rightSub)
                currentY += 15f
                    canvas.drawText("المادة : $taughtSubject", 545f, currentY, rightSub)
            }
 else {
                val rightSub = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(schoolName.ifBlank { "Établissement Scolaire" }, 50f, currentY, Paint(subtitlePaint).apply { textSize = 11f; color = Color.BLACK; typeface = getCustomTypeface(context, fontName, isBold = true) })
                canvas.drawText("Année scolaire : $sy", 545f, currentY, rightSub)
                currentY += 15f
                canvas.drawText("Enseignant : ${teacherName.ifBlank { "L'enseignant" }}", 50f, currentY, subtitlePaint)
                currentY += 15f
                    canvas.drawText("Matière : $taughtSubject", 50f, currentY, subtitlePaint)

            }
            currentY += 10f
            canvas.drawLine(50f, currentY, 545f, currentY, linePaint)
            currentY += 22f

            if (isArabic) {
                canvas.drawText("مخطط الجلوس - $className", 545f, currentY, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT })
            } else {
                canvas.drawText("PLAN DE CLASSE - $className", 50f, currentY, titlePaint)
            }
            currentY += 18f

            // Draw configured teacher desk ("Bureau" with icon and distinct color)
            val classId = studentsList.firstOrNull()?.classId
            val seatingPrefs = context?.getSharedPreferences("seating_chart_prefs", Context.MODE_PRIVATE)
            val deskPlacement = (if (classId != null) seatingPrefs?.getString("teacher_desk_pos_$classId", null) else null)
                ?: seatingPrefs?.getString("teacher_desk_pos_global", "centre")
                ?: "centre"

            val savedRows = if (classId != null) seatingPrefs?.getInt("seating_rows_$classId", 3) ?: 3 else 3
            val savedTables = if (classId != null) seatingPrefs?.getInt("seating_tables_$classId", 0) ?: 0 else 0
            val rowsCount = if (savedRows > 0) savedRows else 3
            val tablesCount = if (savedTables > 0) savedTables else (if (studentsList.any { (it.seatIndex ?: -1) >= 24 } || studentsList.size > 24) 5 else 4)

            // Dynamic columns calculation (from x=50f to x=545f, width=495f)
            val usableWidth = 495f
            val colWidth = usableWidth / rowsCount
            val colCenters = FloatArray(rowsCount) { i -> 50f + (i + 0.5f) * colWidth }
            val orderedCenters = if (isArabic) colCenters.reversedArray() else colCenters

            val deskCenterIdx = when (deskPlacement.lowercase()) {
                "gauche" -> if (isArabic) rowsCount - 1 else 0
                "droite" -> if (isArabic) 0 else rowsCount - 1
                else -> rowsCount / 2
            }
            val deskCenterX = orderedCenters[deskCenterIdx.coerceIn(0, rowsCount - 1)]
            val deskWidth = minOf(130f, colWidth * 0.9f)
            val deskHeight = 22f
            val deskLeft = deskCenterX - (deskWidth / 2f)
            val deskRight = deskCenterX + (deskWidth / 2f)

            val deskBgPaint = Paint().apply {
                color = Color.parseColor("#E0E7FF")
                style = Paint.Style.FILL
            }
            val deskBorderPaint = Paint().apply {
                color = Color.parseColor("#6366F1")
                strokeWidth = 1.2f
                style = Paint.Style.STROKE
            }
            val deskTextPaint = Paint().apply {
                color = Color.parseColor("#3730A3")
                textSize = 9.5f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }

            canvas.drawRoundRect(deskLeft, currentY, deskRight, currentY + deskHeight, 5f, 5f, deskBgPaint)
            canvas.drawRoundRect(deskLeft, currentY, deskRight, currentY + deskHeight, 5f, 5f, deskBorderPaint)

            val deskWord = if (isArabic) "المكتب" else "Bureau"
            val wordWidth = deskTextPaint.measureText(deskWord)
            val iconWidth = 13f
            val iconSpacing = 6f
            val totalDeskContentWidth = iconWidth + iconSpacing + wordWidth
            val deskContentStartX = deskCenterX - (totalDeskContentWidth / 2f)

            // Draw clean desk icon
            val iconX = deskContentStartX
            val iconY = currentY + (deskHeight - 11f) / 2f
            val iconStrokePaint = Paint(deskTextPaint).apply { strokeWidth = 1.4f }
            canvas.drawLine(iconX, iconY + 2f, iconX + iconWidth, iconY + 2f, iconStrokePaint)
            canvas.drawLine(iconX + 1.5f, iconY + 2f, iconX + 1.5f, iconY + 10f, iconStrokePaint)
            canvas.drawLine(iconX + iconWidth - 1.5f, iconY + 2f, iconX + iconWidth - 1.5f, iconY + 10f, iconStrokePaint)
            canvas.drawRect(iconX + 4f, iconY - 1.5f, iconX + 9f, iconY + 1.5f, Paint(deskTextPaint).apply { style = Paint.Style.FILL })

            canvas.drawText(deskWord, deskContentStartX + iconWidth + iconSpacing, currentY + 14.5f, deskTextPaint)
            currentY += deskHeight + 20f

            val colNames = (0 until rowsCount).map { i ->
                when (rowsCount) {
                    2 -> if (isArabic) (if (i == 0) "الصف الأيمن" else "الصف الأيسر") else (if (i == 0) "Rangée Gauche" else "Rangée Droite")
                    3 -> if (isArabic) listOf("الصف الأيمن", "الصف الأوسط", "الصف الأيسر")[i] else listOf("Rangée Gauche", "Rangée Centre", "Rangée Droite")[i]
                    else -> if (isArabic) "الصف ${i + 1}" else "Rangée ${i + 1}"
                }
            }

            val rowNamesY = currentY
            val tableStartY = rowNamesY + 14f

            val seatLabelPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = if (rowsCount > 3 || tablesCount > 5) 6f else 7f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val maxAvailableHeight = 790f - tableStartY
            val tableStep = minOf(66f, maxAvailableHeight / tablesCount)
            val tableHeight = minOf(45f, tableStep - 8f)
            val tableHalfW = minOf(60f, (colWidth - 10f) / 2f)

            for (colIdx in 0 until rowsCount) {
                val cX = orderedCenters[colIdx]
                canvas.drawText(colNames[colIdx], cX, rowNamesY, labelPaint)

                var tableY = tableStartY
                for (tableIdx in 0 until tablesCount) {
                    val tLeft = cX - tableHalfW
                    val tRight = cX + tableHalfW
                    val tTop = tableY
                    val tBottom = tableY + tableHeight
                    val headerH = minOf(13f, tableHeight * 0.28f)

                    val tableHeaderPaint = Paint().apply {
                        color = Color.parseColor("#E2E8F0")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(tLeft, tTop, tRight, tTop + headerH, tableHeaderPaint)
                    canvas.drawRect(tLeft, tTop, tRight, tTop + headerH, borderPaint)
                    val tableHeaderFontSize = minOf(7.5f, headerH * 0.65f)
                    canvas.drawText(if (isArabic) "طاولة ${tableIdx + 1}" else "Table ${tableIdx + 1}", cX, tTop + headerH - 3f, Paint(labelPaint).apply { textSize = tableHeaderFontSize })

                    canvas.drawRect(tLeft, tTop + headerH, tRight, tBottom, rectPaint)
                    canvas.drawRect(tLeft, tTop + headerH, tRight, tBottom, borderPaint)

                    canvas.drawLine(cX, tTop + headerH, cX, tBottom, borderPaint)

                    val placeOffset = tableHalfW / 2f
                    val nameY = tTop + headerH + (tableHeight - headerH) * 0.72f
                    val labelY = tTop + headerH + (tableHeight - headerH) * 0.32f

                    // Place 1
                    val seatIdx1 = (colIdx * tablesCount + tableIdx) * 2 + 0
                    val occ1 = studentsList.find { it.seatIndex == seatIdx1 }
                    val text1 = occ1?.let { "${it.lastName.uppercase()} ${it.firstName.take(1)}." } ?: (if (isArabic) "(شاغر)" else "(Vide)")
                    val seatLabel1 = if (isArabic) "مقعد ${seatIdx1 + 1}" else "Place ${seatIdx1 + 1}"
                    canvas.drawText(seatLabel1, cX - placeOffset, labelY, seatLabelPaint)
                    canvas.drawText(text1, cX - placeOffset, nameY, textPaint)

                    // Place 2
                    val seatIdx2 = (colIdx * tablesCount + tableIdx) * 2 + 1
                    val occ2 = studentsList.find { it.seatIndex == seatIdx2 }
                    val text2 = occ2?.let { "${it.lastName.uppercase()} ${it.firstName.take(1)}." } ?: (if (isArabic) "(شاغر)" else "(Vide)")
                    val seatLabel2 = if (isArabic) "مقعد ${seatIdx2 + 1}" else "Place ${seatIdx2 + 1}"
                    canvas.drawText(seatLabel2, cX + placeOffset, labelY, seatLabelPaint)
                    canvas.drawText(text2, cX + placeOffset, nameY, textPaint)

                    tableY += tableStep
                }
            }

            // Footer (Toujours au pied de la page à 810f)
            if (isArabic) {
                val genDateDayStr = java.text.SimpleDateFormat("EEEE dd/MM/yyyy HH:mm", java.util.Locale("ar")).format(java.util.Date())
                canvas.drawText("تاريخ ويوم الاستخراج : $genDateDayStr", 545f, 810f, Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8.5f })
                canvas.drawText("الصفحة 1/1", 50f, 810f, subtitlePaint)
            } else {
                val genDateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                canvas.drawText("Généré le : $genDateStr", 50f, 810f, Paint(subtitlePaint).apply { textSize = 8.5f; color = Color.GRAY })
                canvas.drawText("Page 1/1", 500f, 810f, subtitlePaint)
            }

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (e: Exception) {}
        }
    }

    fun generateSingleClassListPdf(
        outputStream: java.io.OutputStream,
        schoolName: String,
        taughtSubject: String = "",
        teacherName: String,
        className: String,
        studentsList: List<com.example.data.StudentEntity>,
        fontName: String = "Lora",
        context: Context? = null,
        groupSubtitle: String = ""
    ): Boolean {
        var pdfDocument: PdfDocument? = null
        return try {
            val isArabic = (com.example.util.I18n.getLanguage() == "ar") || (com.example.util.Tr.forcedLanguage == "ar")
            pdfDocument = PdfDocument()
            var pageNumber = 1

            // Common Paints
            val titlePaint = Paint().apply {
                color = Color.parseColor("#1F3A60")
                textSize = 17f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }
            val subtitlePaint = Paint().apply {
                color = Color.GRAY
                textSize = 9.5f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#1F3A60")
                strokeWidth = 1.5f
            }
            val headerPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9.5f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }
            val rowPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }

            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val cd = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val cal = java.util.Calendar.getInstance()
            val sy = if (cal.get(java.util.Calendar.MONTH) >= java.util.Calendar.AUGUST) "${cal.get(java.util.Calendar.YEAR)}/${cal.get(java.util.Calendar.YEAR) + 1}" else "${cal.get(java.util.Calendar.YEAR) - 1}/${cal.get(java.util.Calendar.YEAR)}"

            var currentY = 32f
            if (isArabic) {
                // Header RTL: Nom établissement au-dessus de celui du prof, matière au-dessous sur la droite, année scolaire sur la gauche
                val rightHeaderPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 11f; color = Color.BLACK; typeface = getCustomTypeface(context, fontName, isBold = true) }
                val rightSubPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(schoolName.ifBlank { "المؤسسة التعليمية" }, 545f, currentY, rightHeaderPaint)
                canvas.drawText("السنة الدراسية : $sy", 50f, currentY, subtitlePaint)
                currentY += 15f
                canvas.drawText("الأستاذ(ة) : ${teacherName.ifBlank { "الأستاذ" }}", 545f, currentY, rightSubPaint)
                currentY += 15f
                    canvas.drawText("المادة : $taughtSubject", 545f, currentY, rightSubPaint)
            }
 else {
                // Header LTR: Établissement, Enseignant, Matière superposés à gauche, Année scolaire à droite
                val rightSubPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(schoolName.ifBlank { "Établissement Scolaire" }, 50f, currentY, Paint(subtitlePaint).apply { textSize = 11f; color = Color.BLACK; typeface = getCustomTypeface(context, fontName, isBold = true) })
                canvas.drawText("Année scolaire : $sy", 545f, currentY, rightSubPaint)
                currentY += 15f
                canvas.drawText("Enseignant : ${teacherName.ifBlank { "L'enseignant" }}", 50f, currentY, subtitlePaint)
                currentY += 15f
                    canvas.drawText("Matière : $taughtSubject", 50f, currentY, subtitlePaint)

            }
            currentY += 6f
            canvas.drawLine(50f, currentY, 545f, currentY, Paint().apply { color = Color.parseColor("#1E3A8A"); strokeWidth = 2f })
            canvas.drawLine(50f, currentY + 2.5f, 545f, currentY + 2.5f, Paint().apply { color = Color.parseColor("#D97706"); strokeWidth = 1f })
            currentY += 24f

            // Title with Group Name if provided
            val groupDisplay = if (groupSubtitle.isNotBlank()) " — $groupSubtitle" else ""
            val titleText = if (isArabic) "قائمة التلاميذ : $className$groupDisplay" else "LISTE DES ÉLÈVES : ${className.uppercase()}$groupDisplay"
            if (isArabic) {
                canvas.drawText(titleText, 545f, currentY, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT })
            } else {
                canvas.drawText(titleText, 50f, currentY, titlePaint)
            }
            currentY += 20f

            // Summary Stats Pill
            val boysCount = studentsList.count { it.gender.equals("M", ignoreCase = true) }
            val girlsCount = studentsList.count { it.gender.equals("F", ignoreCase = true) }
            val statsSummary = if (isArabic) {
                val groupPart = if (groupSubtitle.isNotBlank()) "   |   $groupSubtitle" else ""
                "العدد الإجمالي : ${studentsList.size} تلميذ$groupPart   |   ذكور : $boysCount   |   إناث : $girlsCount"
            } else {
                val groupPart = if (groupSubtitle.isNotBlank()) "   |   $groupSubtitle" else ""
                "Effectif total : ${studentsList.size} élèves$groupPart   |   Garçons : $boysCount   |   Filles : $girlsCount"
            }
            val statBoxPaint = Paint().apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(50f, currentY - 12f, 545f, currentY + 12f, 6f, 6f, statBoxPaint)
            if (isArabic) {
                canvas.drawText(statsSummary, 535f, currentY + 4f, Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; color = Color.parseColor("#334155"); textSize = 9.5f; typeface = getCustomTypeface(context, fontName, isBold = true) })
            } else {
                canvas.drawText(statsSummary, 60f, currentY + 4f, Paint(subtitlePaint).apply { color = Color.parseColor("#334155"); textSize = 9.5f; typeface = getCustomTypeface(context, fontName, isBold = true) })
            }
            currentY += 28f

            // Table Columns & Text Alignments
            fun drawTableHeaders(canv: Canvas, y: Float) {
                if (isArabic) {
                    val rightH = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT }
                    val centerH = Paint(headerPaint).apply { textAlign = Paint.Align.CENTER }
                    canv.drawText("الرقم", 530f, y, centerH)
                    canv.drawText("اللقب والاسم", 505f, y, rightH)
                    canv.drawText("الجنس", 337.5f, y, centerH)
                    canv.drawText("تاريخ الميلاد", 315f, y, rightH)
                    canv.drawText("مكان الميلاد", 195f, y, rightH)
                    canv.drawText("ملاحظات", 125f, y, rightH)
                } else {
                    canv.drawText("N°", 50f, y, headerPaint)
                    canv.drawText("Nom & Prénom", 80f, y, headerPaint)
                    canv.drawText("Sexe", 235f, y, headerPaint)
                    canv.drawText("Date Naissance", 265f, y, headerPaint)
                    canv.drawText("Lieu", 395f, y, headerPaint)
                    canv.drawText("Observations", 475f, y, headerPaint)
                }
                canv.drawLine(50f, y + 5f, 545f, y + 5f, Paint().apply { color = Color.GRAY; strokeWidth = 1f })
            }

            drawTableHeaders(canvas, currentY)
            currentY += 20f

            val rightRowPaint = Paint(rowPaint).apply { textAlign = Paint.Align.RIGHT }
            val centerRowPaint = Paint(rowPaint).apply { textAlign = Paint.Align.CENTER }
            val rightBoldRowPaint = Paint(rowPaint).apply { textAlign = Paint.Align.RIGHT; typeface = getCustomTypeface(context, fontName, isBold = true) }
            val leftBoldRowPaint = Paint(rowPaint).apply { typeface = getCustomTypeface(context, fontName, isBold = true) }

            for ((index, student) in studentsList.withIndex()) {
                if (currentY > 780f) {
                    if (isArabic) {
                        val genDateDayStr = SimpleDateFormat("EEEE dd/MM/yyyy HH:mm", Locale("ar")).format(Date())
                        canvas.drawText("تاريخ ويوم الاستخراج : $genDateDayStr", 545f, 810f, Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8.5f })
                        canvas.drawText("الصفحة ${pageNumber - 1}", 50f, 810f, subtitlePaint)
                    } else {
                        val genDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                        canvas.drawText("Page ${pageNumber - 1}", 500f, 810f, subtitlePaint)
                        canvas.drawText("Généré le : $genDateStr", 50f, 810f, Paint(subtitlePaint).apply { textSize = 8.5f; color = Color.GRAY })
                    }
                    pdfDocument.finishPage(page)

                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    val suiteTitle = if (isArabic) "قائمة التلاميذ : $className$groupDisplay (تابع)" else "LISTE DES ÉLÈVES : ${className.uppercase()}$groupDisplay (Suite)"
                    if (isArabic) {
                        canvas.drawText(suiteTitle, 545f, 60f, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 15f })
                    } else {
                        canvas.drawText(suiteTitle, 50f, 60f, Paint(titlePaint).apply { textSize = 15f })
                    }
                    canvas.drawLine(50f, 75f, 545f, 75f, linePaint)
                    currentY = 100f

                    drawTableHeaders(canvas, currentY)
                    currentY += 20f
                }

                // Row background (alternating zebra)
                if (index % 2 == 1) {
                    val zebraPaint = Paint().apply {
                        color = Color.parseColor("#F8FAFC")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(50f, currentY - 12f, 545f, currentY + 6f, zebraPaint)
                }

                val numStr = (index + 1).toString()
                val fullName = "${student.lastName.uppercase()} ${student.firstName}"
                val genderStr = if (isArabic) {
                    if (student.gender.equals("M", ignoreCase = true)) "ذكر" else if (student.gender.equals("F", ignoreCase = true)) "أنثى" else "-"
                } else {
                    if (student.gender.equals("M", ignoreCase = true)) "M" else if (student.gender.equals("F", ignoreCase = true)) "F" else "-"
                }
                val age = com.example.util.calculateAge(student.birthDate)
                val bDateStr = if (age != null) {
                    if (isArabic) "${student.birthDate}     ($age سنة)" else "${student.birthDate}     ($age ans)"
                } else student.birthDate
                val bPlace = if (student.birthPlace.isNotBlank()) student.birthPlace else "-"
                val obs = if (student.observations.isNotBlank()) student.observations else "-"

                if (isArabic) {
                    // RTL columns
                    canvas.drawText(numStr, 530f, currentY, centerRowPaint)
                    canvas.drawText(if (fullName.length > 25) fullName.take(23) + ".." else fullName, 505f, currentY, rightBoldRowPaint)
                    canvas.drawText(genderStr, 337.5f, currentY, centerRowPaint)
                    canvas.drawText(bDateStr, 315f, currentY, rightRowPaint)
                    canvas.drawText(if (bPlace.length > 13) bPlace.take(11) + ".." else bPlace, 195f, currentY, rightRowPaint)
                    canvas.drawText(if (obs.length > 12) obs.take(10) + ".." else obs, 125f, currentY, rightRowPaint)
                } else {
                    // LTR columns
                    canvas.drawText(numStr, 50f, currentY, rowPaint)
                    canvas.drawText(if (fullName.length > 25) fullName.take(23) + ".." else fullName, 80f, currentY, leftBoldRowPaint)
                    canvas.drawText(genderStr, 235f, currentY, rowPaint)
                    canvas.drawText(bDateStr, 265f, currentY, rowPaint)
                    canvas.drawText(if (bPlace.length > 13) bPlace.take(11) + ".." else bPlace, 395f, currentY, rowPaint)
                    canvas.drawText(if (obs.length > 12) obs.take(10) + ".." else obs, 475f, currentY, rowPaint)
                }

                canvas.drawLine(50f, currentY + 6f, 545f, currentY + 6f, Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 0.5f })
                currentY += 20f
            }

            if (isArabic) {
                val genDateDayStr = SimpleDateFormat("EEEE dd/MM/yyyy HH:mm", Locale("ar")).format(Date())
                canvas.drawText("تاريخ ويوم الاستخراج : $genDateDayStr", 545f, 810f, Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8.5f })
                canvas.drawText("الصفحة ${pageNumber - 1}", 50f, 810f, subtitlePaint)
            } else {
                val genDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                canvas.drawText("Page ${pageNumber - 1}", 500f, 810f, subtitlePaint)
                canvas.drawText("Généré le : $genDateStr", 50f, 810f, Paint(subtitlePaint).apply { textSize = 8.5f; color = Color.GRAY })
            }
            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (e: Exception) {}
        }
    }

    /**
     * Génère un document PDF officiel et professionnel pour le Cahier Journal Pédagogique.
     * Prend en charge les séances filtrées par classe et/ou trimestre, le retour à la ligne
     * des objectifs et observations, et la pagination automatique multi-pages (A4).
     */
    fun generateJournalPdf(
        outputStream: java.io.OutputStream,
        schoolName: String,
        taughtSubject: String = "",
        teacherName: String,
        className: String,
        schoolYear: String,
        term: Int, // 0 = Tous, 1 = T1, 2 = T2, 3 = T3
        entriesList: List<JournalEntryEntity>,
        classesList: List<ClassEntity>,
        fontName: String = "Lora",
        context: Context? = null
    ): Boolean {
        var pdfDocument: PdfDocument? = null
        return try {
            val isArabic = (com.example.util.I18n.getLanguage() == "ar") || (com.example.util.Tr.forcedLanguage == "ar")
            pdfDocument = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#1E3A8A") // Bleu marine académique
                textSize = 18f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 9.5f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#1E3A8A")
                strokeWidth = 1.5f
            }

            val cd = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

            fun drawHeader(cv: Canvas, isFirstPage: Boolean) {
                if (isFirstPage) {
                    var y = 32f
                    if (isArabic) {
                        val rightHeaderPaint = Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 11f; color = Color.BLACK; typeface = getCustomTypeface(context, fontName, isBold = true) }
                        val rightSubPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                        cv.drawText(schoolName.ifBlank { "المؤسسة التعليمية" }, 545f, y, rightHeaderPaint)
                        cv.drawText("السنة الدراسية : $schoolYear", 50f, y, subtitlePaint)
                        y += 15f
                        cv.drawText("الأستاذ(ة) : ${teacherName.ifBlank { "الأستاذ" }}", 545f, y, rightSubPaint)
                        y += 15f
                            cv.drawText("المادة : $taughtSubject", 545f, y, rightSubPaint)
            }
 else {
                        val rightSubPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
                        cv.drawText(schoolName.ifBlank { "Établissement Scolaire" }, 50f, y, Paint(titlePaint).apply { textSize = 11f; color = Color.BLACK; typeface = getCustomTypeface(context, fontName, isBold = true) })
                        cv.drawText("Année scolaire : $schoolYear", 545f, y, rightSubPaint)
                        y += 15f
                        cv.drawText("Enseignant : ${teacherName.ifBlank { "L'enseignant" }}", 50f, y, subtitlePaint)
                        y += 15f
                            cv.drawText("Matière : $taughtSubject", 50f, y, subtitlePaint)

                    }
            y += 6f
                    cv.drawLine(50f, y, 545f, y, Paint().apply { color = Color.parseColor("#1E3A8A"); strokeWidth = 2f })
                    cv.drawLine(50f, y + 2.5f, 545f, y + 2.5f, Paint().apply { color = Color.parseColor("#D97706"); strokeWidth = 1f })
                    y += 24f

                    val bigTitlePaint = Paint().apply {
                        color = Color.parseColor("#1E3A8A")
                        textSize = 20f
                        typeface = getCustomTypeface(context, fontName, isBold = true)
                        isAntiAlias = true
                    }
                    val mainTitle = if (isArabic) "الكراس اليومي البيداغوجي" else "CAHIER JOURNAL PÉDAGOGIQUE"
                    val tw = bigTitlePaint.measureText(mainTitle)
                    cv.drawText(mainTitle, (595f - tw) / 2f, y, bigTitlePaint)

                    y += 16f
                    val termLabel = if (isArabic) {
                        when (term) {
                            1 -> "الفصل الأول"
                            2 -> "الفصل الثاني"
                            3 -> "الفصل الثالث"
                            else -> "جميع الفصول"
                        }
                    } else {
                        when (term) {
                            1 -> "1er Trimestre"
                            2 -> "2ème Trimestre"
                            3 -> "3ème Trimestre"
                            else -> "Tous les trimestres"
                        }
                    }
                    val sub = if (isArabic) "القسم : $className  •  $termLabel" else "Classe : ${className.uppercase()}  •  $termLabel"
                    val subW = subtitlePaint.measureText(sub)
                    cv.drawText(sub, (595f - subW) / 2f, y, Paint(subtitlePaint).apply { textSize = 10.5f; color = Color.parseColor("#334155"); typeface = getCustomTypeface(context, fontName, isBold = true) })

                    y += 20f
                    val summaryText = if (isArabic) "إجمالي الحصص المسجلة : ${entriesList.size} مذكرة" else "Total des séances enregistrées : ${entriesList.size} fiche(s)"
                    val boxPaint = Paint().apply {
                        color = Color.parseColor("#F1F5F9")
                        style = Paint.Style.FILL
                    }
                    cv.drawRoundRect(50f, y - 10f, 545f, y + 14f, 6f, 6f, boxPaint)
                    if (isArabic) {
                        cv.drawText(summaryText, 535f, y + 6f, Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; color = Color.parseColor("#1E293B"); typeface = getCustomTypeface(context, fontName, isBold = true); textSize = 9f })
                    } else {
                        cv.drawText(summaryText, 62f, y + 6f, Paint(subtitlePaint).apply { color = Color.parseColor("#1E293B"); typeface = getCustomTypeface(context, fontName, isBold = true); textSize = 9f })
                    }
                } else {
                    var y = 45f
                    if (isArabic) {
                        cv.drawText("الكراس اليومي البيداغوجي : $className (تابع)", 545f, y, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 13f })
                    } else {
                        cv.drawText("CAHIER JOURNAL PÉDAGOGIQUE : ${className.uppercase()} (Suite)", 50f, y, Paint(titlePaint).apply { textSize = 13f })
                    }
                    y += 12f
                    cv.drawLine(50f, y, 545f, y, linePaint)
                }
            }

            fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
                if (text.isBlank()) return emptyList()
                val lines = mutableListOf<String>()
                val paragraphs = text.split("\n")
                for (paragraph in paragraphs) {
                    val words = paragraph.split(" ")
                    var currentLine = ""
                    for (word in words) {
                        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                        if (paint.measureText(testLine) <= maxWidth) {
                            currentLine = testLine
                        } else {
                            if (currentLine.isNotEmpty()) lines.add(currentLine)
                            currentLine = word
                        }
                    }
                    if (currentLine.isNotEmpty()) lines.add(currentLine)
                }
                return lines
            }

            drawHeader(canvas, true)
            var currentY = 175f

            val labelBoldPaint = Paint().apply {
                color = Color.parseColor("#1E3A8A")
                textSize = 9.5f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 9f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }

            val metaPaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 8.5f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }

            fun drawJournalFooter(cv: Canvas, pNum: Int) {
                if (isArabic) {
                    val genDateDayStr = SimpleDateFormat("EEEE dd/MM/yyyy HH:mm", Locale("ar")).format(Date())
                    cv.drawText("تاريخ ويوم الاستخراج : $genDateDayStr", 545f, 810f, Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8.5f })
                    cv.drawText("الصفحة $pNum", 50f, 810f, subtitlePaint)
                } else {
                    val genDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    cv.drawText("Généré le : $genDateStr", 50f, 810f, Paint(subtitlePaint).apply { textSize = 8.5f; color = Color.GRAY })
                    cv.drawText("Page $pNum", 500f, 810f, subtitlePaint)
                }
            }

            if (entriesList.isEmpty()) {
                canvas.drawText(if (isArabic) "لا توجد مذكرات دروس مسجلة وفق المعايير المحددة." else "Aucune fiche de cours enregistrée pour les critères sélectionnés.", 50f, currentY + 30f, subtitlePaint)
            } else {
                for (entry in entriesList) {
                    val entryClassName = classesList.find { it.id == entry.classId }?.name ?: className
                    val objLines = wrapText(entry.objectives, bodyPaint, 450f)
                    val obsLines = wrapText(entry.observations, bodyPaint, 450f)

                    // Calcul de la hauteur de la fiche
                    var entryHeight = 34f // En-tête + Sujet
                    if (objLines.isNotEmpty()) {
                        entryHeight += 14f + (objLines.size * 12f)
                    }
                    if (obsLines.isNotEmpty()) {
                        entryHeight += 14f + (obsLines.size * 12f)
                    }
                    entryHeight += 10f // Marge intérieure

                    if (currentY + entryHeight > 780f) {
                        drawJournalFooter(canvas, pageNumber - 1)
                        pdfDocument.finishPage(page)

                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas

                        drawHeader(canvas, false)
                        currentY = 75f
                    }

                    val cardLeft = 50f
                    val cardRight = 545f
                    val cardTop = currentY
                    val cardBottom = currentY + entryHeight

                    // Fond de la carte
                    val cardBgPaint = Paint().apply {
                        color = Color.parseColor("#F8FAFC")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, 6f, 6f, cardBgPaint)

                    // Bordure
                    val cardStrokePaint = Paint().apply {
                        color = Color.parseColor("#CBD5E1")
                        style = Paint.Style.STROKE
                        strokeWidth = 0.8f
                    }
                    canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, 6f, 6f, cardStrokePaint)

                    // Bandeau couleur (à droite en arabe, à gauche en français)
                    val stripePaint = Paint().apply {
                        color = Color.parseColor("#2563EB")
                        style = Paint.Style.FILL
                    }
                    if (isArabic) {
                        canvas.drawRoundRect(cardRight - 4f, cardTop, cardRight, cardBottom, 2f, 2f, stripePaint)
                    } else {
                        canvas.drawRoundRect(cardLeft, cardTop, cardLeft + 4f, cardBottom, 2f, 2f, stripePaint)
                    }

                    var insideY = cardTop + 14f

                    val rightMetaPaint = Paint(metaPaint).apply { textAlign = Paint.Align.RIGHT }
                    val rightTitlePaint = Paint(labelBoldPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 10.5f; color = Color.parseColor("#0F172A") }
                    val rightLabelBoldPaint = Paint(labelBoldPaint).apply { textAlign = Paint.Align.RIGHT }
                    val rightObsLabelPaint = Paint(labelBoldPaint).apply { textAlign = Paint.Align.RIGHT; color = Color.parseColor("#059669") }
                    val rightBodyPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }

                    // Ligne 1 : Date, Horaire, Classe
                    val metaLine = if (isArabic) "التاريخ : ${entry.date}   •   التوقيت : ${entry.timeSlot.ifBlank { "08:00 - 09:00" }}   •   القسم : $entryClassName" else "Date : ${entry.date}   •   Horaire : ${entry.timeSlot.ifBlank { "08:00 - 09:00" }}   •   Classe : $entryClassName"
                    if (isArabic) {
                        canvas.drawText(metaLine, cardRight - 12f, insideY, rightMetaPaint)
                    } else {
                        canvas.drawText(metaLine, cardLeft + 12f, insideY, metaPaint)
                    }
                    insideY += 14f

                    // Ligne 2 : Matière, Activité, Titre du cours
                    val titleParts = mutableListOf<String>()
                    if (entry.courseSubject.isNotBlank()) {
                        titleParts.add(if (isArabic) "المادة : ${entry.courseSubject}" else "Matière : ${entry.courseSubject}")
                    }
                    if (entry.activity.isNotBlank()) {
                        titleParts.add(if (isArabic) "النشاط : ${entry.activity}" else "Activité : ${entry.activity}")
                    }
                    if (entry.lessonTitle.isNotBlank()) {
                        titleParts.add(if (isArabic) "عنوان الدرس : ${entry.lessonTitle}" else "Titre : ${entry.lessonTitle}")
                    }
                    val subjectTitle = if (titleParts.isNotEmpty()) {
                        titleParts.joinToString("   •   ")
                    } else {
                        if (isArabic) "المادة / عنوان الحصة : حصة تعليمية" else "Matière / Séance : Séance de cours"
                    }
                    if (isArabic) {
                        canvas.drawText(subjectTitle, cardRight - 12f, insideY, rightTitlePaint)
                    } else {
                        canvas.drawText(subjectTitle, cardLeft + 12f, insideY, Paint(labelBoldPaint).apply { textSize = 10.5f; color = Color.parseColor("#0F172A") })
                    }
                    insideY += 14f

                    // Objectifs
                    if (objLines.isNotEmpty()) {
                        if (isArabic) {
                            canvas.drawText("الأهداف البيداغوجية والتعلمية :", cardRight - 12f, insideY, rightLabelBoldPaint)
                            insideY += 12f
                            for (line in objLines) {
                                canvas.drawText(line, cardRight - 18f, insideY, rightBodyPaint)
                                insideY += 12f
                            }
                        } else {
                            canvas.drawText("Objectifs pédagogiques :", cardLeft + 12f, insideY, labelBoldPaint)
                            insideY += 12f
                            for (line in objLines) {
                                canvas.drawText(line, cardLeft + 18f, insideY, bodyPaint)
                                insideY += 12f
                            }
                        }
                    }

                    // Observations
                    if (obsLines.isNotEmpty()) {
                        if (isArabic) {
                            canvas.drawText("الملاحظات والتقويم :", cardRight - 12f, insideY, rightObsLabelPaint)
                            insideY += 12f
                            for (line in obsLines) {
                                canvas.drawText(line, cardRight - 18f, insideY, rightBodyPaint)
                                insideY += 12f
                            }
                        } else {
                            canvas.drawText("Observations :", cardLeft + 12f, insideY, Paint(labelBoldPaint).apply { color = Color.parseColor("#059669") })
                            insideY += 12f
                            for (line in obsLines) {
                                canvas.drawText(line, cardLeft + 18f, insideY, bodyPaint)
                                insideY += 12f
                            }
                        }
                    }

                    currentY += entryHeight + 10f
                }
            }

            drawJournalFooter(canvas, pageNumber - 1)
            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (e: Exception) {}
        }
    }

    private fun calculateRowAverage(grade: StudentWithGrade, subjectCoefficient: Int): Float? {
        return com.example.util.GradeCalculator.computeStudentAverage(grade, subjectCoefficient)
    }

    fun generateAttendanceReportPdf(
        context: Context,
        outputStream: java.io.OutputStream,
        teacherName: String,
        schoolName: String,
        className: String,
        schoolYear: String,
        attendancesList: List<AttendanceEntity>,
        studentsList: List<StudentEntity>,
        taughtSubject: String = "",
        fontName: String = "Lora",
        filterSubtitle: String = ""
    ): Boolean {
        var pdfDocument: PdfDocument? = null
        return try {
            val isArabic = (com.example.util.I18n.getLanguage() == "ar") || (com.example.util.Tr.forcedLanguage == "ar")
            pdfDocument = PdfDocument()
            var pageNumber = 1

            val incidentRecords = attendancesList.filter { it.status in listOf("ABSENT", "RETARD", "EXCLUDED") }
            val totalAbsences = incidentRecords.count { it.status == "ABSENT" }
            val totalRetards = incidentRecords.count { it.status == "RETARD" }
            val totalExclusions = incidentRecords.count { it.status == "EXCLUDED" }
            val totalJustified = incidentRecords.count { it.isJustified }
            val incidentStudentIds = incidentRecords.map { it.studentId }.toSet()
            val incidentStudents = studentsList.filter { incidentStudentIds.contains(it.id) }
            val effectiveStudentCount = incidentStudents.size

            // Typography & Color Palette
            val navyColor = Color.parseColor("#1E3A8A")
            val amberColor = Color.parseColor("#D97706")
            val slateHeaderColor = Color.parseColor("#0F172A")
            val subtitleColor = Color.parseColor("#475569")
            val borderColor = Color.parseColor("#CBD5E1")
            val lightBorderColor = Color.parseColor("#E2E8F0")

            val headerTitlePaint = Paint().apply {
                color = navyColor
                textSize = 14f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }

            val headerSubPaint = Paint().apply {
                color = subtitleColor
                textSize = 9f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }

            val tableHeaderPaint = Paint().apply {
                color = slateHeaderColor
                textSize = 9f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }

            val cellPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 8.5f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }

            val sectionTitlePaint = Paint().apply {
                color = navyColor
                textSize = 11.5f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = lightBorderColor
                strokeWidth = 0.8f
                style = Paint.Style.STROKE
            }

            // Function to draw academic banner and page footer
            fun drawPageHeaderAndFooter(canv: Canvas, title: String, isFirstPage: Boolean, currentPage: Int) {
                val currentDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                if (isArabic) {
                    val rightH = Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 10f; typeface = getCustomTypeface(context, fontName, isBold = true); color = Color.BLACK }
                    val rightSub = Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT }
                    canv.drawText("المؤسسة : ${schoolName.ifBlank { "المؤسسة التعليمية" }}", 555f, 26f, rightH)
                    canv.drawText("السنة الدراسية : ${schoolYear.ifBlank { "2026/2027" }}", 40f, 26f, headerSubPaint)
                    canv.drawText("الأستاذ(ة) : ${teacherName.ifBlank { "الأستاذ" }}", 555f, 38f, rightSub)
                    if (taughtSubject.isNotBlank()) {
                        canv.drawText("المادة : $taughtSubject", 555f, 50f, rightSub)
                    }
                } else {
                    val rightSub = Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT }
                    canv.drawText("Établissement : ${schoolName.ifBlank { "Lycée" }}", 40f, 26f, Paint(headerSubPaint).apply { textSize = 10f; typeface = getCustomTypeface(context, fontName, isBold = true); color = Color.BLACK })
                    canv.drawText("Année Scolaire : ${schoolYear.ifBlank { "2026/2027" }}", 555f, 26f, rightSub)
                    canv.drawText("Enseignant : ${teacherName.ifBlank { "L'enseignant" }}", 40f, 38f, headerSubPaint)
                    if (taughtSubject.isNotBlank()) {
                        canv.drawText("Matière : $taughtSubject", 40f, 50f, headerSubPaint)
                    }
                }

                // Elegant Dual-Color Academic Bar
                val barY = if (taughtSubject.isNotBlank()) 56f else 46f
                canv.drawLine(40f, barY, 555f, barY, Paint().apply { color = navyColor; strokeWidth = 2f })
                canv.drawLine(40f, barY + 2.5f, 555f, barY + 2.5f, Paint().apply { color = amberColor; strokeWidth = 1f })

                val titleY = barY + 18f
                if (isArabic) {
                    canv.drawText(title, 555f, titleY, Paint(headerTitlePaint).apply { textAlign = Paint.Align.RIGHT })
                    if (filterSubtitle.isNotBlank()) {
                        val periodPaint = Paint().apply {
                            color = Color.parseColor("#1E3A8A")
                            textSize = 9.5f
                            typeface = getCustomTypeface(context, fontName, isBold = true)
                            isAntiAlias = true
                        }
                        val textWidth = periodPaint.measureText(filterSubtitle)
                        val badgeH = 20f
                        val badgeW = textWidth + 18f
                        val badgeX = 40f
                        val badgeY = barY + 7f
                        canv.drawRoundRect(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 6f, 6f, Paint().apply { color = Color.parseColor("#EFF6FF"); style = Paint.Style.FILL })
                        canv.drawRoundRect(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 6f, 6f, Paint().apply { color = Color.parseColor("#93C5FD"); style = Paint.Style.STROKE; strokeWidth = 0.8f })
                        canv.drawText(filterSubtitle, badgeX + 9f, badgeY + 13.5f, periodPaint)
                    }
                } else {
                    canv.drawText(title, 40f, titleY, headerTitlePaint)
                    if (filterSubtitle.isNotBlank()) {
                        val periodPaint = Paint().apply {
                            color = Color.parseColor("#1E3A8A")
                            textSize = 9.5f
                            typeface = getCustomTypeface(context, fontName, isBold = true)
                            isAntiAlias = true
                        }
                        val textWidth = periodPaint.measureText(filterSubtitle)
                        val badgeH = 20f
                        val badgeW = textWidth + 18f
                        val badgeX = 555f - badgeW
                        val badgeY = barY + 7f
                        // Période affichée au-dessous de la ligne limite et au-dessus de l'encadré "Exclusions de cours"
                        canv.drawRoundRect(badgeX, badgeY, 555f, badgeY + badgeH, 6f, 6f, Paint().apply { color = Color.parseColor("#EFF6FF"); style = Paint.Style.FILL })
                        canv.drawRoundRect(badgeX, badgeY, 555f, badgeY + badgeH, 6f, 6f, Paint().apply { color = Color.parseColor("#93C5FD"); style = Paint.Style.STROKE; strokeWidth = 0.8f })
                        canv.drawText(filterSubtitle, badgeX + 9f, badgeY + 13.5f, periodPaint)
                    }
                }

                // Bottom Page Footer
                canv.drawLine(40f, 805f, 555f, 805f, Paint().apply { color = borderColor; strokeWidth = 0.8f })
                if (isArabic) {
                    val genDateDayStr = SimpleDateFormat("EEEE dd/MM/yyyy HH:mm", Locale("ar")).format(Date())
                    canv.drawText("تاريخ ويوم الاستخراج : $genDateDayStr", 555f, 818f, Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8f })
                    canv.drawText("الصفحة $currentPage", 40f, 818f, headerSubPaint)
                } else {
                    canv.drawText("Généré le : $currentDateStr", 40f, 818f, Paint(headerSubPaint).apply { textSize = 8f })
                    canv.drawText("Page $currentPage", 510f, 818f, headerSubPaint)
                }
            }

            // ================= PAGE 1: STATS & TABLEAU D'ASSIDUITÉ =================
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            drawPageHeaderAndFooter(canvas, if (isArabic) "سجل الغيابات والمواظبة — $className" else "REGISTRE DES ABSENCES — $className", true, pageNumber)
            var currentY = 94f

            // --- 4 KPI SUMMARY CARDS (Uniquement Absences, Retards et Exclusions) ---
            val cardWidth = 120f
            val cardHeight = 38f
            val cardSpacing = 11f
            var startX = 40f

            val rightHeaderSub = Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT }

            // KPI 1: Élèves concernés
            canvas.drawRoundRect(startX, currentY, startX + cardWidth, currentY + cardHeight, 6f, 6f, Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL })
            canvas.drawRoundRect(startX, currentY, startX + cardWidth, currentY + cardHeight, 6f, 6f, Paint().apply { color = Color.parseColor("#CBD5E1"); style = Paint.Style.STROKE; strokeWidth = 0.8f })
            if (isArabic) {
                canvas.drawText("$effectiveStudentCount", startX + cardWidth - 10f, currentY + 19f, Paint().apply { textAlign = Paint.Align.RIGHT; color = navyColor; textSize = 13.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("تلاميذ معنيون", startX + cardWidth - 10f, currentY + 31f, rightHeaderSub)
            } else {
                canvas.drawText("$effectiveStudentCount", startX + 10f, currentY + 19f, Paint().apply { color = navyColor; textSize = 13.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("Élèves concernés", startX + 10f, currentY + 31f, headerSubPaint)
            }

            // KPI 2: Absences
            startX += cardWidth + cardSpacing
            canvas.drawRoundRect(startX, currentY, startX + cardWidth, currentY + cardHeight, 6f, 6f, Paint().apply { color = Color.parseColor("#FEF2F2"); style = Paint.Style.FILL })
            canvas.drawRoundRect(startX, currentY, startX + cardWidth, currentY + cardHeight, 6f, 6f, Paint().apply { color = Color.parseColor("#FECACA"); style = Paint.Style.STROKE; strokeWidth = 0.8f })
            if (isArabic) {
                canvas.drawText("$totalAbsences", startX + cardWidth - 10f, currentY + 19f, Paint().apply { textAlign = Paint.Align.RIGHT; color = Color.parseColor("#DC2626"); textSize = 13.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("الغيابات ($totalJustified مبررة)", startX + cardWidth - 10f, currentY + 31f, rightHeaderSub)
            } else {
                canvas.drawText("$totalAbsences", startX + 10f, currentY + 19f, Paint().apply { color = Color.parseColor("#DC2626"); textSize = 13.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("Absences ($totalJustified just.)", startX + 10f, currentY + 31f, headerSubPaint)
            }

            // KPI 3: Retards
            startX += cardWidth + cardSpacing
            canvas.drawRoundRect(startX, currentY, startX + cardWidth, currentY + cardHeight, 6f, 6f, Paint().apply { color = Color.parseColor("#FFFBEB"); style = Paint.Style.FILL })
            canvas.drawRoundRect(startX, currentY, startX + cardWidth, currentY + cardHeight, 6f, 6f, Paint().apply { color = Color.parseColor("#FDE68A"); style = Paint.Style.STROKE; strokeWidth = 0.8f })
            if (isArabic) {
                canvas.drawText("$totalRetards", startX + cardWidth - 10f, currentY + 19f, Paint().apply { textAlign = Paint.Align.RIGHT; color = amberColor; textSize = 13.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("حالات التأخر", startX + cardWidth - 10f, currentY + 31f, rightHeaderSub)
            } else {
                canvas.drawText("$totalRetards", startX + 10f, currentY + 19f, Paint().apply { color = amberColor; textSize = 13.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("Retards enregistrés", startX + 10f, currentY + 31f, headerSubPaint)
            }

            // KPI 4: Exclusions
            startX += cardWidth + cardSpacing
            canvas.drawRoundRect(startX, currentY, startX + cardWidth, currentY + cardHeight, 6f, 6f, Paint().apply { color = Color.parseColor("#F5F3FF"); style = Paint.Style.FILL })
            canvas.drawRoundRect(startX, currentY, startX + cardWidth, currentY + cardHeight, 6f, 6f, Paint().apply { color = Color.parseColor("#DDD6FE"); style = Paint.Style.STROKE; strokeWidth = 0.8f })
            if (isArabic) {
                canvas.drawText("$totalExclusions", startX + cardWidth - 10f, currentY + 19f, Paint().apply { textAlign = Paint.Align.RIGHT; color = Color.parseColor("#7C3AED"); textSize = 13.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("حالات الطرد", startX + cardWidth - 10f, currentY + 31f, rightHeaderSub)
            } else {
                canvas.drawText("$totalExclusions", startX + 10f, currentY + 19f, Paint().apply { color = Color.parseColor("#7C3AED"); textSize = 13.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("Exclusions de cours", startX + 10f, currentY + 31f, headerSubPaint)
            }

            currentY += cardHeight + 16f

            // Section 1: Tableau nominatif des élèves concernés
            if (isArabic) {
                canvas.drawText("1. حصيلة الغيابات والتأخرات", 555f, currentY, Paint(sectionTitlePaint).apply { textAlign = Paint.Align.RIGHT })
            } else {
                canvas.drawText("1. BILAN DES ABSENCES & RETARDS", 40f, currentY, sectionTitlePaint)
            }
            currentY += 8f

            val colX = if (isArabic) {
                floatArrayOf(535f, 505f, 310f, 245f, 180f, 100f)
            } else {
                floatArrayOf(40f, 65f, 260f, 325f, 385f, 445f)
            }

            fun drawTableHeaders(canv: Canvas, y: Float) {
                val headerBg = Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL }
                canv.drawRect(40f, y, 555f, y + 16f, headerBg)
                canv.drawRect(40f, y, 555f, y + 16f, linePaint)

                if (isArabic) {
                    val rightH = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.RIGHT }
                    val centerH = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.CENTER }
                    canv.drawText("الرقم", colX[0], y + 11.5f, centerH)
                    canv.drawText("اللقب والاسم", colX[1], y + 11.5f, rightH)
                    canv.drawText("الغيابات", colX[2], y + 11.5f, centerH)
                    canv.drawText("التأخرات", colX[3], y + 11.5f, centerH)
                    canv.drawText("الطرد", colX[4], y + 11.5f, centerH)
                    canv.drawText("وضعية المواظبة", colX[5], y + 11.5f, centerH)
                } else {
                    canv.drawText("N°", colX[0] + 4f, y + 11.5f, tableHeaderPaint)
                    canv.drawText("Nom & Prénom de l'élève", colX[1] + 5f, y + 11.5f, tableHeaderPaint)
                    canv.drawText("Absences", colX[2] + 5f, y + 11.5f, tableHeaderPaint)
                    canv.drawText("Retards", colX[3] + 5f, y + 11.5f, tableHeaderPaint)
                    canv.drawText("Exclusions", colX[4] + 5f, y + 11.5f, tableHeaderPaint)
                    canv.drawText("Statut Assiduité", colX[5] + 5f, y + 11.5f, tableHeaderPaint)
                }
            }

            drawTableHeaders(canvas, currentY)
            currentY += 16f

            if (incidentStudents.isEmpty()) {
                canvas.drawRect(40f, currentY, 555f, currentY + 18f, linePaint)
                if (isArabic) {
                    canvas.drawText("لا توجد أي غيابات أو تأخرات أو حالات طرد مسجلة.", 550f, currentY + 12f, Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT })
                } else {
                    canvas.drawText("Aucune absence, aucun retard ni exclusion enregistré pour cette sélection.", 50f, currentY + 12f, headerSubPaint)
                }
                currentY += 18f
            } else {
                incidentStudents.forEachIndexed { index, student ->
                    val studentRecs = attendancesList.filter { it.studentId == student.id }
                    val absCount = studentRecs.count { it.status == "ABSENT" }
                    val retCount = studentRecs.count { it.status == "RETARD" }
                    val excCount = studentRecs.count { it.status == "EXCLUDED" }

                    if (currentY + 16f > 790f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas

                        drawPageHeaderAndFooter(canvas, if (isArabic) "سجل الغيابات والمواظبة — $className (تابع)" else "REGISTRE DES ABSENCES — $className (Suite)", false, pageNumber)
                        currentY = 92f
                        drawTableHeaders(canvas, currentY)
                        currentY += 16f
                    }

                    // Alternating row background
                    if (index % 2 == 1) {
                        canvas.drawRect(40f, currentY, 555f, currentY + 15f, Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL })
                    }
                    canvas.drawRect(40f, currentY, 555f, currentY + 15f, linePaint)

                    val idxStr = "${index + 1}"
                    val nameStr = "${student.lastName} ${student.firstName}"
                    val statusStr = if (isArabic) {
                        if (absCount == 0 && retCount == 0 && excCount == 0) "مواظب" else if (absCount >= 3) "تنبيه متابعة" else "عادي"
                    } else {
                        if (absCount == 0 && retCount == 0 && excCount == 0) "Assidu" else if (absCount >= 3) "Alerte Suivi" else "Régulier"
                    }

                    if (isArabic) {
                        val rightCell = Paint(cellPaint).apply { textAlign = Paint.Align.RIGHT }
                        val centerCell = Paint(cellPaint).apply { textAlign = Paint.Align.CENTER }
                        canvas.drawText(idxStr, colX[0], currentY + 11f, centerCell)
                        canvas.drawText(nameStr, colX[1], currentY + 11f, rightCell)

                        val absP = Paint(centerCell).apply { if (absCount > 0) { color = Color.parseColor("#DC2626"); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) } }
                        canvas.drawText("$absCount", colX[2], currentY + 11f, absP)

                        val retP = Paint(centerCell).apply { if (retCount > 0) { color = amberColor; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) } }
                        canvas.drawText("$retCount", colX[3], currentY + 11f, retP)

                        val excP = Paint(centerCell).apply { if (excCount > 0) { color = Color.parseColor("#7C3AED"); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) } }
                        canvas.drawText("$excCount", colX[4], currentY + 11f, excP)

                        val statusP = Paint(centerCell).apply {
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            color = when {
                                absCount >= 3 -> Color.parseColor("#DC2626")
                                absCount == 0 && retCount == 0 && excCount == 0 -> Color.parseColor("#059669")
                                else -> Color.parseColor("#2563EB")
                            }
                        }
                        canvas.drawText(statusStr, colX[5], currentY + 11f, statusP)
                    } else {
                        canvas.drawText(idxStr, colX[0] + 5f, currentY + 11f, cellPaint)
                        canvas.drawText(nameStr, colX[1] + 5f, currentY + 11f, cellPaint)

                        val absP = Paint(cellPaint).apply { if (absCount > 0) { color = Color.parseColor("#DC2626"); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) } }
                        canvas.drawText("$absCount", colX[2] + 12f, currentY + 11f, absP)

                        val retP = Paint(cellPaint).apply { if (retCount > 0) { color = amberColor; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) } }
                        canvas.drawText("$retCount", colX[3] + 12f, currentY + 11f, retP)

                        val excP = Paint(cellPaint).apply { if (excCount > 0) { color = Color.parseColor("#7C3AED"); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) } }
                        canvas.drawText("$excCount", colX[4] + 12f, currentY + 11f, excP)

                        val statusP = Paint(cellPaint).apply {
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            color = when {
                                absCount >= 3 -> Color.parseColor("#DC2626")
                                absCount == 0 && retCount == 0 && excCount == 0 -> Color.parseColor("#059669")
                                else -> Color.parseColor("#2563EB")
                            }
                        }
                        canvas.drawText(statusStr, colX[5] + 5f, currentY + 11f, statusP)
                    }

                    currentY += 15f
                }
            }

            // ================= JOURNAL DÉTAILLÉ DES SÉANCES & INCIDENTS =================
            val nonPresentRecords = attendancesList.filter { it.status in listOf("ABSENT", "RETARD", "EXCLUDED") }
                .sortedWith(compareByDescending<AttendanceEntity> { it.date }.thenBy { it.timeSlot })

            if (nonPresentRecords.isNotEmpty()) {
                currentY += 18f
                if (currentY + 50f > 790f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawPageHeaderAndFooter(canvas, if (isArabic) "سجل الغيابات والتأخرات — $className" else "JOURNAL DES ABSENCES — $className", false, pageNumber)
                    currentY = 92f
                }

                if (isArabic) {
                    canvas.drawText("2. السجل المفصل للغيابات والتأخرات", 555f, currentY, Paint(sectionTitlePaint).apply { textAlign = Paint.Align.RIGHT })
                } else {
                    canvas.drawText("2. JOURNAL DES ABSENCES & RETARDS", 40f, currentY, sectionTitlePaint)
                }
                currentY += 8f

                val jColX = if (isArabic) {
                    floatArrayOf(522f, 460f, 422f, 250f, 175f, 135f)
                } else {
                    floatArrayOf(40f, 100f, 175f, 325f, 410f, 480f)
                }

                fun drawJournalHeaders(canv: Canvas, y: Float) {
                    val headerBg = Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL }
                    canv.drawRect(40f, y, 555f, y + 16f, headerBg)
                    canv.drawRect(40f, y, 555f, y + 16f, linePaint)

                    if (isArabic) {
                        val rightH = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.RIGHT }
                        val centerH = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.CENTER }
                        canv.drawText("التاريخ", jColX[0], y + 11.5f, centerH)
                        canv.drawText("التوقيت", jColX[1], y + 11.5f, centerH)
                        canv.drawText("التلميذ المعني", jColX[2], y + 11.5f, rightH)
                        canv.drawText("نوع المخالفة", jColX[3], y + 11.5f, centerH)
                        canv.drawText("الوضعية", jColX[4], y + 11.5f, centerH)
                        canv.drawText("السبب / المبرر", jColX[5], y + 11.5f, rightH)
                    } else {
                        canv.drawText("Date", jColX[0] + 4f, y + 11.5f, tableHeaderPaint)
                        canv.drawText("Horaire", jColX[1] + 4f, y + 11.5f, tableHeaderPaint)
                        canv.drawText("Élève concerné", jColX[2] + 4f, y + 11.5f, tableHeaderPaint)
                        canv.drawText("Type incident", jColX[3] + 4f, y + 11.5f, tableHeaderPaint)
                        canv.drawText("Statut", jColX[4] + 4f, y + 11.5f, tableHeaderPaint)
                        canv.drawText("Motif", jColX[5] + 4f, y + 11.5f, tableHeaderPaint)
                    }
                }

                drawJournalHeaders(canvas, currentY)
                currentY += 16f

                nonPresentRecords.forEachIndexed { idx, att ->
                    val st = studentsList.find { it.id == att.studentId }

                    if (currentY + 15f > 790f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas

                        drawPageHeaderAndFooter(canvas, if (isArabic) "سجل الغيابات والتأخرات — $className (تابع)" else "JOURNAL DES ABSENCES — $className (Suite)", false, pageNumber)
                        currentY = 92f
                        drawJournalHeaders(canvas, currentY)
                        currentY += 16f
                    }

                    if (idx % 2 == 1) {
                        canvas.drawRect(40f, currentY, 555f, currentY + 15f, Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL })
                    }
                    canvas.drawRect(40f, currentY, 555f, currentY + 15f, linePaint)

                    // Format date to DD-MM-YYYY
                    val dateParts = att.date.split("-")
                    val dStr = if (dateParts.size == 3 && dateParts[0].length == 4) "${dateParts[2]}/${dateParts[1]}/${dateParts[0]}" else att.date

                    val typeStr = if (isArabic) {
                        when (att.status) {
                            "ABSENT" -> "غياب"
                            "RETARD" -> "تأخر (${att.delayMinutes}د)"
                            "EXCLUDED" -> "طرد"
                            else -> "حاضر"
                        }
                    } else {
                        when (att.status) {
                            "ABSENT" -> "Absence"
                            "RETARD" -> "Retard (${att.delayMinutes}m)"
                            "EXCLUDED" -> "Exclusion"
                            else -> "Présent"
                        }
                    }

                    val justStr = if (isArabic) {
                        if (att.isJustified) "مبرر" else "غير مبرر"
                    } else {
                        if (att.isJustified) "Justifié" else "Non justifié"
                    }

                    val reasonStr = if (att.reason.isNotBlank()) {
                        if (att.reason.length > 15) att.reason.substring(0, 13) + ".." else att.reason
                    } else "-"

                    val studentName = if (st != null) "${st.lastName} ${st.firstName}" else (if (isArabic) "تلميذ #${att.studentId}" else "Élève #${att.studentId}")

                    if (isArabic) {
                        val rightCell = Paint(cellPaint).apply { textAlign = Paint.Align.RIGHT }
                        val centerCell = Paint(cellPaint).apply { textAlign = Paint.Align.CENTER }
                        canvas.drawText(dStr, jColX[0], currentY + 11f, centerCell)
                        canvas.drawText(att.timeSlot, jColX[1], currentY + 11f, centerCell)
                        canvas.drawText(studentName, jColX[2], currentY + 11f, rightCell)

                        val typePaint = Paint(centerCell).apply {
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            color = when (att.status) {
                                "ABSENT" -> Color.parseColor("#DC2626")
                                "RETARD" -> amberColor
                                "EXCLUDED" -> Color.parseColor("#7C3AED")
                                else -> Color.parseColor("#059669")
                            }
                        }
                        canvas.drawText(typeStr, jColX[3], currentY + 11f, typePaint)

                        val justPaint = Paint(centerCell).apply {
                            color = if (att.isJustified) Color.parseColor("#059669") else Color.parseColor("#DC2626")
                        }
                        canvas.drawText(justStr, jColX[4], currentY + 11f, justPaint)
                        canvas.drawText(reasonStr, jColX[5], currentY + 11f, rightCell)
                    } else {
                        canvas.drawText(dStr, jColX[0] + 4f, currentY + 11f, cellPaint)
                        canvas.drawText(att.timeSlot, jColX[1] + 4f, currentY + 11f, cellPaint)
                        canvas.drawText(studentName, jColX[2] + 4f, currentY + 11f, cellPaint)

                        val typePaint = Paint(cellPaint).apply {
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            color = when (att.status) {
                                "ABSENT" -> Color.parseColor("#DC2626")
                                "RETARD" -> amberColor
                                "EXCLUDED" -> Color.parseColor("#7C3AED")
                                else -> Color.parseColor("#059669")
                            }
                        }
                        canvas.drawText(typeStr, jColX[3] + 4f, currentY + 11f, typePaint)

                        val justPaint = Paint(cellPaint).apply {
                            color = if (att.isJustified) Color.parseColor("#059669") else Color.parseColor("#DC2626")
                        }
                        canvas.drawText(justStr, jColX[4] + 4f, currentY + 11f, justPaint)
                        canvas.drawText(reasonStr, jColX[5] + 4f, currentY + 11f, cellPaint)
                    }

                    currentY += 15f
                }
            }

            // Signature & Cachet stamp box at bottom of final page
            if (currentY + 50f < 780f) {
                currentY += 16f
                val signBoxWidth = 175f
                val signX = if (isArabic) 40f else (555f - signBoxWidth)
                canvas.drawRoundRect(signX, currentY, signX + signBoxWidth, currentY + 42f, 4f, 4f, Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL })
                canvas.drawRoundRect(signX, currentY, signX + signBoxWidth, currentY + 42f, 4f, 4f, linePaint)
                if (isArabic) {
                    canvas.drawText("توقيع وختم الأستاذ(ة) :", signX + signBoxWidth - 8f, currentY + 14f, Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); color = Color.parseColor("#334155") })
                } else {
                    canvas.drawText("Signature & Cachet de l'Enseignant :", signX + 8f, currentY + 12f, Paint(headerSubPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); color = Color.parseColor("#334155") })
                }
            }

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (e: Exception) {}
        }
    }

    fun generateClassStatisticsPdf(
        outputStream: java.io.OutputStream,
        schoolName: String,
        taughtSubject: String = "",
        teacherName: String,
        className: String,
        schoolYear: String,
        studentsList: List<StudentEntity>,
        gradesList: List<StudentWithGrade>,
        subjectCoefficient: Int,
        isPrimary: Boolean,
        fontName: String = "Lora",
        context: Context? = null
    ): Boolean {
        var pdfDocument: PdfDocument? = null
        return try {
            val isArabic = (com.example.util.I18n.getLanguage() == "ar") || (com.example.util.Tr.forcedLanguage == "ar")
            pdfDocument = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val navyColor = Color.parseColor("#1E3A8A")
            val amberColor = Color.parseColor("#D97706")
            val successColor = Color.parseColor("#059669")
            val dangerColor = Color.parseColor("#DC2626")
            val borderColor = Color.parseColor("#CBD5E1")

            val titlePaint = Paint().apply {
                color = navyColor
                textSize = 15f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }
            val headerSubPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }
            val sectionTitlePaint = Paint().apply {
                color = navyColor
                textSize = 11.5f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }
            val cellPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8.5f
                typeface = getCustomTypeface(context, fontName, isBold = false)
                isAntiAlias = true
            }
            val tableHeaderPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 8.5f
                typeface = getCustomTypeface(context, fontName, isBold = true)
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = borderColor
                strokeWidth = 0.8f
                style = Paint.Style.STROKE
            }

            fun drawHeaderAndFooter(canv: Canvas, title: String, pNum: Int) {
                if (isArabic) {
                    val rightH = Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 10f; typeface = getCustomTypeface(context, fontName, isBold = true); color = Color.BLACK }
                    val rightSub = Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT }
                    canv.drawText("المؤسسة : ${schoolName.ifBlank { "المؤسسة التعليمية" }}", 555f, 32f, rightH)
                    canv.drawText("الأستاذ(ة) : ${teacherName.ifBlank { "الأستاذ" }}", 555f, 46f, rightSub)
                    canv.drawText("المادة : ${taughtSubject.ifBlank { "التعليم العام" }}  •  القسم : $className", 555f, 60f, rightSub)
                    canv.drawText("السنة الدراسية : ${schoolYear.ifBlank { "2026/2027" }}", 40f, 32f, headerSubPaint)
                } else {
                    val rightSub = Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT }
                    canv.drawText("Établissement : ${schoolName.ifBlank { "Établissement Scolaire" }}", 40f, 32f, Paint(headerSubPaint).apply { textSize = 10f; typeface = getCustomTypeface(context, fontName, isBold = true); color = Color.BLACK })
                    canv.drawText("Année Scolaire : ${schoolYear.ifBlank { "2026/2027" }}", 555f, 32f, rightSub)
                    canv.drawText("Enseignant : ${teacherName.ifBlank { "L'enseignant" }}", 40f, 46f, headerSubPaint)
                    canv.drawText("Matière : ${taughtSubject.ifBlank { "Enseignement Général" }}  •  Classe : $className", 40f, 60f, headerSubPaint)
            }

                canv.drawLine(40f, 68f, 555f, 68f, Paint().apply { color = navyColor; strokeWidth = 2f })
                canv.drawLine(40f, 70.5f, 555f, 70.5f, Paint().apply { color = amberColor; strokeWidth = 1f })

                val detectedTerm = gradesList.firstOrNull { it.term != null }?.term ?: 1
                if (isArabic) {
                    canv.drawText(title, 555f, 88f, Paint(titlePaint).apply { textAlign = Paint.Align.RIGHT })

                    // 3- le trimestre plus visible et coloré
                    val badgeText = "الفصل الدراسي $detectedTerm"
                    val badgeTextPaint = Paint().apply {
                        color = Color.parseColor("#92400E")
                        textSize = 10.5f
                        typeface = getCustomTypeface(context, fontName, isBold = true)
                        isAntiAlias = true
                    }
                    val bWidth = badgeTextPaint.measureText(badgeText) + 20f
                    val bHeight = 20f
                    val bX = 40f
                    val bY = 74f
                    val bBg = Paint().apply { color = Color.parseColor("#FEF3C7"); style = Paint.Style.FILL }
                    val bStroke = Paint().apply { color = Color.parseColor("#D97706"); style = Paint.Style.STROKE; strokeWidth = 1.2f }
                    canv.drawRoundRect(bX, bY, bX + bWidth, bY + bHeight, 6f, 6f, bBg)
                    canv.drawRoundRect(bX, bY, bX + bWidth, bY + bHeight, 6f, 6f, bStroke)
                    canv.drawText(badgeText, bX + 10f, bY + 14.5f, badgeTextPaint)
                } else {
                    canv.drawText(title, 40f, 88f, titlePaint)
                    val badgeText = "Trimestre $detectedTerm"
                    val badgeTextPaint = Paint().apply {
                        color = Color.parseColor("#1E3A8A")
                        textSize = 10f
                        typeface = getCustomTypeface(context, fontName, isBold = true)
                        isAntiAlias = true
                    }
                    val bWidth = badgeTextPaint.measureText(badgeText) + 16f
                    val bHeight = 20f
                    val bX = 555f - bWidth
                    val bY = 74f
                    val bBg = Paint().apply { color = Color.parseColor("#EFF6FF"); style = Paint.Style.FILL }
                    val bStroke = Paint().apply { color = Color.parseColor("#3B82F6"); style = Paint.Style.STROKE; strokeWidth = 1.2f }
                    canv.drawRoundRect(bX, bY, bX + bWidth, bY + bHeight, 6f, 6f, bBg)
                    canv.drawRoundRect(bX, bY, bX + bWidth, bY + bHeight, 6f, 6f, bStroke)
                    canv.drawText(badgeText, bX + 8f, bY + 14f, badgeTextPaint)
                }

                // Footer
                canv.drawLine(40f, 805f, 555f, 805f, linePaint)
                if (isArabic) {
                    val genDateDayStr = SimpleDateFormat("EEEE dd/MM/yyyy HH:mm", Locale("ar")).format(Date())
                    canv.drawText("تاريخ ويوم الاستخراج : $genDateDayStr", 555f, 818f, Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8.5f })
                    canv.drawText("الصفحة $pNum", 40f, 818f, headerSubPaint)
                } else {
                    val genDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    canv.drawText("Généré le : $genDateStr", 40f, 818f, headerSubPaint)
                    canv.drawText("Page $pNum", 510f, 818f, headerSubPaint)
                }
            }

            drawHeaderAndFooter(canvas, if (isArabic) "التحليل الإحصائي للقسم — $className" else "STATISTIQUES ET ANALYSE DE LA CLASSE — $className", pageNumber)

            var currentY = 105f

            // Data Calculations
            val totalCount = studentsList.size
            val boysCount = studentsList.count { it.gender.equals("M", ignoreCase = true) }
            val girlsCount = studentsList.count { it.gender.equals("F", ignoreCase = true) }

            val averages = gradesList.mapNotNull { g ->
                com.example.util.GradeCalculator.computeStudentAverage(g, subjectCoefficient, isPrimary = isPrimary)
            }

            val passingThreshold = if (isPrimary) 5f else 10f
            val classAverage = if (averages.isNotEmpty()) averages.average().toFloat() else null
            val maxAverage = if (averages.isNotEmpty()) averages.maxOrNull() else null
            val minAverage = if (averages.isNotEmpty()) averages.minOrNull() else null
            val passedCount = averages.count { it >= passingThreshold }
            val failedCount = averages.count { it < passingThreshold }
            val successRate = if (averages.isNotEmpty()) (passedCount * 100f / averages.size) else 0f

            // 1. KPI Cards row
            val cardW = 120f
            val cardH = 46f
            val spacing = 11f
            var sX = 40f

            val rightSubP = Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT }

            // Card 1: Effectif
            canvas.drawRoundRect(sX, currentY, sX + cardW, currentY + cardH, 6f, 6f, Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL })
            canvas.drawRoundRect(sX, currentY, sX + cardW, currentY + cardH, 6f, 6f, linePaint)
            if (isArabic) {
                canvas.drawText("العدد الإجمالي", sX + cardW - 10f, currentY + 14f, rightSubP)
                canvas.drawText("$totalCount", sX + cardW - 10f, currentY + 30f, Paint().apply { textAlign = Paint.Align.RIGHT; color = navyColor; textSize = 14f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("$boysCount ذكور  •  $girlsCount إناث", sX + cardW - 10f, currentY + 41f, Paint(rightSubP).apply { textSize = 7.5f })
            } else {
                canvas.drawText("Effectif Total", sX + 10f, currentY + 14f, headerSubPaint)
                canvas.drawText("$totalCount", sX + 10f, currentY + 30f, Paint().apply { color = navyColor; textSize = 14f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("$boysCount G  •  $girlsCount F", sX + 10f, currentY + 41f, Paint(headerSubPaint).apply { textSize = 7.5f })
            }

            // Card 2: Moyenne
            sX += cardW + spacing
            canvas.drawRoundRect(sX, currentY, sX + cardW, currentY + cardH, 6f, 6f, Paint().apply { color = Color.parseColor("#EFF6FF"); style = Paint.Style.FILL })
            canvas.drawRoundRect(sX, currentY, sX + cardW, currentY + cardH, 6f, 6f, linePaint)
            val avgStr = if (classAverage != null) "%.2f".format(classAverage) else "—"
            if (isArabic) {
                canvas.drawText("معدل القسم", sX + cardW - 10f, currentY + 14f, rightSubP)
                canvas.drawText(avgStr, sX + cardW - 10f, currentY + 30f, Paint().apply { textAlign = Paint.Align.RIGHT; color = navyColor; textSize = 14f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText(if (isPrimary) "على 10" else "على 20", sX + cardW - 10f, currentY + 41f, Paint(rightSubP).apply { textSize = 7.5f })
            } else {
                canvas.drawText("Moyenne Générale", sX + 10f, currentY + 14f, headerSubPaint)
                canvas.drawText(avgStr, sX + 10f, currentY + 30f, Paint().apply { color = navyColor; textSize = 14f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText(if (isPrimary) "sur 10" else "sur 20", sX + 10f, currentY + 41f, Paint(headerSubPaint).apply { textSize = 7.5f })
            }

            // Card 3: Note Max / Min (Séparez les chiffres chacun au-dessous de son texte)
            sX += cardW + spacing
            canvas.drawRoundRect(sX, currentY, sX + cardW, currentY + cardH, 6f, 6f, Paint().apply { color = Color.parseColor("#F0FDF4"); style = Paint.Style.FILL })
            canvas.drawRoundRect(sX, currentY, sX + cardW, currentY + cardH, 6f, 6f, linePaint)
            val halfW = cardW / 2f
            canvas.drawLine(sX + halfW, currentY + 4f, sX + halfW, currentY + cardH - 4f, Paint(linePaint).apply { color = Color.parseColor("#CBD5E1") })

            val maxStr = if (maxAverage != null) "%.2f".format(maxAverage) else "—"
            val minStr = if (minAverage != null) "%.2f".format(minAverage) else "—"

            val centerSubP = Paint(headerSubPaint).apply { textAlign = Paint.Align.CENTER; textSize = 8f }
            val centerValMaxP = Paint().apply { textAlign = Paint.Align.CENTER; color = successColor; textSize = 13f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
            val centerValMinP = Paint().apply { textAlign = Paint.Align.CENTER; color = Color.parseColor("#D97706"); textSize = 13f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }

            if (isArabic) {
                // Right half: أعلى معدل
                val rColX = sX + halfW + (halfW / 2f)
                canvas.drawText("أعلى معدل", rColX, currentY + 14f, centerSubP)
                canvas.drawText(maxStr, rColX, currentY + 31f, centerValMaxP)

                // Left half: أدنى معدل
                val lColX = sX + (halfW / 2f)
                canvas.drawText("أدنى معدل", lColX, currentY + 14f, centerSubP)
                canvas.drawText(minStr, lColX, currentY + 31f, centerValMinP)
            } else {
                // Left half: Note Max
                val lColX = sX + (halfW / 2f)
                canvas.drawText("Note Max", lColX, currentY + 14f, centerSubP)
                canvas.drawText(maxStr, lColX, currentY + 31f, centerValMaxP)

                // Right half: Note Min
                val rColX = sX + halfW + (halfW / 2f)
                canvas.drawText("Note Min", rColX, currentY + 14f, centerSubP)
                canvas.drawText(minStr, rColX, currentY + 31f, centerValMinP)
            }

            // Card 4: Taux de réussite
            sX += cardW + spacing
            canvas.drawRoundRect(sX, currentY, sX + cardW, currentY + cardH, 6f, 6f, Paint().apply { color = Color.parseColor("#FEF3C7"); style = Paint.Style.FILL })
            canvas.drawRoundRect(sX, currentY, sX + cardW, currentY + cardH, 6f, 6f, linePaint)
            if (isArabic) {
                canvas.drawText("نسبة النجاح", sX + cardW - 10f, currentY + 14f, rightSubP)
                canvas.drawText("%.1f%%".format(successRate), sX + cardW - 10f, currentY + 30f, Paint().apply { textAlign = Paint.Align.RIGHT; color = amberColor; textSize = 14f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("$passedCount ناجح  •  $failedCount راسب", sX + cardW - 10f, currentY + 41f, Paint(rightSubP).apply { textSize = 7.5f })
            } else {
                canvas.drawText("Taux de Réussite", sX + 10f, currentY + 14f, headerSubPaint)
                canvas.drawText("%.1f%%".format(successRate), sX + 10f, currentY + 30f, Paint().apply { color = amberColor; textSize = 14f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                canvas.drawText("$passedCount Admis • $failedCount Ajournés", sX + 10f, currentY + 41f, Paint(headerSubPaint).apply { textSize = 7.5f })
            }

            currentY += cardH + 18f

            // 2. Sections: Tranches de Notes & Pyramide des âges
            if (isArabic) {
                canvas.drawText("1. توزيع المعدلات والنتائج الدراسية", 555f, currentY, Paint(sectionTitlePaint).apply { textAlign = Paint.Align.RIGHT })
            } else {
                canvas.drawText("1. RÉPARTITION DES MOYENNES PAR TRANCHE", 40f, currentY, sectionTitlePaint)
            }
            currentY += 8f

            val (r1, r2, r3, r4, r5) = if (isPrimary) {
                listOf(
                    averages.count { it < 2.5f },
                    averages.count { it in 2.5f..4.99f },
                    averages.count { it in 5.0f..6.99f },
                    averages.count { it in 7.0f..8.49f },
                    averages.count { it >= 8.5f }
                )
            } else {
                listOf(
                    averages.count { it < 5f },
                    averages.count { it in 5.0f..9.99f },
                    averages.count { it in 10.0f..13.99f },
                    averages.count { it in 14.0f..15.99f },
                    averages.count { it >= 16.0f }
                )
            }

            val rangeLabels = if (isPrimary) {
                if (isArabic) listOf("< 2.5 (ضعيف جدا)", "2.5 - 4.99 (دون المتوسط)", "5.0 - 6.99 (مقبول)", "7.0 - 8.49 (جيد)", "≥ 8.5 (ممتاز)")
                else listOf("< 2.5 (Très faible)", "2.5 - 4.99 (Insuffisant)", "5.0 - 6.99 (Moyen)", "7.0 - 8.49 (Bien)", "≥ 8.5 (Excellent)")
            } else {
                if (isArabic) listOf("< 5 (ضعيف جدا)", "5.0 - 9.99 (دون المتوسط)", "10.0 - 13.99 (مقبول / حسن)", "14.0 - 15.99 (جيد)", "≥ 16.0 (جيد جدا / ممتاز)")
                else listOf("< 5 (Très faible)", "5.0 - 9.99 (Insuffisant)", "10.0 - 13.99 (Passable/A.Bien)", "14.0 - 15.99 (Bien)", "≥ 16.0 (Très Bien/Exc.)")
            }

            val counts = listOf(r1, r2, r3, r4, r5)

            // Table for ranges
            val rangeColX = if (isArabic) {
                floatArrayOf(545f, 350f, 250f, 180f)
            } else {
                floatArrayOf(40f, 240f, 340f, 440f, 555f)
            }
            canvas.drawRect(40f, currentY, 555f, currentY + 16f, Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL })
            canvas.drawRect(40f, currentY, 555f, currentY + 16f, linePaint)
            if (isArabic) {
                val rightH = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.RIGHT }
                val centerH = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.CENTER }
                canvas.drawText("فئة المعدل", rangeColX[0], currentY + 11.5f, rightH)
                canvas.drawText("العدد", rangeColX[1], currentY + 11.5f, centerH)
                canvas.drawText("النسبة المئوية", rangeColX[2], currentY + 11.5f, centerH)
                canvas.drawText("التقدير", rangeColX[3], currentY + 11.5f, rightH)
            } else {
                canvas.drawText("Tranche de Note", rangeColX[0] + 6f, currentY + 11.5f, tableHeaderPaint)
                canvas.drawText("Nombre", rangeColX[1] + 6f, currentY + 11.5f, tableHeaderPaint)
                canvas.drawText("Pourcentage", rangeColX[2] + 6f, currentY + 11.5f, tableHeaderPaint)
                canvas.drawText("Appréciation", rangeColX[3] + 6f, currentY + 11.5f, tableHeaderPaint)
            }
            currentY += 16f

            val appreciations = if (isArabic) {
                listOf("يحتاج دعم مكثف", "استدراك ومتابعة", "مستوف للشروط", "نتائج مشجعة", "لوحة الشرف")
            } else {
                listOf("Soutien requis", "À encourager", "Admis", "Résultats solides", "Tableau d'Honneur")
            }

            for (i in 0..4) {
                if (i % 2 == 1) canvas.drawRect(40f, currentY, 555f, currentY + 15f, Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL })
                canvas.drawRect(40f, currentY, 555f, currentY + 15f, linePaint)
                val countVal = counts[i]
                val pctVal = if (averages.isNotEmpty()) (countVal * 100f / averages.size) else 0f
                if (isArabic) {
                    val rightC = Paint(cellPaint).apply { textAlign = Paint.Align.RIGHT }
                    val centerC = Paint(cellPaint).apply { textAlign = Paint.Align.CENTER }
                    canvas.drawText(rangeLabels[i], rangeColX[0], currentY + 11f, rightC)
                    canvas.drawText("$countVal", rangeColX[1], currentY + 11f, Paint(centerC).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                    canvas.drawText("%.1f%%".format(pctVal), rangeColX[2], currentY + 11f, centerC)
                    canvas.drawText(appreciations[i], rangeColX[3], currentY + 11f, Paint(rightC).apply {
                        color = if (i >= 3) successColor else if (i == 0) dangerColor else Color.DKGRAY
                    })
                } else {
                    canvas.drawText(rangeLabels[i], rangeColX[0] + 6f, currentY + 11f, cellPaint)
                    canvas.drawText("$countVal", rangeColX[1] + 6f, currentY + 11f, Paint(cellPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) })
                    canvas.drawText("%.1f%%".format(pctVal), rangeColX[2] + 6f, currentY + 11f, cellPaint)
                    canvas.drawText(appreciations[i], rangeColX[3] + 6f, currentY + 11f, Paint(cellPaint).apply {
                        color = if (i >= 3) successColor else if (i == 0) dangerColor else Color.DKGRAY
                    })
                }
                currentY += 15f
            }

            currentY += 18f

            // 3. Section: Tableau nominatif avec moyennes et classement
            if (isArabic) {
                canvas.drawText("2. قائمة التلاميذ والمعدلات المحصل عليها", 555f, currentY, Paint(sectionTitlePaint).apply { textAlign = Paint.Align.RIGHT })
            } else {
                canvas.drawText("2. BILAN NOMINATIF DES ÉLÈVES ET MOYENNES", 40f, currentY, sectionTitlePaint)
            }
            currentY += 8f

            val rankColX = if (isArabic) {
                floatArrayOf(535f, 505f, 320f, 230f, 130f, 40f)
            } else {
                floatArrayOf(40f, 75f, 270f, 340f, 420f, 555f)
            }

            fun drawStudentListHeaders(canv: Canvas, y: Float) {
                canv.drawRect(40f, y, 555f, y + 16f, Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL })
                canv.drawRect(40f, y, 555f, y + 16f, linePaint)
                if (isArabic) {
                    val rightH = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.RIGHT }
                    val centerH = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.CENTER }
                    canv.drawText("الرقم", rankColX[0], y + 11.5f, centerH)
                    canv.drawText("اللقب والاسم", rankColX[1], y + 11.5f, rightH)
                    canv.drawText("الجنس", rankColX[2], y + 11.5f, centerH)
                    canv.drawText("المعدل", rankColX[3], y + 11.5f, centerH)
                    canv.drawText("الوضعية", rankColX[4], y + 11.5f, centerH)
                } else {
                    canv.drawText("N°", rankColX[0] + 4f, y + 11.5f, tableHeaderPaint)
                    canv.drawText("Nom & Prénom", rankColX[1] + 4f, y + 11.5f, tableHeaderPaint)
                    canv.drawText("Sexe", rankColX[2] + 4f, y + 11.5f, tableHeaderPaint)
                    canv.drawText("Moyenne", rankColX[3] + 4f, y + 11.5f, tableHeaderPaint)
                    canv.drawText("Résultat", rankColX[4] + 4f, y + 11.5f, tableHeaderPaint)
                }
            }

            drawStudentListHeaders(canvas, currentY)
            currentY += 16f

            // Sort students by average descending if grades exist, else by name
            val sortedStudentsWithGrades = studentsList.map { st ->
                val g = gradesList.find { it.studentId == st.id }
                val avg = if (g != null) com.example.util.GradeCalculator.computeStudentAverage(g, subjectCoefficient, isPrimary = isPrimary) else null
                Triple(st, g, avg)
            }.sortedWith(compareByDescending<Triple<StudentEntity, StudentWithGrade?, Float?>> { it.third ?: -1f }.thenBy { it.first.lastName })

            for ((idx, triple) in sortedStudentsWithGrades.withIndex()) {
                val (st, _, avg) = triple

                if (currentY + 15f > 780f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawHeaderAndFooter(canvas, if (isArabic) "التحليل الإحصائي للقسم — $className (تابع)" else "STATISTIQUES ET ANALYSE — $className (Suite)", pageNumber)
                    currentY = 100f
                    drawStudentListHeaders(canvas, currentY)
                    currentY += 16f
                }

                if (idx % 2 == 1) canvas.drawRect(40f, currentY, 555f, currentY + 15f, Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL })
                canvas.drawRect(40f, currentY, 555f, currentY + 15f, linePaint)

                val numStr = "${idx + 1}"
                val nameStr = "${st.lastName.uppercase()} ${st.firstName}"
                val genderStr = if (isArabic) {
                    if (st.gender.equals("M", ignoreCase = true)) "ذكر" else if (st.gender.equals("F", ignoreCase = true)) "أنثى" else "—"
                } else {
                    if (st.gender.equals("M", ignoreCase = true)) "M" else if (st.gender.equals("F", ignoreCase = true)) "F" else "—"
                }
                val isFemale = st.gender.equals("F", ignoreCase = true)
                val resStr = if (avg != null) {
                    if (avg >= passingThreshold) {
                        if (isArabic) (if (isFemale) "ناجحة" else "ناجح") else (if (isFemale) "Admise" else "Admis")
                    } else {
                        if (isArabic) (if (isFemale) "راسبة" else "راسب") else (if (isFemale) "Ajournée" else "Ajourné")
                    }
                } else "—"

                val studentAvgStr = if (avg != null) "%.2f".format(avg) else "—"

                if (isArabic) {
                    val rightC = Paint(cellPaint).apply { textAlign = Paint.Align.RIGHT }
                    val centerC = Paint(cellPaint).apply { textAlign = Paint.Align.CENTER }
                    canvas.drawText(numStr, rankColX[0], currentY + 11f, centerC)
                    canvas.drawText(nameStr, rankColX[1], currentY + 11f, Paint(rightC).apply { typeface = getCustomTypeface(context, fontName, isBold = true) })
                    canvas.drawText(genderStr, rankColX[2], currentY + 11f, centerC)

                    val avgP = Paint(centerC).apply {
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        if (avg != null) {
                            color = if (avg >= passingThreshold) successColor else dangerColor
                        }
                    }
                    canvas.drawText(studentAvgStr, rankColX[3], currentY + 11f, avgP)

                    val resP = Paint(centerC).apply {
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        if (avg != null) {
                            color = if (avg >= passingThreshold) successColor else dangerColor
                        }
                    }
                    canvas.drawText(resStr, rankColX[4], currentY + 11f, resP)
                } else {
                    canvas.drawText(numStr, rankColX[0] + 4f, currentY + 11f, cellPaint)
                    canvas.drawText(nameStr, rankColX[1] + 4f, currentY + 11f, Paint(cellPaint).apply { typeface = getCustomTypeface(context, fontName, isBold = true) })
                    canvas.drawText(genderStr, rankColX[2] + 4f, currentY + 11f, cellPaint)

                    val avgP = Paint(cellPaint).apply {
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        if (avg != null) {
                            color = if (avg >= passingThreshold) successColor else dangerColor
                        }
                    }
                    canvas.drawText(studentAvgStr, rankColX[3] + 4f, currentY + 11f, avgP)

                    val resP = Paint(cellPaint).apply {
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        if (avg != null) {
                            color = if (avg >= passingThreshold) successColor else dangerColor
                        }
                    }
                    canvas.drawText(resStr, rankColX[4] + 4f, currentY + 11f, resP)
                }

                currentY += 15f
            }

            // Signature & Cachet stamp box at bottom
            if (currentY + 50f < 780f) {
                currentY += 16f
                val signBoxWidth = 175f
                val signX = if (isArabic) 40f else (555f - signBoxWidth)
                canvas.drawRoundRect(signX, currentY, signX + signBoxWidth, currentY + 42f, 4f, 4f, Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL })
                canvas.drawRoundRect(signX, currentY, signX + signBoxWidth, currentY + 42f, 4f, 4f, linePaint)
                if (isArabic) {
                    canvas.drawText("توقيع وختم الأستاذ(ة) :", signX + signBoxWidth - 8f, currentY + 14f, Paint(headerSubPaint).apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); color = Color.parseColor("#334155") })
                } else {
                    canvas.drawText("Signature & Cachet de l'Enseignant :", signX + 8f, currentY + 12f, Paint(headerSubPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); color = Color.parseColor("#334155") })
                }
            }

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (e: Exception) {}
        }
    }
}

