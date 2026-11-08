package com.surwi.compass360.domain.numerology

import java.time.LocalDate
import java.util.Locale

/**
 * Transparent numerology arithmetic. Interpretations are cultural/traditional,
 * not scientifically validated predictions.
 *
 * Default reduction keeps the commonly used 1..9 root. Alternative master-number
 * handling is explicit so the app never silently changes a calculation convention.
 */
data class NumerologyResult(
    val mulank: Int,
    val bhagyank: Int,
    val nameNumber: Int?,
    val method: String
)

enum class ReductionRule { ROOT_1_TO_9, PRESERVE_11_22_33 }

object NumerologyCalculator {
    private val chaldeanValues = mapOf(
        'A' to 1, 'I' to 1, 'J' to 1, 'Q' to 1, 'Y' to 1,
        'B' to 2, 'K' to 2, 'R' to 2,
        'C' to 3, 'G' to 3, 'L' to 3, 'S' to 3,
        'D' to 4, 'M' to 4, 'T' to 4,
        'E' to 5, 'H' to 5, 'N' to 5, 'X' to 5,
        'U' to 6, 'V' to 6, 'W' to 6,
        'O' to 7, 'Z' to 7,
        'F' to 8, 'P' to 8
    )

    fun calculate(
        dateOfBirth: LocalDate,
        fullName: String? = null,
        rule: ReductionRule = ReductionRule.ROOT_1_TO_9
    ): NumerologyResult {
        require(!dateOfBirth.isAfter(LocalDate.now())) { "Date of birth cannot be in the future." }
        val mulank = reduce(dateOfBirth.dayOfMonth, rule)
        val dateDigitSum = dateOfBirth.toString().filter(Char::isDigit).sumOf { it.digitToInt() }
        val bhagyank = reduce(dateDigitSum, rule)
        val normalizedName = fullName?.trim()?.uppercase(Locale.ROOT)?.filter { it in 'A'..'Z' }
        val nameNumber = if (normalizedName.isNullOrEmpty()) null
            else reduce(normalizedName.sumOf { chaldeanValues[it] ?: 0 }, rule)
        return NumerologyResult(
            mulank = mulank,
            bhagyank = bhagyank,
            nameNumber = nameNumber,
            method = when (rule) {
                ReductionRule.ROOT_1_TO_9 -> "All values reduced to 1–9."
                ReductionRule.PRESERVE_11_22_33 -> "11, 22 and 33 are preserved; other values reduce to 1–9."
            }
        )
    }

    fun reduce(value: Int, rule: ReductionRule = ReductionRule.ROOT_1_TO_9): Int {
        require(value >= 0) { "Value must be zero or positive." }
        var current = value
        while (current > 9 && !(rule == ReductionRule.PRESERVE_11_22_33 && current in setOf(11, 22, 33))) {
            current = current.toString().sumOf { it.digitToInt() }
        }
        return current
    }
}
