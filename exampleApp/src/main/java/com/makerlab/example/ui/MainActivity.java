package com.makerlab.example.ui;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.makerlab.bt.BluetoothConnect;
import com.makerlab.bt.BluetoothScan;
import com.makerlab.ui.BluetoothDevListActivity;

public class MainActivity extends AppCompatActivity implements
        BluetoothConnect.ConnectionHandler, ActivityResultCallback<ActivityResult> {
    static public final boolean D = BuildConfig.DEBUG;
//    static public final int REQUEST_BT_GET_DEVICE = 1112;
    static public final String BLUETOOT_REMOTE_DEVICE = "bt_remote_device";
    static private String LOG_TAG = MainActivity.class.getSimpleName();

    private BluetoothConnect mBluetoothConnect;
    private BluetoothScan mBluetoothScan;

    private Menu mMenuSetting;
    private SharedPreferences mSharedPref;
    private String mSharedPrefFile = "com.makerlab.omni.sharedprefs";
    //
    ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothConnect = new BluetoothConnect(this);
        mBluetoothConnect.setConnectionHandler(this);

        mSharedPref = getSharedPreferences(mSharedPrefFile, MODE_PRIVATE);
        String bluetothDeviceAddr = mSharedPref.getString(BLUETOOT_REMOTE_DEVICE, null);
        if (bluetothDeviceAddr != null) {
            //Log.e(LOG_TAG, "onCreate(): found share perference");
            mBluetoothScan = new BluetoothScan(this);
            BluetoothDevice mBluetoothDevice = mBluetoothScan.getBluetoothDevice(bluetothDeviceAddr);
            mBluetoothConnect.connectBluetooth(mBluetoothDevice);
            if (D)
                Log.e(LOG_TAG, "onCreate() - connecting bluetooth device");
        } else {
            if (D)
                Log.e(LOG_TAG, "onCreate()");
        }
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this);
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
                mBluetoothConnect.disconnectBluetooth();
            }

            Intent intent = new Intent(this, BluetoothDevListActivity.class);
            activityResultLauncher.launch(intent);
            //startActivityForResult(intent, REQUEST_BT_GET_DEVICE);
            return true;
        }

        if (item.getItemId() == R.id.action_bluetooth_disconnect) {
            mBluetoothConnect.disconnectBluetooth();
            closeControlFragment();
            enableConnectMenuItem(true);
            removeSharePerf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent resultIntent = result.getData();
            BluetoothDevice bluetoothDevice = resultIntent.getParcelableExtra(BluetoothDevListActivity.EXTRA_KEY_DEVICE);
            if (bluetoothDevice != null) {
                mBluetoothConnect.connectBluetooth(bluetoothDevice);
                if (D)
                    Log.e(LOG_TAG, "onActivityResult() - connecting");
            }
        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            if (D)
                Log.e(LOG_TAG, "onActivityResult() - canceled");
        }
    }

/*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);

        if (requestCode == REQUEST_BT_GET_DEVICE) {
            if (resultCode == RESULT_OK) {
                BluetoothDevice bluetoothDevice = resultIntent.getParcelableExtra(BluetoothDevListActivity.EXTRA_KEY_DEVICE);
                if (bluetoothDevice != null) {
                    mBluetoothConnect.connectBluetooth(bluetoothDevice);
                    if (D)
                        Log.e(LOG_TAG, "onActivityResult() - connecting");
                }
            } else if (resultCode == RESULT_CANCELED) {
                if (D)
                    Log.e(LOG_TAG, "onActivityResult() - canceled");
            }
        }
    }
*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothConnect.disconnectBluetooth();
        if (D)
            Log.e(LOG_TAG, "onDestroy()");
    }

    public BluetoothConnect getBluetoothConnect() {
        return mBluetoothConnect;
    }

    @Override
    public void onConnect(BluetoothConnect instant) {
        runOnUiThread(new Thread() {
            public void run() {
                Toast.makeText(getApplicationContext(), "Connecting", Toast.LENGTH_SHORT).show();
            }
        });
        if (D)
            Log.e(LOG_TAG, "onConnect() - Connecting");
    }

    @Override
    public void onConnectionSuccess(BluetoothConnect instant) {
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        preferencesEditor.putString(BLUETOOT_REMOTE_DEVICE, mBluetoothConnect.getDeviceAddress());
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

    @Override
    public void onDisconnected(BluetoothConnect instant) {
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


}