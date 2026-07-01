package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = PadelTheme.colors.goldLight,
    trailing: @Composable (() -> Unit)? = null,
) {
    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = value,
                style = PadelTheme.sportType.setGameNumeral,
                color = accent,
            )
            Text(
                text = label.uppercase(),
                style = PadelTheme.sportType.sectionHeader,
                color = PadelTheme.colors.textFaint,
            )
            if (trailing != null) {
                Spacer(modifier = Modifier.height(10.dp))
                trailing()
            }
        }
    }
}
