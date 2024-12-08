package com.example.moodly

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodly.databinding.FragmentHomeBinding
import com.example.moodly.databinding.ItemRecommendationBinding
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val diaryAdapter = DiaryAdapter()
    private val recommendationAdapter = RecommendationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Title image 설정
        binding.titleImage.setImageResource(R.drawable.title)

        // 현재 날짜를 설정
        setCalendarHeader()

        // Express 버튼 클릭 시 이동
        binding.expressButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_expressFragment)
        }

        // 달력 초기화
        setupCalendar()

        // 메뉴 버튼 클릭 이벤트 추가
        binding.menuButton.setOnClickListener {
            showPopupMenu(it)
        }

        // RecyclerView 설정
        setupRecyclerView()

        // 추천 RecyclerView 설정
        setupRecommendationRecyclerView()

        // 추천 데이터 로드
        loadRecommendations()

    }

    private fun setupRecommendationRecyclerView() {
        binding.recommendationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendationAdapter
        }
    }

    private fun loadRecommendations() {
        val mainApiService = RetrofitClient.getMainApiService(requireContext())
        var loadedBooks: List<Book>? = null
        var loadedVideos: List<YoutubeVideo>? = null

        // 책 데이터 로드
        mainApiService.getBooks()
            .enqueue(object : Callback<List<Book>> {
                override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                    if (response.isSuccessful) {
                        loadedBooks = response.body()
                        if (loadedVideos != null) {
                            recommendationAdapter.submitData(loadedBooks ?: listOf(), loadedVideos!!)
                        }
                    }
                }

                override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                    Log.e("Books", "Failed to fetch books", t)
                }
            })

        // 유튜브 비디오 데이터 로드
        mainApiService.getYoutubeVideos()
            .enqueue(object : Callback<List<YoutubeVideo>> {
                override fun onResponse(call: Call<List<YoutubeVideo>>, response: Response<List<YoutubeVideo>>) {
                    if (response.isSuccessful) {
                        loadedVideos = response.body()
                        if (loadedBooks != null) {
                            recommendationAdapter.submitData(loadedBooks!!, loadedVideos ?: listOf())
                        }
                    }
                }

                override fun onFailure(call: Call<List<YoutubeVideo>>, t: Throwable) {
                    Log.e("Videos", "Failed to fetch videos", t)
                }
            })
    }

    private fun setupRecyclerView() {
        binding.diaryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = diaryAdapter
        }
    }

    private fun setCalendarHeader() {
        val currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
        binding.calendarHeader.text = currentDate
    }

    private fun setupCalendar() {
        // 현재 날짜로 초기화할 때는 Calendar가 0-based이므로 +1 필요
        val currentDate = Calendar.getInstance()
        fetchDiariesForMonth(
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH) + 1
        )

        // MaterialCalendarView에서 받은 month에는 +1할 필요 없음 (이미 보정되어 있음)
        binding.calendarView.setOnMonthChangedListener { widget, date ->
            fetchDiariesForMonth(date.year, date.month)
        }

        binding.calendarView.setOnDateChangedListener { widget, date, selected ->
            if (selected) {
                fetchDiariesForDay(date.year, date.month, date.day)
            }
        }
    }

    private fun fetchDiariesForMonth(year: Int, month: Int) {
        // MainApiService를 가져와서 사용
        val mainApiService = RetrofitClient.getMainApiService(requireContext())

        mainApiService.getDiariesForMonth(year, month)
            .enqueue(object : Callback<DaysResponse> {
                override fun onResponse(
                    call: Call<DaysResponse>,
                    response: Response<DaysResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { daysResponse ->
                            highlightDiaryDates(year, month, daysResponse.days)
                        }
                    }
                }

                override fun onFailure(call: Call<DaysResponse>, t: Throwable) {
                    Log.e("Calendar", "Failed to fetch diary dates", t)
                }
            })
    }

    private fun fetchDiariesForDay(year: Int, month: Int, day: Int) {
        // MainApiService를 가져와서 사용
        val mainApiService = RetrofitClient.getMainApiService(requireContext())

        mainApiService.getDiariesForDay(year, month, day)
            .enqueue(object : Callback<List<DayDiary>> {
                override fun onResponse(
                    call: Call<List<DayDiary>>,
                    response: Response<List<DayDiary>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { diaries ->
                            binding.contentCard.visibility = View.VISIBLE
                            if (diaries.isEmpty()) {
                                // 일기가 없는 경우
                                binding.emptyDiaryText.visibility = View.VISIBLE
                                binding.diaryRecyclerView.visibility = View.GONE
                            } else {
                                // 일기가 있는 경우
                                binding.emptyDiaryText.visibility = View.GONE
                                binding.diaryRecyclerView.visibility = View.VISIBLE
                                diaryAdapter.updateDiaries(diaries)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<List<DayDiary>>, t: Throwable) {
                    Log.e("Diary", "Failed to fetch diaries", t)
                    binding.contentCard.visibility = View.VISIBLE
                    binding.emptyDiaryText.text = "Failed to load diaries"
                    binding.emptyDiaryText.visibility = View.VISIBLE
                    binding.diaryRecyclerView.visibility = View.GONE
                }
            })
    }

    private fun highlightDiaryDates(year: Int, month: Int, days: List<Int>) {
        binding.calendarView.removeDecorators()

        val decorator = object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay): Boolean {
                return day.year == year &&
                        day.month == month - 1 && // MaterialCalendarView는 0-based month를 사용
                        days.contains(day.day)
            }

            override fun decorate(view: DayViewFacade) {
                view.addSpan(DotSpan(5f, resources.getColor(R.color.purple_500)))
            }
        }

        binding.calendarView.addDecorator(decorator)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showPopupMenu(anchor: View) {
        val popupMenu = PopupMenu(requireContext(), anchor)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_profile -> {
                    val navController = findNavController()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, true)
                        .setLaunchSingleTop(true)
                        .build()
                    navController.navigate(R.id.action_homeFragment_to_profileFragment, null, navOptions)
                    true
                }
                R.id.menu_logout -> {
                    findNavController().navigate(R.id.action_homeFragment_to_loginActivity)
                    Toast.makeText(requireContext(), "Logged out.", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_contact -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("회사 Contact")
                        .setMessage("contact@company.com")
                        .setPositiveButton("확인", null)
                        .show()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    inner class DiaryAdapter : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {
        private var diaries: List<DayDiary> = listOf()

        inner class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleText: TextView = itemView.findViewById(R.id.titleText)
            val emotionText: TextView = itemView.findViewById(R.id.emotionText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_diary, parent, false)
            return DiaryViewHolder(view)
        }

        override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
            val diary = diaries[position]
            holder.titleText.text = diary.title
            val emoticons = diary.emotion_categories.map { emotion ->
                when (emotion.name) {
                    "Happy" -> "😊"
                    "Excited" -> "😎"
                    "Soso" -> "😐"
                    "Sad" -> "😕"
                    "Angry" -> "😠"
                    "Tired" -> "😪"
                    else -> ""
                }
            }
            holder.emotionText.text = emoticons.joinToString(" ")
        }

        override fun getItemCount() = diaries.size

        fun updateDiaries(newDiaries: List<DayDiary>) {
            diaries = newDiaries
            notifyDataSetChanged()
        }
    }

    class RecommendationAdapter : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {
        private val items = mutableListOf<Any>()

        inner class ViewHolder(private val binding: ItemRecommendationBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: Any) {
                when (item) {
                    is Pair<*, *> -> { // 음악 추천
                        val video = item.second as YoutubeVideo
                        val emotion = item.first as Int  // 하드코딩된 감정 번호

                        val (emotionText, emoji) = when (emotion) {
                            1 -> "Happy" to "Happy 😊"
                            2 -> "Excited" to "Excited 😎"
                            3 -> "Soso" to "Soso 😐"
                            4 -> "Sad" to "Sad 😕"
                            5 -> "Angry" to "Angry 😠"
                            6 -> "Tired" to "Tired 😪"
                            else -> "Unknown" to "🎵"
                        }
                        binding.headerText.text = "$emoji"
                        binding.titleText.text = "How about this music?"
                        binding.subtitleText.text = "Listen to Music"
                        binding.subtitleText.visibility = View.VISIBLE
                        binding.root.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.link))
                            itemView.context.startActivity(intent)
                        }
                    }
                    is Book -> { // 책 추천
                        binding.headerText.text = "How about this book?"
                        binding.titleText.text = item.title
                        binding.subtitleText.text = "Find Book"
                        binding.subtitleText.visibility = View.VISIBLE
                        binding.root.setOnClickListener {
                            val url = "https://www.google.com/search?q=${item.title}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            itemView.context.startActivity(intent)
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemRecommendationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        fun submitData(books: List<Book>, videos: List<YoutubeVideo>) {
            items.clear()
            // 각 감정별로 하나씩
            for (i in 1..6) {
                val video = videos[i-1]  // 비디오는 그대로 사용
                // emotionCategory를 1부터 6까지 순서대로 지정
                items.add(Pair(i, video))

                val bookIndex = (i-1) % books.size
                val book = books[bookIndex]
                items.add(book)
            }
            notifyDataSetChanged()
        }
    }
}