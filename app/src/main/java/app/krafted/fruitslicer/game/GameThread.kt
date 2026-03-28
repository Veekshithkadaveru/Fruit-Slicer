package app.krafted.fruitslicer.game

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val onUpdate: (deltaTime: Float) -> Unit,
    private val onDraw: (canvas: Canvas) -> Unit
) : Thread() {

    @Volatile var running = false
    private val targetFrameNs = 1_000_000_000L / 60L

    override fun run() {
        var lastTime = System.nanoTime()
        while (running) {
            val frameStart = System.nanoTime()
            val deltaTime = (frameStart - lastTime) / 1_000_000_000f
            lastTime = frameStart
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        onUpdate(deltaTime.coerceAtMost(0.05f))
                        onDraw(canvas)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                canvas?.let {
                    try { surfaceHolder.unlockCanvasAndPost(it) }
                    catch (e: Exception) { e.printStackTrace() }
                }
            }
            val sleep = (targetFrameNs - (System.nanoTime() - frameStart)) / 1_000_000L
            if (sleep > 0) try { sleep(sleep) } catch (e: InterruptedException) { break }
        }
    }
}
