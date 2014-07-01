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

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.widget.RemoteViews;

public class TokencodeWidgetService extends Service
		implements TokencodeBackend.OnTokencodeUpdateListener {

	public static final String TAG = "EasyToken";

	private static final String PFX = "app.easytoken.";
	private static final String ACTION_KICK = PFX + "kick";
	private static final String ACTION_RESTART = PFX + "restart";

	private TokencodeBackend mBackend;

	private String mTokencode = "";
	private int mSecondsLeft;
	private int mInterval;

	private boolean mInitDone;
	private Context mContext;
	private ComponentName mComponent;

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
			return false;
		}

		mBackend = new TokencodeBackend();
		mBackend.init(mContext.getApplicationContext(), this, t.id);
		mBackend.onResume();
		mInterval = mBackend.info.lib.getInfo().interval;

		return true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent == null ? null : intent.getAction();
		if (ACTION_RESTART.equals(action) || !mInitDone) {
			mContext = getApplicationContext();
			mComponent = new ComponentName(mContext, TokencodeWidget.class);
			mInitDone = true;

			if (startBackend() == false) {
				mTokencode = "NO TOKEN";
				mSecondsLeft = 60;
				mInterval = 60;
				updateWidgets();
				stopSelf();
			}
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

	private int updateWidgets() {
		AppWidgetManager mgr = AppWidgetManager.getInstance(mContext);
		final int ids[] = mgr.getAppWidgetIds(mComponent);
		final int N = ids.length;

		for (int i = 0; i < N; i++) {
			int id = ids[i];

	        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget);
	        Bundle options = mgr.getAppWidgetOptions(id);

	        views.setTextViewText(R.id.tokencode, mTokencode);
	        views.setTextViewTextSize(R.id.tokencode, TypedValue.COMPLEX_UNIT_PX,
	        		scaleView(options, R.dimen.widget_typical_fontsize));

	        views.setProgressBar(R.id.progress_bar, mInterval - 1, mSecondsLeft - 1, false);
	        int padding = (int)scaleView(options, R.dimen.widget_typical_padding);
	        views.setViewPadding(R.id.box, padding, padding, padding, padding);

	        Intent intent = new Intent(mContext, MainActivity.class);
	        PendingIntent pi =
	        		PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	        views.setOnClickPendingIntent(R.id.box, pi);

			mgr.updateAppWidget(id, views);
		}

		return N;
	}

	@Override
	public void onTokencodeUpdate(String tokencode, String nextTokencode, int secondsLeft) {
		mTokencode = TokencodeBackend.formatTokencode(tokencode);
		mSecondsLeft = secondsLeft;

		if (updateWidgets() == 0) {
			stopBackend();
			stopSelf();
		}
	}
}
