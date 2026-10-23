package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Moteur universel de traduction et localisation (Français <-> Arabe standard moderne / الفصحى).
 * Permet la traduction automatique de termes, expressions et phrases complètes.
 */
object Tr {
    @Volatile
    var forcedLanguage: String? = null

    @Composable
    fun isArabic(): Boolean {
        return forcedLanguage == "ar" || I18n.isRtl() || LocalLayoutDirection.current == LayoutDirection.Rtl
    }

    fun isArabic(activeLang: String?): Boolean {
        if (activeLang != null) return activeLang == "ar"
        return forcedLanguage == "ar" || I18n.isRtl()
    }

    fun isArabicLang(activeLang: String? = null): Boolean {
        if (activeLang != null) return activeLang == "ar"
        return forcedLanguage == "ar" || I18n.isRtl()
    }

    @Composable
    fun get(fr: String, ar: String): String {
        return if (isArabic()) ar else fr
    }

    fun get(fr: String, ar: String, isAr: Boolean): String {
        return if (isAr) ar else fr
    }

    fun translate(text: String, isAr: Boolean): String {
        if (!isAr) return text
        val trimmed = text.trim()
        val direct = dictionary[trimmed]
        if (direct != null) return direct

        // Gestion des préfixes fréquents
        for ((prefix, arPrefix) in prefixMap) {
            if (trimmed.startsWith(prefix, ignoreCase = true)) {
                val remainder = trimmed.substring(prefix.length).trim()
                val translatedRemainder = dictionary[remainder] ?: remainder
                return "$arPrefix $translatedRemainder".trim()
            }
        }

        return text
    }

    /**
     * Traduction dédiée et exhaustive des messages de notification (toasts, snackbars, confirmations).
     */
    fun translateNotificationMessage(raw: String, isAr: Boolean): String {
        if (!isAr) return raw
        val trimmed = raw.trim()

        val direct = notificationDictionary[trimmed] ?: dictionary[trimmed]
        if (direct != null) return direct

        // Expressions régulières dynamiques pour les notifications avec paramètres
        Regex("""Classe '(.+)' ajoutée avec succès !""").matchEntire(trimmed)?.let {
            return "تمت إضافة الفوج '${it.groupValues[1]}' بنجاح!"
        }
        Regex("""Année scolaire '(.+)' supprimée""").matchEntire(trimmed)?.let {
            return "تم حذف السنة الدراسية '${it.groupValues[1]}'"
        }
        Regex("""Élève (.+) ajouté\.""").matchEntire(trimmed)?.let {
            return "تمت إضافة التلميذ(ة) ${it.groupValues[1]} بنجاح."
        }
        Regex("""(.+) assigné\(e\) à la place (\d+)\.""").matchEntire(trimmed)?.let {
            return "تم تعيين ${it.groupValues[1]} في المقعد ${it.groupValues[2]}."
        }
        Regex("""Place (\d+) libérée\.""").matchEntire(trimmed)?.let {
            return "تم تحرير المقعد ${it.groupValues[1]}."
        }
        Regex("""Attribution automatique effectuée \((\d+) élèves\)\.""").matchEntire(trimmed)?.let {
            return "تم التعيين التلقائي للمقاعد (${it.groupValues[1]} تلميذاً)."
        }
        Regex("""Horaire modifié \((.+)\)\.""").matchEntire(trimmed)?.let {
            return "تم تعديل التوقيت (${it.groupValues[1]})."
        }
        Regex("""Horaire mis à jour : (.+)""").matchEntire(trimmed)?.let {
            return "تم تحديث التوقيت : ${it.groupValues[1]}"
        }
        Regex("""Créneau horaire ajouté : (.+)""").matchEntire(trimmed)?.let {
            return "تمت إضافة التوقيت : ${it.groupValues[1]}"
        }
        Regex("""Créneau horaire supprimé \((.+)\)""").matchEntire(trimmed)?.let {
            return "تم حذف التوقيت (${it.groupValues[1]})"
        }
        Regex("""(\d+) élèves importés avec succès !""").matchEntire(trimmed)?.let {
            return "تم استيراد ${it.groupValues[1]} تلميذاً بنجاح!"
        }
        Regex("""(\d+) fichier\(s\) importé\(s\) avec succès !""").matchEntire(trimmed)?.let {
            return "تم استيراد ${it.groupValues[1]} ملف(ات) بنجاح!"
        }
        Regex("""Dossier importé : (\d+) fichier\(s\) dans (\d+) dossier\(s\) !""").matchEntire(trimmed)?.let {
            return "تم استيراد المجلد : ${it.groupValues[1]} ملف(ات) في ${it.groupValues[2]} مجلد(ات)!"
        }
        Regex("""Rapport PDF exporté : (.+)""").matchEntire(trimmed)?.let {
            return "تم تصدير تقرير PDF : ${it.groupValues[1]}"
        }
        Regex("""Erreur d'écriture : (.+)""").matchEntire(trimmed)?.let {
            return "خطأ في الكتابة : ${it.groupValues[1]}"
        }
        Regex("""Erreur PDF : (.+)""").matchEntire(trimmed)?.let {
            return "خطأ PDF : ${it.groupValues[1]}"
        }
        Regex("""Erreur d'import : (.+)""").matchEntire(trimmed)?.let {
            return "خطأ في الاستيراد : ${it.groupValues[1]}"
        }
        Regex("""Impossible d'ouvrir le fichier : (.+)""").matchEntire(trimmed)?.let {
            return "تعذر فتح الملف : ${it.groupValues[1]}"
        }
        Regex("""Erreur lors de l'envoi de la sauvegarde : (.+)""").matchEntire(trimmed)?.let {
            return "خطأ أثناء إرسال النسخة الاحتياطية : ${it.groupValues[1]}"
        }
        Regex("""(.+) enregistré\(e\)? avec succès !""").matchEntire(trimmed)?.let {
            val item = translate(it.groupValues[1], true)
            return "تم حفظ $item بنجاح!"
        }
        Regex("""(.+) enregistré\(e\)? !""").matchEntire(trimmed)?.let {
            val item = translate(it.groupValues[1], true)
            return "تم حفظ $item بنجاح!"
        }
        Regex("""(.+) supprimé\(e\)? avec succès !""").matchEntire(trimmed)?.let {
            val item = translate(it.groupValues[1], true)
            return "تم حذف $item بنجاح!"
        }
        Regex("""(.+) modifié\(e\)? avec succès !""").matchEntire(trimmed)?.let {
            val item = translate(it.groupValues[1], true)
            return "تم تعديل $item بنجاح!"
        }

        return translate(trimmed, true)
    }

