package com.example.miguel.bogglegame;


import android.bluetooth.BluetoothSocket;

/**
 * Created by tpatecky on 3/7/2017.
 */

public class SocketHandler {
    private static BluetoothSocket socket;

    public static synchronized BluetoothSocket getSocket(){
        return socket;
    }

    public static synchronized void setSocket(BluetoothSocket socket){
        SocketHandler.socket = socket;
    }
}
