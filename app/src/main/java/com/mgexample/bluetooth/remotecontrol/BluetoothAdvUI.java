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
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BluetoothAdvUI extends AppCompatActivity {

    private Button butt_R, butt_G, butt_B, butt_Y, butt_W, butt_ON, butt_OFF;
    private Button butt_add_gp1, butt_add_gp2;
    private Handler mHandler;

    private BluetoothAdapter mBluetoothAdapter;
    private Context mContext;
    public static final String MESH_DEVICE_ADDRESS = "MESH_DEVICE_ADDRESS";
    public static final String MESH_GROUP = "MESH_GROUP";

    private int m_aa = 0;
    private int m_groupInfo = 0;

    private int m_group_control_flag = 0;

//    private byte[] data_dbg;
//    private int m_rssi;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 20 seconds.
//    private static final long SCAN_PERIOD = 20000;

    //    MyDrawView my_view = null;
/*
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
*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scaleui);

        final Intent intent = getIntent();
        m_aa = intent.getIntExtra(MESH_DEVICE_ADDRESS, 0);
        m_groupInfo = intent.getIntExtra(MESH_GROUP, 0);
        mContext = this;
        mHandler = new Handler();

        //Toast.makeText(this, "m_aa="+String.valueOf(m_aa), Toast.LENGTH_LONG).show();
        if (m_groupInfo == (int) 0x80) {
            getSupportActionBar().setTitle(mChar2String((char) (m_aa)) + ":" + mChar2String((char) (m_aa >> 8)) + ":" + mChar2String((char) (m_aa >> 16)) + ":" + mChar2String((char) (m_aa >> 24)));
        } else {
            getSupportActionBar().setTitle("Group" + String.valueOf(m_groupInfo)); //group control
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//left arrow displayed

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
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

        butt_R = (Button) findViewById(R.id.button_R);
        butt_R.setBackgroundColor(Color.RED);
        butt_R.setEnabled(true);

        butt_G = (Button) findViewById(R.id.button_G);
        butt_G.setBackgroundColor(Color.GREEN);
        butt_G.setEnabled(true);

        butt_B = (Button) findViewById(R.id.button_B);
        butt_B.setBackgroundColor(Color.BLUE);
        butt_B.setEnabled(true);

        butt_Y = (Button) findViewById(R.id.button_Y);
        butt_Y.setBackgroundColor(Color.YELLOW);
        butt_Y.setEnabled(true);

        butt_W = (Button) findViewById(R.id.button_W);
        butt_W.setBackgroundColor(0x80E0E0E0);
        butt_W.setEnabled(true);

        butt_ON = (Button) findViewById(R.id.button_ON);
        butt_ON.setEnabled(true);

        butt_OFF = (Button) findViewById(R.id.button_OFF);
        butt_OFF.setEnabled(true);

        butt_add_gp1 = (Button) findViewById(R.id.button_addgp1);
        butt_add_gp2 = (Button) findViewById(R.id.button_addgp2);

        m_group_control_flag = 0;
        if (m_groupInfo != (int) 0x80) { //group style, disable the group add function
            butt_add_gp1.setEnabled(false);
            butt_add_gp1.setAlpha((float) 0);

            butt_add_gp2.setEnabled(false);
            butt_add_gp2.setAlpha((float) 0);
            m_group_control_flag = 1;
        }

        if (m_aa == 0xffffffff) {
            //   disable_all_button(false);
        }

        butt_add_gp1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disable_all_button(true);
                set_rx_group(0x81); //add to group1
                set_rx_addr(m_aa);
                generate_addgroup();
                StartAdv(true, 800);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StartAdv(false, 0);
                        ensable_all_button();
                        butt_add_gp1.setEnabled(true);
                        butt_add_gp2.setEnabled(true);
                        // getActionBar().setTitle(R.string.title_devices3);
                    }
                }, 1000);
            }
        });

        butt_add_gp2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disable_all_button(true);
                set_rx_group(0x82); //add to group2
                set_rx_addr(m_aa);
                generate_addgroup();
                StartAdv(true, 800);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StartAdv(false, 0);
                        ensable_all_button();
                        butt_add_gp1.setEnabled(true);
                        butt_add_gp2.setEnabled(true);
                        // getActionBar().setTitle(R.string.title_devices3);
                    }
                }, 1000);
            }
        });

        butt_R.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disable_all_button(true);
                set_rx_group(m_groupInfo);
                set_rx_addr(m_aa);
                generate_RGB_color(255, 0, 0);
                StartAdv(true, 800);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StartAdv(false, 0);
                        ensable_all_button();
                        // getActionBar().setTitle(R.string.title_devices3);
                    }
                }, 1000);
            }
        });

        butt_G.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disable_all_button(true);
                set_rx_group(m_groupInfo);
                set_rx_addr(m_aa);
                generate_RGB_color(0, 255, 0);
                StartAdv(true, 800);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StartAdv(false, 0);
                        ensable_all_button();
                        // getActionBar().setTitle(R.string.title_devices3);
                    }
                }, 1000);
            }
        });

        butt_B.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disable_all_button(true);
                set_rx_group(m_groupInfo);
                set_rx_addr(m_aa);
                generate_RGB_color(0, 0, 255);
                StartAdv(true, 800);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StartAdv(false, 0);
                        ensable_all_button();
                        // getActionBar().setTitle(R.string.title_devices3);
                    }
                }, 1000);
            }
        });

        butt_Y.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disable_all_button(true);
                set_rx_group(m_groupInfo);
                set_rx_addr(m_aa);
                generate_YW_color(1);
                StartAdv(true, 800);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StartAdv(false, 0);
                        ensable_all_button();
                        // getActionBar().setTitle(R.string.title_devices3);
                    }
                }, 1000);
            }
        });

        butt_W.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disable_all_button(true);
                set_rx_group(m_groupInfo);
                set_rx_addr(m_aa);
                generate_YW_color(2);
                StartAdv(true, 800);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StartAdv(false, 0);
                        ensable_all_button();
                        // getActionBar().setTitle(R.string.title_devices3);
                    }
                }, 1000);
            }
        });

        butt_ON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disable_all_button(true);
                set_rx_group(m_groupInfo);
                set_rx_addr(m_aa);
                generate_on_off_cmd(true);
                StartAdv(true, 800);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StartAdv(false, 0);
                        ensable_all_button();
                        // getActionBar().setTitle(R.string.title_devices3);
                    }
                }, 1000);
            }
        });

        butt_OFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disable_all_button(true);
                set_rx_group(m_groupInfo);
                set_rx_addr(m_aa);
                generate_on_off_cmd(false);
                StartAdv(true, 800);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StartAdv(false, 0);
                        ensable_all_button();
                        // getActionBar().setTitle(R.string.title_devices3);
                    }
                }, 1000);
            }
        });

    }

    private void run_menu_comm() {
        disable_all_button(true);
        StartAdv(true, 800);
        //Toast.makeText(mContext, "Start Adv... ", Toast.LENGTH_LONG).show();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                StartAdv(false, 0);
                ensable_all_button();
                // getActionBar().setTitle(R.string.title_devices3);
            }
        }, 1000);
    }

    private void disable_all_button(boolean flag) {
        butt_R.setBackgroundColor(Color.GRAY);
        butt_G.setBackgroundColor(Color.GRAY);
        butt_B.setBackgroundColor(Color.GRAY);
        butt_Y.setBackgroundColor(Color.GRAY);
        butt_W.setBackgroundColor(Color.GRAY);

        butt_R.setEnabled(false);
        butt_G.setEnabled(false);
        butt_B.setEnabled(false);
        butt_Y.setEnabled(false);
        butt_W.setEnabled(false);

        butt_ON.setEnabled(false);
        butt_OFF.setEnabled(false);

        butt_add_gp1.setEnabled(false);
        butt_add_gp2.setEnabled(false);

        if (flag) Toast.makeText(mContext, "Start Adv... ", Toast.LENGTH_LONG).show();
    }

    private void ensable_all_button() {
        //if(m_aa == 0xffffffff)return;//just group control

        butt_R.setBackgroundColor(Color.RED);
        butt_G.setBackgroundColor(Color.GREEN);
        butt_B.setBackgroundColor(Color.BLUE);
        butt_Y.setBackgroundColor(Color.YELLOW);
        butt_W.setBackgroundColor(0x80E0E0E0);

        butt_R.setEnabled(true);
        butt_G.setEnabled(true);
        butt_B.setEnabled(true);
        butt_Y.setEnabled(true);
        butt_W.setEnabled(true);

        butt_ON.setEnabled(true);
        butt_OFF.setEnabled(true);

        if (m_group_control_flag != 1) {
            butt_add_gp1.setEnabled(true);
            butt_add_gp2.setEnabled(true);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);//I just share the mem resource

        menu.findItem(R.id.menu_stop).setVisible(false);
        menu.findItem(R.id.menu_scan).setVisible(false);
        //menu.findItem(R.id.menu_ContorlGp1).setVisible(false);
        menu.findItem(R.id.menu_ContorlGp1).setTitle(" ");
        menu.findItem(R.id.menu_ContorlGp2).setVisible(false);
        menu.add(Menu.NONE, Menu.FIRST + 1, 1, "Report Status").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, Menu.FIRST + 2, 2, "Group1 On").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, Menu.FIRST + 3, 3, "Group1 Off").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, Menu.FIRST + 4, 4, "Group2 On").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, Menu.FIRST + 5, 5, "Group2 Off").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, Menu.FIRST + 6, 6, "All On").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, Menu.FIRST + 7, 7, "All Off").setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onPause();
                finish();
                break;
            case Menu.FIRST + 1://report status
                //set_rx_group(0x80);
                //set_rx_addr(0xffffffff);
                generate_report_status_onoff(0x70);
                run_menu_comm();
                break;
            case Menu.FIRST + 2://group1 on
                generate_group_onoff(1, true);
                run_menu_comm();
                break;
            case Menu.FIRST + 3://group1 off
                generate_group_onoff(1, false);
                run_menu_comm();
                break;
            case Menu.FIRST + 4://group2 on
                generate_group_onoff(2, true);
                run_menu_comm();
                break;
            case Menu.FIRST + 5://group2 off
                generate_group_onoff(2, false);
                run_menu_comm();
                break;
            case Menu.FIRST + 6://all on
                generate_group_onoff(0x80, true);
                run_menu_comm();
                break;
            case Menu.FIRST + 7://all off
                generate_group_onoff(0x80, false);
                run_menu_comm();
                break;

            default:
                break;
        }
        return true;
    }

    final private String mChar2String(char c) {
        String Map[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        int t1, t2;
        String st = "";

        t1 = c & 0x0F;
        t1 = t1 & 0xFF;
        t2 = (c >> 4) & 0x0F;
        t2 = t2 & 0xFF;

        st = Map[t2] + Map[t1];

        return st;
    }

    final int mGetLen(byte[] data) {
        int i;
        for (i = data.length - 1; i >= 0; i--) {
            if ((char) data[i] != 0x00) break;
        }

        return i + 1;
    }


    @Override
    protected void onResume() {
        byte[] m_adv_datax = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x15,
                (byte) 0x80, //group
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,//rx addr
                0x01, 0x04,//sn
                0, 0, 0, 0, 0 //cmd
        };
        super.onResume();

        mesh_cmd_sn = 15;
        {
            int i;

            for (i = 0; i < 17; i++) //init the buf
            {
                m_adv_data[i] = m_adv_datax[i];
            }
        }
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        //init_timer();
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
        StartAdv(false, 0);
    }

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser = null;

    private void StartAdv(boolean startflag, int dur) {
        if (startflag == true) {
            //check the peripheral feature
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            if (mBluetoothLeAdvertiser == null) {
                Toast.makeText(this, "The device does NOT support peripheral(make sure BLE is open)!", Toast.LENGTH_SHORT).show();
                //Log.e(TAG, "the device not support peripheral");
                finish();
            } else {
                //Toast.makeText(this, "OK. the device supports peripheral", Toast.LENGTH_SHORT    ).show();
//            getActionBar().setTitle("Mg Adv...");
            }

            mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(this, false, dur), createAdvertiseData(this), mAdvertiseCallback);
        } else {
            stopAdvertise();
        }
    }

    private void run_adv_once() {
        Toast.makeText(mContext, "Start Adv... ", Toast.LENGTH_LONG).show();

        generate_report_status_onoff(0x70);

        StartAdv(true, 800);
        /*
       // getActionBar().setTitle("adv on");
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
          //      StartAdv(false);
//                butt_start1.setEnabled(true);
//                butt_start2.setEnabled(true);
//                butt_start3.setEnabled(true);
//                butt_start4.setEnabled(true);
                //getActionBar().setTitle("adv off");
                Toast.makeText(mContext, "end Adv... ", Toast.LENGTH_LONG).show();
            }
        }, 1000);*/
    }


    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

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

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
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
    };

    private void stopAdvertise() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mBluetoothLeAdvertiser = null;
        }
    }


    /**
     * create AdvertiseSettings
     */
    public static AdvertiseSettings createAdvSettings(Context myContext, boolean connectable, int timeoutMillis) {
        AdvertiseSettings.Builder mSettingsbuilder = new AdvertiseSettings.Builder();
        //mSettingsbuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        mSettingsbuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        mSettingsbuilder.setConnectable(connectable);
        mSettingsbuilder.setTimeout(timeoutMillis);
        mSettingsbuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        AdvertiseSettings mAdvertiseSettings = mSettingsbuilder.build();
        if (mAdvertiseSettings == null) {
            // if(D){
            Toast.makeText(myContext, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show();
            //   Log.e(TAG,"mAdvertiseSettings == null");
            // }
        }
        return mAdvertiseSettings;
    }

    private int mesh_cmd_sn;
    private byte[] m_adv_data = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x15,
            (byte) 0x80, //group
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,//rx addr
            0x01, 0x04,//sn
            0, 0, 0, 0, 0 //cmd
    };

    private void set_rx_addr(int addr) {
        m_adv_data[6] = (byte) (addr >> 24);
        m_adv_data[7] = (byte) (addr >> 16);
        m_adv_data[8] = (byte) (addr >> 8);
        m_adv_data[9] = (byte) (addr);
    }

    private void set_rx_group(int group) {
        m_adv_data[5] = (byte) (group);
    }

    private void generate_on_off_cmd(boolean on_flag) {
        m_adv_data[12] = 0x01;
        m_adv_data[13] = 0x02;
        m_adv_data[15] = m_adv_data[16] = 0;
        if (on_flag) {
            m_adv_data[14] = 0x01;
        } else {
            m_adv_data[14] = 0x02;
        }

        m_adv_data[11] = (byte) ((mesh_cmd_sn << 3) + 4);
        mesh_cmd_sn++;
        if (mesh_cmd_sn == 32) mesh_cmd_sn = 0;
    }

    private void generate_RGB_color(int r, int g, int b) {
        m_adv_data[12] = 0x02;
        m_adv_data[13] = 0x01;
        m_adv_data[14] = (byte) r;
        m_adv_data[15] = (byte) g;
        m_adv_data[16] = (byte) b;

        m_adv_data[11] = (byte) ((mesh_cmd_sn << 3) + 4);
        mesh_cmd_sn++;
        if (mesh_cmd_sn == 32) mesh_cmd_sn = 0;
    }

    private void generate_addgroup() {
        m_adv_data[12] = (byte) 0x80;
        m_adv_data[13] = (byte) 0x01;
        m_adv_data[14] = 1; //add group
        m_adv_data[15] = 0;
        m_adv_data[16] = 0;

        m_adv_data[11] = (byte) ((mesh_cmd_sn << 3) + 4);
        mesh_cmd_sn++;
        if (mesh_cmd_sn == 32) mesh_cmd_sn = 0;
    }

    private void generate_YW_color(int M) {
        m_adv_data[12] = 0x02;
        m_adv_data[13] = 0x02;
        m_adv_data[14] = (byte) M; //1Y,2W
        m_adv_data[15] = (byte) 0;
        m_adv_data[16] = (byte) 0;

        m_adv_data[11] = (byte) ((mesh_cmd_sn << 3) + 4);
        mesh_cmd_sn++;
        if (mesh_cmd_sn == 32) mesh_cmd_sn = 0;
    }

    private void generate_report_status(int M) {
        m_adv_data[12] = (byte) 0x81;
        m_adv_data[13] = (byte) 0xFF;
        m_adv_data[14] = (byte) M;
        if (M > 255) {
            m_adv_data[15] = (byte) (M - 255);
        } else {
            m_adv_data[15] = (byte) 0;
        }
        m_adv_data[16] = (byte) 0;

        m_adv_data[11] = (byte) ((mesh_cmd_sn << 3) + 4);
        mesh_cmd_sn++;
        if (mesh_cmd_sn == 32) mesh_cmd_sn = 0;
    }

    private void generate_group_onoff(int group, boolean on_flag) {
        set_rx_addr(0xffffffff);
        set_rx_group(group);
        generate_on_off_cmd(on_flag);
    }

    private void generate_report_status_onoff(int dur) {
        set_rx_addr(0xffffffff);
        set_rx_group(0x80);
        generate_report_status(dur);
    }

    public /*static*/ AdvertiseData createAdvertiseData(Context myContext) {
        //byte[]advData = {0x11,0x22,0x33};
        AdvertiseData.Builder mDataBuilder = new AdvertiseData.Builder();
        mDataBuilder.addManufacturerData(/*manufactureuuid16*/0x6d67, m_adv_data);
//        mDataBuilder.addmg_mesh_SNServiceData(ParcelUuid.fromString("08"),name);
        mDataBuilder.setIncludeDeviceName(/*isAdvNameFlag*/false);

        //mDataBuilder.addServiceUuid(ParcelUuid.fromString("0c312388-5d09-4f44-b670-5461605f0b1e"));

        //mDataBuilder.addServiceUuid(ParcelUuid.fromString(HEART_RATE_SERVICE));
        AdvertiseData mAdvertiseData = mDataBuilder.build();
        if (mAdvertiseData == null) {
            //if(D){
            Toast.makeText(myContext, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show();
            //  Log.e(TAG,"mAdvertiseSettings == null");
            //}
        } else {

        }

        return mAdvertiseData;
    }


}

