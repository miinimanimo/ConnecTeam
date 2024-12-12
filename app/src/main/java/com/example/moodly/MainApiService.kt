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

    //인스타 피드 리턴
    @GET("main/feed/")
    fun getFeed(): Call<DiaryResponse>
    //인스타 좋아요 개수
    @GET("main/diary/{diaryId}/like/count/")
    fun getLikeCount(@Path("diaryId") diaryId: Int): Call<LikeCountResponse>

    @POST("main/diary/{diaryId}/like/")
    fun toggleLike(@Path("diaryId") diaryId: Int): Call<LikeResponse>

    @GET("main/diary/{diaryId}/like/status/")
    fun getLikeStatus(@Path("diaryId") diaryId: Int): Call<LikeStatusResponse>

    @GET("main/diaries/{diaryId}/")
    fun getDiaryDetails(@Path("diaryId") diaryId: Int): Call<DiaryEntry>

}
// 데이터 클래스들
data class DaysResponse(
    val days: List<Int>
)

data class DayDiary(
    val id: Int,
    val title: String,
    val emotion_category: Int?, // 단일 감정으로 변경
    val emotion_categories: List<EmotionCategory>? = null
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
// DiaryEntry 데이터 클래스 수정
data class DiaryEntry(
    val id: Int,
    val user: Int,// 일기 고유 ID
    val title: String,          // 일기 제목
    val content: String,        // 일기 내용
    val emotion_category: Int,  // 감정 카테고리
    val image: String?,         // base64로 인코딩된 이미지
    val latitude: Double,       // 위도
    val longitude: Double,      // 경도
    val brightness: Int,        // 밝기
    val created_at: String,      // 작성 시간
    val updated_at: String,
)

data class LikeResponse(
    val message: String,
    val data: LikeData
)

data class LikeData(
    val diary: Int,
    val user: Int,
    val count: Int
)

data class LikeStatusResponse(
    val user_liked: Boolean
)
data class LikeCountResponse(
    val likes_count: Int
)
data class DiaryResponse(
    val diaries: List<DiaryEntry>
)