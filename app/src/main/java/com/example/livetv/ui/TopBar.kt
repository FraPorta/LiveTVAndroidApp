package com.example.livetv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.livetv.data.model.ScrapingSection
import com.example.livetv.ui.theme.Accent
import com.example.livetv.ui.theme.CardRaised
import com.example.livetv.ui.theme.FavouriteGold
import com.example.livetv.ui.theme.OnAccent
import com.example.livetv.ui.theme.TextSecondary

/** Logo mark + app name. */
@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(26.dp).background(Accent, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OnAccent, modifier = Modifier.size(18.dp))
        }
        Text(
            text = "LiveTV",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** A round icon-only button using the shared focus style. */
@Composable
fun BarIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    spinning: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.tvFocus(source, CircleShape, scaleTo = 1.12f),
        shape = CircleShape,
        color = CardRaised,
        interactionSource = source,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = spinning },
            )
        }
    }
}

private data class TabSpec(val label: String, val selected: Boolean, val star: Boolean, val onClick: () -> Unit)

/** Pill tabs: Football, Top live, All and Favourites. */
@Composable
fun SectionTabs(
    currentSection: ScrapingSection,
    favouritesOnly: Boolean,
    onSectionChange: (ScrapingSection) -> Unit,
    onFavourites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = ScrapingSection.entries.map { section ->
        TabSpec(
            label = section.displayName,
            selected = !favouritesOnly && currentSection == section,
            star = false,
            onClick = { onSectionChange(section) },
        )
    } + TabSpec("Favourites", favouritesOnly, true, onFavourites)

    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(tabs) { tab ->
            val source = remember { MutableInteractionSource() }
            val shape = RoundedCornerShape(50)
            Surface(
                onClick = tab.onClick,
                modifier = Modifier.tvFocus(source, shape, scaleTo = 1.06f),
                shape = shape,
                color = if (tab.selected) Accent else CardRaised,
                contentColor = if (tab.selected) OnAccent else TextSecondary,
                interactionSource = source,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (tab.star) {
                        Icon(
                            Icons.Default.Star, null,
                            tint = if (tab.selected) OnAccent else FavouriteGold,
                            modifier = Modifier.size(14.dp),
                        )
                        Box(Modifier.width(6.dp))
                    }
                    Text(tab.label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/**
 * Header: logo, tabs and actions. One row on tablet and TV; on a phone the tabs move
 * to their own row below the logo and actions.
 */
@Composable
fun TopBar(
    compact: Boolean,
    currentSection: ScrapingSection,
    favouritesOnly: Boolean,
    onSectionChange: (ScrapingSection) -> Unit,
    onFavourites: () -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    spin: Float,
    showRefresh: Boolean,
    modifier: Modifier = Modifier,
) {
    val tabs: @Composable (Modifier) -> Unit = { m ->
        SectionTabs(currentSection, favouritesOnly, onSectionChange, onFavourites, m)
    }
    val actions: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BarIconButton(Icons.Default.Search, "Search matches", onSearch)
            if (showRefresh) BarIconButton(Icons.Default.Refresh, "Refresh matches", onRefresh, spinning = spin)
            BarIconButton(Icons.Default.Settings, "Settings", onSettings)
        }
    }
    if (compact) {
        Column(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppLogo(Modifier.weight(1f))
                actions()
            }
            tabs(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AppLogo()
            tabs(Modifier.weight(1f))
            actions()
        }
    }
}
