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

import org.stoken.LibStoken;

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
		implements ImportMethodFragment.OnImportMethodSelectedListener,
		           ImportManualEntryFragment.OnManualEntryDoneListener {

	public static final String TAG = "EasyToken";

	private static final String PFX = "app.easytoken.";

	private static final int STEP_NONE = 0;
	private static final int STEP_METHOD = 1;
	private static final int STEP_URI_INSTRUCTIONS = 2;
	private static final int STEP_IMPORT_TOKEN = 3;
	private static final int STEP_MANUAL_ENTRY = 4;
	private static final int STEP_ERROR = 5;

	private static final int REQ_SCAN_QR = IntentIntegrator.REQUEST_CODE;
	private static final int REQ_PICK_FILE = REQ_SCAN_QR + 1;

	private AlertDialog mDialog;

	/* these get saved if the Activity is destroyed and re-created */
	private int mStep;
	private String mInputMethod;
	private String mUri;
	private String mErrorType;
	private String mErrorData;

	private static final String STATE_STEP = PFX + "step";
	private static final String STATE_INPUT_METHOD = PFX + "input_method";
	private static final String STATE_URI = PFX + "uri";
	private static final String STATE_ERROR_TYPE = PFX + "error_type";
	private static final String STATE_ERROR_DATA = PFX + "error_data";

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);

		if (b != null) {
			mStep = b.getInt(STATE_STEP);
			mInputMethod = b.getString(STATE_INPUT_METHOD);
			mUri = b.getString(STATE_URI);
			mErrorType = b.getString(STATE_ERROR_TYPE);
			mErrorData = b.getString(STATE_ERROR_DATA);
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
		b.putString(STATE_ERROR_TYPE, mErrorType);
		b.putString(STATE_ERROR_DATA, mErrorData);
	}

	private void showFrag(Fragment f, boolean animate) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();

		if (animate) {
			ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit);
		}

		ft.replace(android.R.id.content, f).commit();
	}

	private void importToken(String data) {
		Uri uri = Uri.parse(data);
		String path = uri.getPath();
		boolean isFile = false;

		if ("file".equals(uri.getScheme()) && path != null) {
			/*
			 * Arguably we shouldn't take file:// URIs from QR codes,
			 * and maybe we should be more careful about what we accept
			 * from other apps too
			 */
			isFile = true;
			data = Misc.readStringFromFile(path);
			if (data == null) {
				mStep = STEP_ERROR;
				mErrorType = ImportInstructionsFragment.INST_FILE_ERROR;
				mErrorData = path;
				handleImportStep();
				return;
			}
		}

		LibStoken lib = new LibStoken();
		if (lib.importString(data) != LibStoken.SUCCESS) {
			mStep = STEP_ERROR;
			mErrorType = ImportInstructionsFragment.INST_BAD_TOKEN;
			mErrorData = isFile ? "" : data;
			handleImportStep();
			lib.destroy();
			return;
		}

		/* FIXME: figure out whether to prompt for devid/password/PIN */

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
			b.putString(ImportInstructionsFragment.ARG_INST_TYPE,
					    ImportInstructionsFragment.INST_URI_HELP);

			f = new ImportInstructionsFragment();
			f.setArguments(b);
			showFrag(f, animate);
		} else if (mStep == STEP_MANUAL_ENTRY) {
			showFrag(new ImportManualEntryFragment(), animate);
		} else if (mStep == STEP_IMPORT_TOKEN) {
			importToken(mUri);
		} else if (mStep == STEP_ERROR) {
			Bundle b = new Bundle();
			b.putString(ImportInstructionsFragment.ARG_INST_TYPE, mErrorType);
			b.putString(ImportInstructionsFragment.ARG_TOKEN_DATA, mErrorData);

			f = new ImportInstructionsFragment();
			f.setArguments(b);
			showFrag(f, animate);
		}
	}

	@Override
	public void onImportMethodSelected(String method) {
		mInputMethod = method;

		if (method.equals("uri")) {
			mStep = STEP_URI_INSTRUCTIONS;
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
		} else if (method.equals("manual")) {
			mStep = STEP_MANUAL_ENTRY;
			handleImportStep();
		}
	}

	private void tryImport(String s) {
		mStep = STEP_IMPORT_TOKEN;
		mUri = s;
		handleImportStep();
	}

	@Override
	public void onManualEntryDone(String token) {
		tryImport(token);
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
