package com.mywifiadata.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.AppsSortOrder
import com.mywifiadata.app.core.AppsUsageScope
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.core.appsCountOptions

/** Dropdown anchors remain on screen while the app list is scrolled. */
@Composable
fun AppsUsageFilterBar(
    network: UsageNetwork,
    scope: AppsUsageScope,
    countLimit: Int?,
    sortOrder: AppsSortOrder,
    onNetworkChange: (UsageNetwork) -> Unit,
    onScopeChange: (AppsUsageScope) -> Unit,
    onCountLimitChange: (Int?) -> Unit,
    onSortOrderChange: (AppsSortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterMenu(
                title = stringResource(R.string.apps_network_filter),
                selected = network,
                options = UsageNetwork.entries.map {
                    it to stringResource(if (it == UsageNetwork.WIFI) R.string.apps_network_wifi else R.string.apps_network_mobile)
                },
                onSelect = onNetworkChange,
            )
            if (network == UsageNetwork.WIFI) {
                FilterMenu(
                    title = stringResource(R.string.label_period),
                    selected = scope,
                    options = AppsUsageScope.entries.map {
                        it to stringResource(if (it == AppsUsageScope.ALL_TIME) R.string.apps_scope_all_time else R.string.apps_scope_current_cycle)
                    },
                    onSelect = onScopeChange,
                )
            }
            FilterMenu(
                title = stringResource(R.string.apps_sort_filter),
                selected = sortOrder,
                options = AppsSortOrder.entries.map {
                    it to stringResource(
                        if (it == AppsSortOrder.HIGHEST_USAGE) R.string.apps_sort_highest_usage
                        else R.string.apps_sort_lowest_usage,
                    )
                },
                onSelect = onSortOrderChange,
            )
            FilterMenu(
                title = stringResource(R.string.apps_count_filter),
                selected = countLimit,
                options = appsCountOptions.map {
                    it to (it?.toString() ?: stringResource(R.string.apps_filter_all))
                },
                onSelect = onCountLimitChange,
            )
        }
    }
}

/** شريط فلاتر شاشة السجل: نوع الشبكة، الشهر، والسنة. */
@Composable
fun HistoryUsageFilterBar(
    network: UsageNetwork,
    selectedMonth: Int,
    selectedYear: Int,
    availableMonths: List<Int>,
    availableYears: List<Int>,
    onNetworkChange: (UsageNetwork) -> Unit,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            FilterMenu(
                title = stringResource(R.string.apps_network_filter),
                selected = network,
                options = UsageNetwork.entries.map {
                    it to stringResource(if (it == UsageNetwork.WIFI) R.string.apps_network_wifi else R.string.apps_network_mobile)
                },
                onSelect = onNetworkChange,
            )
            FilterMenu(
                title = stringResource(R.string.history_month_filter),
                selected = selectedMonth,
                selectedLabelOverride = Format.monthName(selectedMonth, selectedYear),
                options = availableMonths.map { month ->
                    month to Format.monthName(month, selectedYear)
                },
                onSelect = onMonthChange,
            )
            FilterMenu(
                title = stringResource(R.string.history_year_filter),
                selected = selectedYear,
                selectedLabelOverride = selectedYear.toString(),
                options = availableYears.map { year ->
                    year to year.toString()
                },
                onSelect = onYearChange,
            )
        }
    }
}

@Composable
private fun <T> FilterMenu(
    title: String,
    selected: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    selectedLabelOverride: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = selectedLabelOverride ?: options.firstOrNull { it.first == selected }?.second.orEmpty()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(
                onClick = { if (options.isNotEmpty()) expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.semantics { contentDescription = "$title: $selectedLabel" },
            ) {
                Text(text = selectedLabel, maxLines = 1)
                Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = expanded && options.isNotEmpty(), onDismissRequest = { expanded = false }) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            onSelect(value)
                        },
                        trailingIcon = {
                            if (value == selected) Icon(Icons.Rounded.Check, contentDescription = null)
                        },
                    )
                }
            }
        }
    }
}
