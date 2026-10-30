package it.sorguido.unconnected

import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

/** Reserve the natural maximum of the short master-button labels; status text must size to what is visible. */
internal fun stableContentMeasurePolicy(selected: Int) = MeasurePolicy { measurables, constraints ->
    val alternatives = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
    layout(
        constraints.constrainWidth(alternatives.maxOf { it.width }),
        constraints.constrainHeight(alternatives.maxOf { it.height }),
    ) {
        alternatives[selected].placeRelative(0, 0)
    }
}
