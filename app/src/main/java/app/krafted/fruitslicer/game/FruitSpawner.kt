package app.krafted.fruitslicer.game

import kotlin.random.Random

class FruitSpawner(var screenWidth: Int, var screenHeight: Int) {
    private val _activeFruits = mutableListOf<FruitObject>()
    private val lock = Any()
    val activeFruits: List<FruitObject> get() = synchronized(lock) { _activeFruits.toList() }

    private var nextId = 0
    private var lastSpawnTime = 0L
    var spawnIntervalMs = 1200L
    var speedMultiplier = 1.0f
    var bombWeight = 0
    var maxOnScreen = 2
    var fruitRadius = 80f

    fun update() {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            if (now - lastSpawnTime > spawnIntervalMs &&
                _activeFruits.count { it.isAlive } < maxOnScreen
            ) {
                spawnFruit()
                lastSpawnTime = now
            }
        }
    }

    private fun spawnFruit() {
        val type = weightedRandom()
        val startX = screenWidth * (0.20f + Random.nextFloat() * 0.60f)
        val startY = screenHeight + fruitRadius + 20f
        val velX = Random.nextFloat() * 240f - 120f
        val velY = -(2400f + Random.nextFloat() * 400f)
        _activeFruits.add(
            FruitObject(
                nextId++,
                type,
                startX,
                startY,
                velX,
                velY,
                radius = fruitRadius * type.sizeMultiplier
            )
        )
    }

    private fun weightedRandom(): FruitType {
        val weights = FruitType.entries.map { if (it == FruitType.BOMB) bombWeight else it.baseWeight }
        val total = weights.sum()
        if (total <= 0) return FruitType.STRAWBERRY
        var roll = Random.nextInt(total)
        for ((i, type) in FruitType.entries.withIndex()) {
            roll -= weights[i]
            if (roll < 0) return type
        }
        return FruitType.STRAWBERRY
    }

    fun removeDeadFruits() { synchronized(lock) { _activeFruits.removeAll { !it.isAlive } } }
    fun clearAll() { synchronized(lock) { _activeFruits.clear() } }
}
