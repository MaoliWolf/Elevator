package com.mgexample.bluetooth.remotecontrol

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mgexample.bluetooth.remotecontrol.Utils.delay
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer
import kotlin.experimental.xor

const val REQUEST_ENABLE_BT = 1

class MainActivity : AppCompatActivity() {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private val mHandler: Handler? = null
    private val m_aa = 0
    private val m_groupInfo = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        Settings.MainActivity = this
        val background = Background()
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            0
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        debug.visibility = View.INVISIBLE
        list.visibility = View.INVISIBLE
        //supportActionBar?.hide()
        unlock.setOnSeekBarChangeListener(SeakBarListener(applicationContext))


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        // Checks if Bluetooth is supported on the device.

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        set_rx_group(m_groupInfo)
        set_rx_addr(m_aa)
        generate_RGB_color(255, 0, 0)
        StartAdv(true, 800)

        mHandler?.postDelayed(Runnable {
            StartAdv(false, 0)
            // getActionBar().setTitle(R.string.title_devices3);
        }, 1000)





        up.setOnClickListener {
            sendData(true)
            arrow(it, "電梯向上")
        }
        down.setOnClickListener {
            sendData(false)
            arrow(it, "電梯向下")
        }
        left.setOnTouchListener(View.OnTouchListener { view, event ->
            floorClick(view, event)
            return@OnTouchListener true
        })
        right.setOnTouchListener(View.OnTouchListener { view, event ->
            floorClick(view, event)
            return@OnTouchListener true
        })

