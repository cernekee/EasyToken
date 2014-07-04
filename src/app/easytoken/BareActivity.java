/*
 * BareActivity: displays a simple layout (probably including one or more fragments)
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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class BareActivity extends Activity {

	public static final String TAG = "EasyToken";

	public static final String EXTRA_LAYOUT_ID = "app.easytoken.layout_id";
	public static final String EXTRA_FRAGMENT_NAME = "app.easytoken.fragment_name";

	public static void startWithLayout(Context ctx, int layoutId) {
		Intent i = new Intent(ctx, BareActivity.class);
		i.putExtra(EXTRA_LAYOUT_ID, layoutId);
		ctx.startActivity(i);
	}

	public static void startWithFrag(Context ctx, Class<?> frag) {
		Intent i = new Intent(ctx, BareActivity.class);
		i.putExtra(EXTRA_FRAGMENT_NAME, frag.getName());
		ctx.startActivity(i);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int layoutId = getIntent().getIntExtra(EXTRA_LAYOUT_ID, -1);
		if (layoutId != -1) {
			setContentView(layoutId);
		}
		
		if(savedInstanceState == null) {
			try {
				String fragName = getIntent().getStringExtra(EXTRA_FRAGMENT_NAME);
				if (fragName != null) {
					Fragment frag = (Fragment)Class.forName(fragName).newInstance();
					getFragmentManager().beginTransaction().add(android.R.id.content, frag).commit();
				}
			} catch (Exception e) {
				Log.e(TAG, "unable to create fragment", e);
				finish();
			}
		}
    }
}
