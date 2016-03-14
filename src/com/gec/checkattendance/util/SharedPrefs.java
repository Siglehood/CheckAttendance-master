package com.gec.checkattendance.util;

import android.content.Context;

public class SharedPrefs {
	private static final String SHARED_PREFS_NAME = "check_attendance_shared";

	/**
	 * �����û���Ϣ
	 * 
	 * @param context
	 * @param idNumber
	 * @return
	 */
	public static boolean saveIdNum(Context context, String idNumber) {
		return context.getSharedPreferences(SHARED_PREFS_NAME, 0).edit().putString("id", idNumber).commit();
	}

	/**
	 * ��ȡ�û���Ϣ
	 * 
	 * @param context
	 * @return
	 */
	public static String getIdNum(Context context) {
		return context.getSharedPreferences(SHARED_PREFS_NAME, 0).getString("id", null);
	}
}
