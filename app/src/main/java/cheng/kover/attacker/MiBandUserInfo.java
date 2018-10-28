package cheng.kover.attacker;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

class MiBandUserInfo
{
	private static final String TAG = "Mi Band User Info";
	private              int    uid;
	private              byte   gender;
	private              byte   age;
	private              byte   height;
	private              byte   weight;
	private              String alias;
	private              byte   type;

	public MiBandUserInfo(int uid, int gender, int age, int height, int weight, String alias, int type)
	{
		this.uid = uid;
		this.gender = (byte) gender;
		this.age = (byte) age;
		this.height = (byte) height;
		this.weight = (byte) weight;
		this.alias = alias;
		this.type = (byte) type;
	}

	// Generate uid from alias.
	public MiBandUserInfo(int gender, int age, int height, int weight, String alias, int type)
	{
		this.gender = (byte) gender;
		this.age = (byte) age;
		this.height = (byte) height;
		this.weight = (byte) weight;
		this.alias = alias;
		this.type = (byte) type;
		calculateUidFromAlias(alias);
	}

	private void calculateUidFromAlias(String alias)
	{
		try
		{
			uid = Integer.parseInt(alias);
		}
		catch (NumberFormatException ex)
		{
			uid = alias.hashCode();
		}
	}

	// Decode Mi Band User Information from Byte stream.
	public static MiBandUserInfo fromByteData(byte[] data)
	{
		MiBandUserInfo info = null;
		if (data.length >= 20)
		{
			try
			{
				info = new MiBandUserInfo(
						(data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF)),
						data[4], data[5],
						data[6], data[7], new String(data, 9, 8, "UTF-8"), data[8]);
			}
			catch (UnsupportedEncodingException e)
			{
				Log.e(TAG, "Unsupported Encoding of alias.");
			}

		}
		return info;
	}

//***********************************Here's the generation of user info bytes.*********************
	// Encode Mi Band User Information to Byte Stream.
	public byte[] toBytes(String bluetoothDeviceAddress)
	{
		byte[] aliasBytes;
		try
		{
			aliasBytes = this.alias.getBytes("UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			aliasBytes = new byte[0];
		}
		ByteBuffer bf = ByteBuffer.allocate(20);
		bf.put((byte) uid);
		bf.put((byte) (uid >>> 8));
		bf.put((byte) (uid >>> 16));
		bf.put((byte) (uid >>> 24));
		bf.put((byte) (gender & 0xff));
		bf.put((byte) (age & 0xff));
		bf.put((byte) (height & 0xff));
		bf.put((byte) (weight & 0xff));
		bf.put((byte) (type & 0xff));
		bf.put((byte) 5);
		bf.put((byte) 0);

		if (aliasBytes.length < 8)
		{
			bf.put(aliasBytes);
			bf.put(new byte[10 - aliasBytes.length]);
		}
		else
		{
			bf.put(aliasBytes, 0, 8);
		}

		byte[] crcSequence = new byte[19];
		for (int u = 0; u < crcSequence.length; u++)
		{
			crcSequence[u] = bf.array()[u];
		}
		byte parityBit = (byte) ((getCRC8(crcSequence) ^ Integer.parseInt(bluetoothDeviceAddress.substring(bluetoothDeviceAddress.length() - 2), 16)) & 0xff);
		bf.put(parityBit);

		return bf.array();
	}

	// Get CRC parity bit.
	private int getCRC8(byte[] crcSequence)
	{
		int  len = crcSequence.length;
		int  i   = 0;
		byte crc = 0x00;

		while (len-- > 0)
		{
			byte extract = crcSequence[i++];
			for (byte tempI = 8; tempI != 0; tempI--)
			{
				byte sum = (byte) ((crc & 0xff) ^ (extract & 0xff));
				sum = (byte) ((sum & 0xff) & 0x01);
				crc = (byte) ((crc & 0xff) >>> 1);
				if (sum != 0)
				{
					crc = (byte) ((crc & 0xff) ^ 0x8c);
				}
				extract = (byte) ((extract & 0xff) >>> 1);
			}
		}
		return (crc & 0xff);
	}

	@Override
	public String toString()
	{
		return "[User Info] uid:" + this.uid + ", gender:" + this.gender + ", age:" + this.age + ", height:" + (this.height & 0xFF) + ", weight:" +
				(this.weight & 0xFF) + ", alias:" + this.alias + ", type:" + this.type;
	}
}
