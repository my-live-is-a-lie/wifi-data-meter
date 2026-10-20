package com.mywifiadata.app.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.mywifiadata.app.data.model.MonitorTarget
import com.mywifiadata.app.data.model.OverlayEdge
import com.mywifiadata.app.ui.components.AppIcon
import com.mywifiadata.app.ui.components.ByteValue
import com.mywifiadata.app.ui.components.MonitorPickerContent
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor

/**
 * محتوى النافذة العائمة: بطاقة كاملة، أو كبسولة صغيرة، أو لسان ملتصق بالحافة.
 * النافذة نفسها تتكفّل بالتحريك والإفلات؛ هذا الملف للعرض والتفاعلات فقط.
 */
@Composable
fun UsageOverlayContent(state: OverlayUiState, actions: OverlayActions) {
    when (state.mode) {
        OverlayMode.EDGE -> OverlayEdgeTab(state = state, actions = actions)
        OverlayMode.PILL -> OverlayPill(state = state, actions = actions)
        OverlayMode.EXPANDED -> OverlayExpanded(state = state, actions = actions)
    }
}

@Composable
private fun OverlayPill(state: OverlayUiState, actions: OverlayActions) {
    val pillAlpha by animateFloatAsState(
        targetValue = if (state.dismissArmed) 0.62f else 1f,
        label = "overlay-pill-alpha",
    )
    val pillScale by animateFloatAsState(
        targetValue = if (state.dismissArmed) 0.86f else 1f,
        label = "overlay-pill-scale",
    )
    Surface(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = pillAlpha
                scaleX = pillScale
                scaleY = pillScale
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            TargetIcon(target = state.target, size = 30.dp)
            Text(
                text = Format.bytes(state.totals.totalBytes),
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
    val (amount, unit) = Format.bytesParts(state.totals.totalBytes)
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

@Composable
private fun OverlayExpanded(state: OverlayUiState, actions: OverlayActions) {
    Surface(
        modifier = Modifier
            .width(312.dp)
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
        ByteValue(
            bytes = state.totals.totalBytes,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
private fun TargetIcon(target: MonitorTarget, size: Dp) {
    if (target.isAllApps) {
        Icon(
            imageVector = Icons.Rounded.Apps,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size),
        )
    } else {
        AppIcon(app = target.asUsage(), size = size)
    }
}
