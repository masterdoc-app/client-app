package pro.masterdoc.client.auth

object IsoDates {
    /** @return epochDay (days since 1970-01-01) or null if invalid YYYY-MM-DD */
    fun parseToEpochDay(iso: String): Long? {
        if (iso.length != 10 || iso[4] != '-' || iso[7] != '-') return null
        val y = iso.substring(0, 4).toIntOrNull() ?: return null
        val m = iso.substring(5, 7).toIntOrNull() ?: return null
        val d = iso.substring(8, 10).toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31) return null
        val epoch = daysFromCivil(y, m, d)
        val (cy, cm, cd) = civilFromDays(epoch)
        if (cy != y || cm != m || cd != d) return null
        return epoch
    }

    fun formatEpochDay(epochDay: Long): String {
        val (y, m, d) = civilFromDays(epochDay)
        return buildString(10) {
            append(y.toString().padStart(4, '0'))
            append('-')
            append(m.toString().padStart(2, '0'))
            append('-')
            append(d.toString().padStart(2, '0'))
        }
    }

    /** ISO-8601: 1=Monday … 7=Sunday */
    fun dayOfWeekIso(epochDay: Long): Int {
        val mod = ((epochDay + 3) % 7).toInt()
        return mod + 1
    }

    /** Howard Hinnant: civil date → days since 1970-01-01 */
    private fun daysFromCivil(y: Int, m: Int, d: Int): Long {
        var year = y
        year -= if (m <= 2) 1 else 0
        val era =
            if (year >= 0) {
                year / 400
            } else {
                (year - 399) / 400
            }
        val yoe = year - era * 400
        val doy = (153 * (m + if (m > 2) -3 else 9) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }

    /** Howard Hinnant: days since 1970-01-01 → (year, month, day) */
    private fun civilFromDays(z: Long): Triple<Int, Int, Int> {
        var days = z + 719468
        val era =
            if (days >= 0) {
                days / 146097
            } else {
                (days - 146096) / 146097
            }
        val doe = (days - era * 146097).toInt()
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        var y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + if (mp < 10) 3 else -9
        y += if (m <= 2) 1 else 0
        return Triple(y.toInt(), m, d)
    }
}
