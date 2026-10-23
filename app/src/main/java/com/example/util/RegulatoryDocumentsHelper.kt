package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.AppRepository
import com.example.data.DocumentCategoryEntity
import com.example.data.DocumentEntity
import java.io.File
import java.io.FileOutputStream

object RegulatoryDocumentsHelper {
    private const val TAG = "RegulatoryDocsHelper"

    suspend fun seedRegulatoryDocuments(context: Context, repository: AppRepository) {
        try {
            val allCats = repository.getAllCategoriesList()
            var regCat = allCats.find {
                it.name.trim().contains("Textes réglementaires", ignoreCase = true) ||
                it.name.trim().contains("reglementaire", ignoreCase = true)
            }

            if (regCat == null) {
                val catId = repository.insertCategory(
                    DocumentCategoryEntity(name = "Textes réglementaires", parentCategoryId = null)
                )
                regCat = repository.getAllCategoriesList().find { it.id == catId.toInt() }
            }

            val targetCatId = regCat?.id ?: return

            val destDir = File(context.filesDir, "copied_documents")
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            // 1. Journal Officiel 2025 N° 04 (Décret exécutif 25-54 - القانون الأساسي)
            val file1 = File(destDir, "A2025004.pdf")
            if (!file1.exists() || file1.length() < 100_000L) {
                copyAssetToFile(context, "documents/A2025004.pdf", file1)
            }

            // 2. Journal Officiel 2008 N° 04 (Loi 08-04 - القانون التوجيهي)
            val file2 = File(destDir, "A2008004.pdf")
            if (!file2.exists() || file2.length() < 100_000L) {
                copyAssetToFile(context, "documents/A2008004.pdf", file2)
            }

            // 3. Suppression définitive du Règlement intérieur selon la demande utilisateur
            val file3 = File(destDir, "Reglement_Interieur_Unifie.pdf")
            if (file3.exists()) {
                file3.delete()
            }

            val existingDocs = repository.getAllDocumentsList()

            // Supprimer uniquement du dossier Textes réglementaires tout document résiduel de règlement intérieur
            existingDocs.filter { it.categoryId == targetCatId }.forEach { doc ->
                val uriLower = doc.fileUri.lowercase()
                val titleLower = doc.title.lowercase()
                if (uriLower.contains("reglement") || uriLower.contains("reglement_interieur") ||
                    titleLower.contains("النظام الداخلي") || titleLower.contains("الموحد") ||
                    titleLower.contains("reglement")
                ) {
                    repository.deleteDocument(doc)
                }
            }

            // 1. Raccourci A2025004.pdf (القانون الأساسي رقم 25-54)
            val doc1Title = "القانون الأساسي رقم 25-54"
            val doc1 = existingDocs.find {
                it.categoryId == targetCatId && (
                    it.title.contains("25-54") || it.title.contains("A2025004") ||
                    it.fileUri.contains("25-54") || it.fileUri.contains("A2025004") ||
                    it.title.contains("الأساسي")
                )
            }
            if (doc1 == null && file1.exists()) {
                repository.insertDocument(
                    DocumentEntity(
                        categoryId = targetCatId,
                        title = doc1Title,
                        fileUri = file1.absolutePath
                    )
                )
            } else if (doc1 != null && (!File(doc1.fileUri).exists() || doc1.fileUri != file1.absolutePath || doc1.title != doc1Title)) {
                repository.insertDocument(doc1.copy(fileUri = file1.absolutePath, title = doc1Title))
            }

            // 2. Raccourci A2008004.pdf (القانون التوجيهي رقم 08-04)
            val doc2Title = "القانون التوجيهي رقم 08-04"
            val doc2 = existingDocs.find {
                it.categoryId == targetCatId && (
                    it.title.contains("08-04") || it.title.contains("A2008004") ||
                    it.fileUri.contains("08-04") || it.fileUri.contains("A2008004") ||
                    it.title.contains("التوجيهي")
                )
            }
            if (doc2 == null && file2.exists()) {
                repository.insertDocument(
                    DocumentEntity(
                        categoryId = targetCatId,
                        title = doc2Title,
                        fileUri = file2.absolutePath
                    )
                )
            } else if (doc2 != null && (!File(doc2.fileUri).exists() || doc2.fileUri != file2.absolutePath || doc2.title != doc2Title)) {
                repository.insertDocument(doc2.copy(fileUri = file2.absolutePath, title = doc2Title))
            }

            // Mettre à jour systématiquement les documents du dossier réglementaire avec ces titres officiels
            existingDocs.filter { it.categoryId == targetCatId }.forEach { doc ->
                val uriLower = doc.fileUri.lowercase()
                val titleLower = doc.title.lowercase()
                if (uriLower.contains("a2008004") || uriLower.contains("loi_08-04") || titleLower.contains("a2008004") || titleLower.contains("08-04") || titleLower.contains("التوجيهي")) {
                    if (doc.title != doc2Title || doc.fileUri != file2.absolutePath) {
                        repository.insertDocument(doc.copy(title = doc2Title, fileUri = file2.absolutePath))
                    }
                } else if (uriLower.contains("a2025004") || uriLower.contains("25-54") || titleLower.contains("a2025004") || titleLower.contains("25-54") || titleLower.contains("الأساسي")) {
                    if (doc.title != doc1Title || doc.fileUri != file1.absolutePath) {
                        repository.insertDocument(doc.copy(title = doc1Title, fileUri = file1.absolutePath))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding regulatory documents", e)
        }
    }

    private fun copyAssetToFile(context: Context, assetPath: String, destFile: File): Boolean {
        return try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset $assetPath to ${destFile.absolutePath}", e)
            false
        }
    }

    private data class ArticleData(val num: Int, val text: String)
    private data class ChapterData(val chapterTitle: String, val subtitle: String, val articles: List<ArticleData>)

    fun generateUnifiedRegulationPdf(context: Context, destFile: File) {
        try {
            val pdfDocument = PdfDocument()

            val pageWidth = 595
            val pageHeight = 842
            val rightMargin = 555f
            val leftMargin = 40f
            val contentWidth = rightMargin - leftMargin // 515f
            val bottomLimit = 780f

            val arabicTypeface = try {
                ResourcesCompat.getFont(context, R.font.amiri) ?: Typeface.SERIF
            } catch (e: Exception) {
                Typeface.SERIF
            }
            val arabicBoldTypeface = try {
                ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT_BOLD
            } catch (e: Exception) {
                Typeface.DEFAULT_BOLD
            }

            // Paints
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59) // Slate 800
                textSize = 9.5f
                typeface = arabicTypeface
                textAlign = Paint.Align.RIGHT
            }
            val articleHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42) // Dark Navy
                textSize = 10f
                typeface = arabicBoldTypeface
                textAlign = Paint.Align.RIGHT
            }
            val chapterTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 11.5f
                typeface = arabicBoldTypeface
                textAlign = Paint.Align.CENTER
            }
            val bannerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 58, 96) // Primary Navy
                style = Paint.Style.FILL
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 0.8f
            }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8.5f
                typeface = arabicTypeface
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8.5f
                typeface = arabicTypeface
            }

            fun wrapText(text: String, paint: Paint, maxW: Float): List<String> {
                val words = text.split(" ")
                val lines = mutableListOf<String>()
                var currentLine = ""
                for (w in words) {
                    val candidate = if (currentLine.isEmpty()) w else "$currentLine $w"
                    if (paint.measureText(candidate) <= maxW) {
                        currentLine = candidate
                    } else {
                        if (currentLine.isNotEmpty()) lines.add(currentLine)
                        currentLine = w
                    }
                }
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                return lines
            }

            val chapters = getOfficialChapters()

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = 40f

            fun drawHeader() {
                headerPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("الجمهورية الجزائرية الديمقراطية الشعبية — وزارة التربية الوطنية", rightMargin, 35f, headerPaint)
                headerPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("النظام الداخلي الموحد لمؤسسات التربية والتعليم", leftMargin, 35f, headerPaint)
                canvas.drawLine(leftMargin, 42f, rightMargin, 42f, linePaint)
            }

            fun drawFooter(pNum: Int) {
                canvas.drawLine(leftMargin, bottomLimit + 15f, rightMargin, bottomLimit + 15f, linePaint)
                footerPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("وزارة التربية الوطنية — المقرر رقم 460 المؤرخ في 07 سبتمبر 2024", rightMargin, bottomLimit + 28f, footerPaint)
                footerPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("الصفحة $pNum", leftMargin, bottomLimit + 28f, footerPaint)
            }

            fun newPage() {
                drawFooter(pageNumber)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
                currentY = 60f
            }

            // --- PAGE 1 OFFICIAL TITLE BANNER ---
            drawHeader()
            currentY = 55f

            // Republic header box
            val titleBoxHeight = 85f
            val titleBoxRect = RectF(leftMargin, currentY, rightMargin, currentY + titleBoxHeight)
            val titleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(241, 245, 249) // Slate 100
                style = Paint.Style.FILL
            }
            val titleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 58, 96) // Navy border
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }
            canvas.drawRoundRect(titleBoxRect, 8f, 8f, titleBgPaint)
            canvas.drawRoundRect(titleBoxRect, 8f, 8f, titleBorderPaint)

            val bigHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 11f
                typeface = arabicBoldTypeface
                textAlign = Paint.Align.CENTER
            }
            val mainDocTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 58, 96)
                textSize = 15f
                typeface = arabicBoldTypeface
                textAlign = Paint.Align.CENTER
            }
            val subtitleDocPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 9.5f
                typeface = arabicTypeface
                textAlign = Paint.Align.CENTER
            }

            val centerX = pageWidth / 2f
            canvas.drawText("الجمهورية الجزائرية الديمقراطية الشعبية", centerX, currentY + 18f, bigHeaderPaint)
            canvas.drawText("وزارة التربية الوطنية", centerX, currentY + 32f, bigHeaderPaint)
            canvas.drawText("النظام الداخلي الموحد لمؤسسات التربية والتعليم", centerX, currentY + 54f, mainDocTitlePaint)
            canvas.drawText("القرار الوزاري رقم 460 المؤرخ في 07 سبتمبر 2024 المتضمن اعتماد النظام الداخلي النموذجي", centerX, currentY + 72f, subtitleDocPaint)

            currentY += titleBoxHeight + 18f

            // Loop through chapters and articles
            for (chapter in chapters) {
                // Check if chapter header fits
                val chapterHeaderHeight = 32f
                if (currentY + chapterHeaderHeight + 60f > bottomLimit) {
                    newPage()
                }

                // Draw Chapter Banner
                val bannerRect = RectF(leftMargin, currentY, rightMargin, currentY + 24f)
                canvas.drawRoundRect(bannerRect, 5f, 5f, bannerBgPaint)
                canvas.drawText("${chapter.chapterTitle} : ${chapter.subtitle}", centerX, currentY + 16.5f, chapterTitlePaint)
                currentY += 32f

                // Articles in chapter
                for (art in chapter.articles) {
                    val headerText = "المادة ${art.num} :"
                    val bodyLines = wrapText(art.text, textPaint, contentWidth - 10f)
                    val artTotalHeight = 16f + (bodyLines.size * 13.5f) + 6f

                    if (currentY + artTotalHeight > bottomLimit) {
                        newPage()
                    }

                    // Draw Article Number
                    canvas.drawText(headerText, rightMargin, currentY + 10f, articleHeaderPaint)
                    currentY += 14f

                    // Draw Article Body Lines
                    for (line in bodyLines) {
                        canvas.drawText(line, rightMargin - 8f, currentY + 9f, textPaint)
                        currentY += 13.5f
                    }
                    currentY += 6f
                }
                currentY += 8f
            }

            // Final page footer
            drawFooter(pageNumber)
            pdfDocument.finishPage(page)

            // Write to destination
            FileOutputStream(destFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            Log.d(TAG, "Unified Regulation PDF successfully generated: ${destFile.absolutePath} ($pageNumber pages)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate Unified Regulation PDF", e)
        }
    }

    private fun getOfficialChapters(): List<ChapterData> {
        return listOf(
            ChapterData(
                chapterTitle = "الفصل الأول",
                subtitle = "أحكام عامة",
                articles = listOf(
                    ArticleData(1, "يهدف هذا النظام الداخلي إلى تحديد الضوابط العامة لتسيير الحياة المدرسية وضمان السير الحسن والمستقر للأنشطة التربوية والتعليمية داخل مؤسسة التربية والتعليم، في جو يسوده الانضباط والاحترام المتبادل ويضمن تحقيق الأهداف التربوية المسطرة."),
                    ArticleData(2, "يرتكز النظام الداخلي على مبادئ الخدمة العمومية للتربية والتعليم، والقيم الوطنية والدينية والاجتماعية للشعب الجزائري، لا سيما ترسيخ قيم المواطنة والمسؤولية والنزاهة، ونبذ العنف بكل أشكاله الجسدية واللفظية والمعنوية."),
                    ArticleData(3, "يطبق هذا النظام الداخلي على كافة أفراد الجماعة التربوية من موظفين وأساتذة وتلاميذ وأولياء أمور، ويلزم الجميع بالاطلاع على أحكامه والتقيد الصارم بها داخل المؤسسة وفي محيطها المباشر."),
                    ArticleData(4, "تلزم إدارة المؤسسة بوضع هذا النظام في متناول الجميع عبر التعليق في الأماكن المخصصة وتوزيعه ونشره عبر الوسائط والمنصات الرقمية المعتمدة وتسليم نسخة منه لأولياء التلاميذ عند التسجيل."),
                    ArticleData(5, "يُمنع منعاً باتاً اعتماد أي تعليمات أو تنظيمات داخلية محلية تتعارض مع نصوص وأحكام هذا النظام الداخلي الموحد الصادر عن وزارة التربية الوطنية.")
                )
            ),
            ChapterData(
                chapterTitle = "الفصل الثاني",
                subtitle = "حقوق وواجبات المتعلمين",
                articles = listOf(
                    ArticleData(6, "يتمتع التلميذ بالحق في التربية والتعليم والتكوين في ظروف ملائمة تكفل كرامته وسلامته الجسدية والمعنوية، وبالمساواة في المعاملة دون أي تمييز."),
                    ArticleData(7, "يحق لكل تلميذ الاستفادة من جميع الحصص التعليمية المقررة في المناهج الرسمية والأنشطة الثقافية والرياضية المبرمجة بالمؤسسة."),
                    ArticleData(8, "يتمتع التلميذ بحرية التفكير والتعبير في إطار الاحترام المتبادل للأساتذة وزملائه وموظفي المؤسسة والتقيد بالقوانين والآداب العامة."),
                    ArticleData(9, "يلتزم التلميذ بالحضور المنتظم والمواظبة في جميع الحصص والأنشطة المقررة واحترام مواقيت الدخول والخروج بدقة."),
                    ArticleData(10, "يلتزم التلميذ بالتحلي بالسلوك الحسن والهندام اللائق وارتداء المئزر المدرسي النظيف والمطابق للضوابط التنظيمية المعتمدة طيلة تواجده بالمؤسسة."),
                    ArticleData(11, "يلتزم التلميذ بإحضار جميع الأدوات المدرسية والكتب والكراريس واللوازم الخاصة بكل مادة وإنجاز الواجبات المنزلية والتمارين المقررة بعناية."),
                    ArticleData(12, "يحترم التلميذ الطاقم الإداري والتربوي والعمال وزملاءه داخل وخارج المؤسسة ويتجنب أي سلوك عنيف أو تصرف يخل بالنظام والانضباط."),
                    ArticleData(13, "يعتبر الغش في الامتحانات والاختبارات والفروض سلوكاً مشيناً ومحظوراً، وتترتب عنه عقوبات تأديبية ومنح علامة الصفر والإحالة على مجلس التأديب."),
                    ArticleData(14, "يُمنع على التلاميذ التجمهر أمام مدخل المؤسسة أو البقاء في الساحة والأروقة بعد دق جرس الدخول وانطلاق الحصص."),
                    ArticleData(15, "يحق للمتعلمين الاستفادة من خدمات مكتبة المؤسسة والمطالعة والرعاية الصحية والاجتماعية والنوادي المدرسية المعتمدة.")
                )
            ),
            ChapterData(
                chapterTitle = "الفصل الثالث",
                subtitle = "قواعد السلامة والأمن داخل المؤسسة التعليمية",
                articles = listOf(
                    ArticleData(16, "يُرفع العلم الوطني ويُخفض يومياً مصحوباً بأداء النشيد الوطني بمشاركة وحضور جميع التلاميذ والأساتذة وكافة موظفي المؤسسة في خشوع ووقار."),
                    ArticleData(17, "لا يُسمح بالدخول إلى حرم المؤسسة إلا للموظفين والتلاميذ المسجلين والأشخاص المرخص لهم قانوناً من طرف مدير المؤسسة."),
                    ArticleData(18, "يخضع دخول الزوار وأولياء التلاميذ لمواقيت الاستقبال المحددة مسبقاً، مع إلزامية استظهار بطاقة الهوية الرسمية وتسجيل البيانات في سجل الزوار."),
                    ArticleData(19, "يُحظر نهائياً جلب واستعمال الهواتف النقالة وكاميرات التصوير والأجهزة الذكية ووسائل التسجيل والوسائط السمعية البصرية داخل حجرات الدراسة والمرافق البيداغوجية، باستثناء حالات الضرورة البيداغوجية القصوى بترخيص كتابي صريح من الإدارة."),
                    ArticleData(20, "يُمنع منعاً باتاً تصوير التلاميذ أو الموظفين أو الأساتذة أو نشر وبث أي صور أو فيديوهات تخص مرافق المؤسسة والأنشطة التربوية على شبكات التواصل الاجتماعي دون ترخيص رسمي من السلطات المعنية."),
                    ArticleData(21, "يُحظر إدخال أو حيازة الأدوات الحادة أو القاطعة، المفرقعات، المواد القابلة للاشتعال، أو أي شيء يشكل خطراً على أمن وسلامة الأفراد والممتلكات."),
                    ArticleData(22, "تلتزم المؤسسة بتأمين منافذ الطوارئ ووسائل الإطفاء وتنظيم تمارين الإخلاء الدورية لضمان سلامة الجميع عند حدوث الكوارث أو المخاطر.")
                )
            ),
            ChapterData(
                chapterTitle = "الفصل الرابع",
                subtitle = "الانضباط والمواظبة وسير الدروس وحركة التلاميذ",
                articles = listOf(
                    ArticleData(23, "تُفتح أبواب المؤسسة 15 دقيقة قبل موعد انطلاق الفترة الصباحية والمسائية، وتُغلق 5 دقائق قبل بدء تحية العلم وانطلاق الحصص التعليمية."),
                    ArticleData(24, "تسجل إدارة المؤسسة غيابات وتأخرات التلاميذ يومياً وبشكل دوري، وتُخطر أولياء الأمور فورا عبر مختلف قنوات الاتصال والمنصات الرسمية."),
                    ArticleData(25, "لا يُسمح للتلميذ المتغيب باستئناف الدراسة إلا بعد تقديم ترخيص بالدخول صادر عن إدارة المؤسسة ومصالح الاستشارة التربوية."),
                    ArticleData(26, "يجب تبرير الغياب بحضور الولي الشرعي أو بتقديم شهادة طبية رسمية ومبررة خلال مهلة لا تتعدى 48 ساعة من تاريخ الغياب."),
                    ArticleData(27, "كل تأخر غير مبرر يحرم التلميذ من الالتحاق بالحصة ويلزمه بالبقاء في قاعة المداومة إلى غاية نهاية الحصة واستلام رخصة دخول للحصة الموالية."),
                    ArticleData(28, "تتدرج العقوبات التأديبية في حالة تكرار الغيابات والتأخرات من التنبيه والإنذار الكتابي إلى الإحالة على مجلس التأديب وصولاً إلى الشطب بعد الإعذارات القانونية."),
                    ArticleData(29, "يُمنع خروج التلاميذ من حرم المؤسسة خلال فترات الاستراحة أو بين الحصص ما لم يكن مصحوباً بجدول توقيت رسمي أو ترخيص مسبق."),
                    ArticleData(30, "يُمنع تحرك التلاميذ في الأروقة أثناء سير الدروس، ويتعين التزام الهدوء التام عند تغيير القاعات أو التوجه للمخابر والورشات.")
                )
            ),
            ChapterData(
                chapterTitle = "الفصل الخامس",
                subtitle = "الوقاية والصحة المدرسية ومكافحة الآفات",
                articles = listOf(
                    ArticleData(31, "يُحظر تماماً حيازة أو ترويج أو استهلاك جميع أنواع التبغ والسجائر الإلكترونية والمخدرات والمؤثرات العقلية ومشروبات الطاقة ومشروبات الكافيين في الوسط المدرسي ومحيطه."),
                    ArticleData(32, "يخضع تلاميذ مؤسسات التربية والتعليم (المتوسط والثانوي) لإجراءات الكشف والتحاليل الطبية حول تعاطي المخدرات أو المؤثرات العقلية بالتنسيق مع المصالح الصحية المختصة وفق الأطر التشريعية المنظمة."),
                    ArticleData(33, "يُمنع الترويج أو المشاركة أو التحريض على الألعاب والتحديات الرقمية الخطيرة والأنشطة المشبوهة التي تهدد سلامة وصحة المتعلمين البدنية والنفسية."),
                    ArticleData(34, "يستفيد التلاميذ بانتظام من الفحوصات الطبية الدورية التي تقدمها وحدات الكشف والمتابعة الصحية المدرسية (UDS)."),
                    ArticleData(35, "تلزم إدارة المؤسسة بالإبلاغ الفوري للمصالح الصحية والأولياء عند رصد أي حالة لمرض معدٍ أو وبائي واتخاذ التدابير الوقائية اللازمة."),
                    ArticleData(36, "يُمنع جلب المأكولات والمشروبات المجهولة المصدر أو غير الصحية حفاظاً على النظافة العامة والوقاية من التسممات الغذائية.")
                )
            ),
            ChapterData(
                chapterTitle = "الفصل السادس",
                subtitle = "حقوق وواجبات الموظفين وهيئة التدريس",
                articles = listOf(
                    ArticleData(37, "يحظى الأستاذ وكافة موظفي المؤسسة بالاحترام والتقدير والحماية القانونية أثناء ممارسة مهامهم التربوية والتعليمية والإدارية."),
                    ArticleData(38, "يلتزم الأساتذة بالحضور الدقيق والمواظبة على إلقاء الدروس وفق المواقيت والمناهج الرسمية والتحضير البيداغوجي اليومي للأنشطة."),
                    ArticleData(39, "يُمنع اللجوء إلى العقاب البدني أو اللفظي أو المعنوي ضد التلاميذ مهما كانت الأسباب، وتعتمد المقاربات التربوية والتأديبية الإيجابية."),
                    ArticleData(40, "يلتزم الأستاذ بمسك الدفاتر والسجلات الإلزامية كدفتر النصوص وسجل الغيابات وكراس التنقيط والمتابعة المستمرة لتقويم المتعلمين."),
                    ArticleData(41, "يسهر الأساتذة والإداريون على مرافقة التلاميذ وملاحظة سلوكهم وبرمجة حصص الدعم والاستدراك والمعالجة البيداغوجية لصعوبات التعلم."),
                    ArticleData(42, "يشارك الأساتذة في المجالس التعليمية والبيداغوجية ومجالس الأقسام والتأديب والامتحانات الرسمية وفق التشريعات السارية.")
                )
            ),
            ChapterData(
                chapterTitle = "الفصل السابع",
                subtitle = "علاقة المؤسسة بأولياء التلاميذ والشركاء",
                articles = listOf(
                    ArticleData(43, "يعتبر أولياء التلاميذ شركاء أساسيين في العملية التربوية ويساهمون بالتعاون والتنسيق في متابعة المسار الدراسي لأبنائهم وضمان انضباطهم."),
                    ArticleData(44, "تنظم المؤسسة لقاءات دورية بين الأساتذة وأولياء التلاميذ لإطلاعهم على نتائج أبنائهم وسلوكهم ومناقشة سبل التحسين والمعالجة."),
                    ArticleData(45, "يحق لأولياء التلاميذ تشكيل جمعية أولياء التلاميذ المعتمدة قانوناً للمساهمة في دعم الأنشطة الثقافية والاجتماعية وتحسين ظروف التمدرس."),
                    ArticleData(46, "يلتزم الأولياء باحترام مواعيد الاستقبال الإداري والتربوي وتجنب التشويش على سير الحصص أو الاتصال المباشر بالأساتذة داخل الأقسام."),
                    ArticleData(47, "يتعين على الولي إعلام إدارة المؤسسة فوراً بأي تغيير في العنوان أو أرقام الهاتف أو الحالة الصحية الخاصة بالتلميذ."),
                    ArticleData(48, "يتعاون أولياء الأمور مع الإدارة في تنفيذ القرارات التأديبية وبرامج الدعم والمعالجة الموجهة لأبنائهم لضمان استقرار مسارهم الدراسي.")
                )
            ),
            ChapterData(
                chapterTitle = "الفصل الثامن",
                subtitle = "المحافظة على الهياكل والبيئة المدرسية",
                articles = listOf(
                    ArticleData(49, "يلتزم الجميع بالمحافظة على نظافة المؤسسة وحجرات الدراسة والساحات والمرافق الصحية والمساحات الخضراء والبيئة المدرسية."),
                    ArticleData(50, "يعتبر الحفاظ على الأثاث المدرسي والتجهيزات العلمية والوسائل التعليمية واجباً مقدساً على عاتق كل فرد من أفراد الجماعة التربوية."),
                    ArticleData(51, "كل إتلاف أو تخريب متعمد أو تشويه للممتلكات والتجهيزات يترتب عنه تعويض مالي يتحمله المتسبب أو وليه الشرعي إلى جانب المتابعة التأديبية."),
                    ArticleData(52, "يُمنع إحضار المبالغ المالية الكبيرة والأشياء الثمينة والحلي إلى المؤسسة، ولا تتحمل إدارة المؤسسة أي مسؤولية في حال ضياعها أو سرقتها.")
                )
            ),
            ChapterData(
                chapterTitle = "الفصل التاسع",
                subtitle = "أحكام ختامية",
                articles = listOf(
                    ArticleData(53, "كل مخالفة لأحكام هذا النظام الداخلي تعرض مرتكبها للعقوبات المنصوص عليها في التشريعات والتنظيمات المدرسية سارية المفعول."),
                    ArticleData(54, "يُعرض هذا النظام الداخلي للمناقشة والمصادقة في مجلس التربية والتسيير أو مجلس التوجيه والتسيير في بداية كل سنة دراسية."),
                    ArticleData(55, "يدخل هذا النظام الداخلي الموحد حيز التطبيق الفوري ابتداءً من تاريخ صدوره، ويُلغى كل تنظيم أو تدبير داخلي مخالف لأحكامه.")
                )
            )
        )
    }
}
