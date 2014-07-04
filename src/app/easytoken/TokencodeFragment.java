/*
 * TokencodeFragment: main screen showing current tokencode and token metadata
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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.stoken.LibStoken;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class TokencodeFragment extends Fragment
		implements TokencodeBackend.OnTokencodeUpdateListener {
	public static final String TAG = "EasyToken";

	private static final String PFX = "app.easytoken.";
	public static final String EXTRA_ID = PFX + "ID";

	private String mPin;
	private boolean mPinRequested;

	private static final String STATE_PIN = PFX + "mPin";
	private static final String STATE_PIN_REQUESTED = PFX + "mPinRequested";

	private TextView mTokencode;
	private String mRawTokencode = "";
	private ProgressBar mProgressBar;
	private View mView;

	private boolean mNeedsPin = false;
	private AlertDialog mDialog;
	private boolean mSuspendTokencode;

	private TokencodeBackend mBackend;

    private void writeStatusField(int id, int header_res, String value, boolean warn) {
    	String html = "<b>" + TextUtils.htmlEncode(getString(header_res)) + "</b><br>";
    	value = TextUtils.htmlEncode(value);
    	if (warn) {
    		/*
    		 * No CSS.  See:
    		 * http://commonsware.com/blog/Android/2010/05/26/html-tags-supported-by-textview.html
    		 */
    		html += "<font color=\"red\"><b>" + value + "</b></font>";
    	} else {
    		html += value;
    	}
    	TextView tv = (TextView)mView.findViewById(id);
    	tv.setText(Html.fromHtml(html));
    }

    private void writeStatusField(int id, int header_res, String value) {
    	writeStatusField(id, header_res, value, false);
    }

	private void populateView(View v) {
		mTokencode = (TextView)v.findViewById(R.id.tokencode);
		mProgressBar = (ProgressBar)v.findViewById(R.id.progress_bar);
		mView = v;

		Button copyButton = (Button)v.findViewById(R.id.copy_button);
		copyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Activity act = getActivity();
				ClipboardManager clipboard = (ClipboardManager)
						act.getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("Tokencode", mRawTokencode);
				clipboard.setPrimaryClip(clip);
				Toast.makeText(act.getBaseContext(), R.string.copied_entry, Toast.LENGTH_SHORT).show();
			}
		});

		Button pinButton = (Button)v.findViewById(R.id.change_pin_button);
		pinButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDialog = changePinDialog();
			}
		});

		/* static fields */
		LibStoken.StokenInfo info = mBackend.info.lib.getInfo();

		mNeedsPin = mBackend.info.pinRequired;
		pinButton.setEnabled(mNeedsPin);

		writeStatusField(R.id.token_sn, R.string.token_sn, info.serial);
		mProgressBar.setMax(info.interval - 1);

		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
		long exp = info.unixExpDate * 1000;

		/* show field in red if expiration is <= 2 weeks away */
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, 14);
		writeStatusField(R.id.exp_date, R.string.exp_date, df.format(exp), cal.getTimeInMillis() >= exp);
		writeStatusField(R.id.dev_id, R.string.dev_id, TokenInfo.getDeviceId());
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {

    	View v;

    	mBackend = new TokencodeBackend();
    	if (mBackend.init(getActivity(), this, getArguments().getInt(EXTRA_ID)) != true) {
    		Log.e(TAG, "Error initializing TokencodeFragment");
    	}

		v = inflater.inflate(R.layout.token_diag_info, container, false);
		// depends on mBackend
		populateView(v);

    	if (savedInstanceState != null) {
    		mPin = savedInstanceState.getString(STATE_PIN);
    		mPinRequested = savedInstanceState.getBoolean(STATE_PIN_REQUESTED);
    	}

		if (mPin == null && mNeedsPin) {
			if (!mBackend.info.pin.equals("")) {
				mPin = mBackend.info.pin;
			}
		}
		// depends on mView from populateView()
		setPin(mPin);

    	return v;
    }

    @Override
	public void onSaveInstanceState(Bundle b) {
    	b.putString(STATE_PIN, mPin);
    	b.putBoolean(STATE_PIN_REQUESTED, mPinRequested);
    }

    @Override
	public void onDestroy() {
    	mBackend.destroy();
    	super.onDestroy();
    }

    private void setPin(String s) {
    	int res;
    	boolean warn = false;

    	mPin = s;
		if (!mNeedsPin) {
			res = R.string.not_required;
		} else if (s == null) {
			mPin = null;
			mBackend.info.pin = "";
			mBackend.info.save();
			warn = true;
			res = R.string.no;
		} else {
			mBackend.info.pin = s;
			mBackend.info.save();
			res = R.string.yes;
		}

		writeStatusField(R.id.using_pin, R.string.using_pin, getString(res), warn);
    }

    private void finishPinDialog(String pin) {
    	mSuspendTokencode = false;
    	setPin(pin);
    	mPinRequested = true;
    	mBackend.updateNow();
    }

    private void setupTextWatcher(AlertDialog d, final TextView tv) {
    	/* gray out the OK button until a sane-looking PIN is entered */
		final Button okButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
		okButton.setEnabled(false);

		tv.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {
				String s = tv.getText().toString();
				okButton.setEnabled(mBackend.info.lib.checkPIN(s));
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
    }

    private AlertDialog enterPinDialog() {
    	Context ctx = getActivity();
    	AlertDialog d;

    	final TextView tv = new EditText(ctx);
		tv.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		tv.setTransformationMethod(PasswordTransformationMethod.getInstance());

    	AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
    		.setView(tv)
    		.setTitle(R.string.enter_pin)
    		.setMessage(R.string.enter_pin_message)
    		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					finishPinDialog(tv.getText().toString().trim());
					mDialog = null;
				}
    		})
    		.setNegativeButton(R.string.no_pin, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					finishPinDialog(null);
					mDialog = null;
				}
    		});
    	d = builder.create();
    	d.setCancelable(false);
    	d.show();
    	setupTextWatcher(d, tv);

		return d;
    }

    private AlertDialog changePinDialog() {
    	Context ctx = getActivity();
    	AlertDialog d;

    	final TextView tv = new EditText(ctx);
		tv.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		tv.setTransformationMethod(PasswordTransformationMethod.getInstance());

    	AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
    		.setView(tv)
    		.setTitle(R.string.new_pin)
    		.setMessage(R.string.new_pin_message)
    		.setPositiveButton(R.string.change, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					finishPinDialog(tv.getText().toString().trim());
				}
    		})
    		.setNeutralButton(R.string.no_pin, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					finishPinDialog(null);
				}
    		})
    		.setNegativeButton(R.string.cancel, null);
    	d = builder.create();
    	d.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				// back button -> cancel PIN change
				mDialog = null;
			}
    	});
    	d.show();
    	setupTextWatcher(d, tv);

		return d;
    }

    @Override
    public void onResume() {
    	super.onResume();

    	if (mNeedsPin && mPin == null && !mPinRequested) {
    		mDialog = enterPinDialog();
    		mSuspendTokencode = true;
    	}
    	mBackend.onResume();
    }

    @Override
    public void onPause() {
    	mBackend.onPause();
    	super.onPause();

    	if (mDialog != null) {
    		mDialog.dismiss();
    		mDialog = null;
    	}
    }

	@Override
	public void onTokencodeUpdate(String tokencode, String nextTokencode, int secondsLeft) {

		if (mSuspendTokencode) {
			// wait for user entry instead of displaying bogus data
			tokencode = nextTokencode = "";
			secondsLeft = 0;
		}

		mProgressBar.setProgress(secondsLeft - 1);
		mRawTokencode = tokencode;
		mTokencode.setText(TokencodeBackend.formatTokencode(mRawTokencode));
		writeStatusField(R.id.next_tokencode, R.string.next_tokencode,
				TokencodeBackend.formatTokencode(nextTokencode));

		Calendar now = Calendar.getInstance();
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		String gmt = df.format(now.getTime()).replaceAll(" GMT.*", "");
		writeStatusField(R.id.gmt, R.string.gmt, gmt);
	}
}
