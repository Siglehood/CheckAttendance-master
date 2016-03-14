package com.gec.checkattendance;

import java.util.ArrayList;
import java.util.List;

import com.gec.checkattendance.adapter.ViewPagerAdapter;
import com.gec.checkattendance.receiver.BLEBrocastReceiver;
import com.gec.checkattendance.util.BLEBase;
import com.gec.checkattendance.util.SharedPrefs;
import com.gec.checkattendance.util.Toaster;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class MainActivity extends Activity {
	public static final int REQUEST_BLE_ENABLE = 0;

	private ViewPager mViewPager = null;
	private ImageButton mMarkBtn = null;
	private ImageButton mRegisterBtn = null;
	private EditText mUuidEt = null;
	private Button mEnterBtn = null;
	private BLEBrocastReceiver mBleBrocastReceiver = null;
	private List<View> mViewList = null;
	private ViewPagerAdapter mPagerAdapter = null;
	private BLEBase mBLEBase = null;

	private boolean mUuidIsFound = false;
	private int mIdNum = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		initUI();
	}

	@Override
	protected void onStart() {
		super.onStart();
		initReceiver();
		initViewPager();
		initBLEBase();
		getSharedPrefsStr();
		setListener();
	}

	@Override
	protected void onStop() {
		mBLEBase.closeScanThread();
		mBLEBase.disconnDevice();
		mBLEBase.closeGatt();
		unregisterReceiver(mBleBrocastReceiver);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		mBLEBase.disableBluetoothAdapter();
		if (mDelayThread != null)
			mHandler.removeCallbacks(mDelayThread);
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_BLE_ENABLE && resultCode == Activity.RESULT_CANCELED) {
			finish();
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btn_mark:
				if (!mUuidIsFound)
					return;
				mBLEBase.mCharacteristicOp = BLEBase.CHAR1;
				mBLEBase.connectGatt();
				mUuidEt.setEnabled(true);
				mEnterBtn.setEnabled(true);
				break;
			case R.id.btn_register:
				if (!mUuidIsFound)
					return;
				mBLEBase.mCharacteristicOp = BLEBase.CHAR3;
				// register();
				break;

			case R.id.btn_enter:
				String id = mUuidEt.getText().toString();
				if (id.length() == 4) {
					Toaster.shortToastShow(getApplicationContext(), R.string.toast_enter_uuid);
					SharedPrefs.saveIdNum(getApplicationContext(), id);
					mUuidIsFound = true;
					mMarkBtn.setBackgroundResource(R.drawable.markbt_style);
					mIdNum = Integer.valueOf(id).intValue();
					mBLEBase.scanLeDevice(mIdNum);
					mMarkBtn.setEnabled(true);
					mMarkBtn.setBackgroundResource(R.drawable.markbt_style);
					mUuidEt.setEnabled(false);
					mEnterBtn.setEnabled(false);
				} else
					Toaster.shortToastShow(getApplicationContext(), R.string.edit_hint);
				break;
			default:
				break;
			}
		}
	};

	private Handler mHandler = new Handler(new Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case BLEBase.WHAT_BLE_ENABLE:
				Intent bluetoothEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(bluetoothEnableIntent, REQUEST_BLE_ENABLE);
				break;
			case BLEBase.WHAT_BLE_DEVICE_CONNECTED:
				Toaster.shortToastShow(getApplicationContext(), R.string.marked);
				break;
			case BLEBase.WHAT_BLE_DEVICE_DISCONNECTED:
				mBLEBase.disconnDevice();
				mBLEBase.closeGatt();
				break;
			}
			return false;
		}
	});

	private void initBLEBase() {
		mBLEBase = new BLEBase(getApplicationContext(), mHandler);
		mBLEBase.BLEInit();
		mBLEBase.BLEEnabled();
	}

	private void initUI() {
		mViewPager = (ViewPager) findViewById(R.id.view_pager);
		mMarkBtn = (ImageButton) findViewById(R.id.btn_mark);
		mRegisterBtn = (ImageButton) findViewById(R.id.btn_register);
		mUuidEt = (EditText) findViewById(R.id.et_uuid);
		mEnterBtn = (Button) findViewById(R.id.btn_enter);
		mMarkBtn.setEnabled(false);
		hideSoftInputMethod();
	}

	/**
	 * 点击小键盘回车符隐藏输入法
	 */
	private void hideSoftInputMethod() {
		mUuidEt.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					InputMethodManager imm = (InputMethodManager) v.getContext()
							.getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm.isActive())
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
					return true;
				}
				return false;
			}
		});
	}

	private void initReceiver() {
		mBleBrocastReceiver = new BLEBrocastReceiver(mHandler);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		registerReceiver(mBleBrocastReceiver, intentFilter);
	}

	private void initViewPager() {
		mViewList = new ArrayList<View>();
		mViewList.add(getLayoutInflater().inflate(R.layout.main_page1, mViewPager, false));
		mViewList.add(getLayoutInflater().inflate(R.layout.main_page2, mViewPager, false));
		mPagerAdapter = new ViewPagerAdapter(mViewList);
		mViewPager.setAdapter(mPagerAdapter);
		setPageAuto();
	}

	private void getSharedPrefsStr() {
		String mIdNum = SharedPrefs.getIdNum(getApplicationContext());
		if (mIdNum != null)
			mUuidEt.setText(mIdNum);
	}

	private void setListener() {
		mMarkBtn.setOnClickListener(mOnClickListener);
		mRegisterBtn.setOnClickListener(mOnClickListener);
		mEnterBtn.setOnClickListener(mOnClickListener);
	}

	// private void register() {
	// final EditText inputServer = new EditText(mContext);
	// inputServer.setInputType(InputType.TYPE_CLASS_NUMBER);
	// new
	// AlertDialog.Builder(mContext).setTitle(R.string.input_student_number).setIcon(android.R.drawable.ic_dialog_info)
	// .setView(inputServer).setNegativeButton(R.string.cancel, null)
	// .setPositiveButton(R.string.confirm, new
	// DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int which) {
	// mBLEBase.mStuNum = inputServer.getText().toString();
	// if (mBLEBase.mStuNum != null) {
	// mBLEBase.scanLeDevice(mIdNum);
	// }
	// }
	// }).show();
	// }

	private void setPageAuto() {
		mHandler.postDelayed(mDelayThread, 5000);
	}

	Runnable mDelayThread = new Runnable() {
		int count = 0;

		@Override
		public void run() {
			if (count > 1)
				count = 0;
			mViewPager.setCurrentItem(count++);
			mHandler.postDelayed(mDelayThread, 5000);
		}
	};
}
