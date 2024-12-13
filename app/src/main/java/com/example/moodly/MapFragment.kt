package com.example.moodly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.moodly.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        // SupportMapFragment를 가져와서 비동기로 맵을 준비
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    // GoogleMap이 준비되면 호출되는 콜백 메서드
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 다이어리 위치를 가져옴
        fetchDiaryLocations()
    }

    private fun fetchDiaryLocations() {
        val apiService = RetrofitClient.getMainApiService(requireContext())
        apiService.getDiaryLocations().enqueue(object : Callback<List<DiaryLocation>> {
            override fun onResponse(call: Call<List<DiaryLocation>>, response: Response<List<DiaryLocation>>) {
                if (response.isSuccessful) {
                    response.body()?.let { locations ->
                        if (locations.isNotEmpty()) {
                            // 첫 번째 위치를 기준으로 카메라 이동
                            val firstLocation = locations[0]
                            val latLng = LatLng(firstLocation.latitude.toDouble(), firstLocation.longitude.toDouble())
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f)) // 줌 레벨 15로 설정

                            // 마커 추가
                            for (location in locations) {
                                val markerLatLng = LatLng(location.latitude.toDouble(), location.longitude.toDouble())
                                mMap.addMarker(MarkerOptions().position(markerLatLng).title(location.title))
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch locations", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<DiaryLocation>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
