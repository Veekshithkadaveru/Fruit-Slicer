package app.krafted.fruitslicer.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

data class TrailPoint(val x: Float, val y: Float, var age: Float = 0f)

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
    private var backgroundResId = app.krafted.fruitslicer.R.drawable.bg_round_1

    private val trailLock = Any()
    private val trail = ArrayDeque<TrailPoint>()
    private val juiceSplashes = mutableListOf<JuiceSplash>()
    private val scoreFloats = mutableListOf<ScoreFloat>()

    
    private val bladeVertexPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val headPaint = Paint().apply {
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

                synchronized(trailLock) {
            trail.forEach { it.age += deltaTime }
            while (trail.isNotEmpty() && trail.first().age > 0.25f) {
                trail.removeFirst()
            }
        }

        juiceSplashes.forEach { it.update(deltaTime) }
        juiceSplashes.removeAll { !it.isAlive }
        scoreFloats.forEach { it.update(deltaTime) }
        scoreFloats.removeAll { !it.isAlive }
    }

    private fun drawFrame(canvas: Canvas) {
        val bg = backgroundBitmap
        if (bg != null) {
            canvas.drawBitmap(bg, 0f, 0f, null)
        } else {
            canvas.drawColor(Color.BLACK)
        }
        drawFruits(canvas)
        drawJuiceSplashes(canvas)
        drawScoreFloats(canvas)
        drawBlade(canvas)
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

    private fun drawBlade(canvas: Canvas) {
        val localTrail = synchronized(trailLock) {
            if (trail.size < 2) return
            trail.toList()
        }
        val maxAge = 0.25f
        val maxWidth = 22f
        val n = localTrail.size

        if (n * 4 > leftVertices.size) {
            leftVertices = FloatArray(n * 8)
            leftColors = IntArray(n * 4)
            rightVertices = FloatArray(n * 8)
            rightColors = IntArray(n * 4)
        }

        fun drawMesh(radiusMult: Float, r: Int, g: Int, b: Int) {
            for (i in 0 until n) {
                val p = localTrail[i]
                val age = p.age.coerceIn(0f, maxAge)
                val linearT = 1f - (age / maxAge)
                val t = Math.pow(linearT.toDouble(), 1.2).toFloat()
                val radius = maxWidth * radiusMult * t

                val dx: Float; val dy: Float
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

                val cAlpha = (linearT * 255).toInt().coerceIn(0, 255)
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

            canvas.drawVertices(Canvas.VertexMode.TRIANGLE_STRIP, n * 2, leftVertices, 0, null, 0, leftColors, 0, null, 0, 0, bladeVertexPaint)
            canvas.drawVertices(Canvas.VertexMode.TRIANGLE_STRIP, n * 2, rightVertices, 0, null, 0, rightColors, 0, null, 0, 0, bladeVertexPaint)
        }

        drawMesh(1.0f, 0, 191, 255)
        drawMesh(0.5f, 255, 255, 255)

        val head = localTrail.last()
        val headT = 1f - (head.age.coerceIn(0f, maxAge) / maxAge)
        val headRadius = maxWidth * Math.pow(headT.toDouble(), 1.2).toFloat()
        if (headRadius > 0.1f) {
            val alpha = (headT * 255).toInt().coerceIn(0, 255)
            headPaint.color = Color.argb(alpha, 0, 191, 255)
            canvas.drawCircle(head.x, head.y, headRadius, headPaint)
            headPaint.color = Color.argb(alpha, 255, 255, 255)
            canvas.drawCircle(head.x, head.y, headRadius * 0.5f, headPaint)
        }
    }

    private fun drawJuiceSplashes(canvas: Canvas) {
        juiceSplashes.forEach { splash ->
            // Expanding ring
            splashPaint.color = splash.color
            splashPaint.alpha = splash.alpha
            splashPaint.style = Paint.Style.FILL
            canvas.drawCircle(splash.x, splash.y, splash.radius, splashPaint)

            // Outer ring stroke
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
                    trail.addLast(TrailPoint(event.x, event.y))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                synchronized(trailLock) {
                    val historySize = event.historySize
                    for (i in 0 until historySize) {
                        val hx = event.getHistoricalX(i)
                        val hy = event.getHistoricalY(i)
                        trail.addLast(TrailPoint(hx, hy))
                    }
                    trail.addLast(TrailPoint(event.x, event.y))
                }
                checkSlices()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Do not clear. Let the blade fade out on its own.
            }
        }
        return true
    }

    private fun checkSlices() {
        val fruits = spawner?.activeFruits ?: return
        val localTrail = synchronized(trailLock) { trail.toList() }
        val hit = SliceDetector.checkTrail(localTrail, fruits)
        hit.forEach { fruit ->
            fruit.isSliced = true
            fruit.isAlive = false
            fruit.sliceTime = System.currentTimeMillis()
            if (fruit.type == FruitType.BOMB) {
                onBombSwiped?.invoke()
            } else {
                juiceSplashes.add(JuiceSplash(fruit.x, fruit.y, fruit.type.juiceColor))
                scoreFloats.add(ScoreFloat(fruit.x, fruit.y - fruitSize / 2f, "+${fruit.type.points}"))
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
        if (resId == backgroundResId) return
        backgroundResId = resId
        if (screenWidth > 0) loadBackground(resId)
    }

    fun applyDifficulty(speedMultiplier: Float, bombWeight: Int, maxOnScreen: Int, spawnIntervalMs: Long) {
        spawner?.speedMultiplier = speedMultiplier
        spawner?.bombWeight = bombWeight
        spawner?.maxOnScreen = maxOnScreen
        spawner?.spawnIntervalMs = spawnIntervalMs
    }

    fun clearFruits() { spawner?.clearAll() }
}
