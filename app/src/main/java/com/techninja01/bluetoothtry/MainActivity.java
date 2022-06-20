package com.techninja01.bluetoothtry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongFunction;

public class MainActivity extends AppCompatActivity {
    ListView mainList, pairedDeviceList;
    EditText message;
    Button send;
    Context context;
    BluetoothAdapter bluetoothAdapter;
    private final int LOCATION_PERMISSION_REQUEST = 101;
    public static final int REQUEST_ENABLE_BT = 0;
    public static final int REQUES_DISCOVER_BT = 1;
    ArrayAdapter<String> adapterPairedDevices;
    TextView pairedDevices, status;
    CardView statusCV;
    TextToSpeech t1;
    BluetoothDevice[] bluetoothDevices;
    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;
    private String CONNETED_DEVICE;
    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";
    public static final int STATE_LISTENING = 0;
    public static final int STATE_CONNETING = 1;
    public static final int STATE_CONNETED = 2;
    public static final int STATE_CONNECTION_FAILED = 3;
    public static final int STATE_MESSAGE_RECEIVED = 4;
    public static final String MY_APP = "BluetoothTry";
    public static final UUID MY_UUID = UUID.fromString("ed78e4a5-dc66-4160-a954-88f138001c03");
    SendReceive sendReceive;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;


//    private Handler handler = new Handler(new Handler.Callback() {
//        @Override
//        public boolean handleMessage(@NonNull Message msg) {
//            switch (msg.what){
//                case MESSAGE_STATE_CHANGED:
//                    switch (msg.arg1){
//                        case ChatUtils.STATE_NONE:setState("Not Connected");break;
//                        case ChatUtils.STATE_LISTEN:setState("Not Connected");break;
//                        case ChatUtils.STATE_CONNECTING:setState("Connecting");break;
//                        case ChatUtils.STATE_CONNECTED:setState("Connected");break;
//                    }
//                    break;
//                case MESSAGE_READ:break;
//                case MESSAGE_WRITE:break;
//                case MESSAGE_DEVICE_NAME:CONNETED_DEVICE = msg.getData().getString(DEVICE_NAME);
//                    Toast.makeText(context, CONNETED_DEVICE, Toast.LENGTH_SHORT).show();break;
//                case MESSAGE_TOAST:
//                    Toast.makeText(context, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();;break;
//            }
//            return false;
//        }
//    });


//    private void setState(CharSequence subTitle){
//        getSupportActionBar().setSubtitle(subTitle);
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        mainList = findViewById(R.id.deviceLV);
        message = findViewById(R.id.msgEDT);
        send = findViewById(R.id.sendBTN);
        pairedDeviceList = findViewById(R.id.pairedDevicesLV);
        pairedDevices = findViewById(R.id.pairedDVTV);
        status = findViewById(R.id.statusTV);
        statusCV = findViewById(R.id.statusSymbol);
        statusCV.setCardBackgroundColor(Color.RED);
//        pairedDeviceList.setAdapter(adapterPairedDevices);


