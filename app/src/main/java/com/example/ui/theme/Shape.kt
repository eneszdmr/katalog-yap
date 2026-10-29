package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Tasarım Sistemi Köşe Yuvarlaklıkları:
 * - Kartlar ve Modallar: rounded-2xl (16.dp)
 * - Butonlar ve Inputlar: rounded-xl (12.dp)
 * - Rozetler (Badge): rounded-lg / tam yuvarlak (8.dp / 999.dp)
 */
val KatalogYapShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),   // rounded-xl (Butonlar & Inputlar)
    large = RoundedCornerShape(16.dp),    // rounded-2xl (Kartlar)
    extraLarge = RoundedCornerShape(24.dp) // BottomSheet & Büyük Modallar
)

val CardRounded2xl = RoundedCornerShape(16.dp)
val ButtonRoundedXl = RoundedCornerShape(12.dp)
val BadgeRoundedPill = RoundedCornerShape(999.dp)
