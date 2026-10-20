package com.mywifiadata.app.core

import java.util.Locale

/**
 * تنسيق نص الساعة بصيغة 12 ساعة بلغة التطبيق، ويُستخدم في بطاقة «ساعة
 * الاستهلاك» داخل التطبيق وفي أداة الشاشة الرئيسية حتى يظهر الوقت بالشكل
 * نفسه في الموضعين: «٥:٠٤ م» بالعربية و«5:04 PM» بالإنجليزية.
 */
object ClockText {

    /** يحوّل رقمًا إلى أرقام عربية-هندية عند الحاجة (٠١٢٣٤٥٦٧٨٩). */
    fun localizedDigits(value: String, locale: Locale): String {
        if (locale.language != "ar") return value
        return buildString {
            value.forEach { char ->
                append(if (char in '0'..'9') '\u0660' + (char - '0') else char)
            }
        }
    }

    /**
     * [hourOfDay] بنظام 24 ساعة، و[minutes] دقائق الساعة. الساعة 0 و12 كلتاهما
     * تُعرضان 12، واللاحقة ص/م تُختار قبل الظهر وبعده.
     */
    fun format(
        hourOfDay: Int,
        minutes: Int,
        locale: Locale,
        amLabel: String,
        pmLabel: String,
    ): String {
        val hour12 = hourOfDay % 12
        val displayHour = if (hour12 == 0) 12 else hour12
        val minuteText = minutes.coerceIn(0, 59).toString().padStart(2, '0')
        val suffix = if (hourOfDay % 24 < 12) amLabel else pmLabel
        return "${localizedDigits("$displayHour:$minuteText", locale)} $suffix"
    }
}
