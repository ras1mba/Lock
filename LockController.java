package com.astar.lock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class LockController {
    //00000000-0000-1000-8000-00805F9B34FB
    // 00001101-0000-1000-8000-00805F9B34FB
    public static final UUID BASE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter btAdapter;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private ConnectionEvents callback;

    public LockController(BluetoothAdapter adapter) {
        this.btAdapter = adapter;
    }

    public void setConnectionEventsCallback(ConnectionEvents callback) {
        this.callback = callback;
    }

    public void connect(String address) {
        if (connectThread == null) {
            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            connectThread = new ConnectThread(device);
            connectThread.start();
        } else {
            Log.i("Controller", "already connect");
        }
    }

    public void disconnect() {
        if (connectThread != null)
            connectThread.cancel();
        if (connectedThread != null)
            connectedThread.cancel();
        closeConnection();
    }

    private void closeConnection() {
        connectedThread = null;
        connectThread = null;
        if (callback != null) {
            callback.onDisconnected();
        }
    }

    private void manageMyConnectedSocket(BluetoothSocket mmSocket) {
        connectedThread = new ConnectedThread(mmSocket);
        connectedThread.start();
    }

    public void sendCommand(String commandStr) {
        byte[] command = commandStr.getBytes();
        if (connectedThread != null) {
            connectedThread.write(command);
        } else {
            Log.e("Controller", "sendCommand: error! Not connected!");
        }
    }

    class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                // tmp = device.createInsecureRfcommSocketToServiceRecord(BASE_UUID);
                tmp = device.createRfcommSocketToServiceRecord(BASE_UUID);
            } catch (IOException e) {
                Log.e("Connect", "Socket's create() method failed", e);
            } catch (SecurityException e) {
                Log.e("Connect", "Connection error, no security permissions", e);
            }
            mmSocket = tmp;
        }

        public void run() {

            try {
                // Cancel discovery because it otherwise slows down the connection.
                btAdapter.cancelDiscovery();
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                if (callback != null) {
                    callback.onConnected();
                }
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    closeConnection();
                } catch (IOException closeException) {
                    Log.e("Connect", "Could not close the client socket", closeException);
                }
                return;
            } catch (SecurityException e) {
                Log.e("Connect", "Нет разрешений безопасности ", e);
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
                closeConnection();
            } catch (IOException e) {
                Log.e("Connect", "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e("Connection", "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("Connection", "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    byte[] readData = new byte[numBytes];
                    System.arraycopy(mmBuffer, 0, readData, 0, numBytes);
                    if (callback != null) {
                        callback.onReadData(readData);
                    }
                    // Send the obtained bytes to the UI activity.
                    // Message readMsg = handler.obtainMessage(
                    //         MessageConstants.MESSAGE_READ, numBytes, -1,
                    //         mmBuffer);
                    // readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d("Connection", "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                if (callback != null) {
                    callback.onWriteData(bytes);
                }

                // Share the sent message with the UI activity.
                // Message writtenMsg = handler.obtainMessage(
                //         MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                // writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e("Connection", "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                // Message writeErrorMsg =
                //         handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                // Bundle bundle = new Bundle();
                // bundle.putString("toast",
                //         "Couldn't send data to the other device");
                // writeErrorMsg.setData(bundle);
                // handler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Connection", "Could not close the connect socket", e);
            }
        }
    }

}
