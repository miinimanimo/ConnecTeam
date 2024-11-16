package com.example.moodly

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.moodly.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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

        // calendarHeader 클릭 시 월과 연도를 선택하는 다이얼로그 표시
        binding.calendarHeader.setOnClickListener {
            showMonthYearPickerDialog()
        }

        // 달력 초기화
        setupCalendar()
    }

    private fun setCalendarHeader() {
        // 현재 날짜를 "일 월 년" 형식으로 표시
        val currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
        binding.calendarHeader.text = currentDate
    }

    private fun showMonthYearPickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        // DatePickerDialog 생성
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, _ ->
                // 선택된 월과 연도를 기반으로 CalendarView 업데이트
                updateCalendarWithSelectedDate(selectedYear, selectedMonth)
            },
            year, month, 1 // day는 임의로 설정
        )

        // 날짜(day) 부분 숨기기
        datePickerDialog.datePicker.findViewById<View>(
            resources.getIdentifier("day", "id", "android")
        )?.visibility = View.GONE

        datePickerDialog.show()
    }

    private fun updateCalendarWithSelectedDate(year: Int, month: Int) {
        // CalendarView의 날짜를 업데이트
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1) // 월의 첫 날로 설정
        binding.calendarView.date = calendar.timeInMillis
    }

    private fun setupCalendar() {
        // 날짜 클릭 이벤트 처리
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            displayDiaryEntriesForDate(selectedDate)
        }
    }

    private fun displayDiaryEntriesForDate(date: LocalDate) {
        // 선택한 날짜에 대한 일기 목록을 가져와서 화면에 표시
        binding.diaryEntries.text = "Entries for ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.year}"
        // 실제 데이터베이스나 API에서 해당 날짜의 일기 데이터를 가져와야 함
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
