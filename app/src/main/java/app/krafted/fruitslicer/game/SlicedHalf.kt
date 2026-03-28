package app.krafted.fruitslicer.game

class SlicedHalf(
    val fruitType: FruitType,
    var x: Float,
    var y: Float,
    var velX: Float,
    var velY: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val isTopHalf: Boolean,
    val size: Float,
    val durationMs: Float = 700f
) {
    var elapsed: Float = 0f
    val isAlive get() = elapsed < durationMs
    val progress get() = (elapsed / durationMs).coerceIn(0f, 1f)
    val alpha get() = ((1f - progress) * 255f).toInt()

    fun update(deltaTime: Float) {
        elapsed += deltaTime * 1000f
        velY += FruitObject.GRAVITY * deltaTime
        x += velX * deltaTime
        y += velY * deltaTime
        rotation += rotationSpeed * deltaTime
    }
}
