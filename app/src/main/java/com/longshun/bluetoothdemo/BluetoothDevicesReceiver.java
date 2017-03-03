package com.longshun.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;

/*蓝牙信息接受*/
public class BluetoothDevicesReceiver extends BroadcastReceiver {
    private static final String TAG = "Bluetooth";

    public BluetoothDevicesReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)){
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG, "onReceive: 发现设备="+device.getName()+":"+device.getAddress());
            if (onDevicesFindListener != null) {
                onDevicesFindListener.findBluetoothDevices(device);
            }
        }
    }

    interface OnDevicesFindListener{
        void findBluetoothDevices(BluetoothDevice device);
    }

    private OnDevicesFindListener onDevicesFindListener;

    public void setOnDevicesFindListener(OnDevicesFindListener onDevicesFindListener) {
        this.onDevicesFindListener = onDevicesFindListener;
    }
}
