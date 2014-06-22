/*
 * AboutFragment: uses a WebView to display the "About" text
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

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

public class AboutFragment extends Fragment {

	public static final String TAG = "EasyToken";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Activity activity = getActivity();
		activity.setTitle(R.string.about);

		View v = inflater.inflate(R.layout.fragment_about, container, false);
		TextView ver = (TextView) v.findViewById(R.id.version);

		try {
			PackageInfo packageinfo = activity.getPackageManager()
					.getPackageInfo(activity.getPackageName(), 0);
			ver.setText("Easy Token v" + packageinfo.versionName);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "can't retrieve package version");
		}

		WebView contents = (WebView) v.findViewById(R.id.about_contents);
		contents.loadUrl("file:///android_asset/about.html");
		contents.setBackgroundColor(0);

		return v;
	}

}
