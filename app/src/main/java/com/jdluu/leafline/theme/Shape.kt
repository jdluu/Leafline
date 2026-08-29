package com.jdluu.leafline.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Leafline shape definitions used across components.
 *
 * The contract mandates one 12dp container radius for every surface. Pill
 * shapes are reserved for compact single-action controls such as chips and
 * small icon buttons; they never apply to large containers.
 */
val LeaflineShapes = Shapes(
    small = RoundedCornerShape(12.dp),  // compact controls: buttons, chips
    medium = RoundedCornerShape(12.dp), // cards, sheets, dialogs, tiles
    large = RoundedCornerShape(12.dp)   // top-level containers and modals
)

/**
 * Pill shape for compact single-action controls only (chips, small buttons).
 * Deliberately not part of [LeaflineShapes] so large surfaces cannot absorb
 * it by accident.
 */
val LeaflinePillShape = RoundedCornerShape(percent = 50)