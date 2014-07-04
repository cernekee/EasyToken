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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends Activity
		implements GettingStartedFragment.OnImportButtonClickedListener {

	public static final String TAG = "EasyToken";

	private static final int REQ_IMPORT = 1;

	private void setupFragment() {
		Fragment frag;
		View divider = findViewById(R.id.divider);
		View frag_1 = findViewById(R.id.frag_1);

		TokenInfo info = TokenInfo.getDefaultToken();
		if (info != null) {
			Bundle args = new Bundle();
			args.putInt(TokencodeFragment.EXTRA_ID, info.id);

			frag = new TokencodeFragment();
			frag.setArguments(args);

			divider.setVisibility(View.GONE);
			frag_1.setVisibility(View.GONE);
		} else {
			frag = new GettingStartedFragment();

			getFragmentManager().beginTransaction()
				.replace(R.id.frag_1, new DevidFragment())
				.commit();
			divider.setVisibility(View.VISIBLE);
			frag_1.setVisibility(View.VISIBLE);
		}

		getFragmentManager().beginTransaction()
			.replace(R.id.frag_0, frag)
			.commit();
	}

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);

		setContentView(R.layout.activity_main);
		if (b == null) {
			setupFragment();
		}
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
			onImportButtonClicked();
			return true;
		case R.id.action_settings:
			BareActivity.startWithFrag(this, SettingsFragment.class);
			return true;
		case R.id.action_help:
			BareActivity.startWithLayout(this, R.layout.activity_help);
			return true;
		case R.id.action_about:
			BareActivity.startWithLayout(this, R.layout.activity_about);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onImportButtonClicked() {
		// clicking either "Import new token" from the menu, or "Import token" from
		// GettingStartedFragment, will end up here
		startActivityForResult(new Intent(this, ImportActivity.class), REQ_IMPORT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (requestCode == REQ_IMPORT) {
			// (re)create TokencodeFragment if a new token was imported
			setupFragment();
		}
	}
}
