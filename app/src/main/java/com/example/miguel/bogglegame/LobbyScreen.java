package com.example.miguel.bogglegame;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class LobbyScreen extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private int difficulty;
    private GameMode mode;

    ArrayAdapter<String> deviceListAdapter;
    Set<BluetoothDevice> devicesArray;
    ArrayList<String> pairedDevices;
    ArrayList<BluetoothDevice> devices;

    ListView deviceList;
    //Button hostButton;

    protected static final int SUCCESS_CLIENT_CONNECT = 0;
    protected static final int SUCCESS_HOST_CONNECT = 1;

    //array index = difficulty
    /*public static final UUID basic2p[] = {UUID.fromString("6f4932cf-e466-4207-befc-8903535ea09b"),
                                        UUID.fromString("8b83eb1d-5e89-43aa-8ea6-3e95f885117e"),
                                        UUID.fromString("537ab9a1-0aec-416f-800f-243d37b7013c")};
    public static final UUID cutthroat2p[] = {UUID.fromString("0610a7fd-2962-4015-ad39-5dec674245dd"),
                                            UUID.fromString("cf57fe1e-4360-43a1-96e6-7ee57f1c8e69"),
                                            UUID.fromString("3cfb97fd-3f64-46ff-ad88-96252698be37")};*/
    public static final UUID myUUID = UUID.fromString("6f4932cf-e466-4207-befc-8903535ea09b");
    AcceptThread accept;

    BluetoothAdapter btAdapter;
    IntentFilter filter;
    BroadcastReceiver receiver;
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case SUCCESS_CLIENT_CONNECT:
                    //connected as client, start the fucking game
                    Toast.makeText(getApplicationContext(), "Connection Established as client", Toast.LENGTH_SHORT).show();
                    startGame((BluetoothSocket) msg.obj, false);
                    break;
                case SUCCESS_HOST_CONNECT:
                    Toast.makeText(getApplicationContext(), "Connection Established as host", Toast.LENGTH_SHORT).show();
                    startGame((BluetoothSocket) msg.obj, true);
                    break;
            }
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_screen);
        init();
        if(btAdapter==null){
            Toast.makeText(getApplicationContext(), "This device is not bluetooth capable.", Toast.LENGTH_SHORT).show();
            goBackToSplash();

        } else {
            if(!btAdapter.isEnabled()) {
                turnOnBT();
            }
            getPairedDevices();
            startDiscovery();
            accept = new AcceptThread();
            accept.start();
        }
    }

    private void startDiscovery() {
        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
    }

    private void turnOnBT() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, 1);
    }

    private void getPairedDevices() {
        devicesArray = btAdapter.getBondedDevices();
        if(devicesArray.size()>0){
            for(BluetoothDevice device:devicesArray){
                pairedDevices.add(device.getName());
            }
        }
    }

    private void goBackToSplash() {
        Intent intent = new Intent(getApplicationContext(), SplashScreen.class);
        startActivity(intent);
        finish();
    }

    private void startGame(BluetoothSocket socket, boolean is_host) {
        SocketHandler.setSocket(socket);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("EXTRA_DIFFICULTY", difficulty);
        intent.putExtra("EXTRA_MODE", mode);
        intent.putExtra("EXTRA_IS_HOST", is_host);
        startActivity(intent);
        finish();
    }

    private void init() {
        //get game mode and difficulty from splash
        mode = (GameMode) getIntent().getSerializableExtra("EXTRA_MODE");
        difficulty = getIntent().getIntExtra("EXTRA_DIFFICULTY", 0);
        //set UUID
        /*if(mode == GameMode.BasicTwoPlayer) {
            myUUID = basic2p[difficulty];
        } else {
            myUUID = cutthroat2p[difficulty];
        }*/
        //hostButton = (Button)findViewById(R.id.hostButton);
        deviceList = (ListView)findViewById(R.id.deviceList);
        deviceList.setOnItemClickListener(this);
        deviceListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,0);
        deviceList.setAdapter(deviceListAdapter);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new ArrayList<String>();
        devices = new ArrayList<BluetoothDevice>();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);
                    String s = "";
                    for(int a = 0; a < pairedDevices.size(); a++){
                        if(device.getName().equals(pairedDevices.get(a))){
                            //append
                            s = " (Paired) ";
                            break;
                        }
                    }
                    deviceListAdapter.add(device.getName()+s+"\n"+device.getAddress());
                } else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    if(btAdapter.getState() == btAdapter.STATE_OFF){
                        turnOnBT();
                    }
                }

            }
        };
        myRegisterReceiver();


        /*hostButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //try to make discoverable
                if(btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(discoverableIntent);
                }
                //start a thread to wait for a client to connect
                if(accept == null) {
                    accept = new AcceptThread();
                    accept.start();
                } else {
                    accept.cancel();
                    accept.start();
                }
            }
        });*/

    }

    private void myRegisterReceiver() {
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        if(accept != null && accept.isAlive()) {
            accept.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        myRegisterReceiver();
        accept.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if(accept != null && accept.isAlive()) {
            accept.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_CANCELED){
            Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
            goBackToSplash();
        }
    }



    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        try {

            if (deviceListAdapter.getItem(arg2).contains("(Paired)")) {
                //connect to device as client
                if (btAdapter.isDiscovering()) {
                    btAdapter.cancelDiscovery();
                }
                BluetoothDevice selectedDevice = devices.get(arg2);
                accept.cancel();
                ConnectThread connect = new ConnectThread(selectedDevice);
                connect.start();
            } else {
                Toast.makeText(getApplicationContext(), "device is not paired, screw u", Toast.LENGTH_SHORT).show();
            }
        } catch (NullPointerException e) {
            return;
        }
    }

    //connecting as client
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
                tmp = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {

            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)

            mHandler.obtainMessage(SUCCESS_CLIENT_CONNECT, mmSocket).sendToTarget();
        }



        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    //listen for connection as server
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = btAdapter.listenUsingRfcommWithServiceRecord("Boggle", myUUID);
            } catch (IOException e) {
                //Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    //Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    mHandler.obtainMessage(SUCCESS_HOST_CONNECT, socket).sendToTarget();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        //Log.e(TAG, "Could not close the connect socket", e);
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                //Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}
