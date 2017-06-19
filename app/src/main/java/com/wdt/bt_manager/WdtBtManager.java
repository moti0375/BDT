package com.wdt.bt_manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.wdt.settings.Devices;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Created by motibartov on 19/06/2017.
 */

public class WdtBtManager {

    private static final UUID uuid = UUID
            .fromString("00001101-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;

    Devices mDeviceVersion = Devices.XICOY_V6;
    WdtBtEventListener mBtEventListener;
    Context mContext;

    BtConnThread mConnThread;
    BtCommThread mCommThread;
    private ArrayList<BluetoothDevice> mAvailableDevices = new ArrayList<>();

    public WdtBtManager(Context context, Devices device, WdtBtEventListener eventListener) {
        mContext = context;
        mDeviceVersion = device;
        mBtEventListener = eventListener;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkDeviceBt();
    }

    private void checkDeviceBt() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(mContext, "Your Device does not support BT!!!",
                    Toast.LENGTH_LONG).show();
            //   finish();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mBtEventListener.onBtAdapterUnavailable();
            }
        }
    }

    public void setEduDevice(Devices device) {
        mDeviceVersion = device;
    }

    public void connectBt(int which) {
        mConnThread = new BtConnThread(mAvailableDevices.get(which));
        mConnThread.start();
    }

    public void addBtDevice(BluetoothDevice newDevice) {
        mAvailableDevices.add(newDevice);
    }

    public void closeBtConnection() {

    }


    public void writeBtdata(byte[] buffer) {

    }


    public void searchBtDevices() {
        mAvailableDevices.clear();
        // List paired devices before scanning
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                .getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                mAvailableDevices.add(device);
            }
            //  showScanResults(true);
        } else {
            //    scanForBTDevices();
        }
    }


    private class BtCommThread extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        private boolean send = false;

        private byte[] bufferOut;

        public BtCommThread(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "Error creating temp streams", e);
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            ArrayList<Integer> buffer = new ArrayList<Integer>();

            while (connectedBluetoothSocket.isConnected()) {
                try {
                    int a = connectedInputStream.read();
                    if (a == (int) 0xfc) {
                        int b = connectedInputStream.read();
                        if (b == (int) 0xfd) {
                            sleep(100);
                            if (send) {
                                connectedOutputStream.write(bufferOut);
                                send = false;
                            }
                            int[] buffer2 = new int[buffer.size()];

                            StringBuilder sb = new StringBuilder();
                            StringBuilder sb2 = new StringBuilder();
                            for (int i = 0; i < buffer.size(); i++) {

                                switch (mDeviceVersion) {
                                    case XICOY_V6:
                                        Log.d(TAG, "Xicoy V6");
                                        buffer2[i] = (buffer.get(i));
                                        break;
                                    case XICOY_V10:
                                        Log.d(TAG, "Xicoy V10");
                                        buffer2[i] = ((buffer.get(i) ^ (int) 0xff) + buffer
                                                .get(33));
                                        break;
                                    default:
                                        buffer2[i] = (buffer.get(i));
                                        break;
                                }

                                buffer2[i] = buffer2[i] & (int) 0xff;

                                if (buffer2[i] == (int) 223) {
                                    buffer2[i] = (int) 176;
                                }
                            }
                            if (buffer2.length > 40) {
                                for (int i = 0; i < 16; i++) {

                                    sb.append((char) buffer2[i]);
                                    sb2.append((char) buffer2[i + 16]);
                                }
                            }
                            final String s = sb.toString();
                            final String s2 = sb2.toString();
                            mBtEventListener.onEcuData(s, s2);
                            buffer.clear();
                        } else {
                            buffer.add(a);
                            buffer.add(b);
                        }
                    } else {
                        buffer.add(a);
                    }
                } catch (IOException e) {
                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    Log.d(TAG, msgConnectionLost);
                } catch (Exception ex) {
                    //do nothing
                }
            }
        }


        public void write(byte[] buffer) {
            /*try {
                connectedOutputStream.write(buffer);
			} catch (IOException e) {
				Log.d(TAG, "Error wrirting message to stream", e);
			}*/
            bufferOut = buffer;
            send = true;
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Error closing socket", e);
            }
        }
    }

    public class BtConnThread extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;

        private BtConnThread(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = bluetoothDevice
                        .createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.d(TAG, "Error creating socket", e);
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
                mBtEventListener.onBtConnected(true);
            } catch (IOException e) {
                Log.d(TAG, "Error connecting to socket", e);
                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    Log.d(TAG, "Error closing socket", e);
                }
            }

            if (success) {
                startCommThread(bluetoothSocket);
            } else {
                // fail
            }
        }

        public void cancel() {

            try {
                bluetoothSocket.close();
                mBtEventListener.onBtConnected(false);
            } catch (IOException e) {
                Log.d(TAG, "Error closing socket", e);
            }

        }

    }

    private void startCommThread(BluetoothSocket socket) {

        mCommThread = new BtCommThread(socket);
        mConnThread.start();
    }


    public interface WdtBtEventListener {
        void onBtAdapterUnavailable();

        void onBtConnected(boolean isConnected);

        void onEcuData(String s1, String s2);
    }
}
