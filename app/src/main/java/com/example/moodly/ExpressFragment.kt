package com.example.moodly

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale


class ExpressFragment : Fragment(R.layout.fragment_express) {

    private var photoUri: Uri? = null
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_GALLERY_PICK = 2
    private val REQUEST_RECORD_AUDIO = 1001
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var latitude: Double? = null
    private var longitude: Double? = null

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

    // 위치 권한 요청 함수
    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // 권한 요청 이유를 사용자에게 설명하는 스낵바
            Snackbar.make(
                requireView(),
                "This app requires location access to save your diary.",
                Snackbar.LENGTH_LONG
            ).setAction("OK") {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }.show()
        } else {
            // 권한 요청
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // 권한 요청 결과 처리
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(
                    requireView(),
                    "Location permission granted.",
                    Snackbar.LENGTH_SHORT
                ).show()
                getLastKnownLocation()
            } else {
                Snackbar.make(
                    requireView(),
                    "Location permission denied. Please enable it in settings.",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Settings") {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", requireContext().packageName, null)
                        }
                        startActivity(intent)
                    }
                    .show()
            }
        }
    }

    // 위치 정보 가져오기
    private fun getLastKnownLocation() {
        if (!isGooglePlayServicesAvailable() || !isLocationServiceEnabled()) {
            Log.e("Location", "Google Play Services or Location Services are unavailable.")
            return
        }

        if (isLocationPermissionGranted()) {
            // 위치 권한 확인
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // 위치 권한이 부여된 경우
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            latitude = location.latitude
                            longitude = location.longitude
                            Log.d("Location", "Latitude: $latitude, Longitude: $longitude")
                        } else {
                            Log.e("Location", "No last known location. Requesting updates...")
                            requestLocationUpdates(fusedLocationClient)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Location", "Failed to get last location: ${exception.message}")
                    }
            } else {
                // 권한이 없는 경우, 권한 요청
                requestLocationPermission()
            }
    }
        }


    // 위치 정보 업데이트 요청
    // 위치 정보 업데이트 요청
    private fun requestLocationUpdates(fusedLocationClient: FusedLocationProviderClient) {
        // 위치 권한 확인
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 100_000
            ).setMinUpdateIntervalMillis(50_000)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val locations = locationResult.locations
                    if (locations.isNotEmpty()) {
                        val location = locations[0]
                        latitude = location.latitude
                        longitude = location.longitude
                        Log.d("Location", "Updated Latitude: $latitude, Longitude: $longitude")
                    } else {
                        Log.e("Location", "No location result.")
                    }
                }
            }

            // 위치 업데이트 요청
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            // 권한이 없는 경우, 권한 요청
            requestLocationPermission()
        }
    }

    // 위치 권한 확인
    private fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 위치 서비스 활성화 확인
    private fun isLocationServiceEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.let {
            it.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    it.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } ?: false
    }

    // Google Play 서비스 확인
    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val result = googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (result != ConnectionResult.SUCCESS) {
            Log.e("Location", "Google Play Services not available. Error code: $result")
            return false
        }
        return true
    }


    // 체크박스에서 선택된 감정 반환
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


        // 감정 체크박스 설정
        val checkBoxes = listOf(
            feelingHappy, feelingExcited, feelingSad, feelingSoso, feelingAngry, feelingTired
        )

        for (checkBox in checkBoxes) {
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // 다른 모든 체크박스를 해제
                    for (otherCheckBox in checkBoxes) {
                        if (otherCheckBox != checkBox) {
                            otherCheckBox.isChecked = false
                        }
                    }
                }
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
        val saveButton: Button = view.findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            try {
            // EditText에서 다이어리 내용과 제목 가져오기
            val diaryTitle = titleEditText.text.toString().trim()  // 제목 입력 EditText
            val diaryContent = textEditText.text.toString().trim()  // 내용 입력 EditText
            val diaryEmotion = emotionTextView.text.toString() // 감정 TextView

            // 감정 emotion ID 설정
            val emotionID = when (diaryEmotion) {
                "Happy" -> 1
                "Excited" -> 2
                "Soso" -> 3
                "Sad" -> 4
                "Angry" -> 5
                "Tired" -> 6
                else -> 0 // 감정이 설정되지 않았거나 알 수 없는 경우
            }

            val drawable = photoImageView.drawable
            val bitmap = try {
                (drawable as? BitmapDrawable)?.bitmap // drawable을 Bitmap으로 변환
            } catch (e: Exception) {
                Log.e("BitmapError", "Failed to convert drawable to bitmap", e)
                null
            }

            // 제목과 내용이 비어있는지 확인
            if (diaryTitle.isEmpty() || diaryContent.isEmpty()) {
                Snackbar.make(
                    requireView(),
                    "Title and content cannot be empty!",
                    Snackbar.LENGTH_SHORT
                ).setAction("OK") { }.show()
                return@setOnClickListener // 제목과 내용이 비어있으면 여기서 동작 중단
            }


            // 위치 권한 확인 및 요청

            if (!isLocationPermissionGranted()) {
                requestLocationPermission() // 위치 권한 요청
                Snackbar.make(
                    requireView(),
                    "Location permission is required to save the diary.",
                    Snackbar.LENGTH_SHORT
                ).setAction("OK") { }.show()
                Log.e("Location", "Location permission not enabled//1125")
                return@setOnClickListener // 위치 권한 없으면 중단

            }


            // 위치 서비스 활성화 확인
                // 위치 서비스 활성화 확인
                if (!isLocationServiceEnabled()) {
                    Snackbar.make(
                        requireView(),
                        "Location services are disabled. Please enable them.",
                        Snackbar.LENGTH_LONG
                    ).setAction("Settings") {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    }.show()
                    return@setOnClickListener
                } else {
                    // Google Play 서비스가 없으면 사용자에게 알림
                    if (!isGooglePlayServicesAvailable()) {
                        Snackbar.make(
                            view,
                            "Google Play Services is required for this feature.",
                            Snackbar.LENGTH_LONG
                        ).setAction("Install") {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms")
                                setPackage("com.android.chrome") // Chrome으로 열기
                            }
                            startActivity(intent)
                        }.show()
                    }
                }


            // 위도와 경도 기본값 설정 (null 안전)
            val currentLatitude = try {
                latitude ?: 0.0
            } catch (e: Exception) {
                Log.e("LatitudeError", "Failed to retrieve latitude", e)
                0.0
            }

            val currentLongitude = try {
                longitude ?: 0.0
            } catch (e: Exception) {
                Log.e("LongitudeError", "Failed to retrieve longitude", e)
                0.0
            }

            // 로그 출력으로 위치 정보 확인
            Log.d("LocationCheck", "Retrieved location - Latitude: $currentLatitude, Longitude: $currentLongitude")

                // 로그 출력
            // 스낵바 표시
            if (currentLatitude == null || currentLongitude == null) {
                Snackbar.make(
                    requireView(),
                    "Location not available.",
                    Snackbar.LENGTH_SHORT
                ).setAction("OK") { }.show()

                // 로그로 알림
                Log.w("LocationCheck", "Location was null. Default values applied.")
            } else {
                // 로그로 성공적으로 위치 정보가 들어갔음을 알림
                Log.i(
                    "LocationCheck",
                    "Location successfully retrieved: Latitude: $currentLatitude, Longitude: $currentLongitude"
                )
            }

            // 현재 시간 저장
            val currentTime = System.currentTimeMillis()

            // Bundle로 다이어리 제목과 내용을 전달
            val bundle = try {
                Bundle().apply {
                    putString("diaryTitle", diaryTitle)
                    putString("diaryContent", diaryContent)
                    putParcelable("diaryImage", bitmap)
                    putString("diaryEmotion", diaryEmotion)
                    putInt("diaryEmotionID", emotionID)
                    putDouble("diaryLatitude", currentLatitude)
                    putDouble("diaryLongitude", currentLongitude)
                    putLong("timestamp", currentTime) // 현재 시간을 밀리초 단위로 저장
                }
            } catch (e: Exception) {
                Log.e("BundleError", "Failed to create bundle", e)
                null
            }

                // 진동 발생
                try {
                    val vibrator =
                        requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    500,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(500)
                        }
                        Log.d("Vibrator", "Success!!!")

                    } else {
                        Log.e(
                            "VibratorError",
                            "Vibrator service is not available or device has no vibrator"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("VibratorError", "Error while triggering vibration", e)
                }

            // SavedDiaryFragment로 이동
            if (bundle != null) {
                try {
                    findNavController().navigate(
                        R.id.action_expressFragment_to_savedDiaryFragment,
                        bundle
                    )
                    Log.d("Navigation", "Navigate to SavedDiaryFragment")
                    Snackbar.make(requireView(), "Diary saved successfully!", Snackbar.LENGTH_SHORT)
                        .setAction("OK") { }.show()
                } catch (e: Exception) {
                    Log.e("NavigationError", "Failed to navigate to SavedDiaryFragment", e)
                }
            }

        } catch (e: Exception) {
            Log.e("SaveDiaryError", "An error occurred while saving the diary", e)
            Snackbar.make(
                requireView(),
                "An error occurred while saving the diary. Please try again.",
                Snackbar.LENGTH_SHORT
            ).setAction("OK") { }.show()
        }
    }
        return view
    }




}
