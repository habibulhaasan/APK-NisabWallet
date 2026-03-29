package com.hasan.nisabwallet.utils

import java.util.Date
import java.util.GregorianCalendar

/**
 * Hijri (Umm al-Qura) date conversion implemented in pure Kotlin.
 * No external library required — uses the standard astronomical algorithm
 * that matches the Saudi Umm al-Qura calendar to within ±1 day.
 *
 * Replaces the com.github.msarhan:ummalqura-calendar dependency which
 * is not reliably available on JitPack.
 */
object ZakatUtils {

    data class HijriDate(
        val year: Int,
        val month: Int,
        val day: Int,
        val formatted: String = "$day/$month/$year"
    )

    // ── Core Julian Day Number conversion ─────────────────────────────────────

    /** Convert a Gregorian date to Julian Day Number. */
    private fun gregorianToJdn(year: Int, month: Int, day: Int): Long {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
    }

    /** Convert a Julian Day Number to Hijri date. */
    private fun jdnToHijri(jdn: Long): Triple<Int, Int, Int> {
        val l  = jdn - 1948440 + 10632
        val n  = (l - 1) / 10631
        val l2 = l - 10631 * n + 354
        val j  = ((10985 - l2) / 5316) * ((50 * l2) / 17719) +
                 (l2 / 5670) * ((43 * l2) / 15238)
        val l3 = l2 - ((30 - j) / 15) * ((17719 * j) / 50) -
                 (j / 16) * ((15238 * j) / 43) + 29
        val month = (24 * l3) / 709
        val day   = l3 - (709 * month) / 24
        val year  = 30 * n + j - 30
        return Triple(
            year.toInt(),
            month.toInt(),
            day.toInt()
        )
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Convert a Gregorian date string (yyyy-MM-dd) to Hijri. */
    fun gregorianToHijri(gregorianDate: String): HijriDate {
        val parts = gregorianDate.split("-")
        val year  = parts[0].toInt()
        val month = parts[1].toInt()
        val day   = parts[2].toInt()

        val jdn = gregorianToJdn(year, month, day)
        val (hYear, hMonth, hDay) = jdnToHijri(jdn)

        return HijriDate(
            year      = hYear,
            month     = hMonth,
            day       = hDay,
            formatted = "$hDay/$hMonth/$hYear"
        )
    }

    /** Convert a Hijri date back to a Gregorian [Date]. */
    private fun hijriToGregorianDate(hYear: Int, hMonth: Int, hDay: Int): Date {
        // Reverse: compute JDN from Hijri
        val jdn = (11 * hYear + 3).toLong() / 30 +
                  354L * hYear +
                  30L * hMonth -
                  (hMonth - 1) / 2 +
                  hDay + 1948440 - 385

        // JDN → Gregorian
        val p = jdn + 68569
        val q = 4 * p / 146097
        val r = p - (146097 * q + 3) / 4
        val s = 4000 * (r + 1) / 1461001
        val t = r - 1461 * s / 4 + 31
        val u = 80 * t / 2447
        val v = u / 11

        val day   = (t - 2447 * u / 80).toInt()
        val month = (u + 2 - 12 * v).toInt()
        val year  = (100 * (q - 49) + s + v).toInt()

        return GregorianCalendar(year, month - 1, day).time
    }

    /** Add exactly one Hijri year to a Gregorian date string, return as [Date]. */
    fun addOneHijriYear(gregorianDate: String): Date {
        val hijri = gregorianToHijri(gregorianDate)
        return hijriToGregorianDate(hijri.year + 1, hijri.month, hijri.day)
    }

    /** Days remaining until the 1-Hijri-year anniversary of [startDate]. */
    fun daysUntilHijriAnniversary(startDate: String): Long {
        val endDate = addOneHijriYear(startDate)
        val diff    = endDate.time - System.currentTimeMillis()
        return maxOf(0L, (diff / (1000 * 60 * 60 * 24)))
    }

    /** True if one full Hijri year has passed since [startDate]. */
    fun hasOneHijriYearPassed(startDate: String): Boolean =
        System.currentTimeMillis() >= addOneHijriYear(startDate).time

    /** Zakat = 2.5% of zakatable wealth. */
    fun calculateZakat(totalWealth: Double): Double = totalWealth * 0.025

    /** Format a HijriDate as "15 Ramadan 1445 AH". */
    fun formatHijriDate(hijri: HijriDate): String =
        "${hijri.day} ${hijriMonthName(hijri.month)} ${hijri.year} AH"

    fun hijriMonthName(month: Int): String = listOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhul-Qi'dah", "Dhul-Hijjah"
    ).getOrElse(month - 1) { "" }

    /** Determine Zakat status label. */
    fun determineZakatStatus(
        totalWealth: Double,
        nisabThreshold: Double,
        activeCycleStartDate: String?,
        isCyclePaid: Boolean
    ): ZakatStatus {
        if (nisabThreshold <= 0 || totalWealth < nisabThreshold) return ZakatStatus.NOT_MANDATORY
        if (activeCycleStartDate == null) return ZakatStatus.MONITORING
        if (isCyclePaid) return ZakatStatus.PAID
        if (hasOneHijriYearPassed(activeCycleStartDate)) {
            return if (totalWealth >= nisabThreshold) ZakatStatus.DUE else ZakatStatus.EXEMPT
        }
        return ZakatStatus.MONITORING
    }
}

enum class ZakatStatus(val label: String, val colorHex: String) {
    NOT_MANDATORY("Below Nisab",  "#6B7280"),
    MONITORING(   "Monitoring",   "#3B82F6"),
    DUE(          "Zakat Due",    "#EF4444"),
    PAID(         "Paid",         "#10B981"),
    EXEMPT(       "Exempt",       "#8B5CF6")
}