package com.mywifiadata.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * أشكال Material 3 Expressive: زوايا أكبر وأكثر نعومة، مع الدرجات الإضافية
 * (`largeIncreased` و `extraLargeIncreased` و `extraExtraLarge`) التي تُستخدم
 * في المكوّنات التعبيرية مثل مجموعات الأزرار والأشرطة.
 */
internal val AppShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    largeIncreased = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
    extraLargeIncreased = RoundedCornerShape(40.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

/** شكل كامل الاستدارة يُستخدم لحلقات الاستهلاك وشرائح القيم. */
internal val PillShape: CornerBasedShape = RoundedCornerShape(percent = 50)
