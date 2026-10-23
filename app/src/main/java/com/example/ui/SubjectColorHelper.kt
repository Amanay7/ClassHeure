package com.example.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

object SubjectColorHelper {
    // 1. Mathématiques : Bleu Marine Royal
    val mathPalette = ClassColorPalette(
        primary = Color(0xFF1E40AF),
        container = Color(0xFFEFF6FF),
        onContainer = Color(0xFF172554),
        label = "Mathématiques"
    )

    // 2. Français & Littérature : Rouge Carmin Vif
    val frenchPalette = ClassColorPalette(
        primary = Color(0xFFDC2626),
        container = Color(0xFFFEE2E2),
        onContainer = Color(0xFF991B1B),
        label = "Français"
    )

    // 3. Histoire : Ambre Brun / Ocre
    val historyPalette = ClassColorPalette(
        primary = Color(0xFFB45309),
        container = Color(0xFFFEF3C7),
        onContainer = Color(0xFF78350F),
        label = "Histoire"
    )

    // 4. Géographie : Vert Olive Pomme
    val geoPalette = ClassColorPalette(
        primary = Color(0xFF65A30D),
        container = Color(0xFFECFCCB),
        onContainer = Color(0xFF3F6212),
        label = "Géographie"
    )

    // 5. Histoire-Géographie & EMC : Orange Safran Chaud
    val historyGeoPalette = ClassColorPalette(
        primary = Color(0xFFD97706),
        container = Color(0xFFFEF3C7),
        onContainer = Color(0xFF92400E),
        label = "Histoire-Géo"
    )

    // 6. Physique : Violet Améthyste Électrique
    val physicsPalette = ClassColorPalette(
        primary = Color(0xFF7C3AED),
        container = Color(0xFFEDE9FE),
        onContainer = Color(0xFF5B21B6),
        label = "Physique"
    )

    // 7. Chimie : Pourpre Magenta Éclatant
    val chemistryPalette = ClassColorPalette(
        primary = Color(0xFFC026D3),
        container = Color(0xFFFAE8FF),
        onContainer = Color(0xFF701A75),
        label = "Chimie"
    )

    // 8. Physique-Chimie : Indigo Électrique
    val physicsChemistryPalette = ClassColorPalette(
        primary = Color(0xFF4F46E5),
        container = Color(0xFFEEF2FF),
        onContainer = Color(0xFF312E81),
        label = "Physique-Chimie"
    )

    // 9. SVT & Biologie : Vert Feuille Lumineux
    val biologySvtPalette = ClassColorPalette(
        primary = Color(0xFF16A34A),
        container = Color(0xFFDCFCE7),
        onContainer = Color(0xFF14532D),
        label = "SVT"
    )

    // 10. Sciences Naturelles & Géologie : Sarcelle Océan
    val naturalSciencePalette = ClassColorPalette(
        primary = Color(0xFF0D9488),
        container = Color(0xFFCCFBF1),
        onContainer = Color(0xFF115E59),
        label = "Sciences"
    )

    // 11. Anglais : Bleu Ciel Lumineux
    val englishPalette = ClassColorPalette(
        primary = Color(0xFF2563EB),
        container = Color(0xFFDBEAFE),
        onContainer = Color(0xFF1D4ED8),
        label = "Anglais"
    )

    // 12. Espagnol : Jaune Ocre Doré
    val spanishPalette = ClassColorPalette(
        primary = Color(0xFFEAB308),
        container = Color(0xFFFEF9C3),
        onContainer = Color(0xFF713F12),
        label = "Espagnol"
    )

    // 13. Allemand : Brun Chocolat Chaud
    val germanPalette = ClassColorPalette(
        primary = Color(0xFF78350F),
        container = Color(0xFFFEF3C7),
        onContainer = Color(0xFF451A03),
        label = "Allemand"
    )

    // 14. Italien : Vert Eau / Aqua
    val italianPalette = ClassColorPalette(
        primary = Color(0xFF14B8A6),
        container = Color(0xFFCCFBF1),
        onContainer = Color(0xFF0F766E),
        label = "Italien"
    )

