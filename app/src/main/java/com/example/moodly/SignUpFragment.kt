package com.example.moodly

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.moodly.databinding.FragmentSignUpBinding
import java.util.*

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private var selectedYear = 2024
    private var selectedMonth = 1
    private var selectedDay = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 년, 월, 일 선택 버튼 클릭 리스너 설정
        binding.yearButton.setOnClickListener { showYearPicker() }
        binding.monthButton.setOnClickListener { showMonthPicker() }
        binding.dayButton.setOnClickListener { showDayPicker() }
    }

    private fun showYearPicker() {
        val years = (1900..2024).toList()
        val yearList = years.map { it.toString() }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Year")
            .setItems(yearList) { _, which ->
                selectedYear = years[which]
                binding.yearButton.text = selectedYear.toString()
            }
            .show()
    }

    private fun showMonthPicker() {
        val months = (1..12).toList()
        val monthList = months.map { it.toString() }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Month")
            .setItems(monthList) { _, which ->
                selectedMonth = months[which]
                binding.monthButton.text = selectedMonth.toString()
            }
            .show()
    }

    private fun showDayPicker() {
        val days = (1..31).toList()
        val dayList = days.map { it.toString() }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Day")
            .setItems(dayList) { _, which ->
                selectedDay = days[which]
                binding.dayButton.text = selectedDay.toString()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}