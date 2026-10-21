package com.mywifiadata.app.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.OverlayMotion
import com.mywifiadata.app.data.model.MonitorTarget
import com.mywifiadata.app.data.model.OverlayEdge
import com.mywifiadata.app.ui.components.AppIcon
import com.mywifiadata.app.ui.components.ByteValue
import com.mywifiadata.app.ui.components.MonitorPickerContent
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor

/** بعد هذه المدة من القياس بلا أي بايت نُصرّح بأن النظام لم يسجّل حركة بعد. */
private const val NO_TRAFFIC_HINT_MILLIS = 20_000L

/**
 * محتوى النافذة العائمة: بطاقة كاملة، أو كبسولة صغيرة، أو لسان ملتصق بالحافة.
 * النافذة نفسها تتكفّل بالتحريك والإفلات؛ هذا الملف للعرض والتفاعلات فقط.
 */
@Composable
fun UsageOverlayContent(state: OverlayUiState, actions: OverlayActions) {
    // الشكل الجديد ينمو من الشكل القديم: الحجم يتدرّج، والمحتوى القديم يخفت
    // متقلّصًا بينما يظهر الجديد متوسّعًا — بدل تبديل مفاجئ بين ثلاث نوافذ.
    AnimatedContent(
        targetState = state.mode,
        transitionSpec = {
            val expanding = initialState.expandsTo(targetState)
            val duration = OverlayMotion.durationMillis(expanding).toInt()
            val enter = fadeIn(
                animationSpec = tween(durationMillis = duration / 2, delayMillis = duration / 5),
            ) + scaleIn(
                initialScale = if (expanding) 0.9f else 1.04f,
                // نابض خفيف يعطي «نبضة» صغيرة عند الوصول بدل توقّف جاف.
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
            )
            val exit = fadeOut(animationSpec = tween(durationMillis = duration / 2)) +
                scaleOut(
                    targetScale = if (expanding) 1.04f else 0.9f,
                    animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                )
            (enter togetherWith exit) using SizeTransform(
                clip = false,
                sizeAnimationSpec = { _, _ ->
                    tween(durationMillis = duration, easing = FastOutSlowInEasing)
                },
            )
        },
        contentAlignment = Alignment.Center,
        label = "overlay-mode",
    ) { mode ->
        when (mode) {
            OverlayMode.EDGE -> OverlayEdgeTab(state = state, actions = actions)
            OverlayMode.PILL -> OverlayPill(state = state, actions = actions)
            OverlayMode.TOP_BAR -> OverlayTopBar(state = state, actions = actions)
            OverlayMode.EXPANDED -> OverlayExpanded(state = state, actions = actions)
        }
    }
}

