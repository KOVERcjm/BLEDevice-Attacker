package cheng.kover.attacker;

import android.Manifest.permission;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity
{
	private static final String           TAG = "Main Activity";
	public final static int REQUEST_ENABLE_BLUETOOTH = 1;
	public final static int REQUEST_ENABLE_LOCATION  = 2;

	private              BluetoothAdapter bluetoothAdapter;
	private HashMap<String, BluetoothDevice> bluetoothDevices = new HashMap<>();
	private ArrayAdapter                     bluetoothDevicesAdapter;
	private ScanCallback scanCallback;
	private BLEDeviceAttacker bleDeviceAttacker;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initView();
		initBluetooth();
		scanDevices();
	}

	private void initBluetooth()
	{
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();

		// When API >= 23, needs location permission for Bluetooth to scan devices.
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (! locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || ! locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
		{
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivityForResult(intent, REQUEST_ENABLE_LOCATION);
		}
		if (VERSION.SDK_INT >= VERSION_CODES.M && ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
		{
			ActivityCompat.requestPermissions(this, new String[] {permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_LOCATION);
		}

		// Ensures Bluetooth is available on the device and it is enabled. If not,
		// displays a dialog requesting user permission to enable Bluetooth.
		if (bluetoothAdapter == null || ! bluetoothAdapter.isEnabled())
		{
			Log.e(TAG, "No Bluetooth Adapter.");
			Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			enableBluetoothIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);

			// Enable Bluetooth without user permission.
			bluetoothAdapter.enable();
		}

		// Get bonded devices
		Log.d(TAG, "Get Bonded Devices.");
		Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
		if (bondedDevices.isEmpty())
		{
			Log.e(TAG, "No Bonded Device.");
		}
		else
		{
			for (BluetoothDevice device : bondedDevices)
			{
				Log.i(TAG, "Bonded Device found: " + device.getName() + ", address: " + device.getAddress());
			}
		}
	}

	private void initView()
	{
		ListView listView = findViewById(R.id.bluetoothDevicesList);

		bluetoothDevicesAdapter = new ArrayAdapter<>(this, R.layout.bluetooth_device_list_item, new ArrayList<String>());
		listView.setAdapter(bluetoothDevicesAdapter);

		bleDeviceAttacker = new BLEDeviceAttacker(this);
		listView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				String deviceItem = ((TextView) view).getText().toString();

				if (bluetoothDevices.containsKey(deviceItem))
				{
					Log.d(TAG, "Stop Scanning.");
					BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
					bluetoothLeScanner.stopScan(scanCallback);

					Log.d(TAG, "Attack on chosen device: " + deviceItem);
					bleDeviceAttacker.attack(bluetoothDevices.get(deviceItem));
				}
			}
		});
	}

	private void scanDevices()
	{
		scanCallback = new ScanCallback()
		{
			@Override
			public void onScanResult(int callbackType, ScanResult result)
			{
				super.onScanResult(callbackType, result);

				BluetoothDevice bluetoothDevice = result.getDevice();
				Log.i(TAG, "Device found: " + bluetoothDevice.getAddress() + ", RSSI: " + result.getRssi());

				if (! bluetoothDevices.containsKey(bluetoothDevice.getAddress()))
				{
					bluetoothDevices.put(bluetoothDevice.getAddress(), bluetoothDevice);
					//TODO Show devices by their names.
					bluetoothDevicesAdapter.add(bluetoothDevice.getAddress());
				}
			}
		};

		Log.i(TAG, "Start scanning devices.");
		bluetoothAdapter.startDiscovery();
		BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
		bluetoothLeScanner.startScan(scanCallback);
	}
}
