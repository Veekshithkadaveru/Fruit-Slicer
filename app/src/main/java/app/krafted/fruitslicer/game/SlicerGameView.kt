package app.krafted.fruitslicer.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt
import kotlin.random.Random

class SlicerGameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    var onFruitSliced: ((FruitType, Float, Float) -> Unit)? = null
    var onFruitMissed: (() -> Unit)? = null
    var onBombSwiped: (() -> Unit)? = null

    private var gameThread: GameThread? = null
    private var spawner: FruitSpawner? = null
    private val bitmaps = mutableMapOf<FruitType, Bitmap>()
    private var backgroundBitmap: Bitmap? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var fruitSize = 0f
    private var backgroundResId = 0

    // Trail - accessed from both main thread (write) and game thread (read)
    private val trailLock = Any()
    private val trail = ArrayDeque<Pair<Float, Float>>()

    // Animation lists (CopyOnWriteArrayList for thread-safe concurrent access)
    private val slicedHalves = CopyOnWriteArrayList<SlicedHalf>()
    private val juiceSplashes = CopyOnWriteArrayList<JuiceSplash>()
    private val scoreFloats = CopyOnWriteArrayList<ScoreFloat>()
    private val sliceFlashes = CopyOnWriteArrayList<SliceFlash>()

    // Outer glow layer - wide, soft, semi-transparent
    private val trailGlowPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Inner bright core of the trail
    private val trailCorePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Yellow-white hot center (like Fruit Ninja's blade edge)
    private val trailHotPaint = Paint().apply {
        color = 0xFFFFF9C4.toInt()
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val particlePaint = Paint().apply { isAntiAlias = true }

    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 64f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        setShadowLayer(6f, 2f, 2f, Color.BLACK)
    }

    private val flashPaint = Paint().apply { isAntiAlias = true }
    private val halfPaint = Paint().apply { isAntiAlias = true }

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
        val newFruitSize = (width * 0.15f).toInt().coerceAtLeast(60).toFloat()
        if (newFruitSize != fruitSize) {
            fruitSize = newFruitSize
            loadBitmaps(fruitSize.toInt())
        }
        if (backgroundResId != 0) loadBackground(backgroundResId)
        if (spawner == null) {
            spawner = FruitSpawner(screenWidth, screenHeight).also {
                it.fruitRadius = fruitSize / 2f
            }
        } else {
            spawner?.screenWidth = screenWidth
            spawner?.screenHeight = screenHeight
            spawner?.fruitRadius = fruitSize / 2f
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
        FruitType.entries.forEach { type ->
            bitmaps[type]?.recycle()
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, type.drawableRes, opts)
            opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, targetSize, targetSize)
            opts.inJustDecodeBounds = false
            val raw = BitmapFactory.decodeResource(context.resources, type.drawableRes, opts)
            if (raw != null) {
                val scaled = Bitmap.createScaledBitmap(raw, targetSize, targetSize, true)
                bitmaps[type] = scaled
                if (raw != scaled) raw.recycle()
            }
        }
    }

    private fun loadBackground(resId: Int) {
        backgroundBitmap?.recycle()
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, opts)
        opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, screenWidth, screenHeight)
        opts.inJustDecodeBounds = false
        val raw = BitmapFactory.decodeResource(context.resources, resId, opts)
        if (raw != null) {
            backgroundBitmap = Bitmap.createScaledBitmap(raw, screenWidth, screenHeight, true)
            if (raw != backgroundBitmap) raw.recycle()
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
        bitmaps.values.forEach { it.recycle() }
        bitmaps.clear()
        backgroundBitmap?.recycle()
        backgroundBitmap = null
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

        slicedHalves.forEach { it.update(deltaTime) }
        slicedHalves.removeIf { !it.isAlive }

        juiceSplashes.forEach { it.update(deltaTime) }
        juiceSplashes.removeIf { !it.isAlive }

        scoreFloats.forEach { it.update(deltaTime) }
        scoreFloats.removeIf { !it.isAlive }

        sliceFlashes.forEach { it.update(deltaTime) }
        sliceFlashes.removeIf { !it.isAlive }
    }

    private fun drawFrame(canvas: Canvas) {
        val bg = backgroundBitmap
        if (bg != null) {
            canvas.drawBitmap(bg, 0f, 0f, null)
        } else {
            canvas.drawColor(Color.BLACK)
        }
        drawFruits(canvas)
        drawSlicedHalves(canvas)
        drawJuiceSplashes(canvas)
        drawSliceFlashes(canvas)
        drawScoreFloats(canvas)
        drawTrail(canvas)
    }

    private fun drawFruits(canvas: Canvas) {
        spawner?.activeFruits?.forEach { fruit ->
            if (!fruit.isAlive || fruit.isSliced) return@forEach
            val bitmap = bitmaps[fruit.type] ?: return@forEach
            val half = fruitSize / 2f
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(fruit.x - half, fruit.y - half, fruit.x + half, fruit.y + half),
                null
            )
        }
    }

    private fun drawSlicedHalves(canvas: Canvas) {
        slicedHalves.forEach { half ->
            val bitmap = bitmaps[half.fruitType] ?: return@forEach
            val sz = half.size
            halfPaint.alpha = half.alpha
            canvas.save()
            canvas.translate(half.x, half.y)
            canvas.rotate(half.rotation)
            if (half.isTopHalf) {
                canvas.clipRect(-sz / 2f, -sz / 2f, sz / 2f, 0f)
            } else {
                canvas.clipRect(-sz / 2f, 0f, sz / 2f, sz / 2f)
            }
            canvas.drawBitmap(bitmap, null, RectF(-sz / 2f, -sz / 2f, sz / 2f, sz / 2f), halfPaint)
            canvas.restore()
        }
    }

    private fun drawJuiceSplashes(canvas: Canvas) {
        juiceSplashes.forEach { splash ->
            splash.particles.forEach { p ->
                if (p.alpha <= 0) return@forEach
                particlePaint.color = p.pColor
                particlePaint.alpha = p.alpha
                canvas.drawCircle(p.px, p.py, p.radius, particlePaint)
            }
        }
    }

    private fun drawSliceFlashes(canvas: Canvas) {
        sliceFlashes.forEach { flash ->
            val alpha = ((1f - flash.progress) * 210f).toInt()
            if (alpha <= 0) return@forEach
            // Outer soft burst
            flashPaint.color = Color.WHITE
            flashPaint.alpha = (alpha * 0.4f).toInt()
            canvas.drawCircle(flash.x, flash.y, 20f + flash.progress * 80f, flashPaint)
            // Inner bright core
            flashPaint.alpha = alpha
            canvas.drawCircle(flash.x, flash.y, 8f + flash.progress * 30f, flashPaint)
        }
    }

    private fun drawScoreFloats(canvas: Canvas) {
        scoreFloats.forEach { sf ->
            if (sf.alpha <= 0) return@forEach
            scorePaint.alpha = sf.alpha
            canvas.drawText(sf.text, sf.startX, sf.currentY, scorePaint)
        }
    }

    /**
     * Draws a Fruit Ninja-style blade trail: wide soft glow that tapers from tail to tip,
     * with a bright hot-white/yellow inner core and a glowing tip circle.
     */
    private fun drawTrail(canvas: Canvas) {
        val points = synchronized(trailLock) { trail.toList() }
        if (points.size < 2) return
        val n = points.size

        for (i in 0 until n - 1) {
            // segProgress: 0 at trail tail, 1 at the leading tip
            val segProgress = (i + 1).toFloat() / n

            // Layer 1: outer soft glow (wide, low alpha, warm white)
            trailGlowPaint.strokeWidth = 6f + segProgress * 36f
            trailGlowPaint.alpha = (segProgress * 70).toInt()
            canvas.drawLine(
                points[i].first, points[i].second,
                points[i + 1].first, points[i + 1].second,
                trailGlowPaint
            )

            // Layer 2: mid core (medium, higher alpha, pure white)
            trailCorePaint.strokeWidth = 3f + segProgress * 18f
            trailCorePaint.alpha = (segProgress * 180).toInt()
            canvas.drawLine(
                points[i].first, points[i].second,
                points[i + 1].first, points[i + 1].second,
                trailCorePaint
            )

            // Layer 3: hot edge (thin, bright yellow-white, near tip only)
            if (segProgress > 0.5f) {
                val hotProgress = (segProgress - 0.5f) * 2f
                trailHotPaint.strokeWidth = 1.5f + hotProgress * 6f
                trailHotPaint.alpha = (hotProgress * 220).toInt()
                canvas.drawLine(
                    points[i].first, points[i].second,
                    points[i + 1].first, points[i + 1].second,
                    trailHotPaint
                )
            }
        }

        // Bright tip circle at the leading point
        val tip = points.last()
        trailCorePaint.style = Paint.Style.FILL
        trailCorePaint.alpha = 255
        canvas.drawCircle(tip.first, tip.second, 9f, trailCorePaint)
        trailCorePaint.style = Paint.Style.STROKE

        // Outer glow halo on tip
        trailGlowPaint.style = Paint.Style.FILL
        trailGlowPaint.alpha = 80
        canvas.drawCircle(tip.first, tip.second, 18f, trailGlowPaint)
        trailGlowPaint.style = Paint.Style.STROKE
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                synchronized(trailLock) {
                    trail.clear()
                    trail.addLast(Pair(event.x, event.y))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                synchronized(trailLock) {
                    trail.addLast(Pair(event.x, event.y))
                    if (trail.size > 16) trail.removeFirst()
                }
                checkSlices()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                synchronized(trailLock) { trail.clear() }
            }
        }
        return true
    }

    private fun checkSlices() {
        val fruits = spawner?.activeFruits ?: return
        val trailSnapshot = synchronized(trailLock) { trail.toList() }
        val hit = SliceDetector.checkTrail(trailSnapshot, fruits)
        if (hit.isEmpty()) return

        // Derive swipe direction from the last two trail points
        val swipeDirX: Float
        val swipeDirY: Float
        if (trailSnapshot.size >= 2) {
            val dx = trailSnapshot.last().first - trailSnapshot[trailSnapshot.size - 2].first
            val dy = trailSnapshot.last().second - trailSnapshot[trailSnapshot.size - 2].second
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
            swipeDirX = dx / len
            swipeDirY = dy / len
        } else {
            swipeDirX = 1f
            swipeDirY = 0f
        }
        // Perpendicular to swipe — halves fly in this direction
        val perpX = -swipeDirY
        val perpY = swipeDirX

        hit.forEach { fruit ->
            fruit.isSliced = true
            fruit.isAlive = false
            fruit.sliceTime = System.currentTimeMillis()

            // Slice flash at impact point
            sliceFlashes.add(SliceFlash(fruit.x, fruit.y))

            if (fruit.type == FruitType.BOMB) {
                onBombSwiped?.invoke()
            } else {
                // Juice particle spray
                juiceSplashes.add(JuiceSplash(fruit.x, fruit.y, fruit.type.juiceColor))

                // Two halves flying apart perpendicular to the blade
                val halfSpeed = 200f + Random.nextFloat() * 200f
                val rotSpeed = (180f + Random.nextFloat() * 360f) * if (Random.nextBoolean()) 1 else -1

                slicedHalves.add(
                    SlicedHalf(
                        fruitType = fruit.type,
                        x = fruit.x, y = fruit.y,
                        velX = perpX * halfSpeed + fruit.velX * 0.3f,
                        velY = perpY * halfSpeed + fruit.velY * 0.3f - 150f,
                        rotation = 0f,
                        rotationSpeed = rotSpeed,
                        isTopHalf = true,
                        size = fruitSize
                    )
                )
                slicedHalves.add(
                    SlicedHalf(
                        fruitType = fruit.type,
                        x = fruit.x, y = fruit.y,
                        velX = -perpX * halfSpeed + fruit.velX * 0.3f,
                        velY = -perpY * halfSpeed + fruit.velY * 0.3f - 150f,
                        rotation = 0f,
                        rotationSpeed = -rotSpeed,
                        isTopHalf = false,
                        size = fruitSize
                    )
                )

                // Floating score text
                scoreFloats.add(ScoreFloat(fruit.x, fruit.y, "+${fruit.type.points}"))

                onFruitSliced?.invoke(fruit.type, fruit.x, fruit.y)
            }
        }
    }

    fun setRound(round: Int) {
        val resId = when {
            round <= 2 -> app.krafted.fruitslicer.R.drawable.bg_round_1
            round == 3 -> app.krafted.fruitslicer.R.drawable.bg_round_2
            round == 4 -> app.krafted.fruitslicer.R.drawable.bg_round_3
            round == 5 -> app.krafted.fruitslicer.R.drawable.bg_round_4
            else       -> app.krafted.fruitslicer.R.drawable.bg_round_5
        }
        backgroundResId = resId
        if (screenWidth > 0) loadBackground(resId)
    }

    fun applyDifficulty(speedMultiplier: Float, bombWeight: Int, maxOnScreen: Int, spawnIntervalMs: Long) {
        spawner?.speedMultiplier = speedMultiplier
        spawner?.bombWeight = bombWeight
        spawner?.maxOnScreen = maxOnScreen
        spawner?.spawnIntervalMs = spawnIntervalMs
    }

    fun clearFruits() {
        spawner?.clearAll()
        slicedHalves.clear()
        juiceSplashes.clear()
        scoreFloats.clear()
        sliceFlashes.clear()
    }
}
