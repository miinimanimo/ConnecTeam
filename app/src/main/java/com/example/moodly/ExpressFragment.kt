package com.example.moodly

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.fragment.findNavController
import java.util.Locale

class ExpressFragment : Fragment(R.layout.fragment_express) {

    private var photoUri: Uri? = null
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_GALLERY_PICK = 2
    private val REQUEST_RECORD_AUDIO = 1001

    private lateinit var backButton: ImageButton
    private lateinit var titleEditText: EditText
    private lateinit var imageView: ImageView

    // private lateinit var dateEditText: EditText
    private lateinit var textEditText: EditText
    private lateinit var insertPhotoButton: Button
    private lateinit var photoViewLayout: LinearLayout
    private lateinit var photoImageView: ImageView
    private lateinit var recordButton: ImageButton

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent

    // 체크박스 뷰 변수들
    private lateinit var emotionTextView: TextView
    private lateinit var feelingHappy: CheckBox
    private lateinit var feelingExcited: CheckBox
    private lateinit var feelingSoso: CheckBox
    private lateinit var feelingSad: CheckBox
    private lateinit var feelingAngry: CheckBox
    private lateinit var feelingTired: CheckBox

    private var isRecording = false


    // 변수: 이미지 선택을 위한 Intent
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                photoViewLayout.visibility = View.VISIBLE
                photoImageView.setImageURI(uri)
            }
        }

    // 갤러리에서 이미지 선택
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            photoUri = uri
            imageView.setImageURI(photoUri)
            photoViewLayout.visibility = View.VISIBLE
        }

    // 사진 촬영 후 이미지 URI 처리
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageView.setImageURI(photoUri)
                photoViewLayout.visibility = View.VISIBLE

                // URI를 Bitmap으로 변환하여 Bundle에 전달
                val bitmap = getBitmapFromUri(photoUri)
            }
        }

    // 카메라 권한 요청
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_express, container, false)

        // 뷰 요소들 찾기
        backButton = view.findViewById(R.id.myVectorImageView)
        titleEditText = view.findViewById(R.id.titleEditText)
        // dateEditText = view.findViewById(R.id.dateEditText)

        textEditText = view.findViewById(R.id.textEditText)
        imageView = view.findViewById(R.id.photoImageView)
        insertPhotoButton = view.findViewById(R.id.insertPhotoButton)
        photoViewLayout = view.findViewById(R.id.photoViewLayout)
        photoImageView = view.findViewById(R.id.photoImageView)
        recordButton = view.findViewById(R.id.recordButton)

        // 감정 체크박스 설정
        emotionTextView = view.findViewById(R.id.emotionTextView)
        feelingHappy = view.findViewById(R.id.feelingHappy)
        feelingExcited = view.findViewById(R.id.feelingExcited)
        feelingSoso = view.findViewById(R.id.feelingSoso)
        feelingSad = view.findViewById(R.id.feelingSad)
        feelingAngry = view.findViewById(R.id.feelingAngry)
        feelingTired = view.findViewById(R.id.feelingTired)

        // 뒤로 가기 버튼 클릭 리스너
        backButton.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        //음성 인식 버튼 클릭 리스너
        recordButton.setOnClickListener {
            if (isRecording) {
                // 음성 인식 중이면 중단
                stopSpeechRecognition()
                // 녹음 중지 메시지 Toast로 표시
                Toast.makeText(requireContext(), "Record stopped.", Toast.LENGTH_SHORT).show()
            } else {
                // 권한이 없으면 권한 요청
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestRecordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    // 권한이 이미 허용되었으면 음성 인식 시작
                    startSpeechRecognition()
                    // 녹음 시작 메시지 Toast로 표시
                    Toast.makeText(requireContext(), "Record started.", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // SpeechRecognizer 설정
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer.setRecognitionListener(recognitionListener)

        // 음성 인식 Intent 설정
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

//        // 날짜 선택 DatePickerDialog
//        dateEditText.setOnClickListener {
//            val calendar = Calendar.getInstance()
//            val year = calendar.get(Calendar.YEAR)
//            val month = calendar.get(Calendar.MONTH)
//            val day = calendar.get(Calendar.DAY_OF_MONTH)
//
//            val datePickerDialog = DatePickerDialog(requireActivity(), { _, selectedYear, selectedMonth, selectedDay ->
//                calendar.set(selectedYear, selectedMonth, selectedDay)
//                val dateFormat = SimpleDateFormat("MMM dd yyyy", Locale.getDefault())
//                dateEditText.setText(dateFormat.format(calendar.time))
//            }, year, month, day)
//
//            datePickerDialog.show()
//        }

        // 감정 체크박스 설정
        val checkBoxes = listOf(
            feelingHappy, feelingExcited, feelingSad, feelingSoso, feelingAngry, feelingTired
        )

        for (checkBox in checkBoxes) {
            checkBox.setOnCheckedChangeListener { _, _ ->
                // 선택된 감정을 emotionTextView에 반영
                emotionTextView.text = "In a ${getSelectedEmotion()} moment..."
            }
        }


        // 사진 추가 버튼
        insertPhotoButton.setOnClickListener {
            val options = arrayOf("Take a photo", "Choose from Gallery")
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Select an option")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()  // 카메라에서 찍기
                    1 -> chooseFromGallery()  // 갤러리에서 선택
                }
            }
            builder.show()
        }


        // 밝기 조정 SeekBar
        val brightnessSeekBar: SeekBar = view.findViewById(R.id.brightness_seekbar)
        val brightnessLabel: TextView = view.findViewById(R.id.brightness_label)

        val currentBrightness = Settings.System.getInt(
            requireContext().contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            125
        )
        brightnessSeekBar.progress = currentBrightness

        // 밝기 조절 권한 체크
        if (!Settings.System.canWrite(requireContext())) {
            // 권한 요청
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            Toast.makeText(requireContext(), "권한을 허용해야 밝기를 조절할 수 있습니다.", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        } else {
            setupBrightnessControl(brightnessSeekBar, brightnessLabel)
        }

        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setScreenBrightness(progress)

                val percentage = (progress.toFloat() / 255 * 100).toInt()
                brightnessLabel.text = "Mood Brightness: $percentage%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        // 저장 버튼
        // express 프레그먼트의 저장 버튼
        val saveButton: Button = view.findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            // EditText에서 다이어리 내용과 제목 가져오기
            val diaryTitle = titleEditText.text.toString()  // 제목 입력 EditText
            val diaryContent = textEditText.text.toString()  // 내용 입력 EditText
            val diaryEmotion = emotionTextView.text.toString() // 감정 TextView

            val drawable = photoImageView.drawable

            // drawable을 Bitmap으로 변환
            val bitmap = (drawable as? BitmapDrawable)?.bitmap


            // 제목과 내용이 비어있는지 확인
            if (diaryTitle.isEmpty() || diaryContent.isEmpty()) {
                Snackbar.make(
                    requireView(),
                    "Title and content cannot be empty!",
                    Snackbar.LENGTH_SHORT
                ).setAction("OK") {
                    // OK 버튼 클릭 시 동작 (필요 없으면 비워둠)
                }.show()
                return@setOnClickListener
            }

            // Bundle로 다이어리 제목과 내용을 전달
            val bundle = Bundle()
            bundle.putString("diaryTitle", diaryTitle)
            bundle.putString("diaryEmotion", diaryEmotion)
            bundle.putString("diaryContent", diaryContent)
            bundle.putParcelable("diaryImage", bitmap)

            // SavedDiaryFragment로 이동
            findNavController().navigate(R.id.action_expressFragment_to_savedDiaryFragment, bundle)

            // 스낵바 표시
            Snackbar.make(requireView(), "Diary saved successfully!", Snackbar.LENGTH_SHORT).setAction("OK"){
            }.show()
        }
        return view
    }


    // 선택된 감정 반환
    private fun getSelectedEmotion(): String {
        val selectedEmotions = mutableListOf<String>()

        if (feelingHappy.isChecked) selectedEmotions.add("Happy")
        if (feelingExcited.isChecked) selectedEmotions.add("Excited")
        if (feelingSoso.isChecked) selectedEmotions.add("Soso")
        if (feelingSad.isChecked) selectedEmotions.add("Sad")
        if (feelingAngry.isChecked) selectedEmotions.add("Angry")
        if (feelingTired.isChecked) selectedEmotions.add("Tired")

        return if (selectedEmotions.isNotEmpty()) {
            selectedEmotions.joinToString(", ") // 쉼표로 감정을 구분
        } else {
            "Neutral" // 아무것도 선택되지 않은 경우
        }
    }

    // 음성 인식 시작
    private fun startSpeechRecognition() {
        try {
            speechRecognizer.startListening(speechRecognizerIntent)
            isRecording = true
            Toast.makeText(requireContext(), "음성 인식중...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "음성 인식 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 음성 인식 중단
    private fun stopSpeechRecognition() {
        speechRecognizer.stopListening()
        isRecording = false
        recordButton.setImageResource(R.drawable.record_button)  // 기본 버튼으로 변경
    }

    // 음성 인식 권한 요청
    private val requestRecordAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startSpeechRecognition() // 권한 허용 시 음성 인식 시작
            } else {
                Toast.makeText(requireContext(), "Audio permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    // 음성 인식 리스너 설정
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            speechRecognizer.startListening(speechRecognizerIntent)
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            Toast.makeText(requireContext(), "음성 인식 오류: $error", Toast.LENGTH_SHORT).show()
        }

        override fun onResults(results: Bundle?) {
            results?.let {
                val result = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (result != null && result.isNotEmpty()) {
                    textEditText.setText(result[0]) // 음성 인식 결과를 EditText에 표시
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // 카메라 권한 요청 및 촬영
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            // 사진 촬영 전 URI 생성
            photoUri = createImageUri()
            if (photoUri != null) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "Failed to create image URI", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // 사진 촬영
    private fun takePhoto() {
        takePictureLauncher.launch(photoUri!!)
    }

    // 갤러리에서 사진 선택
    private fun chooseFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    // 임시 사진 URI 생성
    private fun createImageUri(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "MoodPhoto")
            put(MediaStore.Images.Media.DESCRIPTION, "Mood tracking photo")
        }
        return requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: run {
            Log.e("Camera", "Failed to create URI")
            null
        }
    }


    //밝기 설정 권한
    private fun requestWriteSettingsPermission() {
        if (!Settings.System.canWrite(requireContext())) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            Toast.makeText(requireContext(), "권한을 허용해야 밝기를 조절할 수 있습니다.", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        }
    }

    // 화면 밝기 설정
    private fun setupBrightnessControl(seekBar: SeekBar, label: TextView) {
        try {
            // 현재 밝기 가져오기
            val currentBrightness = Settings.System.getInt(
                requireContext().contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                125 // 기본값
            )
            seekBar.progress = currentBrightness

            // SeekBar 이벤트 리스너 설정
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    // 퍼센트 계산 및 레이블 업데이트
                    val percentage = (progress.toFloat() / 255 * 100).toInt()
                    label.text = "Mood Brightness: $percentage%"

                    // 밝기 설정
                    setScreenBrightness(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        } catch (e: Settings.SettingNotFoundException) {
            Toast.makeText(requireContext(), "현재 밝기를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setScreenBrightness(brightness: Int) {
        if (Settings.System.canWrite(requireContext())) {
            Settings.System.putInt(
                requireContext().contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightness
            )
        } else {
            requestWriteSettingsPermission()
        }
    }

    // URI로부터 Bitmap을 변환하는 함수
    private fun getBitmapFromUri(uri: Uri?): Bitmap? {
        uri?.let {
            val inputStream = requireContext().contentResolver.openInputStream(it)
            return BitmapFactory.decodeStream(inputStream)
        }
        return null
    }
}
