package app.krafted.fruitslicer.game

data class JuiceSplash(
    val x: Float,
    val y: Float,
    val color: Int,
    val durationMs: Float = 300f
) {
    var elapsed: Float = 0f
    val isAlive get() = elapsed < durationMs

    fun update(deltaTime: Float) {
        elapsed += deltaTime * 1000f
    }

    val progress get() = (elapsed / durationMs).coerceIn(0f, 1f)
    val radius get() = progress * 120f
    val alpha get() = ((1f - progress) * 255f).toInt()
}
