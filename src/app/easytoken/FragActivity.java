/*
 * FragActivity: displays a single fragment
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

public class FragActivity extends Activity {

	public static final String TAG = "EasyToken";

	public static final String EXTRA_FRAGMENT_NAME = "app.easytoken.fragment_name";

	public static void start(Context ctx, Class<?> frag) {
		Intent i = new Intent(ctx, FragActivity.class);
		i.putExtra(EXTRA_FRAGMENT_NAME, frag.getName());
		ctx.startActivity(i);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState == null) {
			try {
				String fragName = getIntent().getStringExtra(EXTRA_FRAGMENT_NAME);
				Fragment frag = (Fragment)Class.forName(fragName).newInstance();
				getFragmentManager().beginTransaction().add(android.R.id.content, frag).commit();
			} catch (Exception e) {
				Log.e(TAG, "unable to create fragment", e);
				finish();
			}
		}
    }


}
