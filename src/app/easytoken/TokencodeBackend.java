/*
 * TokencodeBackend: Computes tokencodes and updates a Fragment/AppWidget
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

import java.util.Calendar;

import org.stoken.LibStoken;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class TokencodeBackend extends BroadcastReceiver {

	public static final String TAG = "EasyToken";

	public TokenInfo info;
	private LibStoken.StokenInfo stokenInfo;

	private Context mContext;
	private OnTokencodeUpdateListener mListener;

	private boolean mCallbackEnabled;
	private Calendar mLastUpdate;
	private String mTokencode;
	private String mNextTokencode;

	private static PendingIntent mPendingIntent;
	private static int mRefcount;

	private static final String TOKENCODE_ALARM = "app.easytoken.tokencode_alarm";

	public interface OnTokencodeUpdateListener {
		/* Strings are unformatted.  secondsLeft ranges from 1..30 or 1..60 */
		public void onTokencodeUpdate(String tokencode, String nextTokencode, int secondsLeft);
	};

	private static void updateRefcount(Context ctx, boolean increment) {
		if (increment) {
			if (mRefcount == 0) {
				Intent intent = new Intent(TOKENCODE_ALARM);
				mPendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);

				AlarmManager am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
				am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 1000, mPendingIntent);
			}
			mRefcount++;
		} else {
			mRefcount--;
			if (mRefcount == 0) {
				AlarmManager am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
				am.cancel(mPendingIntent);
				mPendingIntent = null;
			}
		}
	}

	public void onResume() {
		// start callback events
		mCallbackEnabled = true;
		updateRefcount(mContext, true);
	}

	public void onPause() {
		// pause callback events
		updateRefcount(mContext, false);
		mCallbackEnabled = false;
	}

	public boolean init(Context ctx, OnTokencodeUpdateListener listener, int tokenId) {
		mContext = ctx.getApplicationContext();
		mListener = listener;

		info = TokenInfo.getToken(tokenId);
		if (info == null) {
			return false;
		}

		stokenInfo = info.lib.getInfo();

		mContext.registerReceiver(this, new IntentFilter(TOKENCODE_ALARM));

		return true;
	}

	public void setPin(String pin) {
		if (!pin.equals(info.pin)) {
			info.pin = pin;
			info.save();
			onReceive(null, null);
		}
	}

	public void destroy() {
		mContext.unregisterReceiver(this);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!mCallbackEnabled) {
			// somebody else might be using the periodic broadcast, but it's not us
			return;
		}

    	Calendar now = Calendar.getInstance();
		int second = now.get(Calendar.SECOND);
		int interval = stokenInfo.interval;
		String pin = !info.pin.equals("") ? info.pin : "0000";

		// only recompute the tokencodes every <interval> seconds
		if (interval == 30) {
			now.set(Calendar.SECOND, second >= 30 ? 30 : 0);
		} else {
			now.set(Calendar.SECOND, 0);
		}

    	if (mLastUpdate == null || now.compareTo(mLastUpdate) != 0) {
    		long t = now.getTimeInMillis() / 1000;

    		mTokencode = info.lib.computeTokencode(t, pin);
    		mNextTokencode = info.lib.computeTokencode(t + interval, pin);
    		mLastUpdate = now;
    	}

		mListener.onTokencodeUpdate(mTokencode, mNextTokencode, interval - (second % interval));
	}

    public static String formatTokencode(String s) {
    	int midpoint = s.length() / 2;
    	return s.substring(0, midpoint) + " " + s.substring(midpoint);
    }
}
