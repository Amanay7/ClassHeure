package com.example.util

import java.util.Calendar

/**
 * Formate le nom et prénom de l'enseignant pour que SEULES les premières lettres
 * de chaque mot soient en majuscule, le reste étant obligatoirement en minuscule.
 * Exemples :
 * "DUPONT JEAN" -> "Dupont Jean"
 * "ahmed benali" -> "Ahmed Benali"
 * "jean-luc" -> "Jean-Luc"
 * "o'connor" -> "O'Connor"
 */
fun formatTeacherName(input: String): String {
    if (input.isEmpty()) return ""
    val sb = StringBuilder(input.length)
    var isStartOfWord = true

    for (i in input.indices) {
        val ch = input[i]
        if (ch == ' ' || ch == '-' || ch == '\'' || ch == '.' || ch == '_' || ch == '/') {
            sb.append(ch)
            isStartOfWord = true
        } else if (ch.isLetter()) {
            if (isStartOfWord) {
                sb.append(ch.uppercaseChar())
                isStartOfWord = false
            } else {
                sb.append(ch.lowercaseChar())
            }
        } else {
            sb.append(ch)
            isStartOfWord = false
        }
    }
    return sb.toString()
}

/**
 * Formate les heures de cours avec minutes systématiquement affichées (y compris :00) :
 * "09:00 - 10:00" -> "09:00 - 10:00"
 * "10:00 - 11:00" -> "10:00 - 11:00"
 * "8:00 - 9:00" -> "08:00 - 09:00"
 * "08h00 - 09h00" -> "08:00 - 09:00"
 * "8 - 9" -> "08:00 - 09:00"
 */
fun formatSlotHours(timeSlot: String): String {
    val clean = timeSlot.replace("–", "-").replace("—", "-")
    val parts = clean.split("-").map { it.trim() }
    if (parts.size == 2) {
        fun formatH(hStr: String): String {
            val t = hStr.lowercase().replace("h", ":").trim()
            val colonParts = t.split(":")
            val hour = colonParts[0].filter { it.isDigit() }.toIntOrNull()
            val min = colonParts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            if (hour != null) {
                val hourPadded = hour.toString().padStart(2, '0')
                val minPadded = min.toString().padStart(2, '0')
                return "$hourPadded:$minPadded"
            }
            return hStr
        }
        val startFormatted = formatH(parts[0])
        val endFormatted = formatH(parts[1])
        return "$startFormatted - $endFormatted"
    }
    return timeSlot
}

/**
 * Retourne les heures de cours sur deux lignes avec minutes systématiquement affichées (ex: "10:00" au lieu de "10") :
 * Pair(startHourWithMin, endHourWithMin)
 * Exemple : "09:00 - 10:00" -> Pair("09:00", "10:00")
 * "10 - 11" -> Pair("10:00", "11:00")
 */
fun getSlotHoursTwoLines(timeSlot: String): Pair<String, String> {
    return getSlotHoursWithMinutesTwoLines(timeSlot)
}

/**
 * Retourne les heures de cours avec les minutes systématiquement affichées (ex: 10:00 au lieu de 10) :
 * Exemple : "10:00 - 11:00" -> Pair("10:00", "11:00")
 * "10 - 11" -> Pair("10:00", "11:00")
 * "08:00 - 09:00" -> Pair("08:00", "09:00")
 */
fun getSlotHoursWithMinutesTwoLines(timeSlot: String): Pair<String, String> {
    val clean = timeSlot.replace("–", "-").replace("—", "-")
    val parts = clean.split("-").map { it.trim() }
    if (parts.size == 2) {
        fun formatH(hStr: String): String {
            val t = hStr.lowercase().replace("h", ":").trim()
            val colonParts = t.split(":")
            val hour = colonParts[0].filter { it.isDigit() }.toIntOrNull()
            val min = colonParts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            if (hour != null) {
                val hourPadded = hour.toString().padStart(2, '0')
                val minPadded = min.toString().padStart(2, '0')
                return "$hourPadded:$minPadded"
            }
            return hStr
        }
        val startFormatted = formatH(parts[0])
        val endFormatted = formatH(parts[1])
        return Pair(startFormatted, endFormatted)
    }
    return Pair(timeSlot, "")
}

