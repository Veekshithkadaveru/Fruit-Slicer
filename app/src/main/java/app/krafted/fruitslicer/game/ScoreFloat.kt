package app.krafted.fruitslicer.game

data class ScoreFloat(
    val startX: Float,
    val startY: Float,
    val text: String,
    val durationMs: Float = 500f
) {
    var elapsed: Float = 0f
    val isAlive get() = elapsed < durationMs

    fun update(deltaTime: Float) {
        elapsed += deltaTime * 1000f
    }

    val progress get() = (elapsed / durationMs).coerceIn(0f, 1f)
    val currentY get() = startY - progress * 120f
    val alpha get() = ((1f - progress) * 255f).toInt()
}
