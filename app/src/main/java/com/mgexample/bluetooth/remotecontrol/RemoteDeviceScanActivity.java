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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class RemoteDeviceScanActivity extends AppCompatActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private int[] m_rssi = new int[100];
    private long[] m_aa = new long[100];
    private long m_t_aa;
    private String[] m_adv = new String[100];
    private int m_t = 0;
    private String m_t_adv;
    private int MgLedCmdFlag = 0;
    private int[] m_ledCmdFlag = new int[100];

    SharedPreferences settings;

    //mesh data structures
    private int[] mesh_TxAddrList = new int[200];
    private int[] mesh_StatusOnOff = new int[200];
    private int SavedItemNum = 0;

    final int GetAddrIdxFromList(int TxAddr) {
        int i;

        for (i = 0; i < SavedItemNum; i++) {
            if (TxAddr == mesh_TxAddrList[i]) return i;
        }

        SavedItemNum++;
        if (SavedItemNum == 200) return 199; //too much devices found
        return SavedItemNum - 1;
    }

    final int mesh_addDevice(int TxAddr, int OnOff) {
        int idx = GetAddrIdxFromList(TxAddr);

        mesh_StatusOnOff[idx] = OnOff;
        mesh_TxAddrList[idx] = TxAddr;

        return 1;
    }

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 30000;

    private int OS_Version_Num = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        OS_Version_Num = Build.VERSION.SDK_INT;
        if (OS_Version_Num < 21) {
            Toast.makeText(this, "At least Android 5.0 system is needed!", Toast.LENGTH_SHORT).show();
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

        settings = getSharedPreferences("setting", 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            //menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.menu_refresh).setVisible(true);
            menu.findItem(R.id.menu_refresh).setTitle(" "/*"W"*/);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }

        if (OS_Version_Num < 21) {
            menu.getItem(3).setEnabled(false);
            menu.getItem(4).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                getSupportActionBar().setTitle(R.string.title_devices);
                //setListAdapter(mLeDeviceListAdapter);

                for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                    // set item content in view
                    ((LinearLayout) findViewById(R.id.lay)).addView((View) mLeDeviceListAdapter.getItem(i));
                }

                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.menu_refresh:

            /*Intent enableWeight = new Intent(this, RemotescaleUI.class);
        	startActivity(enableWeight);*/
                break;
            case R.id.menu_ContorlGp1: //enter into control pannel
                final Intent intent1 = new Intent(this, BluetoothAdvUI.class);
                scanLeDevice(false);
                intent1.putExtra(BluetoothAdvUI.MESH_DEVICE_ADDRESS, 0xffffffff);
                intent1.putExtra(BluetoothAdvUI.MESH_GROUP, (int) 0x01);
                startActivity(intent1);
                break;
            case R.id.menu_ContorlGp2: //enter into control pannel
                final Intent intent2 = new Intent(this, BluetoothAdvUI.class);
                scanLeDevice(false);
                intent2.putExtra(BluetoothAdvUI.MESH_DEVICE_ADDRESS, 0xffffffff);
                intent2.putExtra(BluetoothAdvUI.MESH_GROUP, (int) 0x02);
                startActivity(intent2);
                break;
            case R.id.menu_ContorlGp3: //enter into control pannel
                final Intent intent3 = new Intent(this, MainActivity.class);
                scanLeDevice(false);
                intent3.putExtra(BluetoothAdvUI.MESH_DEVICE_ADDRESS, 0xffffffff);
                intent3.putExtra(BluetoothAdvUI.MESH_GROUP, (int) 0x03);
                startActivity(intent3);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        OS_Version_Num = Build.VERSION.SDK_INT;

        getSupportActionBar().setTitle(R.string.title_devices);
        SavedItemNum = 0;
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();

        //setListAdapter(mLeDeviceListAdapter);

        for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
            // set item content in view
            ((LinearLayout) findViewById(R.id.lay)).addView((View) mLeDeviceListAdapter.getItem(i));
        }
        scanLeDevice(true);
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
        mLeDeviceListAdapter.clear();
    }

    //@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        //return;

        //final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        //if (device == null) return;

        //Toast.makeText(this, "Selected "+String.valueOf( mesh_TxAddrList[position]), Toast.LENGTH_LONG).show();

        //SharedPreferences.Editor editor = settings.edit();
        //editor.putString("my_address", device.getAddress().toUpperCase());
        //editor.putInt("mesh_mac",mesh_TxAddrList[position]);

        //editor.putString("my_name", "Unknown device");
        //if(device.getName() != null)
        //{
        //  if(device.getName().length() > 0)
        //    editor.putString("my_name", device.getName().toString());
        //}
