package com.mgexample.bluetooth.remotecontrol

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast


class SeakBarListener(val ct: Context) : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(
        seekBar: SeekBar, progress: Int,
        fromUser: Boolean
    ) {

        //Log.d("DEBUG", "Progress is: $progress")
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (seekBar.progress <= 95) {
            seekBar.progress = 0
            Settings.isUnlock = false
            //Here send data

            (Settings.MainActivity as MainActivity).sendData()
        } else {
            seekBar.progress = 100
            Settings.isUnlock = true
            Toast.makeText(ct, "已解鎖", Toast.LENGTH_LONG).show()
            Utils.Vibrate(ct,50)
        }

    }

}