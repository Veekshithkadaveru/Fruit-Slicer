package app.krafted.fruitslicer.game

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class JuiceSplash(
    val x: Float,
    val y: Float,
    val color: Int,
    val durationMs: Float = 600f
) {
    class Particle(
        var px: Float,
        var py: Float,
        var velX: Float,
        var velY: Float,
        val pColor: Int,
        val particleDuration: Float
    ) {
        var elapsed: Float = 0f
        val progress get() = (elapsed / particleDuration).coerceIn(0f, 1f)
        val alpha get() = ((1f - progress) * 220f).toInt()
        val radius get() = (1f - progress * 0.5f) * 9f
    }

    val particles: List<Particle> = buildList {
        repeat(12) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 150f + Random.nextFloat() * 350f
            add(
                Particle(
                    px = x,
                    py = y,
                    velX = cos(angle) * speed,
                    velY = sin(angle) * speed - 300f,
                    pColor = color,
                    particleDuration = durationMs
                )
            )
        }
    }

    var elapsed: Float = 0f
    val isAlive get() = elapsed < durationMs

    fun update(deltaTime: Float) {
        elapsed += deltaTime * 1000f
        particles.forEach { p ->
            p.elapsed += deltaTime * 1000f
            p.velY += 800f * deltaTime
            p.px += p.velX * deltaTime
            p.py += p.velY * deltaTime
        }
    }
}
