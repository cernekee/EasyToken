/*
 * ImportActivity: coordinates importing a new token
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

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import de.blinkt.openvpn.FileSelect;

public class ImportActivity extends Activity
		implements ImportMethodFragment.OnImportMethodSelectedListener {

	public static final String TAG = "EasyToken";

	private static final String PFX = "app.easytoken.";

	private static final int STEP_NONE = 0;
	private static final int STEP_METHOD = 1;
	private static final int STEP_URI_INSTRUCTIONS = 2;
	private static final int STEP_IMPORT_TOKEN = 3;

	private static final int REQ_SCAN_QR = IntentIntegrator.REQUEST_CODE;
	private static final int REQ_PICK_FILE = REQ_SCAN_QR + 1;

	private AlertDialog mDialog;

	/* these get saved if the Activity is destroyed and re-created */
	private int mStep;
	private String mInputMethod;
	private String mUri;

	private static final String STATE_STEP = PFX + "step";
	private static final String STATE_INPUT_METHOD = PFX + "input_method";
	private static final String STATE_URI = PFX + "uri";

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);

		if (b != null) {
			mStep = b.getInt(STATE_STEP);
			mInputMethod = b.getString(STATE_INPUT_METHOD);
			mUri = b.getString(STATE_URI);
		} else {
			Intent i = this.getIntent();
			if (i != null) {
				Uri uri = i.getData();
				if (uri != null) {
					mUri = uri.toString();
				}
			}

			handleImportStep();
		}
	}

	@Override
	protected void onPause() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle b) {
		super.onSaveInstanceState(b);
		b.putInt(STATE_STEP, mStep);
		b.putString(STATE_INPUT_METHOD, mInputMethod);
		b.putString(STATE_URI, mUri);
	}

	private void showFrag(Fragment f, boolean animate) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();

		if (animate) {
			ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit);
		}

		ft.replace(android.R.id.content, f).commit();
	}

	private void handleImportStep() {
		Fragment f;
		boolean animate = true;

		if (mStep == STEP_NONE) {
			/* initial entry */
			mStep = (mUri == null) ? STEP_METHOD : STEP_IMPORT_TOKEN;
			animate = false;
		}

		if (mStep == STEP_METHOD) {
			showFrag(new ImportMethodFragment(), animate);
		} else if (mStep == STEP_URI_INSTRUCTIONS) {
			Bundle b = new Bundle();
			b.putString(ImportInstructionsFragment.ARG_INPUT_METHOD, mInputMethod);

			f = new ImportInstructionsFragment();
			f.setArguments(b);
			showFrag(f, animate);
		} else if (mStep == STEP_IMPORT_TOKEN) {
			// FIXME: parse URI and figure out what to do next
			android.util.Log.d(TAG, "XXX importing URI " + mUri);
		}
	}

	@Override
	public void onImportMethodSelected(String method) {
		if (method.equals("uri")) {
			mStep = STEP_URI_INSTRUCTIONS;
			mInputMethod = method;
			handleImportStep();
		} else if (method.equals("qr")) {
			ArrayList<String> formats = new ArrayList<String>();
			formats.add("QR_CODE");

			IntentIntegrator ii = new IntentIntegrator(this);
			ii.setTitleByID(R.string.qr_prompt_title);
			ii.setMessageByID(R.string.qr_prompt_message);
			ii.setButtonYesByID(R.string.yes);
			ii.setButtonNoByID(R.string.no);

			mDialog = ii.initiateScan(formats);
		} else if (method.equals("browse")) {
			Intent i = new Intent(this, FileSelect.class);
			i.putExtra(FileSelect.START_DATA, Environment.getExternalStorageDirectory().getPath());
			i.putExtra(FileSelect.NO_INLINE_SELECTION, true);
			startActivityForResult(i, REQ_PICK_FILE);
		}
	}

	private void tryImport(String s) {
		mStep = STEP_IMPORT_TOKEN;
		mUri = s;
		handleImportStep();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (requestCode == REQ_SCAN_QR) {
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
			if (scanResult != null) {
				tryImport(scanResult.getContents());
			}
		} else if (requestCode == REQ_PICK_FILE) {
			if (resultCode == Activity.RESULT_OK) {
				String path = intent.getStringExtra(FileSelect.RESULT_DATA);
				tryImport(Uri.fromFile(new File(path)).toString());
			}
		}
	}
}
