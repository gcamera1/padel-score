package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme

@Composable
fun ScoreDisplay(
    label: String,
    points: String,
    sets: Int,
    games: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val motion = PadelTheme.motion
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = PadelTheme.sportType.scoreLabel.copy(fontSize = 22.sp, letterSpacing = 1.5.sp),
            color = accent,
        )

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = points,
            transitionSpec = {
                (scaleIn(
                    initialScale = 0.78f,
                    animationSpec = tween(durationMillis = motion.durationMedium, easing = motion.bounceOut),
                ) + fadeIn(animationSpec = tween(motion.durationFast))) togetherWith
                    (scaleOut(
                        targetScale = 1.12f,
                        animationSpec = tween(durationMillis = motion.durationFast, easing = motion.standard),
                    ) + fadeOut(animationSpec = tween(motion.durationFast)))
            },
            label = "score",
            modifier = Modifier
                .testTag("score-points-$label")
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "$label puntos $points"
                },
        ) { value ->
            Text(
                text = value,
                style = PadelTheme.sportType.scoreNumeral.copy(
                    fontFeatureSettings = "tnum",
                ),
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScoreSecondary(label = "SETS", value = sets.toString())
            ScoreSecondary(label = "GAMES", value = games.toString())
        }
    }
}

@Composable
private fun ScoreSecondary(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = PadelTheme.sportType.scoreLabel,
            color = Color.White.copy(alpha = 0.55f),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = PadelTheme.sportType.setGameNumeral.copy(
                fontFeatureSettings = "tnum",
            ),
            color = Color.White,
        )
    }
}
