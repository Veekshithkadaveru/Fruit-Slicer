package app.krafted.fruitslicer.game

object SliceDetector {

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
        val b = 2 * (fx * dx + fy * dy)
        val c = fx * fx + fy * fy - radius * radius
        val discriminant = b * b - 4 * a * c
        return discriminant >= 0
    }

    fun checkTrail(
        trail: List<Pair<Float, Float>>,
        fruits: List<FruitObject>
    ): List<FruitObject> {
        if (trail.size < 2) return emptyList()
        val hit = mutableSetOf<FruitObject>()
        for (fruit in fruits) {
            if (!fruit.isAlive || fruit.isSliced) continue
            for (i in 0 until trail.size - 1) {
                val (p1x, p1y) = trail[i]
                val (p2x, p2y) = trail[i + 1]
                if (lineIntersectsCircle(p1x, p1y, p2x, p2y, fruit.x, fruit.y, fruit.radius)) {
                    hit.add(fruit)
                    break
                }
            }
        }
        return hit.toList()
    }
}