/**
 * Calcule l'âge d'un élève à partir de sa date de naissance au format YYYY-MM-DD ou DD/MM/YYYY ou DD-MM-YYYY
 */
fun calculateAge(birthDateStr: String): Int? {
    if (birthDateStr.isBlank()) return null
    return try {
        val clean = birthDateStr.trim().replace("/", "-")
        val parts = clean.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size == 3) {
            val (year, month, day) = if (parts[0] > 1000) {
                Triple(parts[0], parts[1], parts[2])
            } else {
                Triple(parts[2], parts[1], parts[0])
            }
            val calBirth = Calendar.getInstance().apply {
                set(year, (month - 1).coerceIn(0, 11), day.coerceIn(1, 31))
            }
            val calNow = Calendar.getInstance()
            var age = calNow.get(Calendar.YEAR) - calBirth.get(Calendar.YEAR)
            if (calNow.get(Calendar.DAY_OF_YEAR) < calBirth.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            if (age in 0..120) age else null
        } else null
    } catch (e: Exception) {
        null
    }
}

/**
 * Gestionnaire intelligent des créneaux horaires des emplois du temps :
 * Évite les superpositions et collisions (ex: si 13:30 - 14:30 existe, supprime 13:00-14:00 et 14:00-15:00,
 * et enchaîne logiquement sur 14:30-15:30).
 */
object TimetableTimeSlotHelper {
    data class SlotInterval(
        val originalString: String,
        val startMin: Int,
        val endMin: Int
    )

    fun parseSlotInterval(slotStr: String): SlotInterval? {
        val clean = slotStr.replace("–", "-").replace("—", "-").trim()
        val parts = clean.split("-").map { it.trim() }
        if (parts.size < 2) return null
        fun parseToMin(str: String): Int? {
            val t = str.lowercase().replace("h", ":").trim()
            val colonParts = t.split(":")
            val h = colonParts[0].filter { it.isDigit() }.toIntOrNull() ?: return null
            val m = colonParts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            return h * 60 + m
        }
        val s = parseToMin(parts[0]) ?: return null
        val e = parseToMin(parts[1]) ?: (s + 60)
        val safeEnd = if (e <= s) s + 60 else e
        val sH = s / 60
        val sM = s % 60
        val eH = safeEnd / 60
        val eM = safeEnd % 60
        val formatted = String.format(java.util.Locale.US, "%02d:%02d - %02d:%02d", sH, sM, eH, eM)
        return SlotInterval(formatted, s, safeEnd)
    }

    fun decomposeToUnitHours(slotStr: String): List<String> {
        val parsed = parseSlotInterval(slotStr) ?: return listOf(slotStr.trim())
        val duration = parsed.endMin - parsed.startMin
        if (duration > 70 && duration % 60 == 0) {
            val list = mutableListOf<String>()
            var cur = parsed.startMin
            while (cur < parsed.endMin) {
                val next = cur + 60
                val sH = cur / 60
                val sM = cur % 60
                val eH = next / 60
                val eM = next % 60
                list.add(String.format(java.util.Locale.US, "%02d:%02d - %02d:%02d", sH, sM, eH, eM))
                cur = next
            }
            return list
        }
        return listOf(parsed.originalString)
    }