@Composable
private fun OverlayPill(state: OverlayUiState, actions: OverlayActions) {
    val pillAlpha by animateFloatAsState(
        targetValue = if (state.dismissArmed) 0.62f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "overlay-pill-alpha",
    )
    val pillScale by animateFloatAsState(
        targetValue = if (state.dismissArmed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "overlay-pill-scale",
    )
    // تلميح «الالتصاق بالحافة»: تنضغط الكبسولة قليلًا حين يصبح الإفلات لسانًا.
    val edgeSnap by animateFloatAsState(
        targetValue = if (state.edgeArmed != null) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "overlay-pill-edge",
    )
    // تلميح منطقة الشريط العلوي أثناء السحب نحو أعلى الشاشة.
    val topHintScale by animateFloatAsState(
        targetValue = if (state.topBarArmed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "overlay-pill-top-scale",
    )
    val topHintAlpha by animateFloatAsState(
        targetValue = if (state.topBarArmed) 0.78f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "overlay-pill-top-alpha",
    )
    Surface(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = pillAlpha * topHintAlpha
                scaleX = pillScale * edgeSnap * topHintScale
                scaleY = pillScale * edgeSnap * topHintScale
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { actions.expand() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { actions.dragStart() },
                    onDragEnd = { actions.dragEnd() },
                    onDragCancel = { actions.dragEnd() },
                ) { change, dragAmount ->
                    change.consume()
                    actions.drag(dragAmount.x, dragAmount.y)
                }
            },
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            if (state.topBarArmed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            TargetIcon(target = state.target, size = 30.dp)
            Text(
                text = if (state.usageAccessMissing || state.collecting) {
                    "—"
                } else {
                    Format.bytes(state.totals.totalBytes)
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                softWrap = false,
            )
            if (state.paused) {
                Icon(
                    imageVector = Icons.Rounded.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OverlayEdgeTab(state: OverlayUiState, actions: OverlayActions) {
    // زوايا مطلقة (لا Start/End) كي يبقى الاستدارة في الجهة الداخلية مهما كان اتجاه الواجهة.
    val shape = if (state.edge == OverlayEdge.LEFT) {
        AbsoluteRoundedCornerShape(topLeft = 0.dp, topRight = 18.dp, bottomRight = 18.dp, bottomLeft = 0.dp)
    } else {
        AbsoluteRoundedCornerShape(topLeft = 18.dp, topRight = 0.dp, bottomRight = 0.dp, bottomLeft = 18.dp)
    }
    val (amount, unit) = if (state.usageAccessMissing || state.collecting) {
        "—" to ""
    } else {
        Format.bytesParts(state.totals.totalBytes)
    }
    Surface(
        modifier = Modifier
            .widthIn(min = 46.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { actions.fromEdge() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { actions.dragStart() },
                    onDragEnd = { actions.dragEnd() },
                    onDragCancel = { actions.dragEnd() },
                ) { change, dragAmount ->
                    change.consume()
                    actions.drag(dragAmount.x, dragAmount.y)
                }
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = amount,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/**
 * الشريط العلوي: الشكل الذي تتقلّص إليه النافذة عند إفلاتها أعلى الشاشة، فيلتصق
 * تحت شريط الحالة مباشرة بعرض الشاشة كله كما في التصميم المرجعي.
 *
 * الشكل مسطّح عمدًا: بلا استدارة ولا إطار ولا ظل ولا خلفيات دائرية حول الأزرار،
 * وخلفيته سوداء شفّافة تمتد من حرف إلى حرف فيندمج مع شريط الحالة بدل أن يبدو
 * بطاقة عائمة أخرى.
 *
 * - الضغط مرة واحدة على الشريط يعيد النافذة المنبثقة، وكذلك سحبه لأسفل مسافة قصيرة
 *   (والإفلات قبلها يرتدّ بالشريط إلى مكانه بانميشن لا بقفزة).
 * - الضغط على أيقونة الهدف يفتح قائمة اختيار التطبيقات منزلقَة تحت الشريط نفسه،
 *   فيبقى الشريط ظاهرًا والقيم تتحدّث أثناء الاختيار.
 * - زرّا الإيقاف المؤقت والإغلاق باقيان في طرف الشريط كما في البطاقة الكاملة.
 */
@Composable
private fun OverlayTopBar(state: OverlayUiState, actions: OverlayActions) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { actions.expand() })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { actions.dragStart() },
                        onDragEnd = { actions.dragEnd() },
                        onDragCancel = { actions.dragEnd() },
                    ) { change, dragAmount ->
                        change.consume()
                        actions.drag(dragAmount.x, dragAmount.y)
                    }
                },
            // بلا استدارة ولا ظل: شريط مسطّح ملتصق بحرفي الشاشة كامتداد لشريط الحالة.
            // (استدارة صفر = مستطيل؛ فالاسم RectangleShape أُزيل من كومبوز الحديث.)
            shape = RoundedCornerShape(0.dp),
            color = OverlayTopBarScrim,
            contentColor = Color.White,
        ) {
            // العرض المتاح يتفاوت (شاشة ضيقة أو وضع أفقي): على الشاشات الضيقة يُخفى
            // المعدّل ثم الوقت كي لا تُقتطع أزرار الإيقاف والإغلاق من طرف الشريط.
            BoxWithConstraints {
                // كل عناصر المرجع ظاهرة دائمًا (الأيقونة، الوقت، تنزيل، رفع،
                // الإجمالي، المعدّل، إيقاف، إغلاق): على الشاشات الضيقة تُضغط
                // الوحدات (‎856K بدل ‎856.4 KB) وتصغر الخطوط والمسافات بدل أن
                // يُحذف عنصر، والمعدّل يُختصر بنقاط داخل عرضه المتاح فقط.
                val compact = maxWidth < 420.dp
                val reserved = 22.dp + 30.dp + 80.dp + 56.dp +
                    (if (compact) 56.dp else 72.dp) +
                    (if (compact) 42.dp else 70.dp) +
                    (if (compact) 12.dp else 20.dp)
                val speedMax = (maxWidth - reserved).coerceIn(28.dp, 150.dp)
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (compact) 6.dp else 10.dp,
                        vertical = 6.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = {
                                if (state.pickerVisible) actions.closePicker() else actions.openPicker()
                            })
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TargetIcon(target = state.target, size = 22.dp, tint = Color.White)
                    }
                    Text(
                        text = Format.elapsed(state.elapsedMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                    BarStat(
                        icon = Icons.Rounded.ArrowDownward,
                        tint = LocalDownloadColor.current,
                        value = Format.bytesShort(state.totals.rxBytes),
                    )
                    BarStat(
                        icon = Icons.Rounded.ArrowUpward,
                        tint = LocalUploadColor.current,
                        value = Format.bytesShort(state.totals.txBytes),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (state.usageAccessMissing || state.collecting) {
                            "—"
                        } else {
                            Format.bytes(state.totals.totalBytes)
                        },
                        style = if (compact) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = when {
                            state.usageAccessMissing -> stringResource(R.string.overlay_no_usage_access_short)
                            state.readFailed -> stringResource(R.string.overlay_read_failed_short)
                            !state.wifiConnected -> stringResource(R.string.overlay_no_wifi_short)
                            state.collecting -> stringResource(R.string.overlay_collecting)
                            state.totals.totalBytes == 0L && state.elapsedMillis >= NO_TRAFFIC_HINT_MILLIS ->
                                stringResource(R.string.overlay_no_traffic_yet)
                            else -> Format.speed(state.rxSpeed + state.txSpeed)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = speedMax),
                    )
                    Icon(
                        imageVector = if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = stringResource(
                            if (state.paused) R.string.overlay_resume else R.string.overlay_pause,
                        ),
                        tint = Color.White,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = actions.togglePause)
                            .padding(5.dp)
                            .size(18.dp),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.overlay_close),
                        tint = Color.White,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = actions.close)
                            .padding(5.dp)
                            .size(18.dp),
                    )
                }
            }
        }
        // قائمة اختيار التطبيقات تنزلق تحت الشريط وتطوى إليه عند الإغلاق.
        AnimatedVisibility(
            visible = state.pickerVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            // لوحة الاختيار تبقى بسطح المادة المعتاد (لا الخلفية السوداء) كي تبقى
            // نصوصها مقروءة في السمة الفاتحة والداكنة معًا.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AbsoluteRoundedCornerShape(
                    topLeft = 0.dp,
                    topRight = 0.dp,
                    bottomLeft = 16.dp,
                    bottomRight = 16.dp,
                ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 8.dp,
            ) {
                MonitorPickerContent(
                    apps = state.pickerApps,
                    isLoading = state.pickerLoading,
                    selected = state.target,
                    history = state.history,
                    onSelect = actions.selectTarget,
                    onDismiss = actions.closePicker,
                    contentPadding = 12.dp,
                    listMaxHeight = 260.dp,
                )
            }
        }
    }
}

