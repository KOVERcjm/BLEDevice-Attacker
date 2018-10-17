package cheng.kover.attacker;

import java.util.UUID;

class MiBandProfile
{
	// ============================================== Service Start ==============================================
	/**
	 * Main service
	 */
	public static final UUID UUID_SERVICE_MILI = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");

	/**
	 * Immediate Alert - Vibration Service
	 */
	public static final UUID UUID_SERVICE_IMMEDIATE_ALERT = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
	// ==============================================  Service End  ==============================================

	// ========================================== Characteristic Start ===========================================
	/**
	 * Read and write User Info
	 */
	public static final UUID UUID_CHAR_USER_INFO = UUID.fromString("0000ff04-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHAR_DEVICE_INFO = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHAR_DEVICE_NAME = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
	public static final UUID UUID_CHAR_DATE_TIME = UUID.fromString("0000ff0a-0000-1000-8000-00805f9b34fb");

	/**
	 * Pair with MiBand
	 */
	public static final UUID UUID_CHAR_PAIR = UUID.fromString("0000ff0f-0000-1000-8000-00805f9b34fb");

	/**
	 * Vibrate to let find MiBand
	 */
	public static final UUID UUID_CHAR_VIBRATION = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
	// =========================================== Characteristic End ============================================
}
