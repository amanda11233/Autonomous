package com.example.autonomusdashboard;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.an.biometric.BiometricCallback;
import com.an.biometric.BiometricManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import eo.view.batterymeter.BatteryMeterView;
import me.ibrahimsn.lib.Speedometer;

public class MainActivity extends AppCompatActivity implements BiometricCallback {

    private BatteryMeterView batteryMeterView;
    private TextView chargeText;
    private int charveLevel = 100;
    private boolean chargeStatus = false;
    private Speedometer speedometer;


    private BluetoothAdapter myBluetooth;
    private Button btnConnect;
    private AlertDialog alertDialog;

    private int bmCount = 0;
    private TextView connecttxt;
    private ProgressBar progressBar;
    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;


    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();



        if (deviceName != null){
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            connecttxt.setText("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();



        }else{
            showCustomDialog();

        }

        communicationHandler();
    }


    private void communicationHandler(){
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                connecttxt.setText("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                break;

                            case -1:
                                connecttxt.setText("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
//                        Log.d(TAG, "handleMessage: " + arduinoMsg);
                        String[] msgSplit =  arduinoMsg.split(":");
                        Log.d(TAG, "handleMessage: " +  arduinoMsg);
                        if(msgSplit.length > 1){
                            String splitMsg = arduinoMsg.split(":")[0];
                            String splitValue = arduinoMsg.split(":")[1];
                            Log.d(TAG, "handleMessage: " + splitValue);

                            switch (splitMsg){
                                case "state" :
                                    if(Integer.parseInt(splitValue.trim()) == 0 ){
                                      if(bmCount == 0){
                                          new BiometricManager.BiometricBuilder(MainActivity.this)
                                                  .setTitle("Fingerprint Authentication")
                                                  .setDescription("Use your fingerprint to start the car")
                                                  .setNegativeButtonText("Cancel")
                                                  .build()
                                                  .authenticate(MainActivity.this);

                                          bmCount = 1;
                                      }
                                    }else{

                                    }
                                    break;
                                case "speed":
                                    speedometer.setSpeed(Integer.parseInt(splitValue.trim()), 1000L, null);
                                    break;
                                case "rpm":
//                                Log.d(TAG, "handleMessage: " + splitValue);
                            }
                            break;
                        }else{
                            Toast.makeText(MainActivity.this, "Someting went Wrong", Toast.LENGTH_SHORT).show();
                        }

                }
            }
        };

        speedometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    private void initView(){

        Intent intent = getIntent();
        deviceName = intent.getStringExtra("deviceName");
        deviceAddress = intent.getStringExtra("deviceAddress");
        speedometer = findViewById(R.id.speedometer);
        speedometer.setSpeed(0, 1000L, null);

        batteryMeterView = findViewById(R.id.batteryview);
        batteryMeterView.setChargeLevel(charveLevel);
        batteryMeterView.setCharging(chargeStatus);
        chargeText = findViewById(R.id.chargeText);
        progressBar = findViewById(R.id.progressbar);
        connecttxt = findViewById(R.id.connecttxt);
        progressBar.setVisibility(View.GONE);
        chargeText.setText("turn on");

    }

    private void showCustomDialog(){
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.bluetooth_connect_modal, null);
        btnConnect = dialogView.findViewById(R.id.btnconnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, BluetoothActivity.class));
            }
        });

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialogView);
        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onSdkVersionNotSupported() {

    }

    @Override
    public void onBiometricAuthenticationNotSupported() {

    }

    @Override
    public void onBiometricAuthenticationNotAvailable() {

    }

    @Override
    public void onBiometricAuthenticationPermissionNotGranted() {

    }

    @Override
    public void onBiometricAuthenticationInternalError(String error) {

    }

    @Override
    public void onAuthenticationFailed() {

    }

    @Override
    public void onAuthenticationCancelled() {
        bmCount = 0;
    }

    @Override
    public void onAuthenticationSuccessful() {
        String cmdText = null;
//        String btnState = chargeText.getText().toString().toLowerCase();
//        switch (btnState){
//            case "turn on":
//                chargeText.setText("Turn Off");
//                // Command to turn on LED on Arduino. Must match with the command in Arduino code
//                cmdText = "<turn on>";
//                break;
//            case "turn off":
//                chargeText.setText("Turn On");
//                // Command to turn off LED on Arduino. Must match with the command in Arduino code
//                cmdText = "<turn off>";
//                break;
//        }
//        // Send command to Arduino board
        connectedThread.write("<turn on>");
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {

    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {

    }

    public static class CreateConnectThread extends Thread {
        private static final String TAG = "CreateConnectThread";
        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */

            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
    public static class ConnectedThread extends Thread {
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
                Log.d(TAG, "ConnectedThread: Error");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.d("Arduino Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }


}