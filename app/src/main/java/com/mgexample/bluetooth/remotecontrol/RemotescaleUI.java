/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mgexample.bluetooth.remotecontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class RemotescaleUI extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private final static String TAG = "SCALE_UI";

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private TextView m_addr;
    private TextView m_data;
    private long m_aa = 0;

    private byte[] data_dbg;
    private int m_rssi;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 20 seconds.
    private static final long SCAN_PERIOD = 20000;

    MyDrawView my_view = null;

    private Timer mTimer;
    private TimerTask mTimerTask;
    private int TimerRunningFlag = 0;

    private void init_timer() {
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {

                if(TimerRunningFlag == 1) {
                    my_view.UpdateItems(System.currentTimeMillis());
                }
                else
                {
                    mTimer.cancel();
                }
            }
        };
        //开始一个定时任务
        TimerRunningFlag = 1;
        mTimer.schedule(mTimerTask, 100, 500);
    }

    private int test_id = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scaleui);

        my_view = new MyDrawView(this);
        final LinearLayout lin = (LinearLayout) findViewById(R.id.LinearLayout01);
        lin.addView(my_view);
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        Point outSize = new Point();
        display.getSize(outSize);
        if(outSize.x >= 1000) my_view.Init(1,0);
        else  my_view.Init(2,outSize.x);

        final Intent intent = getIntent();
        //mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        //mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        SharedPreferences settings = getSharedPreferences("setting", 0);
        mDeviceAddress = settings.getString("my_address", "");
        mDeviceName = settings.getString("my_name", "");

        m_aa = settings.getLong("m_aa",0x7FFFFFFF);

        Toast.makeText(this, "m_aa="+String.valueOf(m_aa), Toast.LENGTH_LONG).show();

        getSupportActionBar().setTitle(mDeviceName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//left arrow displayed

        m_addr = (TextView)findViewById(R.id.device_address);
        m_addr.setText(mDeviceAddress);

		m_data = (TextView)findViewById(R.id.data_dbg);
		m_data.setTextSize(12);
		m_data.setTextColor(Color.GRAY/*.BLUE*/);

        init_timer();

        mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				//super.handleMessage(msg);
				switch (msg.what) {
				  case 5:

					  //scale_v = ((int)data_dbg[0]+256)%256;
					  /*m_data.setText(Integer.toString(scale_v));
					  for(int i=1; i<18; i++){
						  scale_v = ((int)data_dbg[i]+256)%256;
						  m_data.append(" "+Integer.toHexString(scale_v));  
					  }*/
                      char t[] = {0x12,0x34,0x45,0xab};
                      //m_data.setText(String.valueOf(t,0,4));
                      int i,len;
                      String DbgInfo = "";
                      len = mGetLen(data_dbg);
                      for(i = 0 ; i < /*data_dbg.length*/len ; i ++)
                      {
                          DbgInfo += mChar2String((char)(data_dbg[i]));
                      }

                      if(1 == my_view.AddOneRec(m_rssi,0,data_dbg,System.currentTimeMillis(),m_aa))
                      {
                          m_data.setText("Rssi:" +String.valueOf(m_rssi)+ " Adv Data: "+DbgInfo);
                      }
/*
                      my_view.CmdComing(test_id,System.currentTimeMillis());
                      test_id ++;
                      if(test_id > 13)test_id = 0;
*/
                      break;

				  default:
					  break;
				}
			}

        };

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scanLeDevice(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);//I just share the mem resource

        menu.findItem(R.id.menu_stop).setVisible(false);
        menu.findItem(R.id.menu_scan).setVisible(false);
        menu.add(Menu.NONE, Menu.FIRST+1, 1, "Simple").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, Menu.FIRST+2, 1, "Complex").setIcon(android.R.drawable.ic_menu_info_details);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onPause();
                finish();
                break;
            case Menu.FIRST+1:
                my_view.SettingRemoteType(0);
                my_view.UpdateAll();
                break;
            case Menu.FIRST+2:
                my_view.SettingRemoteType(1);
                my_view.UpdateAll();
                break;
            default:break;
        }
        return true;
    }

    final private String mChar2String(char c)
    {
        String Map[] = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        int t1,t2;
        String st="";

        t1 = c&0x0F; t1 = t1 & 0xFF;
        t2 = (c>>4)&0x0F; t2 = t2 & 0xFF;

        st = Map[t2]+Map[t1]+" ";

        return st;
    }

    final int  mGetLen(byte []data)
    {
        int i;
        for(i = data.length - 1; i >= 0 ; i --)
        {
            if((char)data[i] != 0x00)break;
        }

        return i+1;
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        scanLeDevice(true);

        init_timer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);

        //TimerRunningFlag = 0;//stop timer
    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScaleCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScaleCallback);
        }
 //       invalidateOptionsMenu();
    }
   
    
    
 // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScaleCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            //runOnUiThread(new Runnable() {
            //    @Override
            //    public void run() {
                	//if(device.getAddress().toUpperCase().equals(mDeviceAddress)) //I will check the aa domain data later,
                                                                                  // if not check here then old beacon control version MUST NOT supported
                    {
                		//if((scanRecord[14] == 3) && (scanRecord[15] == -1))
                        {
                            int len;
                            len = mGetLen(scanRecord);

                			Log.w(TAG, "weight = "+scanRecord[16]+" Kg");

                			data_dbg = scanRecord;
                            m_rssi = rssi;
                			Message message = new Message();
                			message.what = 5;
                			mHandler.sendMessage(message);

                           // my_view.AddOneRec(rssi,scale_v,data_dbg,AdvTCountDelat);

                            Message message2 = new Message();
                            message2.what = 15;
                            mHandler.sendMessage(message2);

                		}
                	}
          //      }
        //    });
        }
    };
}

