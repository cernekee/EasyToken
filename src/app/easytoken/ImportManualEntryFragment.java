/*
 * ImportManualEntry: allows entering token strings through the onscreen keyboard
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

import org.stoken.LibStoken;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ImportManualEntryFragment extends Fragment {

	public static final String TAG = "EasyToken";

	private OnManualEntryDoneListener mListener;

	private String mToken;
	private LibStoken mLib;

	private Button mButton;
	private EditText mEntry;

	public interface OnManualEntryDoneListener {
		public void onManualEntryDone(String token);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
		View v = inflater.inflate(R.layout.fragment_import_manual_entry, container, false);
		return v;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mListener = (OnManualEntryDoneListener)activity;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		/* Enable horizontal scrolling. Kind of cheesy. */
		TextView tv = (TextView)view.findViewById(R.id.manual_examples);
		tv.setHorizontallyScrolling(true);
		tv.setMovementMethod(new ScrollingMovementMethod());

		tv = (TextView)view.findViewById(R.id.import_uri_examples);
		tv.setHorizontallyScrolling(true);
		tv.setMovementMethod(new ScrollingMovementMethod());

        mButton = (Button)view.findViewById(R.id.next_button);
        mButton.setEnabled(false);
        mButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mListener.onManualEntryDone(mToken);
			}
        });

        mEntry = (EditText)view.findViewById(R.id.token_entry);
        mEntry.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				updateNextButton();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
        });
	}

	private void updateNextButton() {
		if (mLib != null) {
			mToken = mEntry.getText().toString().trim();
			mButton.setEnabled(mLib.importString(mToken) == LibStoken.SUCCESS);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		mLib = new LibStoken();
		updateNextButton();
	}

	@Override
	public void onPause() {
		mLib.destroy();
		mLib = null;

		super.onPause();
	}
}
