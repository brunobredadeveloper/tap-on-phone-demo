package dev.breda.tap

import java.math.BigDecimal

object AmountInput {
    fun cents(raw: String): Long? {
        val normalized = raw.trim().replace(',', '.')
        if (!normalized.matches(Regex("[0-9]+(\\.[0-9]{1,2})?"))) return null
        return runCatching { BigDecimal(normalized).movePointRight(2).longValueExact() }
            .getOrNull()
            ?.takeIf { it > 0 }
    }
}
