package com.longshun.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*蓝牙可检测性状态接收者*/
public class BluetoothDiscoverableReceiver extends BroadcastReceiver {

    private static final String TAG = "Bluetooth";

    public BluetoothDiscoverableReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
            //新的扫描模式
            int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,-100);
            Log.d(TAG, "onReceive: 新的扫描模式"+scanMode);
            switch (scanMode){
                //设备既能被其他设备检测到，又能被其他设备连接
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                    break;
                //设备不可被检测到，但是还是可以被之前发现过这个蓝牙的设备连接到
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                    break;
                //设备既不能被其他设备检测到，又不能被其他设备连接
                case BluetoothAdapter.SCAN_MODE_NONE:
                    break;
                default:
                    break;
            }
            //老的扫描模式
            int preScanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE,-100);
            Log.d(TAG, "onReceive: 老的扫描模式"+preScanMode);
        }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (device.getBondState()) {
                case BluetoothDevice.BOND_BONDING:
                    Log.d(TAG, "正在配对......");
                    if (iBondStateListener != null) {
                        iBondStateListener.bonding(device);
                    }
                    break;
                case BluetoothDevice.BOND_BONDED:
                    Log.d(TAG, "完成配对");
                    if (iBondStateListener != null) {
                        iBondStateListener.bondSuccess(device);
                    }
                    break;
                case BluetoothDevice.BOND_NONE:
                    Log.d(TAG, "取消配对");
                    if (iBondStateListener != null) {
                        iBondStateListener.bondFail(device);
                    }
                default:
                    break;
            }
        }
    }

    private IBondStateListener iBondStateListener;

    public void setiBondStateListener(IBondStateListener iBondStateListener) {
        this.iBondStateListener = iBondStateListener;
    }
}
