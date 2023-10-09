package com.example.weatherappviews

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.example.weatherappviews.databinding.ActivityMainBinding
import com.example.weatherappviews.location.ForegroundLocationService
import com.example.weatherappviews.location.SharedPreferenceUtil
import com.example.weatherappviews.state.UiState
import com.example.weatherappviews.utils.toast
import com.example.weatherappviews.view.WeatherViewModel

private const val TAG = "MainActivity"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity() {

    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundLocationService: ForegroundLocationService? = null

    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private lateinit var foregroundBroadcastReceiver: ForegroundBroadcastReceiver

    private lateinit var sharedPreferences: SharedPreferences

    private val foregroundServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            val binder = service as ForegroundLocationService.LocalBinder
            foregroundLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setLiveDataListeners()
        setViewListeners()
        initWeatherInfoFromSharedPreferences()

        foregroundBroadcastReceiver = ForegroundBroadcastReceiver()

        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val enabled = sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
        if (enabled) {
            Log.d(TAG, "foregroundLocationService unsubscribeToLocationUpdates")
            foregroundLocationService?.unsubscribeToLocationUpdates()
        } else {
            val hasExistingLocation = SharedPreferenceUtil.getLastWeatherInfo(this) != null
            if (foregroundPermissionApproved() && !hasExistingLocation) {
                Log.d(TAG, "foregroundLocationService subscribeToLocationUpdates")
                foregroundLocationService?.subscribeToLocationUpdates()
                    ?: Log.d(TAG, "Service Not Bound")
            } else {
                requestForegroundPermissions()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val serviceIntent = Intent(this, ForegroundLocationService::class.java)
        bindService(serviceIntent, foregroundServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundBroadcastReceiver,
            IntentFilter(
                ForegroundLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST
            )
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            foregroundBroadcastReceiver
        )
        super.onPause()
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            unbindService(foregroundServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }

        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionsResult $requestCode")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() -> Log.d(TAG, "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Permission was granted.
                    Log.d(TAG, "PackageManager.PERMISSION_GRANTED subscribeToLocationUpdates")
                    foregroundLocationService?.subscribeToLocationUpdates()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setLiveDataListeners() {
        viewModel.uiState().observe(this, Observer { uiState ->
            if (uiState != null) {
                render(uiState)
            }
        })
    }

    private fun setViewListeners() {
        binding.searchWeather.setOnClickListener {
            val city = binding.searchEditText.text.toString()
            if (city.isNotEmpty()) {
                viewModel.getWeatherInfoForCity(city)
            }
        }
    }

    private fun initWeatherInfoFromSharedPreferences() {
        viewModel.getWeatherInfoFromPreferences()
    }

    private fun render(uiState: UiState) {
        when (uiState) {
            is UiState.Loading -> {
                onLoad()
            }
            is UiState.Success -> {
                onSuccess(uiState)
            }
            is UiState.Error -> {
                onError(uiState)
            }
        }
    }

    private fun onLoad() = with(binding) {
        progressBar.visibility = View.VISIBLE
        searchWeather.isEnabled = false
    }

    private fun onSuccess(uiState: UiState.Success) = with(binding) {
        progressBar.visibility = View.GONE
        searchWeather.isEnabled = true

        val weatherContent = uiState.weatherContent
        if (!weatherInfoContainer.root.isVisible) {
            weatherInfoContainer.root.isVisible = true
        }
        weatherInfoContainer.apply {
            cityAndCountry.text = weatherContent.cityAndCountry
            temperatureText.text = weatherContent.temperatureInFahrenheit
            humidityText.text = weatherContent.humidity
            pressureText.text = weatherContent.pressure
            Glide.with(this@MainActivity).load(weatherContent.weatherIconUrl).into(weatherInfoIcon)
        }
    }

    private fun onError(uiState: UiState.Error) = with(binding) {
        progressBar.visibility = View.GONE
        searchWeather.isEnabled = true
        toast(uiState.message)
    }

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestForegroundPermissions() {
        val provideRationale = foregroundPermissionApproved()
        if (!provideRationale) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private inner class ForegroundBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                ForegroundLocationService.EXTRA_LOCATION
            )

            Log.d(TAG, "[ForegroundBroadcastReceiver] foregroundLocationService unsubscribeToLocationUpdates")
            foregroundLocationService?.unsubscribeToLocationUpdates()

            if (location != null) {
                Log.d(TAG, "Foreground location: $location")
                viewModel.getWeatherInfoByCoordinate(location.latitude, location.longitude)
            }
        }
    }
}