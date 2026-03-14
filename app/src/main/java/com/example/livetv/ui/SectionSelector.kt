package com.example.livetv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.livetv.data.model.ScrapingSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionSelector(
    currentSection: ScrapingSection,
    onSectionChange: (ScrapingSection) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    if (isCompact) {
        // Modern compact version for horizontal layout
        Column(modifier = modifier) {
            // Modern segmented button style
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp)
            ) {
                ScrapingSection.entries.forEach { section ->
                    val isSelected = currentSection == section
                    Surface(
                        onClick = { onSectionChange(section) },
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = section.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Modern full version for vertical layout
        Column(modifier = modifier.fillMaxWidth()) {
            // Modern button group style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(6.dp)
            ) {
                ScrapingSection.entries.forEach { section ->
                    val isSelected = currentSection == section
                    Surface(
                        onClick = { onSectionChange(section) },
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = section.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
