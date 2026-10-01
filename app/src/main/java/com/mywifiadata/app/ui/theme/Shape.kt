package com.mywifiadata.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * أشكال Material 3 Expressive بحواف «شبه مربّعة»: دوران خفيف فقط بدل
 * الاستدارة الدائرية الكبيرة، حتى تبقى البطاقات والبلاطات أقرب للمستطيل
 * كما طلب المستخدم (مثلاً البلاطات كان نصف قطرها 40dp فأصبح 12dp).
 */
internal val AppShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    largeIncreased = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
    extraLargeIncreased = RoundedCornerShape(12.dp),
    extraExtraLarge = RoundedCornerShape(16.dp),
)

/** حواف الأزرار: مستطيل خفيف الدوران بدل الاستدارة الكاملة (الشكل الدائري). */
internal val AppButtonShape: Shape = RoundedCornerShape(10.dp)

/** شكل كامل الاستدارة يُستخدم لحلقات الاستهلاك وشرائح القيم. */
internal val PillShape: CornerBasedShape = RoundedCornerShape(percent = 50)
