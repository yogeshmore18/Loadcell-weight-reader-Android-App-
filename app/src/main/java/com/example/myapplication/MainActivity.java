package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceListAdapter;
    private ArrayList<String> deviceList = new ArrayList<>();

    private ListView deviceListView;
    private Button scanButton;
    private Button getDataButton;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;

    private BluetoothDevice selectedDevice;
    private String loadCellReadings;

    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceListView = findViewById(R.id.deviceListView);
        scanButton = findViewById(R.id.scanButton);
        getDataButton = findViewById(R.id.getDataButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        deviceListView.setAdapter(deviceListAdapter);

        scanButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                scanForDevices();
            }
        });

        getDataButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                if (selectedDevice != null && isConnected) {
                    connectToDeviceWithPassword(selectedDevice, "your_password_here"); // Replace "your_password_here" with the actual password
                    displayLoadCellReadings();
                } else {
                    Toast.makeText(this, "First connect to a device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            String deviceInfo = deviceList.get(position);
            String[] deviceInfoArray = deviceInfo.split("\n");
            String deviceAddress = deviceInfoArray[1];
            selectedDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
            connectToDeviceWithPassword(selectedDevice, "your_password_here"); // Replace "your_password_here" with the actual password
        });
    }

    private void connectToDeviceWithPassword(BluetoothDevice device, String password) {
        if (password.equals("your_password_here")) {
            if (!isConnected) {
                Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                isConnected = true;
            }

            try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                socket.connect();
                InputStream inputStream = socket.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                loadCellReadings = bufferedReader.readLine();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayLoadCellReadings() {
        if (loadCellReadings != null) {
            TextView weightDataTextView = findViewById(R.id.weightDataTextView);
            weightDataTextView.setText("Weight : " + loadCellReadings);
        } else {
            Toast.makeText(this, "No data received from the device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void scanForDevices() {
        deviceList.clear();
        if (bluetoothAdapter != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            } else {
                if (bluetoothAdapter.isEnabled()) {
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                    try {
                        bluetoothAdapter.startDiscovery();
                        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "Bluetooth is not enabled.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return false;
        } else {
            return true;
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                String deviceInfo = deviceName + "\n" + deviceAddress;
                deviceList.add(deviceInfo);
                deviceListAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}