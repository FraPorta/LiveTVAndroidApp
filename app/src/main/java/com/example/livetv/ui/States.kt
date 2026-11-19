package com.example.livetv.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.livetv.ui.theme.Accent
import com.example.livetv.ui.theme.CardColor
import com.example.livetv.ui.theme.CardRaised
import com.example.livetv.ui.theme.OnAccent
import com.example.livetv.ui.theme.TextSecondary

/** Friendly full-area message with an icon and one action (empty and error states). */
@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(72.dp).background(CardColor, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(34.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        )
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
        ) { Text(actionLabel) }
    }
}

/** Shimmering placeholder cards shown while the match list loads. */
@Composable
fun SkeletonGrid(minCardWidth: Int, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minCardWidth.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false,
    ) {
        items(List(9) { it }) { SkeletonCard(Modifier.alpha(pulse)) }
    }
}

@Composable
private fun SkeletonCard(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().background(CardColor, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.fillMaxWidth(0.4f).height(10.dp).background(CardRaised, RoundedCornerShape(5.dp)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(CardRaised, CircleShape))
            Box(Modifier.size(width = 60.dp, height = 22.dp).background(CardRaised, RoundedCornerShape(6.dp)))
            Box(Modifier.size(40.dp).background(CardRaised, CircleShape))
        }
        Box(Modifier.fillMaxWidth(0.3f).height(14.dp).background(CardRaised, RoundedCornerShape(7.dp)))
    }
}