    @Composable
    fun t(text: String): String {
        return translate(text, isArabic())
    }

    // Termes fréquents d'action
    val save @Composable get() = get("Enregistrer", "حفظ")
    val cancel @Composable get() = get("Annuler", "إلغاء")
    val delete @Composable get() = get("Supprimer", "حذف")
    val edit @Composable get() = get("Modifier", "تعديل")
    val add @Composable get() = get("Ajouter", "إضافة")
    val close @Composable get() = get("Fermer", "إغلاق")
    val confirm @Composable get() = get("Confirmer", "تأكيد")
    val search @Composable get() = get("Rechercher…", "بحث…")
    val apply @Composable get() = get("Appliquer", "تطبيق")
    val reset @Composable get() = get("Réinitialiser", "إعادة تعيين")
    val exportPdf @Composable get() = get("Exporter PDF", "تصدير PDF")
    val share @Composable get() = get("Partager", "مشاركة")
    val understood @Composable get() = get("Compris", "حسناً")
    val back @Composable get() = get("Retour", "رجوع")

    // Trimestres
    val term1 @Composable get() = get("1er Trimestre", "الفصل الأول")
    val term2 @Composable get() = get("2e Trimestre", "الفصل الثاني")
    val term3 @Composable get() = get("3e Trimestre", "الفصل الثالث")

    // Pédagogie et scolarité
    val allClasses @Composable get() = get("Toutes les classes", "جميع الأفواج")
    val selectClass @Composable get() = get("Sélectionner une classe", "اختيار الفوج")
    val classroom @Composable get() = get("Salle", "القاعة")
    val subject @Composable get() = get("Matière", "المادة")
    val date @Composable get() = get("Date", "التاريخ")
    val timeSlot @Composable get() = get("Créneau horaire", "التوقيت")
    val students @Composable get() = get("Élèves", "التلاميذ")
    val student @Composable get() = get("Élève", "تلميذ")
    val boys @Composable get() = get("Garçons", "الذكور")
    val girls @Composable get() = get("Filles", "الإناث")
    val boy @Composable get() = get("Garçon", "ذكر")
    val girl @Composable get() = get("Fille", "أنثى")
    val male @Composable get() = get("Masculin", "ذكر")
    val female @Composable get() = get("Féminin", "أنثى")
    val gender @Composable get() = get("Sexe", "الجنس")
    val age @Composable get() = get("Âge", "السن")
    val birthDate @Composable get() = get("Date de naissance", "تاريخ الميلاد")
    val repeater @Composable get() = get("Redoublant", "معيد")
    val nonRepeater @Composable get() = get("Non redoublant", "غير معيد")
    val average @Composable get() = get("Moyenne", "المعدل")
    val classAverage @Composable get() = get("Moyenne de classe", "معدل الفوج")
    val generalAverage @Composable get() = get("Moyenne générale", "المعدل العام")
    val rank @Composable get() = get("Rang", "الرتبة")
    val observation @Composable get() = get("Observation", "الملاحظة")
    val coefficient @Composable get() = get("Coefficient", "المعامل")

    // Navigation & Onglets
    val tabList @Composable get() = get("Liste", "القائمة")
    val tabPlan @Composable get() = get("Plan", "مخطط الجلوس")
    val tabStats @Composable get() = get("Stats", "الإحصائيات")

    private val prefixMap = mapOf(
        "Classe :" to "الفوج :",
        "Classe" to "الفوج",
        "Trimestre" to "الفصل",
        "Année :" to "السنة :",
        "Année scolaire :" to "السنة الدراسية :",
        "Coeff:" to "المعامل:",
        "Coeff :" to "المعامل :",
        "Salle :" to "القاعة :",
        "Salle" to "القاعة",
        "Total :" to "المجموع :",
        "Moyenne :" to "المعدل :",
        "Date :" to "التاريخ :",
        "Trier par :" to "ترتيب حسب :",
        "Trier par" to "ترتيب حسب"
    )

