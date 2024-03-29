package com.makerlab.example.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.makerlab.bt.BluetoothConnect;
import com.makerlab.ui.BluetoothActivity;

public class MainActivity extends AppCompatActivity implements
        BluetoothConnect.ConnectionHandler {
    static public final boolean D = BuildConfig.DEBUG;
    static public final int REQUEST_BT_GET_DEVICE = 1112;
    static public final String BLUETOOT_REMOTE_DEVICE = "bt_remote_device";
    static private String LOG_TAG = MainActivity.class.getSimpleName();

    private BluetoothConnect mBluetoothConnect;
    private BluetoothDevice mBluetoothDevice;
    private Menu mMenuSetting;
    private SharedPreferences mSharedPref;
    private String mSharedPrefFile = "com.makerlab.omni.sharedprefs";
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        mBluetoothConnect = new BluetoothConnect(this);
        mBluetoothConnect.setConnectionHandler(this);

        mSharedPref = getSharedPreferences(mSharedPrefFile, MODE_PRIVATE);
        String bluetothDeviceAddr = mSharedPref.getString(BLUETOOT_REMOTE_DEVICE, null);
        if (bluetothDeviceAddr != null) {
            //Log.e(LOG_TAG, "onCreate(): found share perference");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothDevice = bluetoothAdapter.getRemoteDevice(bluetothDeviceAddr);
            mBluetoothConnect.connect(mBluetoothDevice);
            if (D)
                Log.e(LOG_TAG, "onCreate() - connecting bluetooth device");
        } else {
            if (D)
                Log.e(LOG_TAG, "onCreate()");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenuSetting = menu;
        getMenuInflater().inflate(R.menu.menu_main, mMenuSetting);
        if (mBluetoothConnect.isConnected()) {
            MenuItem menuItem = mMenuSetting.findItem(R.id.action_bluetooth_scan);
            menuItem.setEnabled(false);
            menuItem = mMenuSetting.findItem(R.id.action_bluetooth_disconnect);
            menuItem.setEnabled(true);
        }
        return true;
    }

    private void enableConnectMenuItem(boolean flag) {
        MenuItem menuItem = mMenuSetting.findItem(R.id.action_bluetooth_scan);
        menuItem.setEnabled(flag);
        menuItem = mMenuSetting.findItem(R.id.action_bluetooth_disconnect);
        menuItem.setEnabled(!flag);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_bluetooth_scan) {
            if (mBluetoothConnect.isConnected()) {
                mBluetoothConnect.disconnect();
            }
            Intent intent = new Intent(this, BluetoothActivity.class);
            startActivityForResult(intent, REQUEST_BT_GET_DEVICE);
            return true;
        }

        if (item.getItemId() == R.id.action_bluetooth_disconnect) {
            mBluetoothConnect.disconnect();
            closeControlFragment();
            enableConnectMenuItem(true);
            removeSharePerf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);

        if (requestCode == REQUEST_BT_GET_DEVICE) {
            if (resultCode == RESULT_OK) {
                mBluetoothDevice = resultIntent.getParcelableExtra("device");
                if (mBluetoothDevice != null) {
                    mBluetoothConnect.connect(mBluetoothDevice);
                    if (D)
                        Log.e(LOG_TAG, "onActivityResult() - connecting");
                }
            } else if (resultCode == RESULT_CANCELED) {
                if (D)
                    Log.e(LOG_TAG, "onActivityResult() - canceled");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothConnect.disconnect();
        if (D)
            Log.e(LOG_TAG, "onDestroy()");
    }

    public BluetoothConnect getBluetoothConnect() {
        return mBluetoothConnect;
    }

/*

    @Override
    public void onConnectionFail(BluetoothConnect instant) {
        runOnUiThread(new Thread() {
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "Connecting fail!",
                        Toast.LENGTH_LONG).show();
            }
        });
        removeSharePerf();
        if (D)
            Log.e(LOG_TAG, "onConnectionFail()");
        runOnUiThread(new Thread() {
            public void run() {
                closeControlFragment();
                Toast.makeText(getApplicationContext(), "Failed to connect!", Toast.LENGTH_LONG).show();
            }
        });
    }
*/


    private void removeSharePerf() {
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        preferencesEditor.remove(BLUETOOT_REMOTE_DEVICE);
        preferencesEditor.apply();
    }

    private void displayControlFragment() {
        MainFragmentControl mainFragmentControl = MainFragmentControl.newInstance();
        // hide the main activity layout containing static fragment
        final View view = findViewById(R.id.layout_main);
        view.setVisibility(View.INVISIBLE);
        //
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container, mainFragmentControl).commit();

    }

    private void closeControlFragment() {
        Fragment mainFragmentControl = getSupportFragmentManager().findFragmentById(R.id.container);
        if (mainFragmentControl != null) {
            FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(mainFragmentControl).commit();
        }
        if (D)
            Log.e(LOG_TAG, "closeControlFragment() :");

        // show the main activity layout containing static fragment
        View view = findViewById(R.id.layout_main);
        view.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(new Thread() {
            public void run() {
                Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_LONG).show();
                closeControlFragment();
                enableConnectMenuItem(true);
            }
        });
        if (D)
            Log.e(LOG_TAG, "onDisconnected()");
    }

    @Override
    public void onConnected() {
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        preferencesEditor.putString(BLUETOOT_REMOTE_DEVICE, mBluetoothDevice.getAddress());
        preferencesEditor.apply();
        if (D)
            Log.e(LOG_TAG, "onConnectionSuccess() - connected");
        runOnUiThread(new Thread() {
            public void run() {
                runOnUiThread(new Thread() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        if (mMenuSetting != null) {
                            enableConnectMenuItem(false);
                        }
                        displayControlFragment();
                    }
                });
            }
        });
    }
}