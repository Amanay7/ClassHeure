package com.example.util

import com.example.data.StudentWithGrade

object GradeCalculator {

    /**
     * Parses numeric grade input supporting Western and Arabic-Indic numerals,
     * as well as standard comma (,), Arabic comma (،), and Arabic decimal separator (٫).
     */
    fun parseGradeInput(text: String?): Float? {
        if (text.isNullOrBlank()) return null
        val normalized = text.trim()
            .replace('٠', '0')
            .replace('١', '1')
            .replace('٢', '2')
            .replace('٣', '3')
            .replace('٤', '4')
            .replace('٥', '5')
            .replace('٦', '6')
            .replace('٧', '7')
            .replace('٨', '8')
            .replace('٩', '9')
            .replace(',', '.')
            .replace('،', '.')
            .replace('٫', '.')
        return normalized.toFloatOrNull()
    }

    /**
     * Note d'évaluation continue : addition des 4 sous-notes
     * (Participation + Discipline + Assiduité + Cahier).
     */
    fun computeEvaluationSum(
        participation: Float?,
        discipline: Float?,
        assiduity: Float?,
        notebook: Float?
    ): Float? {
        val hasAny = participation != null || discipline != null || assiduity != null || notebook != null
        if (!hasAny) return null
        return (participation ?: 0f) + (discipline ?: 0f) + (assiduity ?: 0f) + (notebook ?: 0f)
    }

    /**
     * Note de devoirs :
     * Si 1 devoir saisi -> note du devoir
     * Si 2 devoirs saisis -> (devoir1 + devoir2) / 2
     * Si 3 devoirs saisis -> (devoir1 + devoir2 + devoir3) / 3
     */
    fun computeDevoirsAverage(
        exam1: Float?,
        exam2: Float?,
        exam3: Float?
    ): Float? {
        val list = listOfNotNull(exam1, exam2, exam3)
        if (list.isEmpty()) return null
        return list.sum() / list.size.toFloat()
    }

    /**
     * Calcul de la moyenne trimestrielle pour le Lycée (sur 20) :
     * Moyenne = (note totale évaluation * 1 + note projet * 1 + note moyenne devoirs * 1 + note composition * coefficient) / total des coefficients
     */
    fun computeTrimestrialAverage(
        participation: Float?,
        discipline: Float?,
        assiduity: Float?,
        notebook: Float?,
        exam1: Float?,
        exam2: Float?,
        exam3: Float?,
        project: Float?,
        finalExam: Float?,
        subjectCoefficient: Number,
        @Suppress("UNUSED_PARAMETER") isPrimary: Boolean = false
    ): Float? {
        val evalSum = computeEvaluationSum(participation, discipline, assiduity, notebook)
        val devAvg = computeDevoirsAverage(exam1, exam2, exam3)

        var weightedSum = 0f
        var totalWeight = 0f

        if (evalSum != null) {
            weightedSum += evalSum * 1f
            totalWeight += 1f
        }

        if (project != null) {
            weightedSum += project * 1f
            totalWeight += 1f
        }

        if (devAvg != null) {
            weightedSum += devAvg * 1f
            totalWeight += 1f
        }

        val compoCoeff = subjectCoefficient.toFloat().coerceAtLeast(1f)
        if (finalExam != null) {
            weightedSum += finalExam * compoCoeff
            totalWeight += compoCoeff
        }

        return if (totalWeight > 0f) (weightedSum / totalWeight).coerceIn(0f, 20f) else null
    }

    fun computeStudentAverage(
        grade: StudentWithGrade,
        subjectCoefficient: Number,
        @Suppress("UNUSED_PARAMETER") isPrimary: Boolean = false
    ): Float? {
        return computeTrimestrialAverage(
            participation = grade.participation,
            discipline = grade.discipline,
            assiduity = grade.assiduity,
            notebook = grade.notebook,
            exam1 = grade.exam1,
            exam2 = grade.exam2,
            exam3 = grade.exam3,
            project = grade.project,
            finalExam = grade.finalExam,
            subjectCoefficient = subjectCoefficient
        )
    }

    fun getMaxGrade(@Suppress("UNUSED_PARAMETER") isPrimary: Boolean = false): Float = 20f

    fun getPassingThreshold(@Suppress("UNUSED_PARAMETER") isPrimary: Boolean = false): Float = 10f
}
