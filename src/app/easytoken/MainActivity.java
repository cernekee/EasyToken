/*
 * MainActivity: displays GettingStartedFragment and TokencodeFragment
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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

	public static final String TAG = "EasyToken";

	private Fragment mFrag;

	@Override
	protected void onResume() {
		super.onResume();

		Log.i(TAG, "MainActivity: onResume()");

		TokenInfo info = TokenInfo.getDefaultToken();

		if (info != null) {
			mFrag = new TokencodeFragment();
			Bundle b = new Bundle();
			b.putInt(TokencodeFragment.EXTRA_ID, info.id);
			mFrag.setArguments(b);
		} else {
			mFrag = new GettingStartedFragment();
		}

		getFragmentManager().beginTransaction()
			.replace(android.R.id.content, mFrag)
			.commit();
	}

	@Override
	protected void onPause() {
		if (mFrag != null) {
			getFragmentManager().beginTransaction()
				.remove(mFrag)
				.commit();
			mFrag = null;
		}
		super.onPause();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_import:
			startActivity(new Intent(this, ImportActivity.class));
			return true;
		case R.id.action_help:
			FragActivity.start(this, HelpFragment.class);
			return true;
		case R.id.action_about:
			FragActivity.start(this, AboutFragment.class);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
