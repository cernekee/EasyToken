/*
 * TokenInfo: container for token info; functions for loading/saving tokens
 *
 * This file is part of Easy Token
 * Copyright (c) 2014, Kevin Cernekee <cernekee@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package app.easytoken;

import java.security.MessageDigest;
import java.util.Locale;

import org.stoken.LibStoken;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;

public class TokenInfo {

	public static final String TAG = "EasyToken";

	public LibStoken lib;
	public String name;
	public String pin;
	public int id = -1;
	public boolean pinRequired;

	public static long lastModified;

	private static Context mContext;
	private static SharedPreferences mPrefs;
	private static boolean mSavePin;
	private static int mMaxId;
	private static int mDefaultId;
	private static String mDeviceId;

	public static void init(Context context) {
		mContext = context.getApplicationContext();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mSavePin = mPrefs.getBoolean("save_pin", true);
		mMaxId = mPrefs.getInt("max_id", 0);
		mDefaultId = mPrefs.getInt("default_id", -1);
		lastModified = mPrefs.getLong("last_modified", 0);

		/*
		 * ANDROID_ID is unique, but it is only 64 bits long.  So truncate its SHA1 hash to 12 bytes.
		 * Also, store it in mPrefs in case the ROM provides a random value on each access/boot/whatever.
		 * Because we don't want to get a bound token issued for a random device ID.
		 */
		mDeviceId = mPrefs.getString("device_id", null);
		if (mDeviceId == null) {
			String id = sha1(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
			mDeviceId = id.substring(0, 24);
			mPrefs.edit().putString("device_id", mDeviceId).commit();
		}
	}

	private static String sha1(String input) {
		try {
		    MessageDigest digest = MessageDigest.getInstance("SHA1");
		    digest.reset();

		    byte[] byteData = digest.digest(input.getBytes("UTF-8"));
		    StringBuffer sb = new StringBuffer();

		    for (int i = 0; i < byteData.length; i++){
		      sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		    }
		    return sb.toString().toUpperCase(Locale.getDefault());
		} catch (Exception e) {
			Log.e(TAG, "unable to compute SHA1 hash", e);
		}
		return "000000000000000000000000";
	}

	public static String getDeviceId() {
		return mDeviceId;
	}

	private static String getTokenString(int id) {
		return mPrefs.getString("token_str_" + id, null);
	}

	public static TokenInfo getToken(int id) {
		String s = getTokenString(id);

		if (s == null) {
			Log.e(TAG, "tried to access nonexistent token string #" + id);
			return null;
		}

		LibStoken lib = new LibStoken();
		int ret = lib.importString(s);
		if (ret != LibStoken.SUCCESS) {
			Log.e(TAG, "error importing token string #" + id + ": error " + ret);
			lib.destroy();
			return null;
		}

		ret = lib.decryptSeed(null, null);
		if (ret != LibStoken.SUCCESS) {
			Log.e(TAG, "error decrypting token string #" + id + ": error " + ret);
			lib.destroy();
			return null;
		}

		TokenInfo info = new TokenInfo(lib,
				mPrefs.getString("token_pin_" + id, null),
				mPrefs.getString("token_name_" + id, "UNKNOWN"));
		info.id = id;
		return info;
	}

	public static TokenInfo getDefaultToken() {
		if (mDefaultId < 0) {
			return null;
		}
		return getToken(mDefaultId);
	}

	/*
	 * Notes on using the public constructors:
	 *
	 * LIB must already contain a successfully decrypted token.
	 *
	 * There is no way to save invalid or encrypted seeds in the preferences.  save() will
	 * re-encode the seed into a standard format.
	 *
	 * Do not call lib.destroy() if LIB is owned by a TokenInfo instance.
	 */
	public TokenInfo(LibStoken lib, String pin, String name) {
		this.lib = lib;
		this.pin = (pin == null) ? "" : pin;
		this.name = name;

		// This changes to false if computeTokencode() is called with a PIN
		this.pinRequired = lib.isPINRequired();
	}

	public TokenInfo(LibStoken lib, String pin) {
		this(lib, pin, lib.getInfo().serial);
	}

	private static void updateLastModified() {
		lastModified = System.currentTimeMillis();
		mPrefs.edit().putLong("last_modified", lastModified).commit();
		TokencodeWidgetService.restart(mContext);
	}

	public void makeDefault() {
		mDefaultId = id;
		mPrefs.edit().putInt("default_id", id).commit();
		updateLastModified();
	}

	public void delete() {
		int newMax, newDefault = -1;

		for (newMax = mMaxId; newMax >= 0; newMax--) {
			if (newMax != id && getTokenString(newMax) != null)
				break;
		}

		for (int i = 0; i <= newMax; i++) {
			if (getTokenString(i) != null) {
				newDefault = i;
				break;
			}
		}

		mPrefs.edit()
			.remove("token_str_" + id)
			.remove("token_pin_" + id)
			.remove("token_name_" + id)
			.putInt("max_id", newMax)
			.putInt("default_id", newDefault)
			.commit();
		updateLastModified();

		mMaxId = newMax;
		mDefaultId = newDefault;
	}

	/* returns true if changed, false otherwise */
	private boolean writePrefString(Editor ed, String key, String value) {
		String old = mPrefs.getString(key, null);
		if (value.equals(old)) {
			return false;
		}
		ed.putString(key, value);
		return true;
	}

	public int save() {
		boolean changed = false;

		if (id == -1) {
			/* find the next free slot */
			for (id = 0; id <= mMaxId; id++) {
				if (getTokenString(id) == null) {
					break;
				}
			}
			if (mMaxId < id) {
				mMaxId = id;
				changed = true;
			}
		}

		Editor ed = mPrefs.edit();

		changed |= writePrefString(ed, "token_str_" + id, lib.encryptSeed(null, null));
		changed |= writePrefString(ed, "token_name_" + id, name);

		if (mSavePin) {
			changed |= writePrefString(ed, "token_pin_" + id, pin);
		}
		ed.putInt("max_id", mMaxId);
		ed.commit();

		if (changed) {
			updateLastModified();
		}

		if (getDefaultToken() == null) {
			makeDefault();
		}

		return id;
	}

	public static void setSavePin(boolean val) {
		mSavePin = val;
		if (mSavePin == true) {
			updateLastModified();
			return;
		}

		/* on deselection, clear all saved PINs */
		Editor ed = mPrefs.edit();
		for (int id = 0; id <= mMaxId; id++) {
			ed.remove("token_pin_" + id);
		}
		ed.commit();
		updateLastModified();
	}

	public boolean isPinMissing() {
		return pinRequired && pin.equals("");
	}

	@Override
	protected void finalize() throws Throwable {
		lib.destroy();
		super.finalize();
	}

}
