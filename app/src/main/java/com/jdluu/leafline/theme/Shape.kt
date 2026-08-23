package com.jdluu.leafline.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Leafline shape definitions used across components.
 */
val LeaflineShapes = Shapes(
    small = RoundedCornerShape(8.dp),      // chips, small cards, buttons
    medium = RoundedCornerShape(12.dp),    // bottom sheets, cards, dialogs
    large = RoundedCornerShape(16.dp)      // top-level containers, modals
)