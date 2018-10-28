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
	 * Device info
	 */
	public static final UUID UUID_CHAR_DEVICE_INFO = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
	/**
	 * Device name
	 */
	public static final UUID UUID_CHAR_DEVICE_NAME = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
	/**
	 * User Info
	 */
	public static final UUID UUID_CHAR_NOTIFICATION = UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb");
	/**
	 * User Info
	 */
	public static final UUID UUID_CHAR_USER_INFO = UUID.fromString("0000ff04-0000-1000-8000-00805f9b34fb");
	/**
	 * Control point - sensor notify
	 */
	public static final UUID UUID_CHAR_CONTROL_POINT = UUID.fromString("0000ff05-0000-1000-8000-00805f9b34fb");
	/**
	 * Real-time steps notify
	 */
	public static final UUID UUID_CHAR_REALTIME_STEPS = UUID.fromString("0000ff06-0000-1000-8000-00805f9b34fb");
	/**
	 * Activity data
	 */
	public static final UUID UUID_CHAR_ACTIVITY_DATA = UUID.fromString("0000ff07-0000-1000-8000-00805f9b34fb");
	/**
	 * LE Parameters
	 */
	public static final UUID UUID_CHAR_LE_PARAMS = UUID.fromString("0000ff09-0000-1000-8000-00805f9b34fb");
	/**
	 *  Test
	 */
	public static final UUID UUID_CHAR_STATISTICS = UUID.fromString("0000ff0b-0000-1000-8000-00805f9b34fb");
	/**
	 * Sensor data notify
	 */
	public static final UUID UUID_CHAR_SENSOR_DATA = UUID.fromString("0000ff0e-0000-1000-8000-00805f9b34fb");
	/**
	 * Pair with MiBand
	 */
	public static final UUID UUID_CHAR_PAIR = UUID.fromString("0000ff0f-0000-1000-8000-00805f9b34fb");
	/**
	 * Vibrate to let find MiBand
	 */
	public static final UUID UUID_CHAR_VIBRATION = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
	// =========================================== Characteristic End ============================================

	// =========================================== Notification Start ============================================
	public static final UUID UUID_DESCRIPTOR_UPDATE_NOTIFICATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	// ============================================ Notification End =============================================
}
