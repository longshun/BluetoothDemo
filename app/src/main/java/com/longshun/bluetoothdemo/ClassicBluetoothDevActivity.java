package com.longshun.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Set;

/*传统蓝牙开发*/
public class ClassicBluetoothDevActivity extends AppCompatActivity implements BluetoothDevicesReceiver.OnDevicesFindListener {

    private static final String TAG = "Bluetooth";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevicesReceiver devicesReceiver;
    private ArrayList<String> listDeviceInfos;
    private ArrayList<BluetoothDevice> listDevices = new ArrayList<>();
    private ArrayAdapter<String> arrayAdapter;
    private BluetoothDiscoverableReceiver bluetoothDiscoverableReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pri_dev);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //检查设备是否支持蓝牙
        findViewById(R.id.btn_check).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter == null) {
                    Log.d(TAG, "onClick:不支持蓝牙 ");
                } else {
                    Log.d(TAG, "onClick:支持蓝牙 ");
                }
            }
        });

        //检查蓝牙是否启用
        findViewById(R.id.btn_enable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter != null) {
                    if (!bluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "onClick: 蓝牙未启用");
                        //请求打开蓝牙
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, 100);
                    } else {
                        Log.d(TAG, "onClick: 蓝牙已启用");
                    }
                }
            }
        });

        //查询配对设备
        findViewById(R.id.btn_query).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter == null) {
                    return;
                }
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                // If there are paired devices
                if (pairedDevices.size() > 0) {
                    // Loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        // Add the name and address to an array adapter to show in a ListView
                        Log.d(TAG, "onClick:已配对设备 " + device.getName() + "：" + device.getAddress());
                    }
                } else {
                    Log.d(TAG, "onClick: 暂无配对设备");
                }
            }
        });

        //发现设备
        findViewById(R.id.btn_discover).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter == null) {
                    return;
                }
                //todo 这个 startDiscovery 可以自定义扫描一分钟后 cancelDiscovery
                boolean startDiscovery = bluetoothAdapter.startDiscovery();
                if (startDiscovery) {
                    Log.d(TAG, "onClick: 开始探索设备 成功");
                } else {
                    Log.d(TAG, "onClick: 开始探索设备 失败");
                }
            }
        });

        //发现的设备列表
        ListViewForScrollView lvDevices = (ListViewForScrollView) findViewById(R.id.lv_devices);
        listDeviceInfos = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listDeviceInfos);
        lvDevices.setAdapter(arrayAdapter);
        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //尝试进行设备连接

            }
        });

        //始终打开蓝牙的可检测性
        findViewById(R.id.btn_discoverable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: 打开蓝牙可检测性");
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                startActivity(discoverableIntent);
            }
        });

        //取消发现设备
        findViewById(R.id.btn_cancel_discover).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter == null) {
                    return;
                }
                boolean cancelDiscovery = bluetoothAdapter.cancelDiscovery();
                if (cancelDiscovery) {
                    Log.d(TAG, "cancelDiscovery: 成功取消发现设备");
                } else {
                    Log.d(TAG, "cancelDiscovery: 取消发现设备失败");
                }
            }
        });

        //不要忘记解除注册
        devicesReceiver = new BluetoothDevicesReceiver();
        devicesReceiver.setOnDevicesFindListener(ClassicBluetoothDevActivity.this);
        registerReceiver(devicesReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        //注册蓝牙可检测性状态变化监听广播
        bluetoothDiscoverableReceiver = new BluetoothDiscoverableReceiver();
        registerReceiver(bluetoothDiscoverableReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
    }

    @Override
    public void findBluetoothDevices(BluetoothDevice device) {
        if (!isAdded(device)) {
            listDevices.add(device);
            listDeviceInfos.add(device.getName() + ":" + device.getAddress());
            arrayAdapter.notifyDataSetChanged();
        }
    }

    /*过滤添加过的设备*/
    private boolean isAdded(BluetoothDevice device) {
        for (int i = 0; i < listDevices.size(); i++) {
            String address = listDevices.get(i).getAddress();
            if (TextUtils.equals(address, device.getAddress())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(devicesReceiver);
        unregisterReceiver(bluetoothDiscoverableReceiver);
        super.onDestroy();
    }
}
