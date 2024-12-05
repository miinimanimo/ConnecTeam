package com.example.moodly
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface MainApiService {
    // 달력 관련 API
    @GET("main/diaries/{year}/{month}/")
    fun getDiariesForMonth(
        @Path("year") year: Int,
        @Path("month") month: Int
    ): Call<DaysResponse>

    @GET("main/diaries/{year}/{month}/{day}/")
    fun getDiariesForDay(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Path("day") day: Int
    ): Call<List<DayDiary>>

    //추천 관련
    @GET("main/books/")  // 슬래시 추가 또는 실제 서버 경로로 수정
    fun getBooks(): Call<List<Book>>

    @GET("main/youtube-videos/")  // 슬래시 추가 또는 실제 서버 경로로 수정
    fun getYoutubeVideos(): Call<List<YoutubeVideo>>

    // 일기 생성 API
    @Multipart
    @POST("main/diaries/create/")
    fun saveDiaryEntry(
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("emotion_category") emotionCategory: RequestBody,
        @Part image: MultipartBody.Part?,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("brightness") brightness: RequestBody
    ): Call<DiaryEntry>
}

// 데이터 클래스들
data class DaysResponse(
    val days: List<Int>
)

data class DayDiary(
    val title: String,
    val emotion_categories: List<EmotionCategory>
)

data class EmotionCategory(
    val id: Int,
    val name: String
)

data class Book(
    val rank: Int,
    val title: String,
    val bookClass: String
)

data class YoutubeVideo(
    val title: String,
    val link: String,
    val emotionCategory: Int
)

data class DiaryEntry(
    val title: String,
    val content: String,
    val emotion_category: Int,
    val image: String?,
    val latitude: Double,
    val longitude: Double,
    val brightness: Int
)