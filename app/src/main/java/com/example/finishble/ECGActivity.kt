package com.example.finishble

import android.content.Context
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarEcgData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.util.EnumSet

class ECGActivity(private val context: Context, private val deviceId: String) {
    companion object {
        private const val TAG = "ECGActivity"
    }

    private var ecgDisposable: Disposable? = null

    private var polarApi: PolarBleApi = PolarBleApiDefaultImpl.defaultImplementation(context, EnumSet.of(
            PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
            PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
    ))

    init {
        polarApi.setApiCallback(object : PolarBleApiCallback() {
            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device connected: ${polarDeviceInfo.deviceId}")
                // This might be a good place to ensure permissions and other prerequisites are met
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device disconnected: ${polarDeviceInfo.deviceId}")
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                Log.d(TAG, "BLE SDK Feature ready: $feature for device $identifier")
                if (feature == PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING) {
                    // The Polar device is ready to stream data. Now is a good time to start ECG streaming.
                    startEcgStreaming()
                }
            }
        })
    }

    fun connectToDevice() {
        try {
            polarApi.connectToDevice(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Connecting to device failed: $e")
        }
    }
    private fun startEcgStreaming() {
        ecgDisposable?.dispose() // Dispose any existing subscription

        // Ensure DataCollectorCSV is ready for data collection
        val dataCollector = DataCollectorCSV.getInstance()
        // Optionally, call DataCollectorCSV.startDataCollection() if you have such a method to initialize or reset the data collection process

        ecgDisposable = polarApi.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ECG)
                .toFlowable()
                .flatMap { settings -> polarApi.startEcgStreaming(deviceId, settings.maxSettings()) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        // In the ECG data received subscription in ECGActivity
                        { ecgData: PolarEcgData ->
                            Log.d(TAG, "ECG data received: ${ecgData.samples}")
                            DataCollectorCSV.getInstance().addEcgDataPoints(ecgData.samples) // Directly pass the ECG data
                        },
                        { throwable: Throwable ->
                            Log.e(TAG, "ECG streaming failed: $throwable")
                        }
                )
    }

}