//        editor.putString("my_name", device.getName().toString());
        //editor.putLong("m_aa",m_aa[position]);
        //editor.commit();
        if (OS_Version_Num >= 21) {
            final Intent intent = new Intent(this, BluetoothAdvUI.class);
            scanLeDevice(false);
            intent.putExtra(BluetoothAdvUI.MESH_DEVICE_ADDRESS, mesh_TxAddrList[position]);
            intent.putExtra(BluetoothAdvUI.MESH_GROUP, (int) 0x80);
            startActivity(intent);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = RemoteDeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            int idx;

            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }

            idx = mLeDevices.indexOf(device);
            if ((idx >= 0) && (idx < 100)) {
                m_rssi[idx] = m_t;
                m_adv[idx] = m_t_adv;
                m_ledCmdFlag[idx] = MgLedCmdFlag;
                m_aa[idx] = m_t_aa;
            }
        }

       /* public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }*/

        public void clear() {
            //mLeDevices.clear();
            SavedItemNum = 0;
        }

        @Override
        public int getCount() {
            //return mLeDevices.size();
            return SavedItemNum;
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
/*
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            viewHolder.deviceAddress.setText("MAC: "+device.getAddress());
*/


            //viewHolder.deviceName.setText(R.string.unknown_device);
            ;
            viewHolder.deviceName.setText(mChar2String((char) (mesh_TxAddrList[i])) + ":" + mChar2String((char) (mesh_TxAddrList[i] >> 8)) + ":" +
                    mChar2String((char) (mesh_TxAddrList[i] >> 16)) + ":" + mChar2String((char) (mesh_TxAddrList[i] >> 24)));

            //viewHolder.t_adv.setText(mChar2String((char)(mesh_StatusOnOff[i]>>24)) + ":" + mChar2String((char)(mesh_StatusOnOff[i]>>16)) + ":" +
            //                                   mChar2String((char)(mesh_StatusOnOff[i]>>8)) + ":"  + mChar2String((char)(mesh_StatusOnOff[i])) );
            if ((mesh_StatusOnOff[i] & 0x00FFFFFF) != 0) {
                viewHolder.deviceAddress.setText("On");
                viewHolder.deviceAddress.setTextColor(Color.RED);
            } else {
                viewHolder.deviceAddress.setText("Off");
                viewHolder.deviceAddress.setTextColor(Color.GRAY);
            }

            /*if(i<100) {
                viewHolder.deviceRssi.setText("Rssi: "+String.valueOf(m_rssi[i])+"dB");
                if(m_rssi[i]<-90)viewHolder.deviceRssi.setTextColor(Color.RED);
                else viewHolder.deviceRssi.setTextColor(Color.BLACK);
                viewHolder.device_adv.setText(m_adv[i]);

                if(1 == m_ledCmdFlag[i])viewHolder.deviceName.setTextColor(Color.BLUE);
                else viewHolder.deviceName.setTextColor(Color.BLACK);
            }
            else*/
            //viewHolder.t_adv.setText(R.string.t_adv_data);
            if (((mesh_StatusOnOff[i] & 0x00ff000000) >> 24) == 0x03) //RGB mode
            {
                viewHolder.deviceRssi.setText("Status:RGB Mode:" + mChar2String((char) (mesh_StatusOnOff[i] >> 16)) + ":" + mChar2String((char) (mesh_StatusOnOff[i] >> 8)) + ":" + mChar2String((char) (mesh_StatusOnOff[i])));
                //viewHolder.t_adv.setText("RGB Mode:");

                //viewHolder.deviceAddress.setText( mChar2String((char)(mesh_StatusOnOff[i]>>16)) + ":" +
                //        mChar2String((char)(mesh_StatusOnOff[i]>>8)) + ":"  + mChar2String((char)(mesh_StatusOnOff[i])) );
            } else {
                viewHolder.deviceRssi.setText("Status:Y/W Mode:" + mChar2String((char) (mesh_StatusOnOff[i] >> 16)) + ":" + mChar2String((char) (mesh_StatusOnOff[i] >> 8)));
                //viewHolder.deviceAddress.setText( mChar2String((char)(mesh_StatusOnOff[i]>>16)) + ":"  + mChar2String((char)(mesh_StatusOnOff[i]>>8)) );
            }
            //viewHolder.device_adv.setText("m_adv_data[i]");

            return view;
        }
    }

    private int found_TxAddr, found_onoff;
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    int i, len = mGetLen(scanRecord);
                    m_t = rssi;
                    m_t_adv = "";
                    MgLedCmdFlag = IsMgLedCmd(scanRecord);
                    for (i = 0; i < len; i++) {
                        m_t_adv += mChar2String((char) (scanRecord[i]));
                    }

                    if (IsMgMeshStatusReportCmd(scanRecord) == 1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //mLeDeviceListAdapter.addDevice(device);
                                mesh_addDevice(found_TxAddr, found_onoff);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }

                private final int IsMgMeshStatusReportCmd(byte[] adv) {
                    byte i, len;

                    found_TxAddr = 0;

                    i = 0;
                    while (i < 30) {
                        len = adv[i];
                        if (len == 0) break;

                        if (adv[i + 1] == (byte) 0xff) {
                            if ((adv[i + 2] == 0x67) && (adv[i + 3] == 0x6d) && (len == 0x14)) //found
                            {
                                found_TxAddr = ((adv[i + 3 + 1] << 24) & 0x00FF000000) + ((adv[i + 3 + 2] << 16) & 0x00FF0000) + ((adv[i + 3 + 3] << 8) & 0x0000FF00) + (adv[i + 3 + 4] & 0x000000FF);
                                //status report cmd check
                                if ((adv[i + 16] == (byte) 0x81)) {
                                    found_onoff = ((adv[i + 17] << 24) & 0xFF000000) + ((adv[i + 18] << 16) & 0x00FF0000) + ((adv[i + 19] << 8) & 0x0000FF00) + (adv[i + 20] & 0x000000FF);
                                    if ((adv[i + 17] == 0x02) || (adv[i + 17] == 0x03)) //report status
                                    {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                                return 0;
                            }
                        }
                        /*else if((len == 17) &&(adv[i+1] == 0x07))//service UUID128
                        {
                            if((adv[i+2] == 0x47) && (adv[i+3] == 0x4d) && (adv[i+4] == 0x52)) //found
                            {
                                m_t_aa = ((adv[i+3+6] << 24) & 0x00FF000000) + ((adv[i+3+7] << 16) & 0x00FF0000) + ((adv[i+3+8] << 8) & 0x0000FF00) + (adv[i+3+9] & 0x000000FF);
                                return 1;
                            }
                        }*/

                        i += (len + 1);
                    }
                    return 0;
                }

                private final int IsMgLedCmd(byte[] adv) {
                    byte i, len;

                    m_t_aa = 0;

                    i = 0;
                    while (i < 30) {
                        len = adv[i];
                        if (len == 0) break;

                        if (adv[i + 1] == (byte) 0xff) {
                            if ((adv[i + 2] == 0x47) && (adv[i + 3] == 0x4d)) //found
                            {
                                m_t_aa = ((adv[i + 3 + 2] << 24) & 0x00FF000000) + ((adv[i + 3 + 3] << 16) & 0x00FF0000) + ((adv[i + 3 + 4] << 8) & 0x0000FF00) + (adv[i + 3 + 5] & 0x000000FF);
                                return 1;
                            }
                        } else if ((len == 17) && (adv[i + 1] == 0x07))//service UUID128
                        {
                            if ((adv[i + 2] == 0x47) && (adv[i + 3] == 0x4d) && (adv[i + 4] == 0x52)) //found
                            {
                                m_t_aa = ((adv[i + 3 + 6] << 24) & 0x00FF000000) + ((adv[i + 3 + 7] << 16) & 0x00FF0000) + ((adv[i + 3 + 8] << 8) & 0x0000FF00) + (adv[i + 3 + 9] & 0x000000FF);
                                return 1;
                            }
                        }

                        i += (len + 1);
                    }
                    return 0;
                }
            };

    final int mGetLen(byte[] data) {
        int i;
        for (i = data.length - 1; i >= 0; i--) {
            if ((char) data[i] != 0x00) break;
        }

        return i + 1;
    }

    final private String mChar2String(char c) {
        String Map[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        int t1, t2;
        String st = "";

        t1 = c & 0x0F;
        t1 = t1 & 0xFF;
        t2 = (c >> 4) & 0x0F;
        t2 = t2 & 0xFF;

        //st = Map[t2]+Map[t1]+" ";
        st = Map[t2] + Map[t1];

        return st;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
    }
}