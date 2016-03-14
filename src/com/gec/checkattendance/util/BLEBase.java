package com.gec.checkattendance.util;

import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class BLEBase {
	public static final int WHAT_BLE_ENABLE = 0;
	public static final int WHAT_BLE_DEVICE_CONNECTED = 1;
	public static final int WHAT_BLE_DEVICE_DISCONNECTED = 2;

	public static final int NONE = 0;
	public static final int CHAR1 = 1;
	public static final int CHAR2 = 2;
	public static final int CHAR3 = 3;

	public int mCharacteristicOp = NONE;
	public String mStuNum = null;

	private static final String TAG = BLEBase.class.getSimpleName();
	private static final boolean D = true;

	private static final String CHECK_ATTENDANCE_UUID = "0000fef0-0000-1000-8000-00805f9b34fb";
	private static final String RESPOND_CHARACTERISTIC_UUID = "0000fef2-0000-1000-8000-00805f9b34fb";
	private static final String CLIENT_UUID = "00002902-0000-1000-8000-00805f9b34fb";
	private static final String REGISTER_CHARACTERISTIC_UUID = "0000fef3-0000-1000-8000-00805f9b34fb";
	private static final String MARK_CHARACTERISTIC_UUID = "0000fef1-0000-1000-8000-00805f9b34fb";

	private Context mContext = null;
	private Handler mHandler = null;

	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothDevice mBluetoothDevice = null;
	private LeScanCallback mLeScanCallback = null;
	private BluetoothGatt mBluetoothGatt = null;
	private BluetoothGattService mBluetoothGattService = null;
	private BluetoothGattCharacteristic mMarkGattCharacteristic = null;
	private BluetoothGattCharacteristic mRespondGattCharacteristic = null;
	private BluetoothGattCharacteristic mRegisterGattCharacteristic = null;

	private byte[] mRespondByteArr;
	private byte[] mRegisterByteArr;
	private byte[] mMarkByteArr = { 0x5a };

	private boolean mScanning = false;

	public BLEBase(Context context, Handler handler) {
		this.mContext = context;
		this.mHandler = handler;
	}

	/**
	 * 初始化本地蓝牙设备
	 */
	public void BLEInit() {
		BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
	}

	/**
	 * 检测蓝牙设备是否开启，如果未开启，发起Intent并回调
	 */
	public void BLEEnabled() {
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			mHandler.sendEmptyMessage(WHAT_BLE_ENABLE);
		}
	}

	public void scanLeDevice(int idNum) {
		final int id = idNum;
		mLeScanCallback = new LeScanCallback() {

			@Override
			public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
				for (byte b : scanRecord)
					Log.d(TAG, b + "\n");
				closeScanThread();
				if (id == (scanRecord[7] * 100 + scanRecord[8])) {
					if (D)
						Log.d(TAG, device.getName() + "——" + device.getAddress());
					if (mBluetoothDevice != null)
						mBluetoothDevice = null;
					mBluetoothDevice = device;
				}
			}
		};
		// 启动扫描线程
		mScanThread.run();
	}

	private Runnable mScanThread = new Runnable() {

		@SuppressWarnings("deprecation")
		@Override
		public void run() {
			if (!mScanning) {
				mScanning = true;
				mBluetoothAdapter.startLeScan(mLeScanCallback);
				mHandler.postDelayed(mScanThread, 5 * 1000);
			} else {
				mScanning = false;
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				mHandler.postDelayed(mScanThread, 2 * 1000);
			}
		};
	};

	/**
	 * 连接GATT服务器，会连接该BLE设备并回调
	 */
	public void connectGatt() {
		if (mBluetoothDevice != null)
			mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mBluetoothGattCallback);
	}

	public void disableBluetoothAdapter() {
		if (mBluetoothAdapter != null)
			mBluetoothAdapter.disable();
	}

	/**
	 * 关闭扫描线程
	 */
	public void closeScanThread() {
		if (mScanThread != null)
			mHandler.removeCallbacks(mScanThread);
	}

	/**
	 * 断开BLE设备的连接
	 */
	public void disconnDevice() {
		if (mBluetoothGatt != null) {
			mBluetoothGatt.disconnect();
		}
	}

	/**
	 * 结束通讯，关闭GATT客户端，释放资源
	 */
	public void closeGatt() {
		if (mBluetoothGatt != null)
			mBluetoothGatt.close();
	}

	/**
	 * 开启蓝牙通知
	 */
	public void characteristicNotificationEnabled() {
		mBluetoothGatt.setCharacteristicNotification(mRespondGattCharacteristic, true);
		BluetoothGattDescriptor bluetoothGattDescriptor = mRespondGattCharacteristic
				.getDescriptor(UUID.fromString(CLIENT_UUID));
		bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		mBluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
	}

	/**
	 * BLE GATT服务连接回调
	 */
	private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				gatt.discoverServices();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				// 获取服务
				mBluetoothGattService = gatt.getService(UUID.fromString(CHECK_ATTENDANCE_UUID));
				if (mBluetoothGattService != null) {
					// 连接上后配置通知使能，在回调中写特征值
					mRespondGattCharacteristic = mBluetoothGattService
							.getCharacteristic(UUID.fromString(RESPOND_CHARACTERISTIC_UUID));
					if (mRespondGattCharacteristic != null) {
						characteristicNotificationEnabled();
					}
				}
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			if (characteristic == mRespondGattCharacteristic) {
				mRespondByteArr = characteristic.getValue();
				closeScanThread();
				disconnDevice();
				if (mRespondByteArr[0] == 0x00) {
					if (D)
						Log.d(TAG, "mark");
				} else if (mRespondByteArr[0] == 0x01) {
					if (D)
						Log.d(TAG, "register");
				}
			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			if (mCharacteristicOp == CHAR1) {
				mMarkGattCharacteristic = mBluetoothGattService
						.getCharacteristic(UUID.fromString(MARK_CHARACTERISTIC_UUID));
				if (mMarkGattCharacteristic != null) {
					mCharacteristicOp = NONE;
					mMarkGattCharacteristic.setValue(mMarkByteArr);
					mBluetoothGatt.writeCharacteristic(mMarkGattCharacteristic);
				}
			} else if (mCharacteristicOp == CHAR3) {
				mRegisterGattCharacteristic = mBluetoothGattService
						.getCharacteristic(UUID.fromString(REGISTER_CHARACTERISTIC_UUID));
				if (mRegisterGattCharacteristic != null) {
					mCharacteristicOp = NONE;
					mRegisterByteArr = mStuNum.getBytes();
					mRegisterGattCharacteristic.setValue(mRegisterByteArr);
					mBluetoothGatt.writeCharacteristic(mRegisterGattCharacteristic);
				}
			}
		}
	};
}
