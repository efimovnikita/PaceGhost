package com.paceghost.app.ui.active.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paceghost.app.ui.theme.DarkSurfaceVariant
import com.paceghost.app.ui.theme.TextPrimary
import com.paceghost.app.ui.theme.WarningAmber

@Composable
fun HapticBadge(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(DarkSurfaceVariant.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                .border(1.dp, WarningAmber.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📳",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }
        }
    }
}
