package com.example.livetv.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.livetv.data.model.ScrapingSection

/**
 * Tab bar that switches the active [ScrapingSection].
 *
 * Uses the Material3 [SingleChoiceSegmentedButtonRow] API for proper ripple,
 * built-in selected-state colour, and accessibility semantics. The [isCompact]
 * flag shrinks label typography for the TV header layout, where horizontal
 * space is shared with the settings header and action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionSelector(
    currentSection: ScrapingSection,
    onSectionChange: (ScrapingSection) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val sections = ScrapingSection.entries.toList()

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        sections.forEachIndexed { index, section ->
            SegmentedButton(
                selected = currentSection == section,
                onClick  = { onSectionChange(section) },
                shape    = SegmentedButtonDefaults.itemShape(index = index, count = sections.size),
                label    = {
                    Text(
                        text  = section.displayName,
                        style = if (isCompact) MaterialTheme.typography.labelSmall
                                else           MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}
