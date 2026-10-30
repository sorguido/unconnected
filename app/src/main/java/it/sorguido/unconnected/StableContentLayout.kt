package it.sorguido.unconnected

import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

/** Measure every alternative at its natural height; place only the current one in that stable space. */
internal fun stableContentMeasurePolicy(selected: Int) = MeasurePolicy { measurables, constraints ->
    val alternatives = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
    layout(
        constraints.constrainWidth(alternatives.maxOf { it.width }),
        constraints.constrainHeight(alternatives.maxOf { it.height }),
    ) {
        alternatives[selected].placeRelative(0, 0)
    }
}
