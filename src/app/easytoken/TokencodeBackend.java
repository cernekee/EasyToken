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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

public class TokencodeBackend extends BroadcastReceiver {

	public static final String TAG = "EasyToken";

	public TokenInfo info;
	private LibStoken.StokenInfo stokenInfo;

	private Context mContext;
	private OnTokencodeUpdateListener mListener;

	private final int INTERVAL_MS = 1000;
	private boolean mIsRunning;
	private Handler mHandler;
	private Runnable mRunnable;

	private boolean mCallbackEnabled;
	private Calendar mLastUpdate;
	private String mTokencode;
	private String mNextTokencode;

	private boolean mScreenOn = true;

	public interface OnTokencodeUpdateListener {
		/* Strings are unformatted.  secondsLeft ranges from 1..30 or 1..60 */
		public void onTokencodeUpdate(String tokencode, String nextTokencode, int secondsLeft);
	};

	private void startOrStop() {
		boolean shouldRun = mScreenOn && mCallbackEnabled;

		if (shouldRun && !mIsRunning) {
			// this does one update and then schedules the next one
			doUpdate(true);
		} else if (!shouldRun && mIsRunning) {
			mHandler.removeCallbacks(mRunnable);
		}

		mIsRunning = shouldRun;
	}

	public void onResume() {
		// start callback events
		mCallbackEnabled = true;
		startOrStop();
	}

	public void onPause() {
		// pause callback events
		mCallbackEnabled = false;
		startOrStop();
	}

	public boolean init(Context ctx, OnTokencodeUpdateListener listener, int tokenId) {
		mContext = ctx.getApplicationContext();
		mListener = listener;

		info = TokenInfo.getToken(tokenId);
		if (info == null) {
			return false;
		}

		mHandler = new Handler();
		mRunnable = new Runnable() {
			@Override
			public void run() {
				doUpdate(true);
			}
		};

		stokenInfo = info.lib.getInfo();

		IntentFilter filt = new IntentFilter();
		filt.addAction(Intent.ACTION_SCREEN_OFF);
		filt.addAction(Intent.ACTION_SCREEN_ON);
		mContext.registerReceiver(this, filt);

		return true;
	}

	public void setPin(String pin) {
		if (!pin.equals(info.pin)) {
			info.pin = pin;
			info.save();
		}
	}

	public void destroy() {
		mContext.unregisterReceiver(this);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (Intent.ACTION_SCREEN_OFF.equals(action)) {
			mScreenOn = false;
		} else if (Intent.ACTION_SCREEN_ON.equals(action)) {
			mScreenOn = true;
		}
		startOrStop();
	}

	private void doUpdate(boolean reschedule) {
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

		if (reschedule) {
			mHandler.postDelayed(mRunnable, INTERVAL_MS);
		}
	}

	public void updateNow() {
		doUpdate(false);
	}

    public static String formatTokencode(String s) {
    	int midpoint = s.length() / 2;
    	return s.substring(0, midpoint) + " " + s.substring(midpoint);
    }
}
