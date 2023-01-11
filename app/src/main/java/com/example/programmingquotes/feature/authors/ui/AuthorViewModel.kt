package com.example.programmingquotes.feature.authors.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.viewModelScope
import com.example.programmingquotes.core.common.ResultWrapper
import com.example.programmingquotes.core.ui.BaseViewModel
import com.example.programmingquotes.feature.authors.domain.usecase.GetAuthorsUseCase
import com.example.programmingquotes.feature.authors.domain.usecase.GetRandomQuoteUseCase
import com.example.programmingquotes.feature.authors.domain.usecase.UpdateAuthorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AuthorViewModel @Inject constructor(
    private val getAuthorsUseCase: GetAuthorsUseCase,
    private val updateAuthorsUseCase: UpdateAuthorsUseCase,
    private val getRandomQuoteUseCase: GetRandomQuoteUseCase,
    private val sensorManager: SensorManager
) : BaseViewModel<AuthorViewState, AuthorAction>(AuthorViewState()) {

    private var isNextRequestReady = true

    private var sensorListener: SensorEventListener? = null

    init {
        getAuthors()

        onEachAction { action ->
            when (action) {
                is AuthorAction.RefreshAuthors -> {
                    updateAuthors()
                }
                is AuthorAction.StartSensorManager -> {
                    setUpSensorManager()
                }
                is AuthorAction.StopSensorManager -> {
                    stopSensorManager()
                }
            }
        }
    }

    private fun getAuthors() = getAuthorsUseCase()
        .onEach {
            if (it.isEmpty()) {
                updateAuthors()
            }
        }.execute {
            copy(authors = it)
        }


    private fun updateAuthors() = suspend {
        updateAuthorsUseCase()
    }.execute {
        copy(update = it)
    }


    private fun getRandomQuote() =
        getRandomQuoteUseCase().executeOnResultWrapper(Dispatchers.IO) {
            isNextRequestReady = true
            copy(bottomSheet = it)
        }


    //sensor
    private fun stopSensorManager() = viewModelScope.launch(Dispatchers.IO) {
        sensorManager.unregisterListener(sensorListener)
        setState { copy(bottomSheet = ResultWrapper.UnInitialize) }
    }

    private fun setUpSensorManager() {

        var acceleration = 0f
        var currentAcceleration = 0f
        var lastAcceleration: Float

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                lastAcceleration = currentAcceleration

                currentAcceleration = StrictMath.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta: Float = currentAcceleration - lastAcceleration
                acceleration = acceleration * 0.9f + delta

                if (acceleration > 12) {
                    if (isNextRequestReady) {
                        isNextRequestReady = false
                        getRandomQuote()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        sensorManager.registerListener(
            sensorListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
    }
}