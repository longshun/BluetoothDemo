package com.longshun.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/*传统蓝牙开发*/
public class ClassicBluetoothDevActivity extends AppCompatActivity implements BluetoothDevicesReceiver.OnDevicesFindListener, IBondStateListener {

    private static final String TAG = "Bluetooth";
    private UUID UUID_SERVER = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;

    private ArrayList<String> listDeviceInfos;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayList<BluetoothDevice> listDevices = new ArrayList<>();

    private BluetoothDevicesReceiver devicesReceiver;
    private BluetoothDiscoverableReceiver bluetoothDiscoverableReceiver;
    private UUIDReceiver uuidReceiver;

    private ArrayList<BluetoothDevice> listBondDevices;
    private ArrayAdapter adapterForBondDevices;
    private List<String> listBondDeviceInfo;

    private int connectTime = 0;
    private boolean connected;
    private boolean connecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pri_dev);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        /*try {
            Method getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);

            ParcelUuid[] uuids = (ParcelUuid[]) getUuidsMethod.invoke(bluetoothAdapter, null);

            for (ParcelUuid uuid: uuids) {
                Log.d(TAG, "反射UUID: " + uuid.getUuid().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "反射失败UUID: " );
        }
*/

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
                        addDeviceToBondList(device);
                    }
                } else {
                    clearBondList();
                    Log.d(TAG, "onClick: 暂无配对设备");
                }
            }
        });

        //配对设备列表
        final ListViewForScrollView lvBondDevices = (ListViewForScrollView) findViewById(R.id.lv_bond_devices);
        listBondDevices = new ArrayList<>();
        listBondDeviceInfo = new ArrayList<>();
        adapterForBondDevices = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listBondDeviceInfo);
        lvBondDevices.setAdapter(adapterForBondDevices);
        lvBondDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //尝试进行设备连接
                BluetoothDevice device = listBondDevices.get(position);
                conn(device);
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
        final ListViewForScrollView lvDevices = (ListViewForScrollView) findViewById(R.id.lv_devices);
        listDeviceInfos = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listDeviceInfos);
        lvDevices.setAdapter(arrayAdapter);
        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //首先进行设备配对
                BluetoothDevice device = listDevices.get(position);
                bond(device);
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
        bluetoothDiscoverableReceiver.setiBondStateListener(this);
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bluetoothDiscoverableReceiver, filter);

        //注册UUID接收者
        uuidReceiver = new UUIDReceiver();
        registerReceiver(uuidReceiver, new IntentFilter(BluetoothDevice.ACTION_UUID));
    }

    /*清空配对列表*/
    private void clearBondList() {
        listBondDeviceInfo.clear();
        listBondDevices.clear();
        adapterForBondDevices.notifyDataSetChanged();
    }

    private void addDeviceToBondList(BluetoothDevice device) {
        if (!isAdded(listBondDevices, device)) {
            listBondDevices.add(device);
            listBondDeviceInfo.add(device.getName() + ":" + device.getAddress());
            adapterForBondDevices.notifyDataSetChanged();
            Log.d(TAG, "onClick:已配对设备 " + device.getName() + "：" + device.getAddress());
        }
    }

    /*配对*/
    private void bond(BluetoothDevice device) {
        try {
            // 连接建立之前的先配对
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                device.createBond();
                //Method creMethod = BluetoothDevice.class.getMethod("createBond");
                //creMethod.invoke(device);
            } else {
                Log.d(TAG, "bond: 已经配对，无需重复配对");
            }
        } catch (Exception e) {
            Log.d(TAG, "bond: 配对出错");
            e.printStackTrace();
        }
    }

    /*开始连接设备*/
    private void conn(BluetoothDevice device) {
        if (device != null) {
            Log.d(TAG, "conn: 配对成功,准备连接" + device.getName() + device.getAddress());
            //device.fetchUuidsWithSdp();
            //ParcelUuid[] uuids = device.getUuids();
            new ConnectThread(device).start();
        }
    }

    @Override
    public void findBluetoothDevices(BluetoothDevice device) {
        if (!isAdded(listDevices, device)) {
            listDevices.add(device);
            listDeviceInfos.add(device.getName() + ":" + device.getAddress());
            arrayAdapter.notifyDataSetChanged();
        }
    }

    /*过滤添加过的设备*/
    private boolean isAdded(List<BluetoothDevice> devices, BluetoothDevice device) {
        for (int i = 0; i < devices.size(); i++) {
            String address = devices.get(i).getAddress();
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
        unregisterReceiver(uuidReceiver);
        super.onDestroy();
    }

    @Override
    public void bondSuccess(BluetoothDevice device) {
        //刷新已配对列表
        addDeviceToBondList(device);
    }

    @Override
    public void bondFail(BluetoothDevice device) {

    }

    @Override
    public void bonding(BluetoothDevice device) {

    }

    /*连接线程*/
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(UUID_SERVER);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // TODO: 2017/3/4 处理连接部分
            connecting = true;
            connected = false;
            // Cancel discovery because it will slow down the connection
            if (bluetoothAdapter != null) {
                bluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "run: 取消扫描");
            }

            while (!connected && connectTime <= 10){
                try {
                    // Connect the device through the socket. This will block
                    // until it succeeds or throws an exception
                    mmSocket.connect();
                    connected = true;
                    Log.d(TAG, "run: 连接成功");
                } catch (IOException connectException) {
                    // Unable to connect; close the socket and get out
                    connected = false;
                    connectTime++;
                    try {
                        mmSocket.close();
                        Log.d(TAG, "run: 关闭客户端socket"+connectTime);
                    } catch (IOException closeException) {
                    }
                    return;
                } finally {
                    connecting = false;
                }
            }
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            } finally {
                connecting = false;
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    // TODO: 2017/3/4  
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

}
