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
		           ImportManualEntryFragment.OnManualEntryDoneListener,
		           ImportUnlockFragment.OnUnlockDoneListener,
		           ImportConfirmFragment.OnConfirmDoneListener {

	public static final String TAG = "EasyToken";

	private static final String PFX = "app.easytoken.";

	private static final int STEP_NONE = 0;
	private static final int STEP_METHOD = 1;
	private static final int STEP_URI_INSTRUCTIONS = 2;
	private static final int STEP_IMPORT_TOKEN = 3;
	private static final int STEP_MANUAL_ENTRY = 4;
	private static final int STEP_ERROR = 5;
	private static final int STEP_UNLOCK_TOKEN = 6;
	private static final int STEP_CONFIRM_OVERWRITE = 7;
	private static final int STEP_DONE = 8;

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

	private void showError(String errCode, String errData) {
		mStep = STEP_ERROR;
		mErrorType = errCode;
		mErrorData = errData;
		handleImportStep();
	}

	private LibStoken importToken(String data, boolean decrypt) {
		Uri uri = Uri.parse(data);
		String path = uri.getPath();
		boolean isFile = false;

		if (path != null &&
			("file".equals(uri.getScheme()) || "content".equals(uri.getScheme()))) {
			/*
			 * Arguably we shouldn't take file:// URIs from QR codes,
			 * and maybe we should be more careful about what we accept
			 * from other apps too
			 */
			isFile = true;
			data = Misc.readStringFromUri(this, uri);
			if (data == null) {
				showError(ImportInstructionsFragment.INST_FILE_ERROR, path);
				return null;
			}
		}

		LibStoken lib = new LibStoken();
		if (lib.importString(data) != LibStoken.SUCCESS ||
				(decrypt && lib.decryptSeed(null, null) != LibStoken.SUCCESS)) {
			showError(ImportInstructionsFragment.INST_BAD_TOKEN, isFile ? "" : data);
			lib.destroy();
			return null;
		}

		return lib;
	}

	private void writeNewToken(LibStoken lib) {
		TokenInfo info;

		info = TokenInfo.getDefaultToken();
		if (info != null) {
			info.delete();
		}

		info = new TokenInfo(lib, null);
		info.save();
	}

	private void unlockDone(LibStoken lib) {
		mUri = lib.encryptSeed(null, null);

		if (TokenInfo.getDefaultToken() != null) {
			mStep = STEP_CONFIRM_OVERWRITE;
		} else {
			writeNewToken(lib);
			mStep = STEP_DONE;
		}
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
			LibStoken lib = importToken(mUri, false);
			if (lib == null) {
				/* mStep has already been advanced to an error state */
				return;
			}
			if (lib.isDevIDRequired() || lib.isPassRequired()) {
				mStep = STEP_UNLOCK_TOKEN;
			} else {
				if (lib.decryptSeed(null, null) != LibStoken.SUCCESS) {
					showError(ImportInstructionsFragment.INST_BAD_TOKEN, mUri);
					return;
				}
				/* advances mStep to either STEP_DONE or STEP_CONFIRM_OVERWRITE */
				unlockDone(lib);
			}
			lib.destroy();
			handleImportStep();
		} else if (mStep == STEP_ERROR) {
			Bundle b = new Bundle();
			b.putString(ImportInstructionsFragment.ARG_INST_TYPE, mErrorType);
			b.putString(ImportInstructionsFragment.ARG_TOKEN_DATA, mErrorData);

			f = new ImportInstructionsFragment();
			f.setArguments(b);
			showFrag(f, animate);
		} else if (mStep == STEP_UNLOCK_TOKEN) {
			LibStoken lib = importToken(mUri, false);
			if (lib == null) {
				/* mStep has already been advanced to an error state */
				return;
			}
			Bundle b = new Bundle();
			b.putString(ImportUnlockFragment.ARG_DEFAULT_DEVID, TokenInfo.getDeviceId());
			b.putBoolean(ImportUnlockFragment.ARG_REQUEST_PASS, lib.isPassRequired());
			b.putBoolean(ImportUnlockFragment.ARG_REQUEST_DEVID, lib.isDevIDRequired());

			/*
			 * NOTE: The PIN is not captured here.  isPINRequired() may return false if we're
			 * handling an encrypted v3 token, because the PIN flag is in the encrypted
			 * payload.
			 */
			b.putBoolean(ImportUnlockFragment.ARG_REQUEST_PIN, false);

			f = new ImportUnlockFragment();
			f.setArguments(b);
			showFrag(f, animate);

			lib.destroy();
		} else if (mStep == STEP_CONFIRM_OVERWRITE) {
			LibStoken lib = importToken(mUri, true);
			if (lib == null) {
				/* mStep has already been advanced to an error state */
				return;
			}

			Bundle b = new Bundle();
			b.putString(ImportConfirmFragment.ARG_NEW_TOKEN, mUri);
			b.putString(ImportConfirmFragment.ARG_OLD_TOKEN,
					TokenInfo.getDefaultToken().lib.encryptSeed(null, null));

			f = new ImportConfirmFragment();
			f.setArguments(b);
			showFrag(f, animate);

			lib.destroy();
		} else if (mStep == STEP_DONE) {
			finish();
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
			if (scanResult != null && scanResult.getContents() != null) {
				tryImport(scanResult.getContents());
			}
		} else if (requestCode == REQ_PICK_FILE) {
			if (resultCode == Activity.RESULT_OK) {
				String path = intent.getStringExtra(FileSelect.RESULT_DATA);
				tryImport(Uri.fromFile(new File(path)).toString());
			}
		}
	}

	private AlertDialog errorDialog(int titleRes, int messageRes) {
		final AlertDialog d;

		d = new AlertDialog.Builder(this)
			.setTitle(titleRes)
			.setMessage(messageRes)
			.setPositiveButton(R.string.ok, null)
			.show();
		return d;
	}

	@Override
	public void onUnlockDone(String pass, String devid, String pin) {
		LibStoken lib = importToken(mUri, false);
		if (lib == null) {
			/* mStep has already been advanced to an error state */
			return;
		}

		if (lib.decryptSeed(pass, devid) != LibStoken.SUCCESS) {
			int resId = R.string.general_failure;

			if (lib.isPassRequired() && lib.isDevIDRequired()) {
				resId = R.string.pass_devid_bad;
			} else if (lib.isPassRequired() && !lib.isDevIDRequired()) {
				resId = R.string.pass_bad;
			} else if (!lib.isPassRequired() && lib.isDevIDRequired()) {
				resId = R.string.devid_bad;
			}
			mDialog = errorDialog(R.string.unable_to_process_token, resId);
			lib.destroy();
			return;
		}

		unlockDone(lib);
		handleImportStep();
		lib.destroy();
	}

	@Override
	public void onConfirmDone(boolean accepted) {
		if (accepted) {
			LibStoken lib = importToken(mUri, true);
			if (lib == null) {
				/* mStep has already been advanced to an error state */
				return;
			}
			writeNewToken(lib);
		}
		mStep = STEP_DONE;
		handleImportStep();
	}
}
