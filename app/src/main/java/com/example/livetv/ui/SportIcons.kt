package com.example.livetv.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.SportsRugby
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.ui.graphics.vector.ImageVector

/** True for plain football (soccer), whose teams have crests in the bundled logo set. */
fun isSoccer(sport: String): Boolean {
    val s = sport.lowercase()
    return s.isBlank() || ((s.contains("football") || s.contains("soccer")) && !s.contains("american"))
}

/** An icon for the sport, used where an event has no team logos (F1, tennis, cricket, ...). */
fun sportIcon(sport: String): ImageVector {
    val s = sport.lowercase()
    return when {
        s.contains("american") || s.contains("nfl") -> Icons.Default.SportsFootball
        s.contains("football") || s.contains("soccer") -> Icons.Default.SportsSoccer
        s.contains("basket") -> Icons.Default.SportsBasketball
        s.contains("tennis") -> Icons.Default.SportsTennis
        s.contains("formula") || s.contains("motor") || s.contains("racing") || s.contains("rally") ||
            s.contains("moto") || s.contains("nascar") -> Icons.Default.SportsMotorsports
        s.contains("hockey") -> Icons.Default.SportsHockey
        s.contains("cricket") -> Icons.Default.SportsCricket
        s.contains("rugby") -> Icons.Default.SportsRugby
        s.contains("baseball") -> Icons.Default.SportsBaseball
        s.contains("volley") -> Icons.Default.SportsVolleyball
        s.contains("handball") -> Icons.Default.SportsHandball
        s.contains("golf") -> Icons.Default.SportsGolf
        s.contains("mma") || s.contains("boxing") || s.contains("ufc") || s.contains("wrestl") -> Icons.Default.SportsMma
        s.contains("cycl") -> Icons.Default.DirectionsBike
        s.contains("esport") || s.contains("e-sport") -> Icons.Default.SportsEsports
        else -> Icons.Default.EmojiEvents
    }
}