/** خلفية الشريط: سوداء شبه معتمة فيندمج مع شريط الحالة بدل أن يبدو بطاقة عائمة. */
private val OverlayTopBarScrim = Color.Black.copy(alpha = 0.85f)

/** قيمة صغيرة بسهم ملوّن في الشريط العلوي (تنزيل/رفع). */
@Composable
private fun BarStat(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun OverlayExpanded(state: OverlayUiState, actions: OverlayActions) {
    // تلميح منطقة الشريط العلوي: تنضغط البطاقة وتخفت قليلًا وهي فوق المنطقة،
    // فيفهم المستخدم قبل الإفلات أن النافذة ستُصغّر إلى شريط تحت شريط الحالة.
    val topHintScale by animateFloatAsState(
        targetValue = if (state.topBarArmed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "overlay-expanded-top-scale",
    )
    val topHintAlpha by animateFloatAsState(
        targetValue = if (state.topBarArmed) 0.78f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "overlay-expanded-top-alpha",
    )
    Surface(
        modifier = Modifier
            .width(312.dp)
            .graphicsLayer {
                scaleX = topHintScale
                scaleY = topHintScale
                alpha = topHintAlpha
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { actions.dragStart() },
                    onDragEnd = { actions.dragEnd() },
                    onDragCancel = { actions.dragEnd() },
                ) { change, dragAmount ->
                    change.consume()
                    actions.drag(dragAmount.x, dragAmount.y)
                }
            },
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            if (state.topBarArmed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        shadowElevation = 14.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            OverlayHeader(state = state, actions = actions)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.pickerVisible) {
                MonitorPickerContent(
                    apps = state.pickerApps,
                    isLoading = state.pickerLoading,
                    selected = state.target,
                    history = state.history,
                    onSelect = actions.selectTarget,
                    onDismiss = actions.closePicker,
                    contentPadding = 6.dp,
                    listMaxHeight = 230.dp,
                )
            } else {
                if (state.usageAccessMissing) {
                    OverlayNotice(
                        text = stringResource(R.string.overlay_usage_access_missing),
                        actionLabel = stringResource(R.string.action_open_usage_access),
                        onAction = actions.openUsageAccess,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (state.readFailed) {
                    OverlayNotice(text = stringResource(R.string.overlay_read_failed))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OverlayMetrics(state = state)
            }
        }
    }
}

@Composable
private fun OverlayHeader(state: OverlayUiState, actions: OverlayActions) {
    val targetLabel = if (state.target.isAllApps) {
        stringResource(R.string.monitor_all_apps)
    } else {
        state.target.label.ifBlank { stringResource(R.string.value_unknown) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = actions.openPicker),
            contentAlignment = Alignment.Center,
        ) {
            TargetIcon(target = state.target, size = 22.dp)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = actions.openPicker)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = targetLabel,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.overlay_change_target),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = actions.togglePause, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = stringResource(
                    if (state.paused) R.string.overlay_resume else R.string.overlay_pause,
                ),
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = actions.minimize, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = stringResource(R.string.overlay_minimize),
                modifier = Modifier.size(20.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = actions.close),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.overlay_close),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun OverlayMetrics(state: OverlayUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OverlayMetric(
            label = stringResource(R.string.overlay_time),
            value = Format.elapsed(state.elapsedMillis),
        )
        OverlayMetric(
            label = stringResource(R.string.label_download),
            value = Format.bytes(state.totals.rxBytes),
            accent = LocalDownloadColor.current,
            icon = Icons.Rounded.ArrowDownward,
        )
        OverlayMetric(
            label = stringResource(R.string.label_upload),
            value = Format.bytes(state.totals.txBytes),
            accent = LocalUploadColor.current,
            icon = Icons.Rounded.ArrowUpward,
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            if (state.usageAccessMissing || state.collecting) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ByteValue(
                    bytes = state.totals.totalBytes,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            // معدّل حقيقي من فرق قراءتين؛ يبقى صفرًا حين لا يسجّل النظام أي حركة.
            Text(
                text = when {
                    state.usageAccessMissing -> stringResource(R.string.overlay_no_usage_access_short)
                    state.readFailed -> stringResource(R.string.overlay_read_failed_short)
                    !state.wifiConnected -> stringResource(R.string.overlay_no_wifi_short)
                    state.collecting -> stringResource(R.string.overlay_collecting)
                    // صفر حقيقي: النظام لم يسجّل حركة لهذا الهدف بعد — نقولها بدل معدّل 0.
                    state.totals.totalBytes == 0L && state.elapsedMillis >= NO_TRAFFIC_HINT_MILLIS ->
                        stringResource(R.string.overlay_no_traffic_yet)
                    else -> Format.speed(state.rxSpeed + state.txSpeed)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/**
 * لافتة صريحة داخل البطاقة: صلاحية «الوصول إلى الاستخدام» غير ممنوحة، أو تعذّرت
 * قراءة إحصاءات النظام. بلا هذه الصلاحية/القراءة لا يمكن قياس أي استهلاك، فالأرقام
 * التي تبقى صفرًا ليست قياسًا.
 */
@Composable
private fun OverlayNotice(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun OverlayMetric(
    label: String,
    value: String,
    accent: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun TargetIcon(
    target: MonitorTarget,
    size: Dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (target.isAllApps) {
        Icon(
            imageVector = Icons.Rounded.Apps,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size),
        )
    } else {
        AppIcon(app = target.asUsage(), size = size)
    }
}
