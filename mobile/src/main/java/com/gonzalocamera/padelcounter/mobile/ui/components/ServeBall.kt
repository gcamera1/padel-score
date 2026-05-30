package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gonzalocamera.padelcounter.mobile.R

@Composable
fun ServeBall(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    Image(
        painter = painterResource(id = R.drawable.padelball),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
