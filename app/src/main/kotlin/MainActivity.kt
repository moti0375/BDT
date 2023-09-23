package com.bdt

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.bdt.settings.Devices
import com.bdt.settings.SettingsActivity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity(), View.OnClickListener, OnSharedPreferenceChangeListener {
    private lateinit var upButton: Button
    private lateinit var downButton: Button
    private lateinit var plusButton: Button
    private lateinit var minusButton: Button
    private lateinit var ivBtStatus: ImageView
    private lateinit var output: TextView
    private lateinit var output2: TextView
    private lateinit var progressDialog: ProgressDialog
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private val devices = ArrayList<BluetoothDevice>()
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private lateinit var mToolbar: Toolbar
    private var mDeviceVersion = Devices.XICOY_V6
    private val btPermissions = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED == action) {
                Log.d(TAG, "Starting BT discovery")
                progressDialog = ProgressDialog.show(
                    context,
                    "Scanning for BT devices", "Please wait...", true
                )
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                == action
            ) {
                Log.d(TAG, "BT discovery finished")
                progressDialog!!.dismiss()
                showScanResults(false)
            } else if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent
                    .getParcelableExtra<Parcelable>(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                Log.d(TAG, "Found BT mDeviceVersion: " + device!!.name)
                devices.add(device)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("myTag", "BDT created")
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        mToolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(mToolbar)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        updatePreferences(preferences)
        upButton = findViewById(R.id.bUp)
        upButton.setOnClickListener(this)
        downButton = findViewById<View>(R.id.bDown) as Button
        downButton.setOnClickListener(this)
        plusButton = findViewById<View>(R.id.bPlus) as Button
        plusButton.setOnClickListener(this)
        minusButton = findViewById<View>(R.id.bMinus) as Button
        minusButton.setOnClickListener(this)
        output = findViewById<View>(R.id.tvLine1) as TextView
        output.text = ""
        output2 = findViewById<View>(R.id.tvLine2) as TextView
        output2.text = ""
        ivBtStatus = findViewById<View>(R.id.ivBtStatus) as ImageView
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothAdapter?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showPermissionsDialog()
            } else if (allPermissionsGranted()) {
                checkAndEnableBt()
            }
        } ?: run {
            Toast.makeText(
                this, "Your Device does not support BT!!!",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        }
        if (id == R.id.action_search_device) {
            if (mBluetoothAdapter != null) {
                devices.clear()
                // List paired devices before scanning
                val pairedDevices = mBluetoothAdapter?.bondedDevices ?: emptySet()
                // If there are paired devices
                if (pairedDevices.isNotEmpty()) {
                    // Loop through paired devices
                    for (device in pairedDevices) {
                        devices.add(device)
                    }
                    showScanResults(true)
                } else {
                    scanForBTDevices()
                }
            } else {
                Toast.makeText(this, "This device doesn't has Bluetooth support", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.bUp -> {
                val raw = byteArrayOf(0xde.toByte(), 0xdf.toByte(), 0x70.toByte(), 0x43.toByte(), 0xb3.toByte())
                mConnectedThread?.write(raw)
            }

            R.id.bDown -> {
                val raw = byteArrayOf(0xde.toByte(), 0xdf.toByte(), 0x70.toByte(), 0x44.toByte(), 0xb4.toByte())
                mConnectedThread?.write(raw)
            }

            R.id.bMinus -> {
                val raw = byteArrayOf(0xde.toByte(), 0xdf.toByte(), 0x70.toByte(), 0x42.toByte(), 0xb2.toByte())
                mConnectedThread?.write(raw)
            }

            R.id.bPlus -> {
                val raw = byteArrayOf(0xde.toByte(), 0xdf.toByte(), 0x70.toByte(), 0x41.toByte(), 0xb1.toByte())
                mConnectedThread?.write(raw)
            }
        }
    }

    private fun showScanResults(scan: Boolean) {
        val builderSingle = AlertDialog.Builder(
            this@MainActivity
        )
        builderSingle.setIcon(R.drawable.ic_launcher)
        if (!scan) {
            builderSingle.setTitle("Select BT Device:")
        } else {
            builderSingle.setTitle("Select from paired devices:")
        }
        val arrayAdapter = ArrayAdapter<String>(
            this@MainActivity, android.R.layout.select_dialog_singlechoice
        )
        for (d in devices) {
            arrayAdapter.add(d.name)
        }
        if (scan) {
            builderSingle.setPositiveButton(
                "Search more"
            ) { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
                scanForBTDevices()
            }
        }
        builderSingle.setNegativeButton(
            "Cancel"
        ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        builderSingle.setAdapter(
            arrayAdapter
        ) { dialog: DialogInterface?, which: Int ->
            // chosenDevice = devices.get(which);
            mConnectThread = ConnectThread(devices[which])
            mConnectThread?.start()
        }
        builderSingle.show()
    }

    private fun requestAppPermissions() {
        requestPermissions(btPermissions, BT_PERMISSIONS_REQUEST_CODE)
    }

    private fun checkAndEnableBt() {
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(
                BluetoothAdapter.ACTION_REQUEST_ENABLE
            )
            try {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } catch (e: SecurityException) {
                Toast.makeText(this, "Unable to enable BT, please enable BT manually", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun allPermissionsGranted(): Boolean {
        for (permission in btPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun showPermissionsDialog() {
        if (!allPermissionsGranted()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.app_name))
            builder.setMessage(getString(R.string.permissions_explanation))
            builder.setPositiveButton(R.string.permissions_yes) { dialog, id -> requestAppPermissions() }
            builder.setNegativeButton(R.string.permissions_no) { dialog, id ->
                finish()
                Toast.makeText(this, getText(R.string.permissions_no_toast), Toast.LENGTH_LONG).show()
            }
            builder.create().show()
        }
    }

    private fun scanForBTDevices() {
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothDevice.ACTION_UUID)
        registerReceiver(mReceiver, filter)
        if (mBluetoothAdapter!!.isDiscovering) {
            mBluetoothAdapter!!.cancelDiscovery()
        }
        mBluetoothAdapter!!.startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        mConnectedThread?.let {
            it.cancel()
            mConnectedThread = null
        }

        mConnectThread?.let {
            it.cancel()
            mConnectThread = null
        }

        try {
            unregisterReceiver(mReceiver)
        } catch (e: IllegalArgumentException) {
            Log.d(
                TAG,
                "Error unregistering broadcast receiver:" + e.message
            )
        }
        if (mBluetoothAdapter != null && mBluetoothAdapter?.isDiscovering == true) {
            mBluetoothAdapter?.cancelDiscovery()
        }
        mBluetoothAdapter = null
    }

    private fun startThreadConnected(socket: BluetoothSocket?) {
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread!!.start()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        updatePreferences(sharedPreferences)
    }

    private fun updatePreferences(sharedPreferences: SharedPreferences) {
        val device = Integer.valueOf(sharedPreferences.getString("DeviceKey", "10"))
        when (device) {
            10 -> {
                mDeviceVersion = Devices.XICOY_V6
                supportActionBar?.title = "$title - Xicoy V6"
            }

            20 -> {
                mDeviceVersion = Devices.XICOY_V10
                supportActionBar?.title = "$title - Xicoy V10"
            }
        }
    }

    inner class ConnectThread(private val bluetoothDevice: BluetoothDevice) : Thread() {
        private var bluetoothSocket: BluetoothSocket? = null

        init {
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid)
            } catch (e: IOException) {
                Log.d(TAG, "Error creating socket", e)
            }
        }

        override fun run() {
            var success = false
            try {
                bluetoothSocket?.connect()
                success = true
                runOnUiThread { ivBtStatus!!.setImageDrawable(ActivityCompat.getDrawable(this@MainActivity, R.drawable.ic_bt_conencted)) }
            } catch (e: IOException) {
                Log.d(TAG, "Error connecting to socket", e)
                try {
                    bluetoothSocket?.close()
                } catch (e1: IOException) {
                    Log.d(TAG, "Error closing socket", e)
                }
            }
            if (success) {
                startThreadConnected(bluetoothSocket)
            } else {
                // fail
            }
        }

        fun cancel() {
            Toast.makeText(
                applicationContext, "close bluetoothSocket",
                Toast.LENGTH_LONG
            ).show()
            try {
                bluetoothSocket!!.close()
                runOnUiThread { ivBtStatus.setImageDrawable(ActivityCompat.getDrawable(this@MainActivity, R.drawable.ic_bt_disconnected)) }
            } catch (e: IOException) {
                Log.d(TAG, "Error closing socket", e)
            }
        }
    }

    private inner class ConnectedThread(private val connectedBluetoothSocket: BluetoothSocket?) : Thread() {
        private val connectedInputStream: InputStream?
        private val connectedOutputStream: OutputStream?
        private var send = false
        private lateinit var bufferOut: ByteArray

        init {
            var `in`: InputStream? = null
            var out: OutputStream? = null
            try {
                `in` = connectedBluetoothSocket!!.inputStream
                out = connectedBluetoothSocket.outputStream
            } catch (e: IOException) {
                Log.d(TAG, "Error creating temp streams", e)
            }
            connectedInputStream = `in`
            connectedOutputStream = out
        }

        override fun run() {
            val buffer = ArrayList<Int>()
            while (connectedBluetoothSocket!!.isConnected) {
                try {
                    val a = connectedInputStream!!.read()
                    if (a == 0xfc) {
                        val b = connectedInputStream.read()
                        if (b == 0xfd) {
                            sleep(100)
                            if (send) {
                                connectedOutputStream!!.write(bufferOut)
                                send = false
                            }
                            val buffer2 = IntArray(buffer.size)
                            val sb = StringBuilder()
                            val sb2 = StringBuilder()
                            for (i in buffer.indices) {
                                when (mDeviceVersion) {
                                    Devices.XICOY_V6 -> {
                                        Log.d(TAG, "Xicoy V6")
                                        buffer2[i] = buffer[i]
                                    }

                                    Devices.XICOY_V10 -> {
                                        Log.d(TAG, "Xicoy V10")
                                        buffer2[i] = (buffer[i] xor 0xff) + buffer[33]
                                    }

                                    else -> buffer2[i] = buffer[i]
                                }
                                buffer2[i] = buffer2[i] and 0xff
                                if (buffer2[i] == 223) {
                                    buffer2[i] = 176
                                }
                            }
                            if (buffer2.size > 40) {
                                for (i in 0..15) {
                                    sb.append(buffer2[i].toChar())
                                    sb2.append(buffer2[i + 16].toChar())
                                }
                            }
                            val s = sb.toString()
                            val s2 = sb2.toString()
                            runOnUiThread {
                                output!!.text = s
                                output2!!.text = s2
                            }
                            buffer.clear()
                        } else {
                            buffer.add(a)
                            buffer.add(b)
                        }
                    } else {
                        buffer.add(a)
                    }
                } catch (e: IOException) {
                    val msgConnectionLost = """
                        Connection lost:
                        ${e.message}
                        """.trimIndent()
                    Log.d(TAG, msgConnectionLost)
                } catch (ex: Exception) {
                    //do nothing
                }
            }
        }

        fun write(buffer: ByteArray) {
            /*try {
                connectedOutputStream.write(buffer);
			} catch (IOException e) {
				Log.d(TAG, "Error wrirting message to stream", e);
			}*/
            bufferOut = buffer
            send = true
        }

        fun cancel() {
            try {
                connectedBluetoothSocket!!.close()
            } catch (e: IOException) {
                Log.d(TAG, "Error closing socket", e)
            }
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val TAG = "WDT"
        private val uuid = UUID
            .fromString("00001101-0000-1000-8000-00805f9b34fb")
        private const val BT_PERMISSIONS_REQUEST_CODE = 100
    }
}