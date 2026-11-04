package com.adammg.ibridge.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adammg.ibridge.R

@Composable
fun IBridgeLogo(
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    heightDp: Int = 36
) {
    val logoResId = if (isDark) R.drawable.ibridge_logo_white else R.drawable.ibridge_logo_blue

    Image(
        painter = painterResource(id = logoResId),
        contentDescription = "iBridge Logo",
        modifier = modifier.height(heightDp.dp),
        contentScale = ContentScale.Fit
    )
}

@Preview(showBackground = true)
@Composable
fun IBridgeLogoPreview() {
    MaterialTheme {
        IBridgeLogo()
    }
}
