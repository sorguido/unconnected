package it.sorguido.unconnected

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import org.junit.Assert.*
import org.junit.Test

/**
 * The retained policy serves the two master-button labels only. The status card composes just its
 * visible state, so nothing here may reserve bounds for text that is not on screen.
 * Synthetic measurements: not Android text shaping and not perceived handset flicker.
 */
class StableContentLayoutTest {
    @Test fun masterLabelsKeepTheSameButtonHeightWhenOnlyDeactivateWraps() {
        val constraints = Constraints(minWidth = 180, maxWidth = 180)
        for (selected in 0..1) {
            val labels = listOf(Child(180, 32), Child(180, 64))
            val result = with(stableContentMeasurePolicy(selected)) { Scope.measure(labels, constraints) }
            assertEquals(64, result.height)
            result.placeChildren()
            assertEquals(listOf(selected), labels.indices.filter { labels[it].positions.isNotEmpty() })
            assertEquals(listOf(IntOffset.Zero), labels[selected].positions)
        }
    }

    @Test fun widerAndLargerLabelsGrowTheButtonNaturallyWithoutALabelSpecificCap() {
        for (width in listOf(120, 180, 320)) for (scale in listOf(1, 2, 3)) {
            val heights = listOf(24 * scale, (1 + 360 / width) * 24 * scale)
            val sizes = (0..1).map { selected ->
                val labels = heights.map { Child(width, it) }
                val result = with(stableContentMeasurePolicy(selected)) {
                    Scope.measure(labels, Constraints(minWidth = width, maxWidth = width))
                }
                assertEquals(heights.max(), result.height)
                assertTrue(labels.all { it.measures == 1 })
                IntSize(result.width, result.height)
            }
            assertEquals(1, sizes.distinct().size)
        }
    }

    @Test fun parentBoundsAreRespectedWithoutStretchingEachLabelToMinimumHeight() {
        val labels = listOf(Child(180, 32), Child(180, 64))
        val result = with(stableContentMeasurePolicy(0)) {
            Scope.measure(labels, Constraints(minWidth = 180, maxWidth = 220, minHeight = 100, maxHeight = 200))
        }
        assertEquals(180, result.width)
        assertEquals(100, result.height)
        assertTrue(labels.all { it.lastConstraints!!.minHeight == 0 })
        assertTrue(labels.all { it.lastConstraints!!.maxHeight == 200 })
        val tight = listOf(Child(180, 32), Child(180, 64))
        val capped = with(stableContentMeasurePolicy(1)) { Scope.measure(tight, Constraints(maxWidth = 160, maxHeight = 40)) }
        assertEquals(160, capped.width)
        assertEquals(40, capped.height)
    }

    private object Scope : MeasureScope {
        override val density = 1f
        override val fontScale = 1f
        override val layoutDirection = LayoutDirection.Ltr
        override fun layout(width: Int, height: Int, alignmentLines: Map<AlignmentLine, Int>, placementBlock: Placeable.PlacementScope.() -> Unit): MeasureResult = object : MeasureResult {
            override val width = width
            override val height = height
            override val alignmentLines = alignmentLines
            override fun placeChildren() {
                val scope = object : Placeable.PlacementScope() {
                    override val parentWidth = width
                    override val parentLayoutDirection = LayoutDirection.Ltr
                }
                scope.placementBlock()
            }
        }
    }

    private class Child(private val naturalWidth: Int, private val naturalHeight: Int) : Measurable {
        var measures = 0
        var lastConstraints: Constraints? = null
        val positions = mutableListOf<IntOffset>()
        override val parentData: Any? = null
        override fun minIntrinsicWidth(height: Int) = naturalWidth
        override fun maxIntrinsicWidth(height: Int) = naturalWidth
        override fun minIntrinsicHeight(width: Int) = naturalHeight
        override fun maxIntrinsicHeight(width: Int) = naturalHeight
        override fun measure(constraints: Constraints): Placeable {
            measures++
            lastConstraints = constraints
            return object : Placeable() {
                init {
                    measurementConstraints = constraints
                    measuredSize = IntSize(constraints.constrainWidth(naturalWidth), constraints.constrainHeight(naturalHeight))
                }
                override fun get(alignmentLine: AlignmentLine) = AlignmentLine.Unspecified
                override fun placeAt(position: IntOffset, zIndex: Float, layerBlock: (GraphicsLayerScope.() -> Unit)?) { positions += position }
            }
        }
    }
}
