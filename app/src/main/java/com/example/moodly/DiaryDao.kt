package com.example.moodly

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DiaryDao {
    @Insert
    fun insert(diary: Diary)

    @Query("SELECT * FROM diary_table ORDER BY timestamp DESC")
    fun getAllDiaries(): List<Diary>

    @Query("SELECT COUNT(*) FROM diary_table")
    fun getDiaryCount(): Int

    @Query("SELECT emotion, COUNT(*) as count FROM diary_table GROUP BY emotion")
    fun getEmotionCounts(): List<EmotionCount>
}
data class EmotionCount(
    val emotion: String,
    val count: Int
)
