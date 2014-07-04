/*
 * TokencodeWidgetService: update all widgets every second
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

public class TokencodeWidgetService extends Service
		implements TokencodeBackend.OnTokencodeUpdateListener {

	public static final String TAG = "EasyToken";

	private static final String PFX = "app.easytoken.";
	private static final String ACTION_KICK = PFX + "kick";
	private static final String ACTION_RESTART = PFX + "restart";
	private static final String ACTION_TOGGLE_CLOCK = PFX + "toggle_clock";

	private static boolean mFgPref;
	private boolean mClockMode;

	private boolean mIsForeground;
	private TokencodeBackend mBackend;

	private boolean mError;
	private String mTokencode = "";
	private int mSecondsLeft;
	private int mInterval;

	private boolean mInitDone;
	private Context mContext;
	private ComponentName mComponent;

	public static void init(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		mFgPref = sp.getBoolean("fg_service", false);
		kick(context);
	}

	public static void kick(Context context) {
		Intent i = new Intent(context, TokencodeWidgetService.class);
		i.setAction(ACTION_KICK);
		context.startService(i);
	}

	public static void restart(Context context) {
		Intent i = new Intent(context, TokencodeWidgetService.class);
		i.setAction(ACTION_RESTART);
		context.startService(i);
	}

	public static void setFgService(boolean val) {
		mFgPref = val;
	}

	private void stopBackend() {
		if (mBackend != null) {
			mBackend.onPause();
			mBackend.destroy();
			mBackend = null;
		}
	}

	private boolean startBackend() {
		stopBackend();

		TokenInfo t = TokenInfo.getDefaultToken();
		if (t == null) {
			mTokencode = "NO TOKEN";
			return false;
		}

		mBackend = new TokencodeBackend();
		mBackend.init(mContext.getApplicationContext(), this, t.id);

		if (mBackend.info.isPinMissing()) {
			mTokencode = "NO PIN";
			return false;
		}

		mInterval = mBackend.info.lib.getInfo().interval;
		mBackend.onResume();
		return true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent == null ? null : intent.getAction();
		if (ACTION_RESTART.equals(action) || !mInitDone) {
			mContext = getApplicationContext();
			mComponent = new ComponentName(mContext, TokencodeWidget.class);
			mInitDone = true;

			mError = !startBackend();
			if (mError) {
				stopBackend();
				mSecondsLeft = mInterval = 60;
				updateWidgets();
				stopSelf();
			}
		} else if (ACTION_TOGGLE_CLOCK.equals(action)) {
			mClockMode = !mClockMode;
			updateWidgets();
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private float scaleView(Bundle options, int defFontSizeId) {
        Resources res = mContext.getResources();

        float fontSizePx = res.getDimension(defFontSizeId);

        int widgetWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        if (widgetWidthDp == 0) {
        	return fontSizePx;
        }

        float widgetWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widgetWidthDp,
        		res.getDisplayMetrics());
        float typWidthPx = res.getDimension(R.dimen.widget_typical_width);

        return fontSizePx * widgetWidthPx / typWidthPx;
	}

	private void updateTokencode(RemoteViews views, Bundle options) {
    	int padding = (int)scaleView(options, R.dimen.widget_typical_padding);
    	views.setViewPadding(R.id.box, padding, padding, padding, padding);

        views.setTextViewTextSize(R.id.tokencode, TypedValue.COMPLEX_UNIT_PX,
        		scaleView(options, R.dimen.widget_typical_tokencode_fontsize));

        views.setViewVisibility(R.id.progress_bar, mError ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.date, View.GONE);

        views.setTextViewText(R.id.tokencode, mTokencode);
        views.setProgressBar(R.id.progress_bar, mInterval - 1, mSecondsLeft - 1, false);
	}

	private void updateClock(RemoteViews views, Bundle options) {
    	int padding = (int)scaleView(options, R.dimen.widget_typical_padding);
    	views.setViewPadding(R.id.box, padding, padding, padding, padding);

        views.setTextViewTextSize(R.id.tokencode, TypedValue.COMPLEX_UNIT_PX,
        		scaleView(options, R.dimen.widget_typical_time_fontsize));
        views.setTextViewTextSize(R.id.date, TypedValue.COMPLEX_UNIT_PX,
        		scaleView(options, R.dimen.widget_typical_date_fontsize));

        views.setViewVisibility(R.id.progress_bar, View.GONE);
        views.setViewVisibility(R.id.date, View.VISIBLE);

        Date now = Calendar.getInstance().getTime();
        DateFormat dfTime = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        DateFormat dfDate = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String weekday = new SimpleDateFormat("EEEE", Locale.getDefault()).format(now);

        views.setTextViewText(R.id.tokencode, dfTime.format(now));
        views.setTextViewText(R.id.date, weekday + ", " + dfDate.format(now));
	}

	private void updateLockScreenWidget(RemoteViews views, Bundle options) {
		if (mClockMode && !mError) {
			updateClock(views, options);
		} else {
			updateTokencode(views, options);
		}

		Intent intent = new Intent(mContext, TokencodeWidgetService.class);
		intent.setAction(ACTION_TOGGLE_CLOCK);

        PendingIntent pi = PendingIntent.getService(mContext, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.box, pi);
	}

	private void updateNormalWidget(RemoteViews views, Bundle options) {
		updateTokencode(views, options);

        Intent intent = new Intent(mContext, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.box, pi);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int updateWidgets() {
		AppWidgetManager mgr = AppWidgetManager.getInstance(mContext);
		final int ids[] = mgr.getAppWidgetIds(mComponent);
		final int N = ids.length;

		for (int i = 0; i < N; i++) {
			int id = ids[i];

	        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget);
	        Bundle options = mgr.getAppWidgetOptions(id);

	        int category = options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
	        if (category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
	        	updateLockScreenWidget(views, options);
	        } else {
	        	updateNormalWidget(views, options);
	        }

			mgr.updateAppWidget(id, views);
		}

		if (!mIsForeground && mFgPref) {
			Notification note = new Notification.Builder(mContext).build();
			startForeground(1, note);
		} else if (mIsForeground && !mFgPref) {
			stopForeground(true);
		}
		mIsForeground = mFgPref;

		return N;
	}

	@Override
	public void onTokencodeUpdate(String tokencode, String nextTokencode, int secondsLeft) {
		mTokencode = TokencodeBackend.formatTokencode(tokencode);
		mSecondsLeft = secondsLeft;

		if (updateWidgets() == 0) {
			stopBackend();
			if (mIsForeground) {
				stopForeground(true);
			}
			stopSelf();
		}
	}
}
