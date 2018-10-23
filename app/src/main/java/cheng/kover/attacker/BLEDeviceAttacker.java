package cheng.kover.attacker;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
	private static final String                TAG = "DeviceAttacker";
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
					Log.i(TAG + "Callback", "Connection established. Start discovering services.");
					gatt.discoverServices();
				}
				else
				{
					if (newState == BluetoothProfile.STATE_DISCONNECTED)
					{
						Log.w(TAG + "Callback", "Disconnected!!!");
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
					Log.i(TAG + "Callback", "Services discovered.");
//					for (BluetoothGattService bluetoothGattService : gatt.getServices())
//					{
//						Log.d(TAG + "Callback", " - Service: " + bluetoothGattService.getUuid());
//						for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics())
//						{
//							Log.d(TAG + "Callback", " -- Characteristic: " + bluetoothGattCharacteristic.getUuid());
//						}
//					}
				}
				else
				{
					Log.e(TAG + "Callback", "onServicesDiscovered received: " + status);
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
						Log.i(TAG + "Callback", "Device Info: Firmware Version - " + firmwareVersion);
					}
					if (MiBandProfile.UUID_CHAR_DEVICE_NAME.equals(characteristic.getUuid()))
					{
						// Decode device name
						try
						{
							Log.i(TAG + "Callback", "Device Name" + Arrays.toString(characteristic.getValue()) + "," + new String(characteristic.getValue(), "UTF-8"));
						}
						catch (UnsupportedEncodingException e)
						{
							e.printStackTrace();
						}
					}
				}
				else
				{
					Log.e(TAG + "Callback", characteristic.getUuid() + "onServicesDiscovered received: " + status);
				}
			}

			@Override
			public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
			{
				super.onCharacteristicChanged(gatt, characteristic);

				Log.d("test", "onCharacteristicChanged received: " + Arrays.toString(characteristic.getValue()));

				// Receive characteristic notification data
				if (MiBandProfile.UUID_CHAR_NOTIFICATION.equals(characteristic.getUuid()))
				{
					byte[] notificationValue = characteristic.getValue();

					if (notificationValue.length != 1)
					{
						Log.e(TAG + "Callback", "Notification Char return wrong with " + notificationValue.length + " byte(s).");
					}
					else
						switch (notificationValue[0])
						{
							case 0x6:
								Log.e(TAG + "Callback", "Need to authenticate by tapping.");
								break;
							case 0x5: case 0xa: case 0x15:
								Log.i(TAG + "Callback", "Successfully authenticated.");
								break;
							default:
								Log.e(TAG + "Callback", "Unsolved message: " + notificationValue[0]);
						}
				}

				// Receive real time steps data
				if (MiBandProfile.UUID_CHAR_REALTIME_STEPS.equals(characteristic.getUuid()))
				{
					byte[] stepsData = characteristic.getValue();
					if (stepsData.length == 4)
					{
						int steps = stepsData[3] << 24 | (stepsData[2] & 0xFF) << 16 | (stepsData[1] & 0xFF) << 8 | (stepsData[0] & 0xFF);
						Log.i(TAG + "Callback", "Real time steps: " + steps);
					}
				}

				// Receive sensor data
				if (MiBandProfile.UUID_CHAR_SENSOR_DATA.equals(characteristic.getUuid()))
				{
					Log.i(TAG + "Callback", "Sensor data notification: " + Arrays.toString(characteristic.getValue()));
					//TODO Decode sensor data
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
						delay();
						setNotify(MiBandProfile.UUID_CHAR_NOTIFICATION);
						delay();
						// Pair with MiBand
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_PAIR, new byte[] {2});
						delay();
						// Check pair status
						readCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_PAIR);
						delay();
						// Read device info
						readCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_DEVICE_INFO);
						delay();
						// Read device name
						readCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_DEVICE_NAME);
						delay();
						// Write user info
						MiBandUserInfo miBandUserInfo      = new MiBandUserInfo(0, 0, 175, 70, "mi_user_alias", 0);
						byte[]         miBandUserInfoBytes = miBandUserInfo.toBytes(bluetoothDevice.getAddress());
						Log.d(TAG, "Set User Info: " + miBandUserInfo.toString() + ", bytes: " + bytesToHex(miBandUserInfoBytes));
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_USER_INFO, miBandUserInfoBytes);
//						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_USER_INFO, new byte[]{15,5,13,2,2,9,8,7,6,5,0,10,1,13,8,2,0,5,10,11,8,2,11,14,11,9,3,8,5,9,12,15});
						delay();
						delay();
						delay();
						delay();
						// Enable real time steps notify
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_REALTIME_STEPS, new byte[] {3, 1});
						delay();
						setNotify(MiBandProfile.UUID_CHAR_REALTIME_STEPS);
						delay();
						// Enable sensor notify
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_CONTROL_POINT, new byte[] {18, 1});
						delay();
						setNotify(MiBandProfile.UUID_CHAR_SENSOR_DATA);
//						// Disable notify
//						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_REALTIME_STEPS, new byte[] {3, 0});
//						delay();
//						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_CONTROL_POINT, new byte[] {12, 0});
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
		BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
		if (null == bluetoothGattCharacteristic)
		{
			Log.e(TAG + "IO", "Characteristic " + characteristicUUID + " not exist.");
			return;
		}
		bluetoothGattCharacteristic.setValue(value);
		if (! bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic))
		{
			Log.e(TAG + "IO", "Bluetooth GATT write characteristic " + characteristicUUID + " failed.");
			return;
		}
		Log.i(TAG + "IO", "Characteristic" + characteristicUUID + " successfully written value: " + Arrays.toString(value));
	}

	public void setNotify(UUID characteristicUUID)
	{
		Log.d(TAG + "IO", "Set char " + characteristicUUID + " Notify.");
		BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(MiBandProfile.UUID_SERVICE_MILI).getCharacteristic(characteristicUUID);
		if (null == bluetoothGattCharacteristic)
		{
			Log.e(TAG + "IO", "Characteristic " + characteristicUUID + " not exist.");
			return;
		}
		bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
		BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(MiBandProfile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
		bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
	}

	public void readCharacteristic(UUID serviceUUID, UUID characteristicUUID)
	{
		BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
		if (! bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic))
		{
			Log.e(TAG + "IO", "Bluetooth GATT read characteristic " + characteristicUUID + " failed.");
			return;
		}
	}
}
