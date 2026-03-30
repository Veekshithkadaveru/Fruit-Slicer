package app.krafted.fruitslicer.game

object SliceDetector {

    sealed interface SliceHitResult {
        data class Bomb(val fruit: FruitObject) : SliceHitResult
        data class Fruits(val fruits: List<FruitObject>) : SliceHitResult
    }

    fun lineIntersectsCircle(
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        cx: Float, cy: Float,
        radius: Float
    ): Boolean {
        val dx = p2x - p1x
        val dy = p2y - p1y
        val fx = p1x - cx
        val fy = p1y - cy
        val a = dx * dx + dy * dy
        if (a == 0f) return fx * fx + fy * fy <= radius * radius
        val b = 2f * (fx * dx + fy * dy)

        val t = (-b / (2f * a)).coerceIn(0f, 1f)
        val closestX = fx + t * dx
        val closestY = fy + t * dy
        return closestX * closestX + closestY * closestY <= radius * radius
    }

    fun checkTrail(
        trail: List<TrailPoint>,
        fruits: List<FruitObject>
    ): List<FruitObject> {
        if (trail.size < 2) return emptyList()
        val hit = mutableSetOf<FruitObject>()
        for (fruit in fruits) {
            if (!fruit.isAlive || fruit.isSliced) continue
            for (i in 0 until trail.size - 1) {
                val p1x = trail[i].x
                val p1y = trail[i].y
                val p2x = trail[i + 1].x
                val p2y = trail[i + 1].y
                if (lineIntersectsCircle(p1x, p1y, p2x, p2y, fruit.x, fruit.y, fruit.radius)) {
                    hit.add(fruit)
                    break
                }
            }
        }
        return hit.toList()
    }

    fun resolveTrailHit(
        trail: List<TrailPoint>,
        fruits: List<FruitObject>
    ): SliceHitResult? {
        val hit = checkTrail(trail, fruits)
        if (hit.isEmpty()) return null

        val bomb = hit.firstOrNull { it.type == FruitType.BOMB }
        return if (bomb != null) {
            SliceHitResult.Bomb(bomb)
        } else {
            SliceHitResult.Fruits(hit)
        }
    }
}
