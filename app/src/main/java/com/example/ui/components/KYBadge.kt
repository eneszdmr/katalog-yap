package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.DangerRedDark
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.PrimaryBlueDark
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.SuccessGreenDark
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberDark
import com.example.ui.theme.WarningAmberLight

enum class BadgeVariant {
    PRIMARY,
    ACCENT,
    SUCCESS,
    WARNING,
    DANGER,
    NEUTRAL
}

/**
 * Ortak Rozet Bileşeni (Badge).
 */
@Composable
fun KYBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.PRIMARY,
    icon: ImageVector? = null,
    tag: String = "ky_badge"
) {
    val (bgColor, textColor) = when (variant) {
        BadgeVariant.PRIMARY -> PrimaryBlueLight to PrimaryBlueDark
        BadgeVariant.ACCENT -> AccentAmberLight to WarningAmberDark
        BadgeVariant.SUCCESS -> SuccessGreenLight to SuccessGreenDark
        BadgeVariant.WARNING -> WarningAmberLight to WarningAmberDark
        BadgeVariant.DANGER -> DangerRedLight to DangerRedDark
        BadgeVariant.NEUTRAL -> SurfaceMuted to TextSecondary
    }

    Row(
        modifier = modifier
            .clip(BadgeRoundedPill)
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
