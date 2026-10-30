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

/** Tests the production Compose policy, not Android text shaping or perceived handset flicker. */
class StableContentLayoutTest {
    @Test fun repeatedSavesToggleAndRealPauseKeepTheSameHeaderBounds() {
        val sequence = listOf(
            filterStatus(false, true, false, false),
            filterStatus(false, true, true, true),
            filterStatus(true, true, true, false), // Disk completed, UI save still finishing.
            filterStatus(true, true, false, false),
            filterStatus(true, true, true, true),
            filterStatus(true, true, false, true), // Failure remains a genuine pause.
            filterStatus(true, true, true, true),
            filterStatus(true, true, false, false),
            filterStatus(true, true, true, true),
            filterStatus(false, true, false, false),
            filterStatus(true, false, false, false),
        )
        repeat(3) {
            for (status in sequence) {
                val children = children()
                val result = measure(status, children, Constraints(maxWidth = 320))
                assertEquals(320, result.width)
                assertEquals(150, result.height)
                assertTrue(children.all { it.measures == 1 })
                result.placeChildren()
                assertEquals(listOf(status.ordinal), children.indices.filter { children[it].positions.isNotEmpty() })
                assertEquals(listOf(IntOffset.Zero), children[status.ordinal].positions)
            }
        }
    }

    @Test fun largerTextAndNarrowerWidthsGrowNaturallyWithoutAStatusSpecificHeightCap() {
        for (width in listOf(200, 320, 600)) for (scale in listOf(1, 2, 3)) {
            val heights = FilterStatus.entries.map { status ->
                // Synthetic measured text blocks: intentionally not an Android font-rendering test.
                (1 + (status.ordinal + 1) * 400 / width) * 24 * scale
            }
            val sizes = FilterStatus.entries.map { status ->
                val children = heights.map { Child(width, it) }
                val result = measure(status, children, Constraints(minWidth = width, maxWidth = width))
                assertEquals(heights.max(), result.height)
                IntSize(result.width, result.height)
            }
            assertEquals(1, sizes.distinct().size)
        }
    }

    @Test fun parentBoundsAreRespectedWithoutStretchingEachTextAlternativeToMinimumHeight() {
        val children = children()
        val result = measure(FilterStatus.ACTIVE, children, Constraints(minWidth = 360, maxWidth = 400, minHeight = 200, maxHeight = 300))
        assertEquals(360, result.width)
        assertEquals(200, result.height)
        assertTrue(children.all { it.lastConstraints!!.minHeight == 0 })
        assertTrue(children.all { it.lastConstraints!!.maxHeight == 300 })
        val constrained = measure(FilterStatus.SAVING, children(), Constraints(maxWidth = 280, maxHeight = 100))
        assertEquals(280, constrained.width)
        assertEquals(100, constrained.height)
    }

    @Test fun masterLabelsKeepTheSameButtonHeightWhenOnlyDeactivateWraps() {
        val constraints = Constraints(minWidth = 180, maxWidth = 180)
        for (selected in 0..1) {
            val labels = listOf(Child(180, 32), Child(180, 64))
            val result = with(stableContentMeasurePolicy(selected)) { Scope.measure(labels, constraints) }
            assertEquals(64, result.height)
            result.placeChildren()
            assertEquals(listOf(selected), labels.indices.filter { labels[it].positions.isNotEmpty() })
        }
    }

    private fun children() = listOf(80, 60, 110, 150, 120).map { Child(320, it) }

    private fun measure(status: FilterStatus, children: List<Child>, constraints: Constraints): MeasureResult =
        with(stableContentMeasurePolicy(status.ordinal)) { Scope.measure(children, constraints) }

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
