package com.mgexample.bluetooth.remotecontrol

import android.app.Activity

class Settings {
    companion object {
        var isUnlock = false
        var MainActivity: Activity? = null
        var floorMax = 5
        var floorMin = 0
        var lastTransmitId: Int = 0
    }
}