    // 15. Langue Arabe : Vert Émeraude / Jade Lumineux
    val arabicPalette = ClassColorPalette(
        primary = Color(0xFF047857),
        container = Color(0xFFD1FAE5),
        onContainer = Color(0xFF064E3B),
        label = "Langue Arabe"
    )

    // 16. Éducation Islamique / Sciences Islamiques : Bordeaux Grenat Impérial (Totalement distinct de l'Arabe)
    val islamicTarbiyaPalette = ClassColorPalette(
        primary = Color(0xFF881337),
        container = Color(0xFFFFE4E6),
        onContainer = Color(0xFF4C0519),
        label = "Éducation Islamique"
    )

    // 17. Éducation Physique & Sport (EPS) : Orange Vif / Mandarine
    val sportPalette = ClassColorPalette(
        primary = Color(0xFFEA580C),
        container = Color(0xFFFFEDD5),
        onContainer = Color(0xFF9A3412),
        label = "EPS"
    )

    // 18. Arts Plastiques & Dessin : Rose Fuchsia Éclatant
    val artsPalette = ClassColorPalette(
        primary = Color(0xFFDB2777),
        container = Color(0xFFFCE7F3),
        onContainer = Color(0xFF9D174D),
        label = "Arts Plastiques"
    )

    // 19. Musique & Chant : Turquoise Lagon Océan (Totalement distinct du violet de la Physique)
    val musicPalette = ClassColorPalette(
        primary = Color(0xFF0891B2),
        container = Color(0xFFCFFAFE),
        onContainer = Color(0xFF155E75),
        label = "Musique"
    )

    // 20. Informatique : Cyan Turquoise
    val informaticsPalette = ClassColorPalette(
        primary = Color(0xFF0284C7),
        container = Color(0xFFE0F2FE),
        onContainer = Color(0xFF0369A1),
        label = "Informatique"
    )

    // 21. Technologie & Robotique : Vert Kaki Mousse
    val technologyPalette = ClassColorPalette(
        primary = Color(0xFF4D7C0F),
        container = Color(0xFFF7FEE7),
        onContainer = Color(0xFF365314),
        label = "Technologie"
    )

    // 22. Philosophie & Pensée : Gris Ardoise Anthracite
    val philosophyPalette = ClassColorPalette(
        primary = Color(0xFF475569),
        container = Color(0xFFF1F5F9),
        onContainer = Color(0xFF0F172A),
        label = "Philosophie"
    )

    // 23. Économie / Gestion / Droit / Compta : Rouille Terre Cuite
    val economyPalette = ClassColorPalette(
        primary = Color(0xFF9A3412),
        container = Color(0xFFFFEDD5),
        onContainer = Color(0xFF7C2D12),
        label = "Économie / Gestion"
    )

    // 24. Éducation Civique / Morale : Bleu Canard Océan
    val civicsPalette = ClassColorPalette(
        primary = Color(0xFF0891B2),
        container = Color(0xFFCFFAFE),
        onContainer = Color(0xFF155E75),
        label = "Éducation Civique"
    )

    // 25. Tamaziɣt / Berbère : Vert Émeraude Menthe Exclusif
    val tamazightPalette = ClassColorPalette(
        primary = Color(0xFF10B981),
        container = Color(0xFFD1FAE5),
        onContainer = Color(0xFF065F46),
        label = "Tamaziɣt"
    )

    val receptionPalette = ClassColorPalette(
        primary = Color(0xFF6B7280),
        container = Color(0xFFF3F4F6),
        onContainer = Color(0xFF374151),
        label = "Réception"
    )

    // Liste étendue de palettes distinctes
    private val allKnownPalettes = listOf(
        mathPalette, frenchPalette, historyPalette, geoPalette, historyGeoPalette,
        physicsPalette, chemistryPalette, physicsChemistryPalette, biologySvtPalette,
        naturalSciencePalette, englishPalette, spanishPalette, germanPalette,
        italianPalette, arabicPalette, islamicTarbiyaPalette, sportPalette,
        artsPalette, musicPalette, informaticsPalette, technologyPalette,
        philosophyPalette, economyPalette, civicsPalette, tamazightPalette
    )

