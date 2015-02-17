package com.example.timmae.test;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.timmae.test.android.RotaryKnobView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;




public class MainActivity extends Activity {


    private Context mContext;

    private TextView m_edit_log;
    private TextView speed_log;
    private TextView direction_log;
    private TextView device_text;
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private Button m_btn_connect;
    private Button m_btn_send;
    private Button m_btn_choose_device;
    private ConnectThread mThrConnect;
    private ConnectedThread mThrConnected;
    private RotaryKnobView mSpeedknob;
    private ArrayList<String> m_devicenames;
    private ArrayList<String> m_deviceaddresses;
    private String m_chosendevice;
    private String m_deviceaddress;
    public  ArrayList<String> m_devices;

    private int mSpeed;
    private int mDirection;

    private AlertDialog.Builder mDlgBuilder;
    private String mMsg = "";
    private EditText mDlgInput;

    private boolean mConnected = false;

    private BluetoothDevice mBTdevice;
    private ArrayList<BluetoothDevice> m_devicelist= new ArrayList<BluetoothDevice>();
    private Button m_btn_disconnect;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        sharedPref = getPreferences(MODE_PRIVATE);
        editor = sharedPref.edit();
        setContentView(R.layout.activity_main);
        m_devicenames=new ArrayList<String>();
        m_deviceaddresses= new ArrayList<String>();
        m_devices= new ArrayList<String>();
        device_text = (TextView) findViewById(R.id.device_chosen);
        m_chosendevice=sharedPref.getString("devname",null);
        m_deviceaddress=sharedPref.getString("devaddr",null);
        if(m_chosendevice!=null)
            device_text.setText(m_chosendevice);
        mContext = this.getApplicationContext();
        speed_log=(TextView) findViewById(R.id.show_speed_textView);
        direction_log=(TextView) findViewById(R.id.show_direction_textView);
        mDlgBuilder = new AlertDialog.Builder(this);
        mDlgBuilder.setTitle("Title");