        Dexter.withContext(MainActivity.this).withPermissions(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                if (multiplePermissionsReport.areAllPermissionsGranted()) {
                    Toast.makeText(MainActivity.this, "All permissions are granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Not all permissions are granted", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
        MultiplePermissionsListener dialogMultiplePermissionsListener =
                DialogOnAnyDeniedMultiplePermissionsListener.Builder
                        .withContext(getApplicationContext())
                        .withTitle("Phone and bluetooth permissions required")
                        .withMessage("This application requires both phone and bluetooth permissions")
                        .withButtonText(android.R.string.ok)
                        .build();


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "No bluetooth found", Toast.LENGTH_SHORT).show();
        }

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver2, intentFilter);
        pairedDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);
                Toast.makeText(context, "Device address = " + address + "\n" + bluetoothDevices[position], Toast.LENGTH_SHORT).show();
                ClientClass clientClass = new ClientClass(bluetoothDevices[position]);
                clientClass.start();
                status.setText("Connecting");
            }
        });


        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = String.valueOf(message.getText());
                sendReceive.write(string.getBytes(StandardCharsets.UTF_8));
            }
        });


        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

    }


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                String total = deviceName + deviceHardwareAddress;
                Toast.makeText(context, "Device found " + total, Toast.LENGTH_SHORT).show();
            }
        }
    };


    private void enableBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            Toast.makeText(context, "Bluetooth already enabled", Toast.LENGTH_SHORT).show();
            AlertDialog dialog = new AlertDialog.Builder(context).setTitle("Caution").setMessage("Do you wish to disable bluetooth").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    bluetoothAdapter.disable();
                }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(MainActivity.this, "Press yes if  you wish to disable bluetooth", Toast.LENGTH_SHORT).show();
                }
            }).create();
            dialog.show();
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.enable();
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 100);
        startActivityForResult(intent, REQUES_DISCOVER_BT);
        Set<BluetoothDevice> bt = bluetoothAdapter.getBondedDevices();
        String[] strings = new String[bt.size()];
        String[] address = new String[bt.size()];
        String[] total = new String[bt.size()];
        bluetoothDevices = new BluetoothDevice[bt.size()];
        int index = 0;
        if (bt.size() > 0) {
            for (BluetoothDevice device : bt) {
                bluetoothDevices[index] = device;
                strings[index] = device.getName();
                address[index] = device.getAddress();
                total[index] = strings[index] + "\t" + address[index];
                index++;
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, total);
            pairedDeviceList.setAdapter(arrayAdapter);
            statusCV.setCardBackgroundColor(Color.rgb(255, 165, 0));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                AlertDialog dlg = new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission is required")
                        .setTitle("Caution")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkPermissions();
                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).create();
                dlg.show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private final BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d("Bonding", "Bonded");
                } else if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d("Bonding", "Bonding");
                } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d("Bonding", "Failed");
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bluetoothSearch:
                enableBluetooth();
                break;
            case R.id.bluetoothConnect:
                checkPermissions();
                break;
            case R.id.bluetoothPair:
                pairing();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void pairing() {
        ServerClass serverClass = new ServerClass();
        serverClass.start();
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case STATE_LISTENING:
                    status.setText("Listening");
                    statusCV.setCardBackgroundColor(Color.rgb(255, 165, 0));
                    break;
                case STATE_CONNETING:
                    status.setText("Connecting");
                    statusCV.setCardBackgroundColor(Color.YELLOW);
                    break;
                case STATE_CONNETED:
                    status.setText("Connected");
                    statusCV.setCardBackgroundColor(Color.GREEN);
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Failed");
                    statusCV.setCardBackgroundColor(Color.MAGENTA);
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[])msg.obj;
                    String tempMSG = new String(readBuff,0,msg.arg1);
                    Toast.makeText(context, "Message = "+tempMSG, Toast.LENGTH_SHORT).show();
                    t1.speak(tempMSG, TextToSpeech.QUEUE_FLUSH, null);
                    break;
            }
            return true;
        }
    });

    private class ServerClass extends Thread {
        BluetoothServerSocket serverSocket;

        public ServerClass() {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(MY_APP, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            BluetoothSocket socket = null;
            while (socket == null) {
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNETING;
                    handler.sendMessage(message);
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                    e.printStackTrace();
                }
                if (socket != null) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNETED;
                    handler.sendMessage(message);
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    //Code for send receive
                    break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("TAG", "Could not close the connect socket", e);
            }
        }
    }

    private class ClientClass extends Thread {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device1) {
            device = device1;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.cancelDiscovery();
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNETED;
                handler.sendMessage(message);
            }catch (IOException e){
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
            sendReceive = new SendReceive(socket);
            sendReceive.start();
        }
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("TAG", "Could not close the client socket", e);
            }
        }
    }

    public class SendReceive extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private byte[] mmBuffer;
        public SendReceive(BluetoothSocket socket){
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;
            try {
                tempIn = bluetoothSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tempIn;
            outputStream = tempOut;
        }
        public void run(){
            mmBuffer = new byte[1024];
            int numBytes;
            while(true){
                try {
                    numBytes = inputStream.read(mmBuffer);
                    Message readMSG = handler.obtainMessage(STATE_MESSAGE_RECEIVED,numBytes,-1,mmBuffer);
                    readMSG.sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e("TAG", "Could not close the connect socket", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        unregisterReceiver(broadcastReceiver2);
    }
}