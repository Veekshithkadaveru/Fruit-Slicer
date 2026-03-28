package app.krafted.fruitslicer.game

class SliceFlash(
    val x: Float,
    val y: Float,
    val durationMs: Float = 200f
) {
    var elapsed: Float = 0f
    val isAlive get() = elapsed < durationMs
    val progress get() = (elapsed / durationMs).coerceIn(0f, 1f)

    fun update(deltaTime: Float) {
        elapsed += deltaTime * 1000f
    }
}