    private val dictionary = mapOf(
        // Termes d'horaires & Emploi du temps
        "Enseignant" to "الأستاذ",
        "Enseignants" to "الأساتذة",
        "Classes" to "الأفواج",
        "Classe" to "الفوج",
        "jour" to "اليوم",
        "Jour" to "اليوم",
        "jours" to "أيام",
        "Jours" to "الأيام",
        "Pause" to "استراحة",
        "Pause déjeuner" to "استراحة الغداء",
        "Pause de midi" to "استراحة منتصف النهار",
        "Pause déjeuner (après 12H)" to "استراحة الغداء (بعد الساعة 12)",
        "Vue générale" to "نظرة عامة",
        "vue générale" to "نظرة عامة",
        "+45m" to "+45 د",
        "+1h" to "+1 سا",
        "+1h30" to "+1 سا 30 د",
        "+30m" to "+30 د",
        "semaine" to "الأسبوع",
        "Semaine" to "الأسبوع",
        "Horaires du" to "جدول توقيت",
        "Horaires de la semaine" to "جدول توقيت الأسبوع",
        "journée libre(Repos)" to "يوم راحة (عطلة)",
        "Journée libre (Repos)" to "يوم راحة (عطلة)",
        "Journée libre" to "يوم راحة",
        "JOURNÉE LIBRE" to "يوم راحة",
        "Jour de repos" to "يوم الراحة الأسبوعي",
        "cours programmé(s)" to "درس مبرمج",
        "cours programmés" to "دروس مبرمجة",
        "cours programmé" to "درس مبرمج",
        "Année" to "السنة",
        "Coeff" to "المعامل",
        "Trimestre" to "الفصل",

        // Documents & Dossiers
        "Rechercher un dossier ou son contenu" to "البحث عن مجلد أو محتواه",
        "Rechercher un dossier ou son contenu..." to "البحث عن مجلد أو محتواه...",
        "Dossier Pédagogique" to "الملف البيداغوجي",
        "Dossier Pédagogique Principal" to "الملف البيداغوجي الرئيسي",
        "Fichiers" to "الملفات",
        "Sous-dossiers" to "المجلدات الفرعية",
        "Ce dossier est vide" to "هذا المجلد فارغ",
        "Créez un nouveau sous-dossier ou importez des fichiers directement depuis les boutons en haut" to "أنشئ مجلداً فرعياً جديداً أو استورد ملفات مباشرة من الأزرار أعلاه",
        "Créez un nouveau sous-dossier ou importez des fichiers directement depuis les boutons en haut." to "أنشئ مجلداً فرعياً جديداً أو استورد ملفات مباشرة من الأزرار أعلاه.",

        // Actions & Boutons généraux
        "Enregistrer" to "حفظ",
        "Annuler" to "إلغاء",
        "Supprimer" to "حذف",
        "Modifier" to "تعديل",
        "Ajouter" to "إضافة",
        "Fermer" to "إغلاق",
        "Confirmer" to "تأكيد",
        "Appliquer" to "تطبيق",
        "Réinitialiser" to "إعادة تعيين",
        "Rechercher" to "بحث",
        "Rechercher…" to "بحث…",
        "Rechercher un élève…" to "بحث عن تلميذ…",
        "Rechercher un cours…" to "بحث عن درس…",
        "Rechercher une note…" to "بحث في الملاحظات…",
        "Rechercher un document…" to "بحث في الوثائق…",
        "Rechercher un rappel…" to "بحث في التذكيرات…",
        "Filtrer" to "تصفية",
        "Trier par" to "ترتيب حسب",
        "Exporter" to "تصدير",
        "Importer" to "استيراد",
        "Partager" to "مشاركة",
        "Imprimer" to "طباعة",
        "Exporter PDF" to "تصدير PDF",
        "Créer" to "إنشاء",
        "Archiver" to "أرشفة",
        "Initialiser" to "تهيئة",
        "Compris" to "حسناً",
        "Retour" to "رجوع",
        "Continuer" to "متابعة",
        "Valider" to "تأكيد",
        "Sauvegarder" to "حفظ",
        "Restaurer" to "استعادة",
        "Synchroniser" to "مزامنة",
        "Détails" to "التفاصيل",
        "Options" to "خيارات",
        "Créer dossier" to "إنشاء مجلد",
        "Renommer" to "إعادة تسمية",
        "Renommer le dossier" to "إعادة تسمية المجلد",

        // Navigation et entêtes
        "Accueil" to "الرئيسية",
        "Classes" to "الأفواج",
        "Groupe" to "الفوج",
        "Groupes" to "الأفواج",
        "Sans groupe" to "بدون فوج",
        "Diviser la classe en groupes" to "تقسيم القسم إلى أفواج",
        "Appui long pour modifier" to "اضغط مطولاً للتعديل",
        "(Appui long pour modifier)" to "(اضغط مطولاً للتعديل)",
        "Liste" to "القائمة",
        "Plan" to "مخطط الجلوس",
        "Stats" to "الإحصائيات",
        "Statistiques" to "الإحصائيات",
        "Profil Démographique" to "الملف الديمغرافي",
        "Profil démographique" to "الملف الديمغرافي",
        "Répartition par Genre" to "التوزيع حسب الجنس",
        "Répartition par genre" to "التوزيع حسب الجنس",
        "Tranches d'Âge" to "الفئات العمرية",
        "élève" to "تلميذ",
        "élèves" to "تلاميذ",
        "Élève" to "تلميذ",
        "Élèves" to "تلاميذ",
        "Élèves inscrits" to "تلاميذ مسجلون",
        "Moyennes par Composante d'Évaluation" to "المعدلات حسب عناصر التقييم",
        "Moyennes par composante d'évaluation" to "المعدلات حسب عناصر التقييم",
        "Projet Pédagogique" to "المشروع البيداغوجي",
        "Projet pédagogique" to "المشروع البيداغوجي",
        "Composition" to "الاختبار الفصلي",
        "Performance comparative par Genre" to "الأداء المقارن حسب الجنس",
        "Performance comparative par genre" to "الأداء المقارن حسب الجنس",
        "Participation (Orale)" to "المشاركة (الشفوية)",
        "Discipline & Comportement" to "الانضباط والسلوك",
        "Assiduité & Présence" to "المواظبة والحضور",
        "Cahier & Devoirs" to "الكراس والواجبات",
        "Devoirs / Évaluations" to "الفروض والتقويمات",
        "Devoirs / Tests" to "الفروض والاختبارات",
        "Garçons ♂" to "ذكور ♂",
        "Filles ♀" to "إناث ♀",
        "Carnet" to "التنقيط",
        "Carnet de notes" to "التنقيط",
        "Carnet de notation" to "التنقيط",
        "Notes" to "التنقيط",
        "Absences" to "دفتر الغيابات",
        "Carnet d'absences" to "دفتر الغيابات",
        "Cahier de textes" to "دفتر النصوص",
        "Journal" to "دفتر النصوص",
        "Emploi du temps" to "جدول التوقيت",
        "Horaires" to "جدول التوقيت",
        "Documents" to "الوثائق الرسمية",
        "Rappels" to "التذكيرات",
        "Mémos" to "الملاحظات",
        "Prises de notes" to "تدوين الملاحظات",
        "Paramètres" to "الإعدادات",
        "À propos" to "حول التطبيق",

        // Trimestres
        "1er Trimestre" to "الفصل الأول",
        "2e Trimestre" to "الفصل الثاني",
        "3e Trimestre" to "الفصل الثالث",
        "Trimestre 1" to "الفصل الأول",
        "Trimestre 2" to "الفصل الثاني",
        "Trimestre 3" to "الفصل الثالث",
        "T1" to "ف1",
        "T2" to "ف2",
        "T3" to "ف3",

        // Classes & Élèves
        "Nom de la classe" to "اسم الفوج",
        "Toutes les classes" to "جميع الأفواج",
        "Sélectionner une classe" to "اختيار الفوج",
        "Sélectionner une classe..." to "اختر الفوج...",
        "Sélection de la classe" to "اختيار الفوج",
        "Aucune classe disponible" to "لا توجد أفواج متاحة",
        "Aucune classe sélectionnée" to "لم يتم اختيار أي فوج",
        "Ajouter une classe" to "إضافة فوج",
        "Modifier la classe" to "تعديل الفوج",
        "Supprimer la classe" to "حذف الفوج",
        "Supprimer la classe et ses données" to "حذف الفوج وجميع بياناته",
        "Coefficient de la matière" to "معامل المادة",
        "Coefficient" to "المعامل",
        "Par défaut : 3" to "افتراضياً : 3",
        "Effectif" to "التعداد",
        "Total élèves" to "إجمالي التلاميذ",
        "Ajouter un élève" to "إضافة تلميذ",
        "Modifier l'élève" to "تعديل بيانات التلميذ",
        "Supprimer l'élève" to "حذف التلميذ",
        "Nom" to "اللقب",
        "Nom de famille" to "اللقب",
        "Nom de famille :" to "اللقب :",
        "Prénom" to "الاسم",
        "Nom & Prénom" to "اللقب والاسم",
        "Date de naissance" to "تاريخ الميلاد",
        "Date de naissance :" to "تاريخ الميلاد :",
        "Date de naissance " to "تاريخ الميلاد ",
        "Lieu de naissance" to "مكان الميلاد",
        "lieu de naissance" to "مكان الميلاد",
        "Lieu de naissance :" to "مكان الميلاد :",
        "Sexe" to "الجنس",
        "Garçon" to "ذكر",
        "Fille" to "أنثى",
        "Garçons" to "الذكور",
        "Filles" to "الإناث",
        "Garçons ♂" to "الذكور ♂",
        "Filles ♀" to "الإناث ♀",
        "Masculin" to "ذكر",
        "Féminin" to "أنثى",
        "Âge" to "السن",
        "Âge moyen" to "متوسط العمر",
        "Âge calculé" to "السن المحسوب",
        "Tranches d'Âge" to "الفئات العمرية",
        "Pyramide des Âges" to "هرم الأعمار",
        "Pyramide des âges" to "هرم الأعمار",
        "Résumé de la Classe" to "ملخص الفوج",
        "Élèves inscrits" to "مسجلون",
        "Meilleur résultat" to "الأفضل",
        "Moins bon résultat" to "الأدنى",
        "Statistiques de Réussite" to "نسبة النجاح",
        "Statistiques de réussite" to "نسبة النجاح",
        "Aucune note saisie pour ce trimestre." to "لا توجد نقاط مسجلة لهذا الفصل.",
        "Aucune note saisie pour ce trimestre" to "لا توجد نقاط مسجلة لهذا الفصل",
        "Répartition des Moyennes" to "توزيع المعدلات",
        "Redoublant" to "معيد",
        "Non redoublant" to "غير معيد",
        "Redoublants" to "المعيدون",
        "Non redoublants" to "غير المعيدين",
        "Filtres" to "عوامل التصفية",
        "Filtres avancés" to "تصفية وترتيب متقدم",
        "Tous" to "الكل",
        "Toutes" to "الكل",
        "Toutes les options" to "جميع الخيارات",
        "Ordre personnalisé (N°)" to "ترتيب مخصص (الرقم)",
        "Nom (A-Z)" to "اللقب (أ - ي)",
        "Nom (Z-A)" to "اللقب (ي - أ)",
        "Moyenne (+)" to "المعدل (الأعلى)",
        "Moyenne (-)" to "المعدل (الأدنى)",
        "Âge (Le plus jeune)" to "السن (الأصغر سناً)",
        "Âge (Le plus âgé)" to "السن (الأكبر سناً)",
        "Statut scolaire" to "الوضعية الدراسية",
        "Performance (Moyennes)" to "الأداء (المعدلات)",
        "Admis (≥ 10)" to "ناجح (≥ 10)",
        "Non-admis (< 10)" to "غير ناجح (< 10)",
        "Excellent (≥ 16)" to "ممتاز (≥ 16)",
        "Sans Moyenne" to "دون معدل",
        "Non évalué" to "غير مقيم",
        "Évaluations et Notes" to "التقييمات والنقاط",
        "Devoir manquant" to "فرض ناقص",
        "Composition manquante" to "اختبار ناقص",
        "Aucune note" to "دون أي نقطة",
        "Ordre officiel" to "الترتيب الرسمي",
        "Enregistrer ce classement comme ordre officiel" to "حفظ هذا الترتيب كترتيب رسمي للفوج",
        "Numéro" to "الرقم",
        "N°" to "الرقم",

        // Évaluation & Carnet de notes
        "Évaluation continue" to "التقويم المستمر",
        "Devoir 1" to "الفرض 1",
        "Devoir 2" to "الفرض 2",
        "Composition" to "الاختبار",
        "Examen" to "الاختبار",
        "Moyenne" to "المعدل",
        "Moyenne générale" to "المعدل العام",
        "Moyenne de classe" to "معدل الفوج",
        "Moyenne du groupe" to "معدل الفوج",
        "Plus haute" to "أعلى نقطة",
        "Plus basse" to "أدنى نقطة",
        "Note max" to "أعلى نقطة",
        "Note min" to "أدنى نقطة",
        "Taux de réussite" to "نسبة النجاح",
        "Admis" to "ناجح",
        "Ajourné" to "مؤجل",
        "Appréciation" to "الملاحظة",
        "Rang" to "الرتبة",
        "Coefficients et formules" to "المعاملات والصيغ",
        "Participation" to "المشاركة",
        "Discipline" to "الانضباط",
        "Cahier" to "كراس الدروس",
        "Cahier de cours" to "كراس الدروس",
        "Assiduité" to "المواظبة",
        "Projet" to "المشروع",
        "Enregistrer les notes" to "حفظ النقاط",
        "Saisie rapide des notes" to "الرقن السريع للنقاط",
        "Notes de la classe" to "نقاط الفوج التربوي",
        "Touchez une case pour modifier la note" to "المس الخانة لتعديل النقطة",
        "Exporter le carnet de notes" to "تصدير دفتر التنقيط",
        "Bulletin de notes" to "كشف النقاط",

        // Absences & Discipline & Carnet d'assiduité
        "Appel" to "المناداة",
        "1. Appel" to "1. المناداة",
        "Registre" to "السجل",
        "2. Registre" to "2. السجل",
        "Stats" to "الإحصائيات",
        "stats" to "إحصائيات",
        "3. Stats" to "3. الإحصائيات",
        "Bilan d'Assiduité" to "حصيلة المواظبة والغيابات",
        "Bilan d'assiduité" to "حصيلة المواظبة والغيابات",
        "Bilan d'Assiduité - Toutes les classes" to "حصيلة المواظبة والغيابات — جميع الأفواج",
        "Bilan d'Assiduité — Toutes les classes" to "حصيلة المواظبة والغيابات — جميع الأفواج",
        "Absence" to "غياب",
        "Absences" to "الغيابات",
        "Retard" to "تأخر",
        "Retards" to "التأخرات",
        "Justifié" to "مبرر",
        "Justifiées" to "المبررة",
        "Non justifié" to "غير مبرر",
        "Exclusions" to "حالات الطرد",
        "Présence" to "الحضور",
        "Suivi nominatif des élèves" to "متابعة التلاميذ اسمياً",
        "Toutes les classes" to "جميع الأفواج",
        "Liste des élèves" to "قائمة التلاميذ",
        "Aucun incident d'assiduité enregistré pour cette sélection" to "لا توجد أي حالة مواظبة مسجلة لهذا التحديد",
        "Aucun incident d'assiduité enregistré pour cette sélection." to "لا توجد أي حالة مواظبة مسجلة لهذا التحديد.",
        "Tout présent" to "الكل حاضر",
        "Classe concernée :" to "الفوج المعني :",
        "Rechercher un élève..." to "بحث عن تلميذ...",
        "Sélectionnez une classe" to "اختر الفوج",
        "Aucun élève dans cette classe" to "لا يوجد تلاميذ في هذا الفوج",
        "Motif Justifié" to "مبرر",
        "Filtrer par classe :" to "تصفية حسب الفوج :",
        "Export PDF" to "تصدير PDF",
        "Tout plier" to "طي الكل",
        "Tout déplier" to "توسيع الكل",
        "Rechercher (élève, motif, date, matière...)" to "بحث (تلميذ، سبب، تاريخ، مادة...)",
        "Classe :" to "الفوج :",
        "Statistiques de la classe :" to "إحصائيات الفوج :",
        "Séances" to "الحصص",
        "contrôlées" to "المسجلة",
        "cumul total" to "المجموع الكلي",
        "taux des cas" to "نسبة الحالات",
        "Aucun élève trouvé." to "لم يتم العثور على أي تلميذ.",
        "Alerte Suivi (≥3 abs)" to "تنبيه متابعة (≥3 غ)",
        "100% Assidu" to "مواظب 100%",
        "Régulier" to "منتظم",
        "Options d'exportation PDF" to "خيارات تصدير PDF",
        "Billet d'entrée" to "وصل الدخول",
        "Séance" to "الحصة",
        "Présent" to "حاضر",
        "Absent" to "غائب",
        "En retard" to "متأخر",
        "Taux d'absence" to "نسبة الغياب",
        "Historique des absences" to "سجل الغيابات",
        "Marquer tous présents" to "تعليم الجميع كحاضرين",
        "Date de la séance" to "تاريخ الحصة",
        "Motif" to "السبب",
        "Justification" to "التبرير",
        "Appel du jour" to "المناداة اليومية",

        // Emploi du temps
        "Salle" to "القاعة",
        "Matière" to "المادة",
        "Créneau horaire" to "التوقيت",
        "Dimanche" to "الأحد",
        "Lundi" to "الإثنين",
        "Mardi" to "الثلاثاء",
        "Mercredi" to "الأربعاء",
        "Jeudi" to "الخميس",
        "Vendredi" to "الجمعة",
        "Samedi" to "السبت",
        "Aujourd'hui" to "اليوم",
        "Demain" to "غداً",
        "Journée libre" to "يوم راحة",
        "Cours actuel" to "الدرس الحالي",
        "À venir" to "الدرس القادم",
        "Réception des parents" to "استقبال الأولياء",
        "Ajouter une séance" to "إضافة حصة",
        "Modifier la séance" to "تعديل الحصة",
        "Supprimer la séance" to "حذف الحصة",
        "Jour de repos" to "يوم الراحة الأسبوعي",

        // Cahier de textes (Journal)
        "Titre de la séance" to "عنوان الحصة",
        "Titre du cours" to "عنوان الدرس",
        "Objectifs pédagogiques" to "الأهداف التعلمية",
        "Contenu et activités" to "محتوى وأنشطة الحصة",
        "Travail à faire (Devoirs)" to "الواجبات والمهام المنزلية",
        "Observations" to "ملاحظات وتوجيهات",
        "Ajouter un cours" to "إضافة درس جديد",
        "Modifier le cours" to "تعديل الدرس",
        "Supprimer le cours" to "حذف الدرس",
        "Résumé IA hebdomadaire" to "الملخص الأسبوعي بالذكاء الاصطناعي",
        "Générer le résumé" to "توليد الملخص بالذكاء الاصطناعي",
        "Enregistrement vocal" to "تسجيل صوتي",
        "Photo / Document" to "صورة / وثيقة",

        // Documents officiels
        "Textes officiels" to "النصوص الرسمية",
        "Textes officiels et réglementaires" to "النصوص الرسمية والتنظيمية",
        "Dossier protégé" to "مجلد محمي",
        "Texte officiel protégé" to "نص رسمي محمي",
        "Supprimer le dossier ?" to "هل تريد حذف المجلد؟",
        "Supprimer le document ?" to "هل تريد حذف الوثيقة؟",
        "Importer un document" to "استيراد وثيقة",
        "Nouveau dossier" to "مجلد جديد",
        "Nom de la catégorie" to "اسم المجلد / الفئة",
        "Fiches pédagogiques" to "المذكرات البيداغوجية",
        "Répartitions annuelles" to "التوزيعات السنوية",
        "Législation scolaire" to "التشريع المدرسي",

        // Paramètres & Profil
        "Profil de l'enseignant" to "ملف الأستاذ",
        "Nom de l'enseignant" to "اسم الأستاذ",
        "Matière enseignée" to "المادة الم Tatiana",
        "Matière enseignée" to "المادة المدرسَة",
        "Établissement scolaire" to "المؤسسة التعليمية",
        "Année scolaire" to "السنة الدراسية",
        "Année scolaire active" to "السنة الدراسية الحالية",
        "Gestion de l'Année Scolaire" to "إدارة السنة الدراسية",
        "Liste des Années Scolaires :" to "قائمة السنوات الدراسية :",
        "Enregistrer les modifications" to "حفظ التعديلات",
        "Profil enseignant enregistré !" to "تم حفظ ملف الأستاذ بنجاح!",
        "Année scolaire archivée !" to "تمت أرشفة السنة الدراسية بنجاح!",
        "Nouvelle Année Scolaire" to "سنة دراسية جديدة",
        "Créer une Année Scolaire" to "إنشاء سنة دراسية",
        "Apparence" to "المظهر",
        "Apparence & Langue" to "المظهر واللغة",
        "Langue de l'interface" to "لغة الواجهة",
        "Mode d'affichage" to "وضع العرض",
        "Clair" to "فاتح",
        "Sombre" to "داكن",
        "Système" to "تلقائي (النظام)",
        "Taille de police" to "حجم الخط",
        "Petit" to "صغير",
        "Grand" to "كبير",
        "Palette de couleurs" to "لوحة الألوان",
        "Police de caractères" to "نوع الخط",
        "Police en gras" to "خط عريض (Bold)",
        "Sécurité & Verrouillage" to "الأمان والقفل",
        "Code PIN" to "رمز PIN",
        "Activer le verrouillage par code PIN" to "تفعيل القفل برمز PIN",
        "Désactiver le code PIN" to "إلغاء قفل PIN",
        "Modifier le code PIN" to "تغيير رمز PIN",
        "Déverrouillage biométrique" to "الفتح بالبصمة الحيوية",
        "Sauvegarde & Restauration" to "النسخ الاحتياطي والاستعادة",
        "Sauvegarde & Synchronisation Cloud" to "النسخ الاحتياطي والمزامنة السحابية",
        "Synchronisation Google Drive" to "مزامنة Google Drive",
        "Exporter une sauvegarde" to "تصدير نسخة احتياطية",
        "Restaurer une sauvegarde" to "استعادة نسخة احتياطية",
        "Exportations globales (PDF)" to "التقارير والمخرجات (PDF)",
        "Générer le rapport complet" to "توليد التقرير الشامل PDF",

        // Rappels et Mémos
        "Tâches & Rappels" to "المهام والتذكيرات",
        "Nouveau rappel" to "تذكير جديد",
        "Nouvelle note" to "ملاحظة جديدة",
        "Priorité" to "الأولوية",
        "Haute" to "عالية",
        "Moyenne" to "متوسطة",
        "Basse" to "منخفضة",
        "Terminé" to "مكتمل",
        "En attente" to "قيد الانتظار"
    )

