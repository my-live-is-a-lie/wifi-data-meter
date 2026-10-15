package com.mywifiadata.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.AppsSearchAction
import com.mywifiadata.app.core.AppsSortOrder
import com.mywifiadata.app.core.appsSearchAction

@Composable
fun AppsSearchControls(
    expanded: Boolean,
    query: String,
    sortOrder: AppsSortOrder,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onReverseSortOrder: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val transition = updateTransition(targetState = expanded, label = "App search")
    val expansion by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 260, easing = FastOutSlowInEasing) },
        label = "Search width",
    ) { if (it) 1f else 0f }
    // Keep the editor through the entire closing animation; don't flash the hint underneath it.
    val fieldVisible = transition.currentState || transition.targetState
    val searchLabel = stringResource(R.string.apps_search_label)
    val action = appsSearchAction(expanded, query)
    val actionDescription = stringResource(
        when (action) {
            AppsSearchAction.OPEN -> R.string.apps_search_open
            AppsSearchAction.CLEAR_QUERY -> R.string.apps_search_clear
            AppsSearchAction.CLOSE -> R.string.apps_search_close
        },
    )
    val sortDescription = stringResource(
        if (sortOrder == AppsSortOrder.HIGHEST_USAGE) R.string.apps_sort_highest_usage
        else R.string.apps_sort_lowest_usage,
    )

    LaunchedEffect(expanded) {
        if (expanded) {
            focusRequester.requestFocus()
            keyboard?.show()
        } else {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                if (!fieldVisible) {
                    Text(
                        text = stringResource(R.string.apps_list_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterStart).padding(end = 52.dp),
                    )
                }
                // One pill, anchored beside the sort button. In RTL its right edge slides
                // toward the left-hand search icon when closing; the button itself never jumps.
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(48.dp + (maxWidth - 48.dp).coerceAtLeast(0.dp) * expansion)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = expansion)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (fieldVisible) {
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clipToBounds()
                                    .padding(start = 12.dp)
                                    .focusRequester(focusRequester)
                                    .semantics { contentDescription = searchLabel },
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (query.isEmpty()) {
                                            Text(
                                                text = searchLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    keyboard?.hide()
                                    focusManager.clearFocus()
                                }),
                            )
                        }
                        IconButton(
                            modifier = Modifier.size(48.dp),
                            onClick = {
                                when (action) {
                                    AppsSearchAction.OPEN -> onExpandedChange(true)
                                    AppsSearchAction.CLEAR_QUERY -> onQueryChange("")
                                    AppsSearchAction.CLOSE -> {
                                        keyboard?.hide()
                                        focusManager.clearFocus()
                                        onExpandedChange(false)
                                    }
                                }
                            },
                        ) {
                            androidx.compose.animation.AnimatedContent(
                                targetState = expanded,
                                transitionSpec = {
                                    (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.7f)) togetherWith
                                        (fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.7f))
                                },
                                label = "Search action icon",
                            ) { isOpen ->
                                Icon(
                                    imageVector = if (isOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                                    contentDescription = actionDescription,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
            IconButton(
                onClick = onReverseSortOrder,
                modifier = Modifier.semantics { stateDescription = sortDescription },
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = stringResource(R.string.apps_reverse_sort),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
