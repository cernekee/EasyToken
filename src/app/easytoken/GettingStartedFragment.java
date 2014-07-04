/*
 * GettingStartedFragment: provides users with introductory text + directions
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
import android.view.ViewGroup;
import android.widget.Button;

public class GettingStartedFragment extends Fragment {

	public static final String TAG = "EasyToken";

	public interface OnImportButtonClickedListener {
		public void onImportButtonClicked();
	};

	private void setupButtons(View v) {

		final Activity act = getActivity();

		Button button = (Button)v.findViewById(R.id.import_button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OnImportButtonClickedListener callback = (OnImportButtonClickedListener)act;
				callback.onImportButtonClicked();
			}
		});

		button = (Button)v.findViewById(R.id.help_button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BareActivity.startWithLayout(act, R.layout.activity_help);
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_getting_started, container, false);

		setupButtons(v);
		return v;
	}
}