        if(mBluetoothAdapter == null)
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Set up the buttons
        mDlgBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mMsg = mDlgInput.getText().toString();
                if(mThrConnected != null) {

                    if(mMsg.startsWith("calc")) {
                        String echo;
                        echo = mThrConnected.talk(mMsg + "\n");
                    }
                    else {
                        writeLog("try to write data ...");
                        mThrConnected.write((mMsg + "\n").getBytes());
                    }
                }
                else {
                    writeLog("not connected!");
                }
            }
        });

        mDlgBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        m_edit_log = (TextView) findViewById(R.id.editText);
        m_btn_connect = (Button) findViewById(R.id.btn_connect);
        m_btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectSegway();

            }
        });
        m_btn_choose_device = (Button) findViewById(R.id.button_device);
        m_btn_disconnect = (Button) findViewById(R.id.btn_disconnect);
        m_btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectBT();
            }
        });
        m_btn_send = (Button) findViewById(R.id.btn_send);
        m_btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                // Set up the input
                mDlgInput = new EditText(mContext);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                mDlgInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                mDlgInput.setTextColor(Color.BLACK);
                mDlgInput.setHint(R.string.hint_msg);
                mDlgInput.setHintTextColor(Color.LTGRAY);
                mDlgBuilder.setView(mDlgInput);

                AlertDialog dialog = mDlgBuilder.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();

            }
        });

        m_btn_choose_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_deviceaddresses.clear();
                m_devicenames.clear();
                m_devices.clear();
                m_devicelist.clear();
                clearLog();
                if (mBluetoothAdapter == null) {
                    writeLog("Device does not support Bluetooth");
                    return;
                }

                if (!mBluetoothAdapter.isEnabled()) {
                    writeLog("bluetooth is disabled");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    return;
                }
                else {
                    writeLog("bluetooth already enabled");
                }

                discoverBT();



            }
        });







        // Register the BroadcastReceiver for find a device
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        // Register the BroadcastReceiver for start discovery
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        this.registerReceiver(mReceiver, filter);

        // Register the BroadcastReceiver for finish discovery
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);


        setConnectedState(mConnected);


        mSpeedknob = (RotaryKnobView)findViewById(R.id.speedknob);
        mSpeedknob.setKnobListener(new RotaryKnobView.RotaryKnobListener() {
            @Override
            public void onKnobChanged(int arg) {
                //m_edit_log.setText(Double.toString(knobView.getangle())+" "+Double.toString(knobView.thetaold()));
                if (mConnected) {
                    int angle = (int) mSpeedknob.getangle();
                    mSpeed = (int) ((angle + 450) * (100.0 / (-270.0 + 450.0)));
                    //m_edit_log.setText(String.valueOf(mSpeed) + ", angle:" + Double.toString(mSpeedknob.getangle()));

                    //mSpeed = mSldSpeed.getProgress();
                    speed_log.setText(String.valueOf(mSpeed));
                    mThrConnected.sendParam("spee", mSpeed);
                }
            }

            ;
        });
        final RotaryKnobView directionknob = (RotaryKnobView)findViewById(R.id.directionknob);
        directionknob.setKnobListener(new RotaryKnobView.RotaryKnobListener() {
            @Override
            public void onKnobChanged(int arg) {
                //m_edit_log.setText(Double.toString(knobView.getangle())+" "+Double.toString(knobView.thetaold()));
                if (mConnected) {
                    int angle = (int) directionknob.getangle();
                    mDirection = (int) ((angle + 450) * (100.0 / (-270.0 + 450.0)));
                    //m_edit_log.setText(String.valueOf(mSpeed) + ", angle:" + Double.toString(directionknob.getangle()));


                    //mSpeed = mSldSpeed.getProgress();
                    if(mDirection==50)
                        direction_log.setText("0");
                    if(mDirection>50)
                        direction_log.setText(String.valueOf(mDirection-50)+"R");
                    if(mDirection<50)
                        direction_log.setText(String.valueOf(50-mDirection)+"L");

                    mThrConnected.sendParam("dire", mDirection);
                }
            }

            ;
        });
    }





    private void connectSegway() {

        clearLog();

        if(mConnected) {
            writeLog("already connected!");
            return;
        }

        if (mBluetoothAdapter == null) {
            writeLog("Device does not support Bluetooth");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            writeLog("bluetooth is disabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        else {
            writeLog("bluetooth already enabled");
        }
        if(mBTdevice==null) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            writeLog("checking paired devices...");
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {

                    if (device.getName().equals(m_chosendevice) && device.getAddress().equals(m_deviceaddress)) {

                        mBTdevice = device;
                    }
                }
            }
        }

        // If there are paired devices
        writeLog("checking paired devices...");
        editor.putString("devname",m_chosendevice);
        editor.putString("devaddr",m_deviceaddress);
        editor.commit();
        connectBT();
        setConnectedState(mConnected);
        return;

    }





    public void discoverBT() {
        if(mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();

        if(mBluetoothAdapter.isDiscovering()) {
            writeLog("already trying to connect...");
            return;
        }
        mBluetoothAdapter.startDiscovery();
    }



    private void connectBT() {

        if(mBTdevice != null) {
            if(mThrConnect != null)
                mThrConnect.cancel();

            mThrConnect = new ConnectThread(mBTdevice);
            mThrConnect.run();
        }
        else {
            writeLog("device not found. please try again!");
        }
    }


    private void disconnectBT() {
        if(mConnected) {
            mThrConnected.cancel();
            mThrConnected = null;
            mThrConnect.cancel();
            mThrConnect = null;
            //mBTdevice = null;
            mConnected = false;
            setConnectedState(mConnected);
            //clearLog();
            writeLog("disconnected device");
        }
        else {
            writeLog("not connected!");
        }
    }



    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            //writeLog("got one!");
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if(device!=null) {
                    writeLog(device.getName() + " -> " + device.getAddress());
                    m_devicenames.add(device.getName());
                    m_deviceaddresses.add(device.getAddress());
                    m_devices.add(m_devicenames.get(m_devicenames.size() - 1) + " " + m_deviceaddresses.get(m_deviceaddresses.size() - 1));
                    m_devicelist.add(device);
//                if(device != null && device.getName() != null && device.getName().equals("Segway") && device.getAddress().equals("00:06:66:6B:B6:36")) {
//                    //clearLog();
//                    writeLog("Found BT-Device 'Seqway'");
//                    //writeLog(device.getAddress());
//                    mBTdevice = device;
//                    if(!mConnected)
//                        connectBT();
//                    setConnectedState(mConnected);
//               }
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                writeLog("discovery finished!");
                Intent i=new Intent(MainActivity.this,Devicelist.class);
                i.putStringArrayListExtra("dev",m_devices);
                startActivityForResult(i,9000);
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                writeLog("discovering devices...");
            }
        }
    };


    private void setConnectedState(boolean connected) {
        if(connected) {
            m_btn_connect.setEnabled(false);
            m_btn_disconnect.setEnabled(true);
            m_btn_send.setEnabled(true);
            m_btn_choose_device.setEnabled(false);
        }
        else {
            m_btn_connect.setEnabled(true);
            m_btn_disconnect.setEnabled(false);
            m_btn_send.setEnabled(false);
            m_btn_choose_device.setEnabled(true);
        }
    }


    private void writeLog(String msg) {
        String text = m_edit_log.getText().toString();
        msg += "\n" + text;
        m_edit_log.setText(msg + "\n");
        //int scroll_amount = m_edit_log.getBottom();
        //m_edit_log.scrollTo(0, scroll_amount);
    }

    private void clearLog() {
        m_edit_log.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }
    @Override
    protected void onPause() {
        super.onPause();
        disconnectBT();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1) {
            connectSegway();
        }
        if(requestCode == 9000) {
            //Bundle extras = getIntent().getExtras();
            int pos = data.getExtras().getInt("item");
            clearLog();
           m_chosendevice=m_devicenames.get(pos);
           m_deviceaddress=m_deviceaddresses.get(pos);
           mBTdevice=m_devicelist.get(pos);
           device_text.setText(m_chosendevice);
        }
    }














    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            if(mBluetoothAdapter.isDiscovering())
                mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                writeLog("connection established");
                mConnected = true;

                mThrConnected = new ConnectedThread(mmSocket);
                //thread.run();

            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                writeLog("unable to connect!");
                try {
                    mmSocket.close();
                    mConnected = false;
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mConnected = false;
                mmSocket.close();
            } catch (IOException e) { }
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
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            writeLog("waiting for msg ...");
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    writeLog("got msg: '" + mmInStream.toString() + "'");
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                //writeLog("'" + mMsg + "' sent!");
            } catch (IOException e) {
                //writeLog("error sending data!");
            }
        }


        public String read() {

            byte[] tmp = new byte[1];
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            String msg = "";

            boolean reachedEnd = false;
            int i = 0;
            int timeout = 0;
            int interCharacterTimeout = 0;
            // Keep listening to the InputStream until an exception occurs
            clearLog();
            writeLog("waiting for msg ...");
            try {
                /*
                while(timeout < 3 && interCharacterTimeout < 2) {

                    if(mmInStream.available() == 0 && i == 0) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        timeout++;
                    }
                    else if(mmInStream.available() == 0 && i > 0) {
                        interCharacterTimeout++;
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        bytes = mmInStream.read(tmp, 0, 1);
                        Log.i("mymessage", new String(tmp));

                        buffer[i++] = tmp[0];
                    }

                }
                */

                while(timeout < 5) {
                    if(mmInStream.available() == 0) {
                        timeout++;
                        Thread.sleep(100);
                    }
                    else {
                        bytes = mmInStream.read(buffer);
                        break;
                    }
                }
                // Read from the InputStream

                //bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                msg = new String(buffer);
                writeLog("read "+ bytes + " bytes");
                writeLog("got msg: '" + msg + "'");
            } catch (IOException e) {
                writeLog(e.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return msg;
        }


        public String talk(String send) {
            write(send.getBytes());
            return read();
        }


        public void sendParam(String what, int val) {
            mMsg = what;
            //mThrConnected.write(mMsg.getBytes());
            if(val == 100) {
                mMsg += Integer.toString(val);
            } else if(val >= 10) {
                mMsg += "0" + val;
            } else {
                mMsg += "00" + val;
            }

            mMsg += "\n";

            this.write(mMsg.getBytes());
            //this.talk(mMsg);
        }


        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
