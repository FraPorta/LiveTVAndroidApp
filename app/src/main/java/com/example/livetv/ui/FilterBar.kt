package com.example.livetv.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.livetv.ui.theme.Accent
import com.example.livetv.ui.theme.CardColor
import com.example.livetv.ui.theme.CardRaised
import com.example.livetv.ui.theme.OnAccent
import com.example.livetv.ui.theme.TextSecondary

/**
 * Sport and league filters. Each is a dropdown pill that lights up in the accent colour
 * while it is filtering; a clear button appears when either is on. The bar only shows
 * when the loaded matches offer a real choice.
 */
@Composable
fun FilterBar(viewModel: MatchViewModel, horizontalPadding: Int, modifier: Modifier = Modifier) {
    val sports = viewModel.availableSports.value.filter { it.isNotBlank() }
    val leagues = viewModel.availableLeagues.value.filter { it.isNotBlank() }
    val selectedSport = viewModel.selectedSport.value
    val selectedLeague = viewModel.selectedLeague.value
    val showSport = sports.size > 2
    val showLeague = leagues.size > 2
    if (!showSport && !showLeague && selectedSport == null && selectedLeague == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSport) {
            FilterDropdown("Sport", "All Sports", sports, selectedSport) { viewModel.setSportFilter(it) }
        }
        if (showLeague) {
            FilterDropdown("League", "All Leagues", leagues, selectedLeague) { viewModel.setLeagueFilter(it) }
        }
        if (selectedSport != null || selectedLeague != null) {
            val source = remember { MutableInteractionSource() }
            val shape = RoundedCornerShape(50)
            Surface(
                onClick = {
                    viewModel.setSportFilter(null)
                    viewModel.setLeagueFilter(null)
                },
                modifier = Modifier.tvFocus(source, shape, scaleTo = 1.06f),
                shape = shape,
                color = CardColor,
                interactionSource = source,
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text("Clear", style = MaterialTheme.typography.labelLarge, color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    title: String,
    allLabel: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(50)
    val active = selected != null
    Box {
        Surface(
            onClick = { open = true },
            modifier = Modifier.tvFocus(source, shape, scaleTo = 1.06f),
            shape = shape,
            color = if (active) Accent else CardRaised,
            contentColor = if (active) OnAccent else TextSecondary,
            interactionSource = source,
        ) {
            Row(
                Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (active) selected.orEmpty() else title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        open = false
                        onSelect(if (option == allLabel) null else option)
                    },
                )
            }
        }
    }
}
