package com.example.xposuredetectorsmart.ui.camera

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Lightweight camera-screen state holder. The actual capture pipeline (image processing, dose
 * calculation, persistence) lives in the nav-graph-scoped [com.example.xposuredetectorsmart.viewmodel.DoseAnalysisViewModel]
 * so results survive the Camera -> Results navigation.
 */
@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    fun onCaptureStarted() {
        _isCapturing.value = true
    }

    fun onCaptureFinished() {
        _isCapturing.value = false
    }
}
