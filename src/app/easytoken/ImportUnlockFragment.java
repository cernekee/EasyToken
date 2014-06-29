/*
 * ImportUnlockFragment: prompt for password/devid/PIN to unlock token
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ImportUnlockFragment extends Fragment {

	public static final String TAG = "EasyToken";

	public static final String PFX = "app.easytoken.";
	public static final String ARG_DEFAULT_DEVID = PFX + "default_devid";
	public static final String ARG_REQUEST_PASS = PFX + "request_pass";
	public static final String ARG_REQUEST_DEVID = PFX + "request_devid";
	public static final String ARG_REQUEST_PIN = PFX + "request_pin";

	private OnUnlockDoneListener mListener;

	private EditText mPassEntry;
	private EditText mDevidEntry;
	private EditText mPinEntry;

	private Bundle mArgs;

	public interface OnUnlockDoneListener {
		public void onUnlockDone(String pass, String devid, String pin);
	}

	private EditText setupField(View v, String argName, int labelRes, int fieldRes) {
		TextView label = (TextView)v.findViewById(labelRes);
		EditText entry = (EditText)v.findViewById(fieldRes);

		if (mArgs.getBoolean(argName)) {
			label.setVisibility(View.VISIBLE);
			entry.setVisibility(View.VISIBLE);
			return entry;
		} else {
			label.setVisibility(View.GONE);
			entry.setVisibility(View.GONE);
			return null;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
		View v = inflater.inflate(R.layout.fragment_import_unlock, container, false);

		mArgs = getArguments();
		mPassEntry = setupField(v, ARG_REQUEST_PASS, R.id.pass_label, R.id.pass_entry);
		mDevidEntry = setupField(v, ARG_REQUEST_DEVID, R.id.devid_label, R.id.devid_entry);
		mPinEntry = setupField(v, ARG_REQUEST_PIN, R.id.pin_label, R.id.pin_entry);

		if (b == null && mDevidEntry != null) {
			mDevidEntry.setText(mArgs.getString(ARG_DEFAULT_DEVID));
		}

		return v;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mListener = (OnUnlockDoneListener)activity;
	}

	private void submitEntries() {
		String pass = null, devid = null, pin = null;

		if (mPassEntry != null) {
			pass = mPassEntry.getText().toString().trim();
		}
		if (mDevidEntry != null) {
			devid = mDevidEntry.getText().toString().trim();
		}
		if (mPinEntry != null) {
			pin = mPinEntry.getText().toString().trim();
		}

		mListener.onUnlockDone(pass, devid, pin);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        Button b = (Button)view.findViewById(R.id.next_button);
        b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				submitEntries();
			}
        });
	}
}
