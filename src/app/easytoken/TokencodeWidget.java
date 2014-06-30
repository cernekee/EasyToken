/*
 * TokencodeWidget: provides a home screen / lock screen widget
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

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class TokencodeWidget extends AppWidgetProvider {

	public static final String TAG = "EasyToken";

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		TokencodeWidgetService.kick(context);
	}
}
