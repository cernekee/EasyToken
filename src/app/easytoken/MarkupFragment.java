/*
 * MarkupFragment: Displays marked-up text from an array
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MarkupFragment extends Fragment {

	public static final String TAG = "EasyToken";

	private String getPkgInfo() {
		try {
			Activity activity = getActivity();
			PackageInfo packageinfo = activity.getPackageManager()
					.getPackageInfo(activity.getPackageName(), 0);

			StringBuilder sb = new StringBuilder();
			sb.append(getString(packageinfo.applicationInfo.labelRes));
			sb.append(" v");
			sb.append(packageinfo.versionName);
			return sb.toString();
		} catch (NameNotFoundException e) {
			Log.e(TAG, "can't retrieve package version");
			return "Unknown package v0.00";
		}
	}

	private void setHtml(TextView tv, String in) {

		if (in.contains("{pkg-info}")) {
			in = in.replaceAll("\\{pkg-info\\}", getPkgInfo());
		}

		in = TextUtils.htmlEncode(in).replace("\n", "<br>");

		// match markdown-formatted links: [link text](http://foo.bar.com)
		// replace with: <a href="http://foo.bar.com">link text</a>
		StringBuilder out = new StringBuilder();
		Pattern p = Pattern.compile("\\[(.+?)\\]\\((\\S+?)\\)");
		Matcher m;

		while (true) {
			m = p.matcher(in);
			if (!m.find()) {
				break;
			}
			out.append(in.substring(0, m.start()));
			out.append("<a href=\"" + m.group(2) + "\">");
			out.append(m.group(1));
			out.append("</a>");
			in = in.substring(m.end());
		}

		out.append(in);
		tv.setText(Html.fromHtml(out.toString()));
		tv.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private class KvPair implements Comparable<KvPair> {
		public String key;
		public String value;

		public KvPair(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public int compareTo(KvPair another) {
			return key.compareTo(another.key);
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Activity activity = getActivity();

		LinearLayout ll = new LinearLayout(activity);
		ll.setOrientation(LinearLayout.VERTICAL);

		ArrayList<KvPair> text = new ArrayList<KvPair>();

		// fetch all R.string.<tag>_* strings, where <tag> comes from the android:tag element (xml layout)
		try {
			String regex = getTag() + "_\\d+.*";
			String titleName = getTag() + "_title";

			Class<?> res = R.string.class;
			for (Field f : res.getDeclaredFields()) {
				String key = f.getName();
				if (key.equals(titleName)) {
					activity.setTitle(f.getInt(null));
				} else if (key.matches(regex)) {
					text.add(new KvPair(key, getString(f.getInt(null))));
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "error finding markup text", e);
			return ll;
		}

		Collections.sort(text);

		for (KvPair kv : text) {
			TextView tv = new TextView(activity);
			setHtml(tv, kv.value);
			ll.addView(tv);
		}

		return ll;
	}

}
