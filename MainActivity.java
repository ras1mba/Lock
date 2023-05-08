package com.astar.lock;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.astar.lock.databinding.ActivityMainBinding;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private LockController controller;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = manager.getAdapter();
        BluetoothDevice device = btAdapter.getRemoteDevice("00:22:06:01:AE:BE");

        controller = new LockController(btAdapter);
        controller.setConnectionEventsCallback(connectionEvents);

        binding.buttonKey1.setOnClickListener(onClickListener);
        binding.buttonKey2.setOnClickListener(onClickListener);
        binding.buttonKey3.setOnClickListener(onClickListener);
        binding.buttonKey4.setOnClickListener(onClickListener);
        binding.buttonKey5.setOnClickListener(onClickListener);
        binding.buttonKey6.setOnClickListener(onClickListener);
        binding.buttonKey7.setOnClickListener(onClickListener);
        binding.buttonKey8.setOnClickListener(onClickListener);
        binding.buttonKey9.setOnClickListener(onClickListener);
        binding.buttonKey0.setOnClickListener(onClickListener);
        binding.buttonStar.setOnClickListener(onClickListener);
        binding.buttonConnect.setOnClickListener(onClickListener);
        binding.buttonDisconnect.setOnClickListener(onClickListener);
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button button = (Button) v;
            String textButton = button.getText().toString();
            if (textButton.equals("Connect")) {
                connect();
                return;
            }
            if (textButton.equals("Disconnect")) {
                controller.disconnect();
                return;
            }
            controller.sendCommand(textButton); // Modified line
        }
    };

    private final ConnectionEvents connectionEvents = new ConnectionEvents() {
        @Override
        public void onConnected() {
            binding.textConnectStatus.setText("Connected");
        }

        @Override
        public void onDisconnected() {
            binding.textConnectStatus.setText("Disconnected");
        }

        @Override
        public void onWriteData(byte[] data) {
            showWriteData(data);
        }

        @Override
        public void onReadData(byte[] data) {
            showReadData(data);
        }
    };

    private void connect() {
        String address = binding.textMacAddress.getText().toString();
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            controller.connect(address);
        } else {
            showToast("Неверный мак адрес");
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void showWriteData(byte[] data) {
        runOnUiThread(() -> binding.textSendingData.setText("Write: " + hexString(data)));
    }

    private void showReadData(byte[] data) {
        runOnUiThread(() -> binding.textReceivedData.setText("Received: " + hexString(data)));
    }

    private String hexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append("");
        for (byte b : data) {
            sb.append(String.format("", b)).append(" ");
        }
        return sb.toString();
    }
}