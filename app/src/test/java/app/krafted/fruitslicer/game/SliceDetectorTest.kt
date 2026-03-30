package app.krafted.fruitslicer.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SliceDetectorTest {

    @Test
    fun `resolveTrailHit prioritizes bomb over fruit hits`() {
        val trail = listOf(
            TrailPoint(0f, 0f),
            TrailPoint(120f, 0f)
        )
        val fruit = fruit(type = FruitType.ORANGE, x = 40f, y = 0f)
        val bomb = fruit(type = FruitType.BOMB, x = 80f, y = 0f)

        val result = SliceDetector.resolveTrailHit(trail, listOf(fruit, bomb))

        assertTrue(result is SliceDetector.SliceHitResult.Bomb)
        assertEquals(bomb, (result as SliceDetector.SliceHitResult.Bomb).fruit)
    }

    @Test
    fun `resolveTrailHit returns fruit hits when no bomb is sliced`() {
        val trail = listOf(
            TrailPoint(0f, 0f),
            TrailPoint(120f, 0f)
        )
        val orange = fruit(type = FruitType.ORANGE, x = 40f, y = 0f)
        val lemon = fruit(type = FruitType.LEMON, x = 80f, y = 0f)

        val result = SliceDetector.resolveTrailHit(trail, listOf(orange, lemon))

        assertTrue(result is SliceDetector.SliceHitResult.Fruits)
        assertEquals(listOf(orange, lemon), (result as SliceDetector.SliceHitResult.Fruits).fruits)
    }

    private fun fruit(type: FruitType, x: Float, y: Float): FruitObject {
        return FruitObject(
            type = type,
            x = x,
            y = y,
            velX = 0f,
            velY = 0f,
            radius = 24f
        )
    }
}
