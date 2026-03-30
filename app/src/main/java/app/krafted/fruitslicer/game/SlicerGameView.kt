package app.krafted.fruitslicer.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.cos
import kotlin.math.sin

data class TrailPoint(val x: Float, val y: Float, var age: Float = 0f)
data class BombSliceEffect(val x: Float, val y: Float, var age: Float = 0f)

class SlicerGameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    var onFruitSliced: ((FruitType, Float, Float) -> Unit)? = null
    var onFruitMissed: (() -> Unit)? = null
    var onBombSwiped: (() -> Unit)? = null

    private var gameThread: GameThread? = null
    private var spawner: FruitSpawner? = null
    private val bitmaps = mutableMapOf<FruitType, Bitmap>()
    private val bitmapLock = Any()
    private var backgroundBitmap: Bitmap? = null
    private val backgroundDestRect = RectF()
    private val bombSrcRect = Rect()
    private var screenWidth = 0
    private var screenHeight = 0
    private var fruitSize = 0f
    private var backgroundResId = app.krafted.fruitslicer.R.drawable.bg_round_1
    private var speedMultiplier = 1.0f
    private var bombWeight = 0
    private var maxOnScreen = 2
    private var spawnIntervalMs = 1200L
    private val bombFrameCount = 6
    private val bombFrameDurationMs = 100L
    private val bombFeedbackDuration = 0.34f
    private val bombSliceEffectDuration = 0.45f
    private var bombFeedbackTime = 0f
    private var bombImpactX = 0f
    private var bombImpactY = 0f

    private val trailLock = Any()
    private val trail = ArrayDeque<TrailPoint>()
    private val juiceSplashes = mutableListOf<JuiceSplash>()
    private val scoreFloats = mutableListOf<ScoreFloat>()
    private val bombSliceEffects = mutableListOf<BombSliceEffect>()

    private val bladeVertexPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val bladeGlowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val bladeCorePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val bladeSparkPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val headGlowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val headCorePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private var leftVertices = FloatArray(0)
    private var leftColors = IntArray(0)
    private var rightVertices = FloatArray(0)
    private var rightColors = IntArray(0)

    private val splashPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val bombFlashPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = Color.RED
    }
    private val bombRingPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.argb(220, 255, 120, 120)
        strokeWidth = 8f
    }

    private val scoreTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameThread = GameThread(holder, ::update, ::drawFrame).also {
            it.running = true
            it.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        backgroundDestRect.set(0f, 0f, width.toFloat(), height.toFloat())
        val newFruitSize = (width * 0.15f).toInt().coerceAtLeast(60).toFloat()
        if (newFruitSize != fruitSize) {
            fruitSize = newFruitSize
            loadBitmaps(fruitSize.toInt())
        }
        if (backgroundResId != 0) loadBackground(backgroundResId)
        if (spawner == null) {
            spawner = FruitSpawner(screenWidth, screenHeight).also {
                it.fruitRadius = fruitSize / 2f
                it.speedMultiplier = speedMultiplier
                it.bombWeight = bombWeight
                it.maxOnScreen = maxOnScreen
                it.spawnIntervalMs = spawnIntervalMs
            }
        } else {
            spawner?.screenWidth = screenWidth
            spawner?.screenHeight = screenHeight
            spawner?.fruitRadius = fruitSize / 2f
            spawner?.speedMultiplier = speedMultiplier
            spawner?.bombWeight = bombWeight
            spawner?.maxOnScreen = maxOnScreen
            spawner?.spawnIntervalMs = spawnIntervalMs
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread?.running = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join(2000)
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        gameThread = null
        recycleBitmaps()
    }

    private fun loadBitmaps(targetSize: Int) {
        val loadedBitmaps = mutableMapOf<FruitType, Bitmap>()
        FruitType.entries.forEach { type ->
            val scaledTargetSize = (targetSize * type.sizeMultiplier).toInt().coerceAtLeast(1)
            val targetWidth = if (type == FruitType.BOMB) {
                scaledTargetSize * bombFrameCount
            } else {
                scaledTargetSize
            }
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val resId = if (type == FruitType.BOMB) {
                app.krafted.fruitslicer.R.drawable.bomb_anim
            } else {
                type.drawableRes
            }
            BitmapFactory.decodeResource(context.resources, resId, opts)
            opts.inSampleSize =
                calculateInSampleSize(
                    opts.outWidth,
                    opts.outHeight,
                    targetWidth,
                    scaledTargetSize
                )
            opts.inJustDecodeBounds = false
            val raw = BitmapFactory.decodeResource(context.resources, resId, opts)
            if (raw != null) {
                val scaled = Bitmap.createScaledBitmap(
                    raw,
                    targetWidth,
                    scaledTargetSize,
                    true
                )
                loadedBitmaps[type] = scaled
                if (raw != scaled) raw.recycle()
            }
        }
        synchronized(bitmapLock) {
            bitmaps.values.forEach { it.recycle() }
            bitmaps.clear()
            bitmaps.putAll(loadedBitmaps)
        }
    }

    private fun loadBackground(resId: Int) {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, opts)
        opts.inSampleSize =
            calculateInSampleSize(opts.outWidth, opts.outHeight, screenWidth, screenHeight)
        opts.inJustDecodeBounds = false
        val decoded = BitmapFactory.decodeResource(context.resources, resId, opts)
        if (decoded != null) {
            synchronized(bitmapLock) {
                val previous = backgroundBitmap
                backgroundBitmap = decoded
                previous?.recycle()
            }
        }
    }

    private fun calculateInSampleSize(rawW: Int, rawH: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (rawH > reqH || rawW > reqW) {
            val halfH = rawH / 2
            val halfW = rawW / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun recycleBitmaps() {
        synchronized(bitmapLock) {
            bitmaps.values.forEach { it.recycle() }
            bitmaps.clear()
            backgroundBitmap?.recycle()
            backgroundBitmap = null
        }
    }

    private fun update(deltaTime: Float) {
        val s = spawner ?: return
        s.update()
        val fruits = s.activeFruits
        fruits.forEach { fruit ->
            fruit.update(deltaTime, screenHeight) {
                onFruitMissed?.invoke()
            }
        }
        s.removeDeadFruits()

        synchronized(trailLock) {
            trail.forEach { it.age += deltaTime }
            while (trail.isNotEmpty() && trail.first().age > 0.18f) {
                trail.removeFirst()
            }
        }

        juiceSplashes.forEach { it.update(deltaTime) }
        juiceSplashes.removeAll { !it.isAlive }
        scoreFloats.forEach { it.update(deltaTime) }
        scoreFloats.removeAll { !it.isAlive }
        bombSliceEffects.forEach { it.age += deltaTime }
        bombSliceEffects.removeAll { it.age >= bombSliceEffectDuration }
        bombFeedbackTime = (bombFeedbackTime - deltaTime).coerceAtLeast(0f)
    }

    private fun drawFrame(canvas: Canvas) {
        val feedbackStrength = (bombFeedbackTime / bombFeedbackDuration).coerceIn(0f, 1f)
        if (feedbackStrength > 0f) {
            val time = System.currentTimeMillis() / 16f
            val shakeAmount = 14f * feedbackStrength
            canvas.save()
            canvas.translate(
                sin(time * 1.85f) * shakeAmount,
                cos(time * 2.35f) * shakeAmount * 0.7f
            )
        }
        synchronized(bitmapLock) {
            val bg = backgroundBitmap
            if (bg != null) {
                canvas.drawBitmap(bg, null, backgroundDestRect, null)
            } else {
                canvas.drawColor(Color.BLACK)
            }
            drawFruits(canvas)
        }
        drawJuiceSplashes(canvas)
        drawBombSliceEffects(canvas)
        drawScoreFloats(canvas)
        drawBlade(canvas)
        if (feedbackStrength > 0f) {
            canvas.restore()
            drawBombFeedback(canvas, feedbackStrength)
        }
    }

    private fun drawBombFeedback(canvas: Canvas, feedbackStrength: Float) {
        bombFlashPaint.alpha = (110f * feedbackStrength).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), bombFlashPaint)

        val flashRadius = (fruitSize * 0.8f) + ((1f - feedbackStrength) * fruitSize * 1.7f)
        bombRingPaint.alpha = (220f * feedbackStrength).toInt().coerceIn(0, 255)
        bombRingPaint.strokeWidth = 6f + (10f * feedbackStrength)
        canvas.drawCircle(bombImpactX, bombImpactY, flashRadius, bombRingPaint)
    }

    private fun drawBombSliceEffects(canvas: Canvas) {
        val bitmap = bitmaps[FruitType.BOMB] ?: return
        val frameWidth = bitmap.width / bombFrameCount
        bombSliceEffects.forEach { effect ->
            val progress = (effect.age / bombSliceEffectDuration).coerceIn(0f, 1f)
            val currentFrame = (progress * (bombFrameCount - 1)).toInt().coerceIn(0, bombFrameCount - 1)
            val size = (fruitSize * FruitType.BOMB.sizeMultiplier) * (1f + progress * 0.55f)
            val half = size / 2f
            val destRect = RectF(effect.x - half, effect.y - half, effect.x + half, effect.y + half)
            bombSrcRect.set(
                currentFrame * frameWidth,
                0,
                (currentFrame + 1) * frameWidth,
                bitmap.height
            )
            bombFlashPaint.alpha = ((1f - progress) * 160f).toInt().coerceIn(0, 255)
            canvas.drawCircle(effect.x, effect.y, half * 0.9f, bombFlashPaint)
            canvas.drawBitmap(bitmap, bombSrcRect, destRect, null)
        }
    }

    private fun drawFruits(canvas: Canvas) {
        val timeNow = System.currentTimeMillis()
        spawner?.activeFruits?.forEach { fruit ->
            if (!fruit.isAlive || fruit.isSliced) return@forEach
            val bitmap = bitmaps[fruit.type] ?: return@forEach
            val half = (fruitSize * fruit.type.sizeMultiplier) / 2f
            val destRect = RectF(fruit.x - half, fruit.y - half, fruit.x + half, fruit.y + half)
            if (fruit.type == FruitType.BOMB) {
                val frameWidth = bitmap.width / bombFrameCount
                val currentFrame = ((timeNow / bombFrameDurationMs) % bombFrameCount).toInt()
                bombSrcRect.set(
                    currentFrame * frameWidth,
                    0,
                    (currentFrame + 1) * frameWidth,
                    bitmap.height
                )
                canvas.drawBitmap(bitmap, bombSrcRect, destRect, null)
            } else {
                canvas.drawBitmap(bitmap, null, destRect, null)
            }
        }
    }

    private fun drawBlade(canvas: Canvas) {
        val localTrail = synchronized(trailLock) {
            if (trail.size < 2) return
            trail.toList()
        }
        val maxAge = 0.18f
        val maxWidth = 18f
        val n = localTrail.size

        if (n * 4 > leftVertices.size) {
            leftVertices = FloatArray(n * 8)
            leftColors = IntArray(n * 4)
            rightVertices = FloatArray(n * 8)
            rightColors = IntArray(n * 4)
        }

        fun drawMesh(radiusMult: Float, r: Int, g: Int, b: Int, alphaScale: Float) {
            for (i in 0 until n) {
                val p = localTrail[i]
                val age = p.age.coerceIn(0f, maxAge)
                val linearT = 1f - (age / maxAge)
                val t = Math.pow(linearT.toDouble(), 1.45).toFloat()
                val radius = maxWidth * radiusMult * t

                val dx: Float;
                val dy: Float
                if (i == 0) {
                    dx = localTrail[i + 1].x - p.x
                    dy = localTrail[i + 1].y - p.y
                } else if (i == n - 1) {
                    dx = p.x - localTrail[i - 1].x
                    dy = p.y - localTrail[i - 1].y
                } else {
                    dx = localTrail[i + 1].x - localTrail[i - 1].x
                    dy = localTrail[i + 1].y - localTrail[i - 1].y
                }
                val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val nx = if (len > 0) -dy / len else 0f
                val ny = if (len > 0) dx / len else 0f

                val lx = p.x + nx * radius
                val ly = p.y + ny * radius
                val rx = p.x - nx * radius
                val ry = p.y - ny * radius

                val cAlpha = (linearT * 255f * alphaScale).toInt().coerceIn(0, 255)
                val color = Color.argb(cAlpha, r, g, b)

                leftVertices[i * 4 + 0] = lx
                leftVertices[i * 4 + 1] = ly
                leftColors[i * 2 + 0] = color

                leftVertices[i * 4 + 2] = p.x
                leftVertices[i * 4 + 3] = p.y
                leftColors[i * 2 + 1] = color

                rightVertices[i * 4 + 0] = p.x
                rightVertices[i * 4 + 1] = p.y
                rightColors[i * 2 + 0] = color

                rightVertices[i * 4 + 2] = rx
                rightVertices[i * 4 + 3] = ry
                rightColors[i * 2 + 1] = color
            }

            canvas.drawVertices(
                Canvas.VertexMode.TRIANGLE_STRIP,
                n * 2,
                leftVertices,
                0,
                null,
                0,
                leftColors,
                0,
                null,
                0,
                0,
                bladeVertexPaint
            )
            canvas.drawVertices(
                Canvas.VertexMode.TRIANGLE_STRIP,
                n * 2,
                rightVertices,
                0,
                null,
                0,
                rightColors,
                0,
                null,
                0,
                0,
                bladeVertexPaint
            )
        }

        drawMesh(1.0f, 118, 229, 255, 0.38f)
        drawMesh(0.62f, 215, 248, 255, 0.78f)
        drawMesh(0.22f, 255, 255, 255, 1f)

        for (i in 0 until n - 1) {
            val start = localTrail[i]
            val end = localTrail[i + 1]
            val age = ((start.age + end.age) * 0.5f).coerceIn(0f, maxAge)
            val linearT = 1f - (age / maxAge)
            val distance = kotlin.math.hypot(end.x - start.x, end.y - start.y)
            val speedBoost = (distance / 70f).coerceIn(0.85f, 1.35f)
            val glowWidth = (maxWidth * 0.95f * linearT * speedBoost).coerceAtLeast(1.5f)

            bladeGlowPaint.strokeWidth = glowWidth
            bladeGlowPaint.color = Color.argb(
                (linearT * 78f).toInt().coerceIn(0, 255),
                125,
                231,
                255
            )
            canvas.drawLine(start.x, start.y, end.x, end.y, bladeGlowPaint)

            bladeSparkPaint.strokeWidth = (glowWidth * 0.38f).coerceAtLeast(1.2f)
            bladeSparkPaint.color = Color.argb(
                (linearT * 168f).toInt().coerceIn(0, 255),
                215,
                245,
                255
            )
            canvas.drawLine(start.x, start.y, end.x, end.y, bladeSparkPaint)

            bladeCorePaint.strokeWidth = (glowWidth * 0.16f).coerceAtLeast(1.6f)
            bladeCorePaint.color = Color.argb(
                (linearT * 255f).toInt().coerceIn(0, 255),
                255,
                255,
                255
            )
            canvas.drawLine(start.x, start.y, end.x, end.y, bladeCorePaint)
        }

        val head = localTrail.last()
        val headT = 1f - (head.age.coerceIn(0f, maxAge) / maxAge)
        val headRadius = maxWidth * Math.pow(headT.toDouble(), 1.5).toFloat()
        if (headRadius > 0.1f) {
            val alpha = (headT * 255).toInt().coerceIn(0, 255)
            headGlowPaint.color =
                Color.argb((alpha * 0.55f).toInt().coerceIn(0, 255), 130, 233, 255)
            canvas.drawCircle(head.x, head.y, headRadius * 1.1f, headGlowPaint)
            headCorePaint.color = Color.argb(alpha, 255, 255, 255)
            canvas.drawCircle(head.x, head.y, headRadius * 0.34f, headCorePaint)
        }

        if (n >= 2) {
            val prev = localTrail[n - 2]
            val dx = head.x - prev.x
            val dy = head.y - prev.y
            val length = kotlin.math.hypot(dx, dy)
            if (length > 0.1f) {
                val ux = dx / length
                val uy = dy / length
                val tipX = head.x + ux * maxWidth * 1.45f
                val tipY = head.y + uy * maxWidth * 1.45f

                bladeSparkPaint.strokeWidth = 6f
                bladeSparkPaint.color = Color.argb(172, 175, 239, 255)
                canvas.drawLine(head.x, head.y, tipX, tipY, bladeSparkPaint)

                bladeCorePaint.strokeWidth = 2.2f
                bladeCorePaint.color = Color.argb(245, 255, 255, 255)
                canvas.drawLine(head.x, head.y, tipX, tipY, bladeCorePaint)
            }
        }
    }

    private fun drawJuiceSplashes(canvas: Canvas) {
        juiceSplashes.forEach { splash ->

            splashPaint.color = splash.color
            splashPaint.alpha = splash.alpha
            splashPaint.style = Paint.Style.FILL
            canvas.drawCircle(splash.x, splash.y, splash.radius, splashPaint)

            splashPaint.style = Paint.Style.STROKE
            splashPaint.strokeWidth = 4f
            splashPaint.alpha = (splash.alpha * 0.6f).toInt()
            canvas.drawCircle(splash.x, splash.y, splash.radius * 1.2f, splashPaint)
        }
    }

    private fun drawScoreFloats(canvas: Canvas) {
        scoreFloats.forEach { sf ->
            scoreTextPaint.alpha = sf.alpha
            canvas.drawText(sf.text, sf.startX, sf.currentY, scoreTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                synchronized(trailLock) {
                    trail.clear()
                    appendTrailPointLocked(event.x, event.y)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                synchronized(trailLock) {
                    val historySize = event.historySize
                    for (i in 0 until historySize) {
                        val hx = event.getHistoricalX(i)
                        val hy = event.getHistoricalY(i)
                        appendTrailPointLocked(hx, hy)
                    }
                    appendTrailPointLocked(event.x, event.y)
                }
                checkSlices()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

            }
        }
        return true
    }

    private fun appendTrailPointLocked(x: Float, y: Float) {
        val last = trail.lastOrNull()
        if (last == null) {
            trail.addLast(TrailPoint(x, y))
            return
        }

        val dx = x - last.x
        val dy = y - last.y
        val distance = kotlin.math.hypot(dx, dy)
        if (distance < 4f) return

        val steps = (distance / 18f).toInt().coerceAtLeast(1)
        for (step in 1..steps) {
            val t = step / steps.toFloat()
            trail.addLast(
                TrailPoint(
                    x = last.x + dx * t,
                    y = last.y + dy * t
                )
            )
        }
    }

    private fun checkSlices() {
        val fruits = spawner?.activeFruits ?: return
        val localTrail = synchronized(trailLock) { trail.toList() }
        when (val hitResult = SliceDetector.resolveTrailHit(localTrail, fruits)) {
            null -> return
            is SliceDetector.SliceHitResult.Bomb -> {
                val fruit = hitResult.fruit
                fruit.isSliced = true
                fruit.isAlive = false
                fruit.sliceTime = System.currentTimeMillis()
                triggerBombFeedback(fruit.x, fruit.y)
                triggerBombSliceEffect(fruit.x, fruit.y)
                onBombSwiped?.invoke()
            }

            is SliceDetector.SliceHitResult.Fruits -> {
                hitResult.fruits.forEach { fruit ->
                    fruit.isSliced = true
                    fruit.isAlive = false
                    fruit.sliceTime = System.currentTimeMillis()
                    juiceSplashes.add(JuiceSplash(fruit.x, fruit.y, fruit.type.juiceColor))
                    scoreFloats.add(
                        ScoreFloat(
                            fruit.x,
                            fruit.y - fruitSize / 2f,
                            "+${fruit.type.points}"
                        )
                    )
                    onFruitSliced?.invoke(fruit.type, fruit.x, fruit.y)
                }
            }
        }
    }

    private fun triggerBombFeedback(x: Float, y: Float) {
        bombFeedbackTime = bombFeedbackDuration
        bombImpactX = x
        bombImpactY = y
    }

    private fun triggerBombSliceEffect(x: Float, y: Float) {
        bombSliceEffects.add(BombSliceEffect(x = x, y = y))
    }

    fun setRound(round: Int) {
        val resId = app.krafted.fruitslicer.R.drawable.bg_round_1
        if (resId == backgroundResId && backgroundBitmap != null) return
        backgroundResId = resId
        if (screenWidth > 0) loadBackground(resId)
    }

    fun applyDifficulty(
        speedMultiplier: Float,
        bombWeight: Int,
        maxOnScreen: Int,
        spawnIntervalMs: Long
    ) {
        this.speedMultiplier = speedMultiplier
        this.bombWeight = bombWeight
        this.maxOnScreen = maxOnScreen
        this.spawnIntervalMs = spawnIntervalMs
        spawner?.speedMultiplier = speedMultiplier
        spawner?.bombWeight = bombWeight
        spawner?.maxOnScreen = maxOnScreen
        spawner?.spawnIntervalMs = spawnIntervalMs
    }

    fun clearFruits() {
        spawner?.clearAll()
    }
}