    fun computeHarmonizedTimeSlots(
        allSlots: List<com.example.data.TimetableSlotEntity>,
        timeSlotReplacements: Map<String, String> = emptyMap(),
        deletedTimeSlots: Set<String> = emptySet(),
        customTimeSlots: Set<String> = emptySet()
    ): List<String> {
        // 1. Gather all actual slots from database and custom slots, applying replacements and exclusions
        val rawSlotStrings = (allSlots.map { it.timeSlot.trim() } + customTimeSlots.map { it.trim() })
            .filter { it.isNotBlank() && !deletedTimeSlots.contains(it) }

        val actualIntervals = rawSlotStrings
            .map { original ->
                var mapped = original
                while (timeSlotReplacements.containsKey(mapped)) {
                    mapped = timeSlotReplacements[mapped] ?: mapped
                }
                mapped
            }
            .filter { !deletedTimeSlots.contains(it) && !timeSlotReplacements.containsKey(it) }
            .flatMap { decomposeToUnitHours(it) }
            .distinct()
            .mapNotNull { parseSlotInterval(it) }

        val morningActual = actualIntervals.filter { it.startMin < 720 }
        val afternoonActual = actualIntervals.filter { it.startMin >= 720 }

        // 2. Morning rhythm
        val hasMorning30 = morningActual.any { it.startMin % 60 == 30 }
        val hasMorning00 = morningActual.any { it.startMin % 60 == 0 }
        val morningBase = if (hasMorning30 && !hasMorning00) {
            listOf("08:30 - 09:30", "09:30 - 10:30", "10:30 - 11:30", "11:30 - 12:30")
        } else {
            listOf("08:00 - 09:00", "09:00 - 10:00", "10:00 - 11:00", "11:00 - 12:00")
        }

        // 3. Afternoon rhythm: if any afternoon slot is at :30 (like 13:30 - 14:30), use the :30 chain!
        val hasAfternoon30 = afternoonActual.any { it.startMin % 60 == 30 }
        val hasAfternoon00 = afternoonActual.any { it.startMin % 60 == 0 }
        val afternoonBase = if (hasAfternoon30 && !hasAfternoon00) {
            listOf("13:30 - 14:30", "14:30 - 15:30", "15:30 - 16:30", "16:30 - 17:30")
        } else if (hasAfternoon30 && hasAfternoon00) {
            val has1330 = afternoonActual.any { it.startMin == 13 * 60 + 30 }
            if (has1330) {
                listOf("13:30 - 14:30", "14:30 - 15:30", "15:30 - 16:30", "16:30 - 17:30")
            } else {
                listOf("13:00 - 14:00", "14:00 - 15:00", "15:00 - 16:00", "16:00 - 17:00")
            }
        } else {
            listOf("13:00 - 14:00", "14:00 - 15:00", "15:00 - 16:00", "16:00 - 17:00")
        }

        val mappedMorningBase = morningBase.map { orig ->
            var m = orig
            while (timeSlotReplacements.containsKey(m)) {
                m = timeSlotReplacements[m] ?: m
            }
            m
        }.filter { !deletedTimeSlots.contains(it) && !timeSlotReplacements.containsKey(it) }

        val mappedAfternoonBase = afternoonBase.map { orig ->
            var m = orig
            while (timeSlotReplacements.containsKey(m)) {
                m = timeSlotReplacements[m] ?: m
            }
            m
        }.filter { !deletedTimeSlots.contains(it) && !timeSlotReplacements.containsKey(it) }

        fun overlaps(a: SlotInterval, b: SlotInterval): Boolean {
            return Math.max(a.startMin, b.startMin) < Math.min(a.endMin, b.endMin)
        }

        val resultIntervals = mutableListOf<SlotInterval>()
        resultIntervals.addAll(actualIntervals)

        val allBaseCandidates = (mappedMorningBase + mappedAfternoonBase).mapNotNull { parseSlotInterval(it) }
        for (candidate in allBaseCandidates) {
            val isExact = resultIntervals.any { it.startMin == candidate.startMin && it.endMin == candidate.endMin }
            if (isExact) continue
            val hasOverlapWithActual = actualIntervals.any { actual -> overlaps(candidate, actual) }
            if (!hasOverlapWithActual) {
                val hasOverlapWithResult = resultIntervals.any { res -> overlaps(candidate, res) }
                if (!hasOverlapWithResult) {
                    resultIntervals.add(candidate)
                }
            }
        }

        return resultIntervals
            .sortedBy { it.startMin }
            .map { it.originalString }
    }
}