    val notificationDictionary = mapOf(
        "Classe modifiée." to "تم تعديل الفوج بنجاح.",
        "Classe supprimée." to "تم حذف الفوج بنجاح.",
        "Élève modifié." to "تم تعديل بيانات التلميذ(ة) بنجاح.",
        "Élève supprimé." to "تم حذف التلميذ(ة) بنجاح.",
        "Places échangées." to "تم تبديل المقاعد بنجاح.",
        "Toutes les places ont été libérées." to "تم تحرير جميع المقاعد.",
        "Absences de la séance enregistrées !" to "تم تسجيل غيابات الحصة بنجاح!",
        "Saisie d'absence supprimée" to "تم حذف تسجيل الغياب.",
        "Saisie d'absence mise à jour !" to "تم تحديث تسجيل الغياب بنجاح!",
        "Journée libre configurée" to "تم ضبط يوم الراحة الأسبوعي.",
        "Journée libre réinitialisée" to "تمت إعادة ضبط يوم الراحة.",
        "Entrée journal pédagogique ajoutée." to "تمت إضافة بطاقة دفتر النصوص بنجاح.",
        "Fiche journal modifiée." to "تم تعديل بطاقة دفتر النصوص بنجاح.",
        "Entrée journal supprimée." to "تم حذف بطاقة دفتر النصوص بنجاح.",
        "Créneau ajouté à l'emploi du temps." to "تمت إضافة الحصة إلى جدول التوقيت.",
        "Créneau mis à jour." to "تم تحديث الحصة في جدول التوقيت.",
        "Créneau supprimé." to "تم حذف الحصة من جدول التوقيت.",
        "Rappel ajouté." to "تمت إضافة التذكير بنجاح.",
        "Rappel supprimé." to "تم حذف التذكير بنجاح.",
        "Note ajoutée." to "تمت إضافة النقطة بنجاح.",
        "Note modifiée." to "تم تعديل النقطة بنجاح.",
        "Note supprimée." to "تم حذف النقطة بنجاح.",
        "Catégorie ajoutée." to "تمت إضافة الفئة بنجاح.",
        "Catégorie supprimée." to "تم حذف الفئة بنجاح.",
        "Dossier renommé avec succès." to "تمت إعادة تسمية المجلد بنجاح.",
        "Document ajouté." to "تمت إضافة الوثيقة بنجاح.",
        "Document supprimé." to "تم حذف الوثيقة بنجاح.",
        "Document renommé avec succès." to "تمت إعادة تسمية الوثيقة بنجاح.",
        "Emploi du temps exporté en PDF !" to "تم تصدير جدول التوقيت إلى PDF بنجاح!",
        "Tous les emplois du temps exportés en PDF !" to "تم تصدير جميع جداول التوقيت إلى PDF بنجاح!",
        "Registre des absences exporté en PDF !" to "تم تصدير سجل الغيابات إلى PDF بنجاح!",
        "Carnet de notes exporté en PDF !" to "تم تصدير دفتر النقط إلى PDF بنجاح!",
        "Plan de classe exporté en PDF !" to "تم تصدير مخطط الجلوس إلى PDF بنجاح!",
        "Liste de classe exportée en PDF !" to "تم تصدير قائمة التلاميذ إلى PDF بنجاح!",
        "Cahier journal exporté en PDF !" to "تم تصدير دفتر النصوص إلى PDF بنجاح!",
        "Erreur lors de la génération du PDF." to "خطأ أثناء إنشاء ملف PDF.",
        "Impossible d'ouvrir le fichier de destination." to "تعذر فتح ملف الوجهة.",
        "Impossible de créer le fichier PDF." to "تعذر إنشاء ملف PDF.",
        "Échec de l'exportation du PDF." to "فشل تصدير ملف PDF.",
        "Base de données restaurée !" to "تمت استعادة قاعدة البيانات بنجاح!",
        "Échec de restauration (Mot de passe incorrect ou données corrompues)." to "فشلت الاستعادة (كلمة المرور غير صحيحة أو البيانات تالفة).",
        "Sauvegarde enregistrée dans le fichier !" to "تم حفظ النسخة الاحتياطية في الملف بنجاح!",
        "Erreur d'écriture du fichier." to "خطأ في كتابة الملف.",
        "Erreur lors de la génération de la sauvegarde." to "خطأ أثناء إنشاء النسخة الاحتياطية.",
        "Données restaurées avec succès !" to "تمت استعادة البيانات بنجاح!",
        "Sauvegarde Gmail restaurée avec succès !" to "تمت استعادة نسخة Gmail الاحتياطية بنجاح!",
        "Restauration échouée : mot de passe invalide." to "فشلت الاستعادة: كلمة المرور غير صحيحة.",
        "Restauration échouée : mot de passe invalide ou fichier corrompu." to "فشلت الاستعادة: كلمة المرور غير صحيحة أو الملف تالف.",
        "Fichier vide ou invalide." to "الملف فارغ أو غير صالح.",
        "Erreur de lecture du fichier." to "خطأ في قراءة الملف.",
        "Profil enseignant enregistré !" to "تم حفظ ملف الأستاذ بنجاح!",
        "Classement sauvegardé avec succès !" to "تم حفظ الترتيب بنجاح!",
        "Notes enregistrées !" to "تم حفظ النقاط بنجاح!",
        "Échec de l'enregistrement du fichier." to "فشل حفظ الملف.",
        "Fichier introuvable. Il se peut qu'il ait été déplacé ou supprimé." to "الملف غير موجود. ربما تم نقله أو حذفه.",
        "Aucune application de lecture compatible trouvée pour ouvrir ce document." to "لم يتم العثور على أي تطبيق مناسب لفتح هذه الوثيقة.",
        "Veuillez remplir le titre et sélectionner un fichier." to "يرجى إدخال العنوان واختيار ملف.",
        "Le dossier 'Textes réglementaires' est protégé contre la suppression." to "مجلد 'النصوص التشريعية والتنظيمية' محمي ضد الحذف.",
        "Ce texte réglementaire officiel est protégé contre la suppression." to "هذا النص التشريعي الرسمي محمي ضد الحذف.",
        "Ce dossier réglementaire est protégé contre la suppression." to "هذا المجلد التنظيمي محمي ضد الحذف.",
        "Enregistrement effectué avec succès !" to "تم الحفظ بنجاح!",
        "Enregistrement effectué avec succès" to "تم الحفظ بنجاح",
        "Enregistré avec succès !" to "تم الحفظ بنجاح!",
        "Enregistré avec succès" to "تم الحفظ بنجاح",
        "Enregistrement réussi !" to "تم الحفظ بنجاح!",
        "Modifications enregistrées avec succès !" to "تم حفظ التعديلات بنجاح!",
        "Modifications enregistrées !" to "تم حفظ التعديلات بنجاح!",
        "Données enregistrées avec succès !" to "تم حفظ البيانات بنجاح!",
        "Données enregistrées !" to "تم حفظ البيانات بنجاح!",
        "Sauvegarde effectuée avec succès !" to "تم إجراء النسخ الاحتياطي بنجاح!",
        "Sauvegarde réussie !" to "تم النسخ الاحتياطي بنجاح!",
        "Sauvegarde enregistrée !" to "تم حفظ النسخة الاحتياطية!",
        "Profil enseignant mis à jour !" to "تم تحديث ملف الأستاذ بنجاح!",
        "Profil enseignant modifié !" to "تم تعديل ملف الأستاذ بنجاح!",
        "Séance enregistrée avec succès !" to "تم تسجيل الحصة بنجاح!",
        "Séance enregistrée !" to "تم تسجيل الحصة بنجاح!",
        "Appel enregistré avec succès !" to "تم تسجيل المناداة بنجاح!",
        "Appel enregistré !" to "تم تسجيل المناداة بنجاح!",
        "Notes enregistrées avec succès !" to "تم حفظ النقاط بنجاح!",
        "Fiche enregistrée avec succès !" to "تم حفظ البطاقة بنجاح!",
        "Fiche enregistrée !" to "تم حفظ البطاقة بنجاح!",
        "Document enregistré avec succès !" to "تم حفظ الوثيقة بنجاح!",
        "Document enregistré !" to "تم حفظ الوثيقة بنجاح!",
        "Élève enregistré avec succès !" to "تم تسجيل التلميذ(ة) بنجاح!",
        "Classe enregistrée avec succès !" to "تم تسجيل الفوج بنجاح!",
        "Rappel enregistré avec succès !" to "تم حفظ التذكير بنجاح!",
        "Rappel enregistré !" to "تم حفظ التذكير بنجاح!",
        "Rappel modifié !" to "تم تعديل التذكير بنجاح!",
        "Tâche terminée !" to "تم إكمال المهمة بنجاح!",
        "Confirmation" to "تأكيد",
        "Confirmation de l'enregistrement" to "تأكيد الحفظ",
        "Confirmation de suppression" to "تأكيد الحذف",
        "Enregistrement..." to "جاري الحفظ...",
        "Sauvegarde en cours..." to "جاري النسخ الاحتياطي...",
        "Restauration en cours..." to "جاري الاستعادة...",
        "Génération du PDF en cours..." to "جاري إنشاء ملف PDF...",
        "Exportation en cours..." to "جاري التصدير...",
        "Importation en cours..." to "جاري الاستيراد..."
    )
}

@Composable
fun tr(fr: String, ar: String): String = Tr.get(fr, ar)

fun tr(fr: String, ar: String, isAr: Boolean): String = Tr.get(fr, ar, isAr)

@Composable
fun tr(fr: String): String = Tr.t(fr)

fun tr(fr: String, isAr: Boolean): String = Tr.translate(fr, isAr)

@Composable
fun String.ar(): String = Tr.t(this)

fun String.ar(isAr: Boolean): String = Tr.translate(this, isAr)
