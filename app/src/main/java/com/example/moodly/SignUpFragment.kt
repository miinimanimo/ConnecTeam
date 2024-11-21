package com.example.moodly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.example.moodly.databinding.FragmentSignUpBinding
import java.util.*

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSignUpBinding.bind(view)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val yearRange = (currentYear downTo currentYear - 100).map { it.toString() }
        val monthRange = (1..12).map { it.toString() }
        val dayRange = (1..31).map { it.toString() }

        // 각각의 스피너에 대해 defaultText를 전달하여 초기값을 설정
        setupSpinner(binding.yearSpinner, yearRange, "Year")
        setupSpinner(binding.monthSpinner, monthRange, "Month")
        setupSpinner(binding.daySpinner, dayRange, "Date")
    }

    private fun setupSpinner(spinner: Spinner, data: List<String>, defaultText: String) {
        // 기본값을 표시할 항목 추가
        val items = listOf(defaultText) + data
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.setSelection(0) // 처음에 기본값 표시
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}