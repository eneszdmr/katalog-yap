package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    GHOST
}

/**
 * Ortak Buton Bileşeni (Button: primary / secondary / ghost).
 * - Köşe yuvarlaklığı: rounded-xl (12.dp)
 * - Minimum dokunma alanı: 48.dp (44px+ standardı)
 * - Buton basma efekti (Spring Scale animasyonu: 0.96f)
 * - Erişilebilirlik etiketi (semantics contentDescription) ve taşma koruması (Ellipsis)
 */
@Composable
fun KYButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    tag: String = "ky_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ky_button_press_scale"
    )

    val minSizeModifier = modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .semantics { contentDescription = text }
        .testTag(tag)
    val contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)

    when (variant) {
        ButtonVariant.PRIMARY -> {
            Button(
                onClick = { if (!isLoading) onClick() },
                enabled = enabled,
                interactionSource = interactionSource,
                shape = ButtonRoundedXl,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = SurfaceWhite,
                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.45f),
                    disabledContentColor = SurfaceWhite.copy(alpha = 0.8f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 1.dp,
                    pressedElevation = 0.dp
                ),
                modifier = minSizeModifier
            ) {
                ButtonInnerContent(
                    text = text,
                    isLoading = isLoading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    contentColor = SurfaceWhite
                )
            }
        }

        ButtonVariant.SECONDARY -> {
            OutlinedButton(
                onClick = { if (!isLoading) onClick() },
                enabled = enabled,
                interactionSource = interactionSource,
                shape = ButtonRoundedXl,
                contentPadding = contentPadding,
                border = BorderStroke(1.dp, if (enabled) BorderStrong else BorderStrong.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceWhite,
                    contentColor = TextPrimary,
                    disabledContainerColor = SurfaceWhite,
                    disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                ),
                modifier = minSizeModifier
            ) {
                ButtonInnerContent(
                    text = text,
                    isLoading = isLoading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    contentColor = TextPrimary
                )
            }
        }

        ButtonVariant.GHOST -> {
            TextButton(
                onClick = { if (!isLoading) onClick() },
                enabled = enabled,
                interactionSource = interactionSource,
                shape = ButtonRoundedXl,
                contentPadding = contentPadding,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PrimaryBlue,
                    disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                ),
                modifier = minSizeModifier
            ) {
                ButtonInnerContent(
                    text = text,
                    isLoading = isLoading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    contentColor = PrimaryBlue
                )
            }
        }
    }
}

@Composable
private fun ButtonInnerContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!isLoading && trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
