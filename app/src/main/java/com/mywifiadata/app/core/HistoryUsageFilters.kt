package com.mywifiadata.app.core

/** التبويب الفرعي المختار في شاشة السجل (الشبكة أو الباقة). */
enum class HistorySection { NETWORK, PLAN }

/** شهر وسنة في سجل الاستهلاك (الشهر من 1 إلى 12). */
data class HistoryYearMonth(
    val year: Int,
    val month: Int,
) : Comparable<HistoryYearMonth> {
    override fun compareTo(other: HistoryYearMonth): Int {
        val yearDiff = year.compareTo(other.year)
        return if (yearDiff != 0) yearDiff else month.compareTo(other.month)
    }
}

/**
 * السنوات التي سُجّل فيها استهلاك بيانات فعلي فقط، مرتبة من الأحدث إلى الأقدم.
 */
fun availableHistoryYears(recordedMonths: List<HistoryYearMonth>): List<Int> =
    recordedMonths
        .map { it.year }
        .distinct()
        .sortedDescending()

/**
 * الأشهر التي سُجّل فيها استهلاك بيانات فعلي ضمن [year] المحددة فقط، مرتبة تصاعديًا (1..12).
 */
fun availableHistoryMonths(recordedMonths: List<HistoryYearMonth>, year: Int): List<Int> =
    recordedMonths
        .asSequence()
        .filter { it.year == year && it.month in 1..12 }
        .map { it.month }
        .distinct()
        .sorted()
        .toList()

/**
 * عند تغيير السنة في الفلتر: إن كان الشهر المختار سابقًا متاحًا في السنة الجديدة
 * يُحتفظ به، وإلا يُختار أحدث شهر سُجّل فيه استهلاك في تلك السنة.
 */
fun resolveHistoryMonthForYear(
    recordedMonths: List<HistoryYearMonth>,
    targetYear: Int,
    currentSelectedMonth: Int,
): Int {
    val months = availableHistoryMonths(recordedMonths, targetYear)
    if (months.isEmpty()) return currentSelectedMonth
    return if (currentSelectedMonth in months) currentSelectedMonth else months.max()
}

/**
 * هل التاريخ المختار في السجل يختلف عن الشهر والسنة الحاليين؟
 * يتحكم في ظهور زر إعادة الضبط بجانب اسم الشبكة.
 */
fun isCustomHistoryMonth(
    selectedYear: Int,
    selectedMonth: Int,
    currentYearMonth: HistoryYearMonth,
): Boolean = selectedYear != currentYearMonth.year || selectedMonth != currentYearMonth.month
