
package com.robocar.controller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    BluetoothAdapter adapter;
    BluetoothSocket socket;
    OutputStream output;
    TextView status;

    final UUID uuid = UUID.fromString(
        "00001101-0000-1000-8000-00805F9B34FB"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = BluetoothAdapter.getDefaultAdapter();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(20, 20, 20, 20);
        root.setBackgroundColor(Color.rgb(20, 25, 35));

        TextView title = new TextView(this);
        title.setText("ROBO CAR");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setText("Bluetooth Disconnected");
        status.setTextColor(Color.YELLOW);
        status.setGravity(Gravity.CENTER);
        status.setPadding(10, 20, 10, 20);
        root.addView(status);

        Button connect = makeButton("CONNECT HC-05");
        root.addView(connect);

        connect.setOnClickListener(v -> chooseDevice());

        addMovementRow(root, null, "FORWARD", 'F', null);
        addMovementRow(root, "LEFT", 'L', "STOP", 'S',
                       "RIGHT", 'R');
        addMovementRow(root, null, "BACKWARD", 'B', null);

        setContentView(root);
    }

    Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        return b;
    }

    void addMovementRow(LinearLayout root,
                        String left, char lc,
                        String middle, char mc,
                        String right, char rc) {

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);

        if (left != null) addControl(row, left, lc);
        if (middle != null) addControl(row, middle, mc);
        if (right != null) addControl(row, right, rc);

        root.addView(row);
    }

    void addMovementRow(LinearLayout root,
                        String unused, String label,
                        char command, String unused2) {

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        addControl(row, label, command);
        root.addView(row);
    }

    void addControl(LinearLayout row,
                    String label, char command) {

        Button b = makeButton(label);
        row.addView(b, new LinearLayout.LayoutParams(
            0, 110, 1
        ));

        b.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                send(command);
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
                send('S');
                return true;
            }
            return true;
        });
    }

    void chooseDevice() {

        if (adapter == null) {
            status.setText("Bluetooth not supported");
            return;
        }

        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT
                }, 101
            );
            return;
        }

        if (!adapter.isEnabled()) {
            startActivityForResult(
                new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                102
            );
            return;
        }

        try {
            Set<BluetoothDevice> paired =
                adapter.getBondedDevices();

            ArrayList<BluetoothDevice> devices =
                new ArrayList<>(paired);

            ArrayList<String> names = new ArrayList<>();

            for (BluetoothDevice d : devices) {
                names.add(d.getName() + " (" + d.getAddress() + ")");
            }

            if (devices.isEmpty()) {
                status.setText("Pair HC-05 in phone settings first");
                return;
            }

            new AlertDialog.Builder(this)
                .setTitle("Select Bluetooth Device")
                .setItems(names.toArray(new String[0]),
                    (dialog, which) ->
                        connectDevice(devices.get(which)))
                .show();

        } catch (SecurityException e) {
            status.setText("Bluetooth permission required");
        }
    }

    void connectDevice(BluetoothDevice device) {

        status.setText("Connecting...");

        new Thread(() -> {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid);
                socket.connect();
                output = socket.getOutputStream();

                runOnUiThread(() ->
                    status.setText("HC-05 Connected"));

            } catch (Exception e) {
                runOnUiThread(() ->
                    status.setText("Connection failed"));
                closeConnection();
            }
        }).start();
    }

    void send(char command) {
        try {
            if (output != null) {
                output.write(command);
                output.flush();
            }
        } catch (Exception e) {
            status.setText("Bluetooth disconnected");
            closeConnection();
        }
    }

    void closeConnection() {
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        socket = null;
        output = null;
    }

    @Override
    protected void onDestroy() {
        send('S');
        closeConnection();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] results) {

        super.onRequestPermissionsResult(
            requestCode, permissions, results);

        if (requestCode == 101 &&
            results.length > 0 &&
            results[0] == PackageManager.PERMISSION_GRANTED) {
            chooseDevice();
        } else {
            status.setText("Bluetooth permission denied");
        }
    }
}
