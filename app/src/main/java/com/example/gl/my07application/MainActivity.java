package com.example.gl.my07application;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

public class MainActivity extends AppCompatActivity {

    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private TextView mText;
    private TextView visibleText;
    private Button connectBt;
    private Button disconnectBt;
    private Button scanBt;
    private Button visibleBt, listBt;
    BluetoothAdapter mbluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    ListView listDevices, discoveredList;
    ArrayAdapter<String> mpairedArrayAdapter;
    ArrayAdapter<String> mnewArrayAdapter;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private int mState;
    public static final int STATE_NONE=0;
    public static final int STATE_LISTEN=1;
    public static final int STATE_CONNECTING=2;
    public static final int STATE_CONNECTED=3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setResult(Activity.RESULT_CANCELED);

        mText = (TextView) findViewById(R.id.textView);
        visibleText = (TextView) findViewById(R.id.textView2);

        connectBt = (Button) findViewById(R.id.connectBT);
        disconnectBt = (Button) findViewById(R.id.disconnectBT);
        scanBt = (Button) findViewById(R.id.scanBT);
        visibleBt = (Button) findViewById(R.id.visibleBt);
        listBt = (Button) findViewById(R.id.listBt);
        discoveredList = (ListView) findViewById(R.id.listView2);
        listDevices = (ListView) findViewById(R.id.listView);
        mbluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BluetoothDevice.ACTION_FOUND);
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mnewArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredList.setAdapter(mnewArrayAdapter);
        discoveredList.setOnItemClickListener(mDeviceClickListener);

        connectBt.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (mbluetoothAdapter == null) {
                            Toast.makeText(getApplicationContext(), "no bluetooth adapter", Toast.LENGTH_SHORT).show();
                        } else {
                            if (!mbluetoothAdapter.isEnabled()) {
                                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(enableIntent, 0);
                                mText.setText("bluetooth is on");
                            } else {
                                Toast.makeText(getApplicationContext(), "the Bluetooth device has already been trun on", Toast.LENGTH_LONG).show();
                            }

                        }
                    }
                }
        );

        disconnectBt.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mbluetoothAdapter == null) {
                            Toast.makeText(getApplicationContext(), "no bluetooth adapter", Toast.LENGTH_SHORT).show();
                        } else {
                            if (mbluetoothAdapter.isEnabled()) {

                                mbluetoothAdapter.disable();
                                mText.setText("bluetooth is off");
                            } else {
                                Toast.makeText(getApplicationContext(), "the Bluetooth device has already been turn off", Toast.LENGTH_LONG).show();
                            }

                        }
                    }
                }
        );

        scanBt.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mbluetoothAdapter == null) {
                            Toast.makeText(getApplicationContext(), "no bluetooth adapter", Toast.LENGTH_SHORT).show();
                        } else {
                            if (mbluetoothAdapter.isEnabled()) {
                                scanBluetoothDevice();

                            } else {
                                Toast.makeText(getApplicationContext(), "the Bluetooth device has already been turn off", Toast.LENGTH_LONG).show();
                            }
                        }
                    }


                }
        );

        listBt.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mbluetoothAdapter == null) {
                            Toast.makeText(getApplicationContext(), "no bluetooth adapter", Toast.LENGTH_SHORT).show();
                        } else {
                            if (!mbluetoothAdapter.isEnabled()) {
                                Toast.makeText(getApplicationContext(), "the Bluetooth device has already been turn off", Toast.LENGTH_LONG).show();

                            } else {
                                listBluetoothDevices();
                            }
                        }
                    }
                }
        );

        visibleBt.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (mbluetoothAdapter == null) {
                            Toast.makeText(getApplicationContext(), "no bluetooth adapter", Toast.LENGTH_SHORT).show();
                        } else {
                            if (mbluetoothAdapter.isEnabled()) {
                                Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                                startActivityForResult(getVisible, 0);
                                visibleText.setText("bluetooth is visible");
                            } else {
                                Toast.makeText(getApplicationContext(), "the Bluetooth device has already been turn off", Toast.LENGTH_LONG).show();
                                visibleText.setText("bluetooth is invisible");
                            }
                        }
                    }
                }
        );

    }

    private void scanBluetoothDevice() {
        setProgressBarIndeterminateVisibility(true);
        setTitle("scanning");

        if (mbluetoothAdapter.isDiscovering()) {
            mbluetoothAdapter.cancelDiscovery();
        }
        mbluetoothAdapter.startDiscovery();


    }


    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mbluetoothAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra("device_adress", address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
////                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                mnewArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//               }
                // When discovery is finished, change the Activity title
                mnewArrayAdapter.add("+2");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);


                if (mnewArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mnewArrayAdapter.add(noDevices);
                } else {
//                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    mnewArrayAdapter.add(device.getName());
//                    Toast.makeText(getApplicationContext(), "devices found", Toast.LENGTH_SHORT).show();
                    mnewArrayAdapter.add("+4");
                }
            }
        }
    };

    private void listBluetoothDevices() {

        pairedDevices = mbluetoothAdapter.getBondedDevices();
        ArrayList list = new ArrayList();

        for (BluetoothDevice btdevice : pairedDevices)
            list.add(btdevice.getName());
        Toast.makeText(getApplicationContext(), "devices as following", Toast.LENGTH_SHORT).show();
        mpairedArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        listDevices.setAdapter(mpairedArrayAdapter);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        if (mbluetoothAdapter != null) {
            mbluetoothAdapter.cancelDiscovery();
        }
        super.onDestroy();
    }

    }


}
