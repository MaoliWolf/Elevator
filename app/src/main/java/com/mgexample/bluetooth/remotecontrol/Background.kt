package com.mgexample.bluetooth.remotecontrol

class Background() : Thread() {
    override fun run(): Unit {
        while (true) {
            (Settings.MainActivity as MainActivity).runOnUiThread(java.lang.Runnable {
                //Toast.makeText((Settings.MainActivity as MainActivity),(Settings.MainActivity as MainActivity).rxBleDevice.connectionState.toString(),Toast.LENGTH_SHORT).show()
            })
            sleep(500)
        }
    }
}