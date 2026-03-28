package app.krafted.fruitslicer.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

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

    private val trail = ArrayDeque<Pair<Float, Float>>()

    private val trailPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
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
    }

    private fun drawFrame(canvas: Canvas) {
        val bg = backgroundBitmap
        if (bg != null) {
            canvas.drawBitmap(bg, 0f, 0f, null)
        } else {
            canvas.drawColor(Color.BLACK)
        }
        drawFruits(canvas)
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

    private fun drawTrail(canvas: Canvas) {
        if (trail.size < 2) return
        val path = Path()
        path.moveTo(trail.first().first, trail.first().second)
        trail.drop(1).forEach { path.lineTo(it.first, it.second) }
        canvas.drawPath(path, trailPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                trail.clear()
                trail.addLast(Pair(event.x, event.y))
            }
            MotionEvent.ACTION_MOVE -> {
                trail.addLast(Pair(event.x, event.y))
                if (trail.size > 8) trail.removeFirst()
                checkSlices()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> trail.clear()
        }
        return true
    }

    private fun checkSlices() {
        val fruits = spawner?.activeFruits ?: return
        val hit = SliceDetector.checkTrail(trail, fruits)
        hit.forEach { fruit ->
            fruit.isSliced = true
            fruit.isAlive = false
            fruit.sliceTime = System.currentTimeMillis()
            if (fruit.type == FruitType.BOMB) {
                onBombSwiped?.invoke()
            } else {
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

    fun clearFruits() { spawner?.clearAll() }
}
