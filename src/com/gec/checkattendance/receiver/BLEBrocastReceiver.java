package com.gec.checkattendance.receiver;

import com.gec.checkattendance.util.BLEBase;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class BLEBrocastReceiver extends BroadcastReceiver {
	private Handler mHandler = null;

	public BLEBrocastReceiver(Handler handler) {
		mHandler = handler;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action == BluetoothDevice.ACTION_ACL_CONNECTED)
			mHandler.sendEmptyMessage(BLEBase.WHAT_BLE_DEVICE_CONNECTED);
		else if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED)
			mHandler.sendEmptyMessage(BLEBase.WHAT_BLE_DEVICE_DISCONNECTED);
	}
}
