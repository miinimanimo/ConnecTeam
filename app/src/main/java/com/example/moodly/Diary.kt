package com.example.moodly

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_table")
data class Diary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val emotion: String,
    val emotion_id: Int,
    val content: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val image: String // Base64 이미지 문자열
)
