package com.custom.galaxylauncher.repository

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlashlightManager(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    init {
        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    break
                }
            }
            if (cameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                cameraId = cameraManager.cameraIdList[0]
            }

            cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(camId: String, enabled: Boolean) {
                    if (camId == cameraId) {
                        _isTorchOn.value = enabled
                    }
                }

                override fun onTorchModeUnavailable(camId: String) {
                    if (camId == cameraId) {
                        _isTorchOn.value = false
                    }
                }
            }, null)
        } catch (e: Exception) {
            Log.e("FlashlightManager", "Error initializing flashlight manager", e)
        }
    }

    fun toggleTorch(): Boolean {
        val targetId = cameraId ?: return false
        return try {
            val newState = !_isTorchOn.value
            cameraManager.setTorchMode(targetId, newState)
            _isTorchOn.value = newState
            true
        } catch (e: Exception) {
            Log.e("FlashlightManager", "Failed to toggle torch mode", e)
            false
        }
    }

    fun setTorch(on: Boolean): Boolean {
        val targetId = cameraId ?: return false
        return try {
            cameraManager.setTorchMode(targetId, on)
            _isTorchOn.value = on
            true
        } catch (e: Exception) {
            Log.e("FlashlightManager", "Failed to set torch mode", e)
            false
        }
    }
}