        delay(2, object : Utils.DelayCallback {
            override fun afterDelay() {
                background.start()
                //status.text = "無法取得詳細資訊"
            }
        })
    }

    private var mesh_cmd_sn = 0

    private fun generate_RGB_color(r: Int, g: Int, b: Int) {
        m_adv_data[11] = ((mesh_cmd_sn shl 3) + 4).toByte()

        m_adv_data[12] = 0x02
        m_adv_data[13] = 0x01
        m_adv_data[14] = r.toByte()
        m_adv_data[15] = g.toByte()
        m_adv_data[16] = b.toByte()

        mesh_cmd_sn++
        if (mesh_cmd_sn == 32) mesh_cmd_sn = 0
    }

    private fun set_rx_addr(addr: Int) {
        m_adv_data[6] = (addr shr 24).toByte()
        m_adv_data[7] = (addr shr 16).toByte()
        m_adv_data[8] = (addr shr 8).toByte()
        m_adv_data[9] = addr.toByte()
    }

    private fun set_rx_group(group: Int) {
        m_adv_data[5] = group.toByte()
    }

    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private fun StartAdv(startflag: Boolean, dur: Int) {
        if (startflag) {
            //check the peripheral feature
            mBluetoothLeAdvertiser = mBluetoothAdapter!!.bluetoothLeAdvertiser
            if (mBluetoothLeAdvertiser == null) {
                Toast.makeText(
                    this,
                    "The device does NOT support peripheral(make sure BLE is open)!",
                    Toast.LENGTH_SHORT
                ).show()
                //Log.e(TAG, "the device not support peripheral");
                finish()
            } else {
                //Toast.makeText(this, "OK. the device supports peripheral", Toast.LENGTH_SHORT    ).show();
//            getActionBar().setTitle("Mg Adv...");
            }
            (mBluetoothLeAdvertiser as BluetoothLeAdvertiser).startAdvertising(
                BluetoothAdvUI.createAdvSettings(
                    this,
                    false,
                    dur
                ), createAdvertiseData(this), mAdvertiseCallback
            )
        } else {
            stopAdvertise()
        }
    }     /*static*/

    private val mAdvertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            if (settingsInEffect != null) {
                //    Log.d(TAG, "onStartSuccess TxPowerLv=" + settingsInEffect.getTxPowerLevel()     + " mode=" + settingsInEffect.getMode()
                //            + " timeout=" + settingsInEffect.getTimeout());
                //Toast.makeText(mContext, "Start Adv... ", Toast.LENGTH_LONG).show();
                //getActionBar().setTitle(R.string.title_devices2);
            } else {
                //  Log.e(TAG, "onStartSuccess, settingInEffect is null");
                //Toast.makeText(mContext, "settingInEffect is null", Toast.LENGTH_LONG).show();
            }
            //Log.e(TAG,"onStartSuccess settingsInEffect" + settingsInEffect);
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            /*
            if(errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE){
                Toast.makeText(mContext, "ADVERTISE_FAILED_DATA_TOO_LARGE:\n disable adv name or shorten the adv data!", Toast.LENGTH_LONG).show();
            }else if(errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS){
                Toast.makeText(mContext, "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS", Toast.LENGTH_LONG).show();
            }else if(errorCode == ADVERTISE_FAILED_ALREADY_STARTED){
                Toast.makeText(mContext, "ADVERTISE_FAILED_ALREADY_STARTED", Toast.LENGTH_LONG).show();
            }else if(errorCode == ADVERTISE_FAILED_INTERNAL_ERROR){
                Toast.makeText(mContext, "ADVERTISE_FAILED_INTERNAL_ERROR", Toast.LENGTH_LONG).show();
            }else if(errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED){
                Toast.makeText(mContext, "ADVERTISE_FAILED_FEATURE_UNSUPPORTED", Toast.LENGTH_LONG).show();
            }*/
        }
    }
    private var m_adv_data = byteArrayOf(
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0x15,
        0x80.toByte(),  //group
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),  //rx addr
        0x01,
        0x04,  //sn
        0,
        0,
        0,
        0,
        0 //cmd
    )

    fun createAdvertiseData(myContext: Context?): AdvertiseData? {
        //byte[]advData = {0x11,0x22,0x33};
        val mDataBuilder = AdvertiseData.Builder()
        mDataBuilder.addManufacturerData( /*manufactureuuid16*/0x6d67, m_adv_data)
        //        mDataBuilder.addmg_mesh_SNServiceData(ParcelUuid.fromString("08"),name);
        mDataBuilder.setIncludeDeviceName( /*isAdvNameFlag*/false)

        //mDataBuilder.addServiceUuid(ParcelUuid.fromString("0c312388-5d09-4f44-b670-5461605f0b1e"));

        //mDataBuilder.addServiceUuid(ParcelUuid.fromString(HEART_RATE_SERVICE));
        val mAdvertiseData = mDataBuilder.build()
        if (mAdvertiseData == null) {
            //if(D){
            Toast.makeText(myContext, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show()
            //  Log.e(TAG,"mAdvertiseSettings == null");
            //}
        } else {
        }
        return mAdvertiseData
    }

    private fun stopAdvertise() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
            mBluetoothLeAdvertiser = null
        }
    }

    fun sendData(isUp: Boolean? = null) {
        val byteSend = ByteBuffer.allocate(22)
        byteSend.putInt(++Settings.lastTransmitId)
        var floor: Long = 0
        try {
            floor = status.text.toString().toLong()
        } catch (e: Exception) {

        }
        byteSend.putLong(floor)

        var toFloor: Int = 0
        if (isUp == null) {
            toFloor = left.text.toString().toInt() * 10
            toFloor += right.text.toString().toInt()
        }

        byteSend.put(toFloor.toByte())
        if (isUp != null && isUp) {
            byteSend.put(1.toByte())
            byteSend.put(0.toByte())
        } else {
            byteSend.put(0.toByte())
            byteSend.put(1.toByte())
        }
        byteSend.put(0.toByte())
        byteSend.put(0.toByte())
        byteSend.put(0.toByte())
        byteSend.put(0.toByte())
        byteSend.put(0.toByte())
        byteSend.put(0.toByte())

        var checkSum: Byte = 0x7B
        for (i in 0..20) {
            checkSum = checkSum xor byteSend[i]
        }
        byteSend.put(checkSum)
        //byteSend.get(m_adv_data, 0, byteSend.capacity());
        //TODO send byteSendArray


        set_rx_group(m_groupInfo)
        set_rx_addr(m_aa)

        generate_RGB_color(0, 255, 0)

        StartAdv(true, 800)

        mHandler?.postDelayed(Runnable {
            StartAdv(false, 0)
            // getActionBar().setTitle(R.string.title_devices3);
        }, 1000)
    }

    fun readData(characteristicValue: ByteArray) {
        Log.d("Debug-BT", "BT readed")
        if (characteristicValue.size == 16) {
            //處理電梯送來的資料
            left.text =
                (characteristicValue[12].toInt() / 10).toString()
            right.text =
                (characteristicValue[12].toInt() % 10).toString()
            Settings.floorMax = characteristicValue[13].toInt()
            Settings.floorMin = characteristicValue[14].toInt()


            val byteNumber = ByteBuffer.allocate(4)
            for (i in 0..3) {
                byteNumber.put(i, characteristicValue[i])
            }
            Settings.lastTransmitId = byteNumber.int
            val byteId = ByteBuffer.allocate(8)
            for (i in 4..11) {
                byteId.put(i - 4, characteristicValue[i])
            }
            status.text = byteId.long.toString()
            characteristicValue[0]
            //Log.d("Debug", error.message)
            //Log.d("Debug", byteId.long.toString())
        }
    }

    fun arrow(view: View, text: String) {
        val button = view as ImageView
        if (Settings.isUnlock) {
            Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
            button.drawable.setTint(Color.rgb(255, 88, 9))
            delay(2, object : Utils.DelayCallback {
                override fun afterDelay() {
                    button.drawable.setTint(Color.BLACK)
                }
            })
        } else {
            Toast.makeText(applicationContext, "請先解鎖", Toast.LENGTH_SHORT).show()
        }
    }

    fun floorClick(view: View, event: MotionEvent) {

        if (Settings.isUnlock) {

            val from = view as TextView

            val x = event.x
            val y = event.y
            var side = -1

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (y < from.height / 2) {
                        side = 1
                    }
                    if (from == left) {
                        Utils.setFloor(
                            (left.text.toString().toInt() + side).toString(),
                            right.text.toString(),
                            side
                        )
                    } else {
                        Utils.setFloor(
                            left.text.toString(),
                            (right.text.toString().toInt() + side).toString(),
                            side
                        )
                    }
                }
            }
        } else {
            Toast.makeText(applicationContext, "請先解鎖", Toast.LENGTH_SHORT).show()
        }
    }
}
