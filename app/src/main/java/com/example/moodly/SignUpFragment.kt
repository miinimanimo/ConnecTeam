package com.example.moodly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.example.moodly.databinding.FragmentSignUpBinding
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

        setupSpinners()
        setupSignUpButton()
    }

    private fun setupSpinners() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val yearRange = (currentYear downTo currentYear - 100).map { it.toString() }
        val monthRange = (1..12).map { it.toString() }
        val dayRange = (1..31).map { it.toString() }

        setupSpinner(binding.yearSpinner, yearRange, "Year")
        setupSpinner(binding.monthSpinner, monthRange, "Month")
        setupSpinner(binding.daySpinner, dayRange, "Date")
    }

    private fun setupSpinner(spinner: Spinner, data: List<String>, defaultText: String) {
        val items = listOf(defaultText) + data
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(0) // 기본값 표시
    }

    private fun setupSignUpButton() {
        binding.signUpButton.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = binding.nameEditText.text.toString()
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val passwordCheck = binding.passwordCheckEditText.text.toString()
        val phoneNumber = binding.phoneNumEditText.text.toString()
        val dateOfBirth = "${binding.yearSpinner.selectedItem}-${binding.monthSpinner.selectedItem}-${binding.daySpinner.selectedItem}"

        // 성별 선택 처리
        val selectedGenderId = binding.genderRadioGroup.checkedRadioButtonId
        val gender = when (selectedGenderId) {
            binding.radioMale.id -> "M"
            binding.radioFemale.id -> "F"
            else -> "N" // 선택하지 않은 경우
        }

        // 비밀번호와 비밀번호 확인 비교
        if (password != passwordCheck) {
            Snackbar.make(binding.root, "PasswordCheck is not correct.", Snackbar.LENGTH_SHORT).show()
            return // 비밀번호가 다르면 함수 종료
        }

        // 회원가입 데이터 준비
        val signUpData = SignUpData(
            email = email,
            password = password,
            date_of_birth = dateOfBirth,
            nameKorea = name,
            gender = gender,
            phonenumber = phoneNumber
        )

        // API 호출
        val authApiService = RetrofitClient.getAuthApiService(requireContext())
        authApiService.register(signUpData).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                if (response.isSuccessful) {
                    Snackbar.make(binding.root, "Signup Success.", Snackbar.LENGTH_SHORT).show()
                    navigateToLoginActivity() // 로그인 페이지로 이동
                } else {
                    Snackbar.make(binding.root, "Signup Fail.", Snackbar.LENGTH_SHORT).show()
                    Log.e("SignUp", "회원가입 실패: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                Log.e("SignUp", "요청 실패: ${t.message}")
            }
        })
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish() // 현재 Fragment가 포함된 Activity 종료
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
