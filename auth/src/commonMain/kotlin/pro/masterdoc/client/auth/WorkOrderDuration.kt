package pro.masterdoc.client.auth

data class WeekClip(val startColumn: Int, val spanColumns: Int)

object WorkOrderDuration {
    /** Maximum supported duration: 30 working days at 8 hours per day. */
    const val MAX_DURATION_HOURS = 240

    fun spanDays(durationHours: Int): Int {
        val h = durationHours.coerceIn(1, MAX_DURATION_HOURS)
        return (h + 7) / 8
    }

    /** Occupied calendar dates (YYYY-MM-DD), working days only */
    fun occupiedDates(dueAt: String, durationHours: Int): List<String> {
        val start = IsoDates.parseToEpochDay(dueAt) ?: return emptyList()
        val need = spanDays(durationHours)
        val out = ArrayList<String>(need)
        var day = start
        while (out.size < need) {
            val dow = IsoDates.dayOfWeekIso(day) // 1..7
            if (dow in 1..5) out.add(IsoDates.formatEpochDay(day))
            day++
        }
        return out
    }

    /**
     * Clip to week Monday..Sunday.
     * @return startColumn 0..6, spanColumns >=1, or null if no intersection
     */
    fun clipToWeek(
        occupiedIsoDates: List<String>,
        weekMondayIso: String,
    ): WeekClip? {
        val monday = IsoDates.parseToEpochDay(weekMondayIso) ?: return null
        val weekDays = (0L..6L).map { IsoDates.formatEpochDay(monday + it) }
        val hitIndexes = weekDays.mapIndexedNotNull { i, d -> if (d in occupiedIsoDates.toSet()) i else null }
        if (hitIndexes.isEmpty()) return null
        val start = hitIndexes.first()
        val end = hitIndexes.last()
        return WeekClip(startColumn = start, spanColumns = end - start + 1)
    }
}
