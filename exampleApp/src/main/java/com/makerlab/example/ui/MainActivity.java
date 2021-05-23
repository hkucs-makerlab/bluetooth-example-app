package com.makerlab.example.ui;

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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.makerlab.bt.BluetoothConnect;
import com.makerlab.bt.BluetoothScan;
import com.makerlab.ui.BluetoothDevListActivity;

public class MainActivity extends AppCompatActivity implements
        BluetoothConnect.ConnectionHandler {
    static public final boolean D = BuildConfig.DEBUG;
    static public final int REQUEST_BT_GET_DEVICE = 1112;
    static public final String BLUETOOT_REMOTE_DEVICE = "bt_remote_device";
    static private String LOG_TAG = MainActivity.class.getSimpleName();

    private BluetoothConnect mBluetoothConnect;
    private BluetoothScan mBluetoothScan;
    private boolean isFragmentDisplayed = false;
    private Menu mMenuSetting;
    private SharedPreferences mSharedPref;
    private String mSharedPrefFile = "com.makerlab.omni.sharedprefs";
    //
    private MainFragmentControl mainFragmentControl;
    private FragmentManager fragmentManager = getSupportFragmentManager();

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
        } else {
            // Log.e(LOG_TAG, "onCreate(): no share perference");
        }
        //
        fragmentManager = getSupportFragmentManager();
        mainFragmentControl = MainFragmentControl.newInstance();
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
            startActivityForResult(intent, REQUEST_BT_GET_DEVICE);
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

                if (!isFragmentDisplayed) {
                    runOnUiThread(new Thread() {
                        public void run() {
                            if (mMenuSetting != null) {
                                enableConnectMenuItem(false);
                            }
                            displayControlFragment();
                            Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
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
                Toast.makeText(getApplicationContext(), "Failed to connect!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDisconnected(BluetoothConnect instant) {
        if (isFragmentDisplayed) {
            runOnUiThread(new Thread() {
                public void run() {
                    closeControlFragment();
                    enableConnectMenuItem(true);
                }
            });
        }
        if (mBluetoothConnect.isConnected()) {
            mBluetoothConnect.disconnectBluetooth();
        }
    }

    private void removeSharePerf() {
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        preferencesEditor.remove(BLUETOOT_REMOTE_DEVICE);
        preferencesEditor.apply();
    }

    private void displayControlFragment() {
        // hide the main activity layout
        final View view = findViewById(R.id.layout_main);
        view.setVisibility(View.INVISIBLE);
        //
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.container, mainFragmentControl).commit();
        isFragmentDisplayed = true;
    }

    private void closeControlFragment() {
        FragmentTransaction fragmentTransaction =
                fragmentManager.beginTransaction();
        fragmentTransaction.remove(mainFragmentControl).commit();
        if (D)
            Log.e(LOG_TAG, "closeControlFragment() :");

        isFragmentDisplayed = false;
        // show the main activity layout
        View view = findViewById(R.id.layout_main);
        view.setVisibility(View.VISIBLE);
    }

}