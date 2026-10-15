package com.mywifiadata.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.AppsSortOrder

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
    val sortDescription = stringResource(
        if (sortOrder == AppsSortOrder.HIGHEST_USAGE) R.string.apps_sort_highest_usage
        else R.string.apps_sort_lowest_usage,
    )
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().animateContentSize().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (!expanded) {
                    Text(
                        text = stringResource(R.string.apps_list_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // The trailing edge is beside the buttons: expand rightward in Arabic.
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandHorizontally(expandFrom = Alignment.End),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.End),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        label = { Text(stringResource(R.string.apps_search_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.apps_search_clear))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                        }),
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                }
            }
            IconButton(onClick = {
                if (expanded) {
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
                onExpandedChange(!expanded)
            }) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = stringResource(if (expanded) R.string.apps_search_close else R.string.apps_search_open),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            // No extra horizontal padding: move the sort action closer to the left edge in RTL.
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
