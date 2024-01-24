package com.joinforage.android.example.pos.k9sdk

import com.pos.sdk.DevicesFactory
import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.pos.sdk.DeviceManager
import com.pos.sdk.callback.ResultCallback
import com.pos.sdk.magcard.IMagCardListener
import com.pos.sdk.magcard.MagCardDevice
import com.pos.sdk.magcard.TrackData
import com.pos.sdk.printer.PrinterDevice
import com.pos.sdk.sys.SystemDevice
import okio.ByteString.Companion.decodeHex

enum class DeviceType {
    K9_TERMINAL,
    EMULATOR,
    UNKNOWN
}

fun hexToAscii(hex: String) = String(hex.decodeHex().toByteArray())

class K9SDK() {
    private var _deviceManager: DeviceManager? = null
    private val _printDevice: PrinterDevice?
        get() = _deviceManager?.printDevice
    private val _magCardDevice: MagCardDevice?
        get() = _deviceManager?.magneticDevice
    private val _systemDevice: SystemDevice?
        get() = _deviceManager?.systemDevice

    val terminalId: String
        get() = _systemDevice?.getSystemInfo(SystemDevice.SystemInfoType.SN) ?: "Android Emulator"

    var currentDeviceType: DeviceType = DeviceType.UNKNOWN
    val isUsable
        get() = currentDeviceType == DeviceType.K9_TERMINAL

    fun init (context: Context) : K9SDK {
        DevicesFactory.create(
            context,
            object : ResultCallback<DeviceManager> {
                override fun onFinish(deviceManager: DeviceManager) {
                    _deviceManager = deviceManager
                    currentDeviceType = DeviceType.K9_TERMINAL
                    Log.i("CPay SDK","initialized successfully")
                }

                override fun onError(i: Int, s: String) {
                    Log.i("CPay SDK","failed to initialize successfully")
                    currentDeviceType = DeviceType.EMULATOR
                }
            }
        )
        return this
    }

    fun listenForMagneticCardSwipe(
        onSwipeCardSuccess: (track2Data: String) -> Unit
    ) : Boolean {
        // CPay SDK methods should only run if they are supported
        // by the current runtime env (i.e. the app is being run
        // on Centerm K9 POS terminal). Otherwise don't run this
        if (!isUsable) return false

        _magCardDevice?.swipeCard(20000, true, object : IMagCardListener.Stub() {
            @Throws(RemoteException::class)
            override fun onSwipeCardTimeout() {
                Log.i("CPay SDK", "Swipe card time out")
            }

            @Throws(RemoteException::class)
            override fun onSwipeCardException(i: Int) {
                Log.i("CPay SDK", "Swipe card error $i")
            }

            @Throws(RemoteException::class)
            override fun onSwipeCardSuccess(trackData: TrackData) {
                val track2Data = hexToAscii(trackData.secondTrackData)
                Log.i("CPay SDK", "Parsed track2Data: $track2Data")
                onSwipeCardSuccess(track2Data)
            }

            @Throws(RemoteException::class)
            override fun onSwipeCardFail() {
                Log.i("CPay SDK", "Swipe card failed ")
            }

            @Throws(RemoteException::class)
            override fun onCancelSwipeCard() {
                Log.i("CPay SDK", "Swipe card canceled")
            }
        })

        return true
    }
}