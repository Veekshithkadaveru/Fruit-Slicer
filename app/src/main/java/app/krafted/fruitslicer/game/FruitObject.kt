package app.krafted.fruitslicer.game

import app.krafted.fruitslicer.R

enum class FruitType(val points: Int, val baseWeight: Int, val drawableRes: Int, val juiceColor: Int) {
    STRAWBERRY(10, 20, R.drawable.fruit_strawberry, 0xFFE53935.toInt()),
    WATERMELON(10, 20, R.drawable.fruit_watermelon, 0xFF43A047.toInt()),
    ORANGE    (10, 20, R.drawable.fruit_orange,     0xFFFB8C00.toInt()),
    GRAPES    (20, 15, R.drawable.fruit_grapes,     0xFF8E24AA.toInt()),
    PINEAPPLE (20, 15, R.drawable.fruit_pineapple,  0xFFFDD835.toInt()),
    LEMON     (30,  8, R.drawable.fruit_lemon,      0xFFF9A825.toInt()),
    BOMB      ( 0,  0, R.drawable.bomb,             0xFF212121.toInt())
}

data class FruitObject(
    val id: Int,
    val type: FruitType,
    var x: Float,
    var y: Float,
    var velX: Float,
    var velY: Float,
    val radius: Float = 80f,
    var isSliced: Boolean = false,
    var isAlive: Boolean = true,
    var sliceTime: Long = 0L
) {
    companion object {
        const val GRAVITY = 1800f
    }

    fun update(deltaTime: Float, screenHeight: Int, onMissed: () -> Unit) {
        velY += GRAVITY * deltaTime
        x += velX * deltaTime
        y += velY * deltaTime
        if (y > screenHeight + radius) {
            isAlive = false
            if (!isSliced && type != FruitType.BOMB) onMissed()
        }
    }
}
