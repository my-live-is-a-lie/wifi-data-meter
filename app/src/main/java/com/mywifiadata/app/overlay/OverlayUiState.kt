package com.mywifiadata.app.overlay

import com.mywifiadata.app.data.model.AppSearchSelection
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.MonitorTarget
import com.mywifiadata.app.data.model.OverlayEdge
import com.mywifiadata.app.data.model.TrafficTotals

/** أشكال النافذة العائمة الأربعة. */
enum class OverlayMode { EXPANDED, PILL, EDGE, TOP_BAR }

/**
 * هل الانتقال من شكل إلى آخر «تكبير»؟ يحدّد مدة الحركة: التكبير أبطأ من الطيّ.
 * الانتقال إلى اللسان أو إلى الشريط العلوي تصغير دائمًا، والخروج منهما تكبير دائمًا.
 */
fun OverlayMode.expandsTo(target: OverlayMode): Boolean = when {
    target == OverlayMode.EDGE -> false
    this == OverlayMode.EDGE -> true
    target == OverlayMode.TOP_BAR -> false
    this == OverlayMode.TOP_BAR -> true
    target == OverlayMode.EXPANDED -> true
    else -> false
}

/** حالة ما تعرضه النافذة العائمة في لحظة معيّنة. */
data class OverlayUiState(
    val target: MonitorTarget = MonitorTarget.AllApps,
    val mode: OverlayMode = OverlayMode.EXPANDED,
    val edge: OverlayEdge = OverlayEdge.LEFT,
    val totals: TrafficTotals = TrafficTotals.Empty,
    val elapsedMillis: Long = 0L,
    val paused: Boolean = false,
    /** معدّل لحظي حقيقي (بايت/ثانية) محسوب من فرق قراءتين لإحصاءات الشبكة. */
    val rxSpeed: Double = 0.0,
    val txSpeed: Double = 0.0,
    /**
     * صلاحية «الوصول إلى الاستخدام» غير ممنوحة، فلا يمكن قراءة أي إحصاءات.
     * تُعرض عندها لافتة صريحة بدل أرقام صامتة تساوي صفرًا.
     */
    val usageAccessMissing: Boolean = false,
    /** القياس على الواي فاي؛ بلا اتصال لن تتغيّر الأرقام، فنقولها صراحة. */
    val wifiConnected: Boolean = true,
    /** أُخذ خط الأساس ولم تصل قراءة تالية بعد؛ لا نعرض صفرًا مضلّلًا. */
    val collecting: Boolean = false,
    /** فشلت قراءة إحصاءات النظام في هذه الدورة (تمييزًا عن الاستهلاك الصفري). */
    val readFailed: Boolean = false,
    /** تُستخدم لإبراز منطقة الإغلاق أثناء سحب النافذة المصغرة. */
    val dismissArmed: Boolean = false,
    /** الحافة التي ستلتصق بها الكبسولة إن أُفلتت الآن؛ تلميح بصري أثناء السحب. */
    val edgeArmed: OverlayEdge? = null,
    /**
     * النافذة فوق منطقة الشريط العلوي الآن: إن أُفلتت هناك تصغّر إلى شريط تحت
     * شريط الحالة. تلميح بصري (انضغاط وخفوت) أثناء السحب نحو الأعلى.
     */
    val topBarArmed: Boolean = false,
    val pickerVisible: Boolean = false,
    val pickerApps: List<AppUsage> = emptyList(),
    val pickerLoading: Boolean = false,
    val history: List<AppSearchSelection> = emptyList(),
)

/** كل تفاعلات النافذة العائمة مجمّعة حتى تبقى حمولة العرض واضحة. */
class OverlayActions(
    val expand: () -> Unit,
    /** النقر على اللسان الجانبي يعيد النافذة المصغرة لا البطاقة الكاملة. */
    val fromEdge: () -> Unit,
    val minimize: () -> Unit,
    val close: () -> Unit,
    val togglePause: () -> Unit,
    /** فتح إعدادات النظام لمنح صلاحية «الوصول إلى الاستخدام» من داخل النافذة. */
    val openUsageAccess: () -> Unit,
    val openPicker: () -> Unit,
    val closePicker: () -> Unit,
    val selectTarget: (MonitorTarget) -> Unit,
    val dragStart: () -> Unit,
    val drag: (Float, Float) -> Unit,
    val dragEnd: () -> Unit,
)