    fun getPaletteForSubject(subject: String): ClassColorPalette {
        val s = subject.lowercase(java.util.Locale.getDefault()).trim()

        if (s.contains("réception") || s.contains("reception") || s.contains("parent")) {
            return receptionPalette
        }

        // Tamaziɣt / Tamazight / Amazigh (Vert Émeraude)
        if (s.contains("tamazi") || s.contains("tamazight") || s.contains("amazigh") || s.contains("berbèr") || s.contains("berber") || s.contains("tifinagh")) {
            return tamazightPalette
        }

        // 1. Mathématiques
        if (s.contains("math") || s.contains("algèbr") || s.contains("algebr") || s.contains("géom") || s.contains("geom") || s.contains("calcul") || s.contains("stat")) {
            return mathPalette
        }
        // 2. Français & Littérature
        if (s.contains("franç") || s.contains("franc") || s.contains("littér") || s.contains("litter") || s.contains("lect") || s.contains("dict") || s.contains("gramm") || s.contains("conjug") || s.contains("orthog") || s.contains("rédaction") || s.contains("redaction") || s.contains("poésie") || s.contains("poesie") || s.contains("texte")) {
            return frenchPalette
        }
        // 3. Histoire seule
        if (s.contains("hist") && !s.contains("géo") && !s.contains("geo")) {
            return historyPalette
        }
        // 4. Géographie seule
        if ((s.contains("géo") || s.contains("geo") || s.contains("géographie") || s.contains("geographie")) && !s.contains("hist")) {
            return geoPalette
        }
        // 5. Histoire-Géo combinée
        if (s.contains("hist") || s.contains("géo") || s.contains("geo") || s.contains("histoire-géo") || s.contains("hg")) {
            return historyGeoPalette
        }
        // 6. Physique-Chimie combinée
        if (s.contains("phys") && s.contains("chim")) {
            return physicsChemistryPalette
        }
        // 7. Physique seule
        if (s.contains("phys")) {
            return physicsPalette
        }
        // 8. Chimie seule
        if (s.contains("chim")) {
            return chemistryPalette
        }
        // 9. SVT & Biologie
        if (s.contains("svt") || s.contains("biol")) {
            return biologySvtPalette
        }
        // 10. Éducation Islamique / Sciences Islamiques / Tarbiya
        if (s.contains("islam") || s.contains("tarbiya") || s.contains("diniya") || s.contains("coran") || s.contains("hadith") || s.contains("charia")) {
            return islamicTarbiyaPalette
        }
        // 11. Sciences Naturelles & Géologie
        if (s.contains("science") || s.contains("sc.") || s.contains("nature") || s.contains("terre") || s.contains("géol") || s.contains("geol")) {
            return naturalSciencePalette
        }
        // 12. Anglais
        if (s.contains("angl") || s.contains("eng")) {
            return englishPalette
        }
        // 13. Espagnol
        if (s.contains("espag") || s.contains("span")) {
            return spanishPalette
        }
        // 14. Allemand
        if (s.contains("allem") || s.contains("germ")) {
            return germanPalette
        }
        // 15. Italien
        if (s.contains("ital")) {
            return italianPalette
        }
        // 16. Langue Arabe
        if (s.contains("arab") || s.contains("arabe") || s.contains("arabiya")) {
            return arabicPalette
        }
        // 17. EPS / Sport
        if (s.contains("eps") || s.contains("sport") || s.contains("gym") || s.contains("physique et sport") || s.contains("natation") || s.contains("athlét") || s.contains("athlet")) {
            return sportPalette
        }
        // 18. Arts Plastiques & Dessin
        if (s.contains("art") || s.contains("dessin") || s.contains("peinture") || s.contains("théâtre") || s.contains("theatre") || s.contains("plastique")) {
            return artsPalette
        }
        // 19. Musique & Chant
        if (s.contains("musiq") || s.contains("chant") || s.contains("solfège") || s.contains("solfege")) {
            return musicPalette
        }
        // 20. Informatique
        if (s.contains("info") || s.contains("informatique") || s.contains("prog") || s.contains("algo") || s.contains("bureautique") || s.contains("code")) {
            return informaticsPalette
        }
        // 21. Technologie & Robotique
        if (s.contains("tech") || s.contains("numér") || s.contains("numer") || s.contains("robot") || s.contains("mécanique") || s.contains("mecanique")) {
            return technologyPalette
        }
        // 22. Philosophie & Pensée
        if (s.contains("philo") || s.contains("pensée") || s.contains("pensee") || s.contains("éthique") || s.contains("ethique")) {
            return philosophyPalette
        }
        // 23. Économie & Gestion / Droit
        if (s.contains("éco") || s.contains("eco") || s.contains("gestion") || s.contains("droit") || s.contains("compta") || s.contains("commerce") || s.contains("finance")) {
            return economyPalette
        }
        // 24. Éducation Civique / EMC
        if (s.contains("emc") || s.contains("civique") || s.contains("citoyen") || s.contains("morale")) {
            return civicsPalette
        }

        // --- GÉNÉRATEUR DYNAMIQUE POUR TOUTES NOUVELLES MATIÈRES NON ENCORE CRÉÉES ---
        // Utilise la dispersion à ratio d'or (Golden Ratio) sur la roue chromatique (0..360°)
        // pour garantir que chaque nouvelle matière ait une couleur unique, distincte et bien contrastée.
        val hash = abs(s.hashCode())
        val index = hash % allKnownPalettes.size
        val basePalette = allKnownPalettes[index]

        // Dynamic HSL calculation for truly unique custom subjects
        val hue = (hash * 137.508f) % 360f // Golden angle distribution
        val hsvPrimary = floatArrayOf(hue, 0.85f, 0.82f)
        val hsvContainer = floatArrayOf(hue, 0.15f, 0.98f)
        val primaryInt = android.graphics.Color.HSVToColor(hsvPrimary)
        val containerInt = android.graphics.Color.HSVToColor(hsvContainer)

        val dynPrimary = Color(primaryInt)
        val dynContainer = Color(containerInt)
        val dynOnContainer = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.95f, 0.45f)))

        return ClassColorPalette(
            primary = dynPrimary,
            container = dynContainer,
            onContainer = dynOnContainer,
            label = subject.trim()
        )
    }

    fun getIntColorForSubject(subject: String): Int {
        val c = getPaletteForSubject(subject).primary
        return android.graphics.Color.rgb((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }

    fun getContainerIntColorForSubject(subject: String): Int {
        val c = getPaletteForSubject(subject).container
        return android.graphics.Color.rgb((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }

    /**
     * Découpe intelligemment le nom de la matière sur deux lignes s'il est trop long
     * pour garantir un affichage optimal et esthétique dans les cases d'emploi du temps et dans le PDF.
     */
    fun splitSubjectIntoTwoLines(subject: String, maxLineLength: Int = 11): Pair<String, String>? {
        val clean = subject.trim()
        if (clean.length <= maxLineLength && !clean.contains("-") && !clean.contains("/")) {
            return null // Tient parfaitement sur une seule ligne
        }

        // Priorité 1 : coupure au tiret (ex: Histoire-Géo, Physique-Chimie)
        if (clean.contains("-")) {
            val idx = clean.indexOf("-")
            val p1 = clean.substring(0, idx + 1).trim()
            val p2 = clean.substring(idx + 1).trim()
            if (p1.isNotBlank() && p2.isNotBlank()) return Pair(p1, p2)
        }

        // Priorité 2 : coupure au slash (ex: SVT / Géologie)
        if (clean.contains("/")) {
            val idx = clean.indexOf("/")
            val p1 = clean.substring(0, idx).trim()
            val p2 = clean.substring(idx + 1).trim()
            if (p1.isNotBlank() && p2.isNotBlank()) return Pair(p1, p2)
        }

        // Priorité 3 : coupure au mot le plus proche du milieu
        val words = clean.split(Regex("\\s+"))
        if (words.size >= 2) {
            val mid = (words.size + 1) / 2
            val p1 = words.subList(0, mid).joinToString(" ")
            val p2 = words.subList(mid, words.size).joinToString(" ")
            return Pair(p1, p2)
        }

        // Mot unique très long (ex: Mathématiques)
        if (clean.length > maxLineLength) {
            val cut = maxLineLength - 1
            return Pair(clean.substring(0, cut) + ".", clean.substring(cut))
        }

        return null
    }
}
