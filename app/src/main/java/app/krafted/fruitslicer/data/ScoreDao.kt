package app.krafted.fruitslicer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Insert
    suspend fun insert(score: ScoreEntity)

    @Query("SELECT MAX(score) FROM scores")
    fun getHighestScore(): Flow<Int?>
}
