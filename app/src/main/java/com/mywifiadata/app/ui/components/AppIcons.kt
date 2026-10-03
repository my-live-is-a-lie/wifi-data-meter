package com.mywifiadata.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * أيقونة «حلقة» مرسومة خصيصًا لتطابق أيقونة التصميم المرجعي بجانب الإجمالي:
 * حلقة دائرية مفتوحة (بلا رؤوس أسهم كما في `Refresh`/`Autorenew`) مع فراغ
 * صغير في أعلى اليسار وأطراف دائرية.
 *
 * تُرسم بخط (stroke) لا بتعبئة، لذا يقبلها `Icon(tint = …)` مثل أي أيقونة أخرى.
 */
val RingRefreshIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "RingRefresh",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.4f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // حلقة مركزها (12, 12) ونصف قطرها 7.3، من الزاوية 247° إلى 203°
            // (قوس 316°) فيبقى فراغ صغير في أعلى اليسار.
            moveTo(9.15f, 5.28f)
            arcTo(
                horizontalEllipseRadius = 7.3f,
                verticalEllipseRadius = 7.3f,
                theta = 0f,
                isMoreThanHalf = true,
                isPositiveArc = true,
                x1 = 5.28f,
                y1 = 9.15f,
            )
        }
    }.build()
}
