/*
 * ImportMethodFragment: choose where to import the token from
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
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class ImportMethodFragment extends ListFragment {

	public static final String TAG = "EasyToken";

	private static final String PFX = "app.easytoken.";

	private OnImportMethodSelectedListener mListener;
	private String mKeys[];

	private static final int NO_SELECTION = -1;

	private Button mButton;
	private int mPos = NO_SELECTION;

	private static final String STATE_POS = PFX + "pos";

	public interface OnImportMethodSelectedListener {
		public void onImportMethodSelected(String method);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
		if (b != null) {
			mPos = b.getInt(STATE_POS);
		}

		View v = inflater.inflate(R.layout.fragment_import_method, container, false);
		return v;
	}

	@Override
	public void onSaveInstanceState(Bundle b) {
		super.onSaveInstanceState(b);
		b.putInt(STATE_POS, mPos);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mListener = (OnImportMethodSelectedListener)activity;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		mPos = position;
		mButton.setEnabled(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mKeys = getResources().getStringArray(R.array.input_methods_keys);
		String[] names = getResources().getStringArray(R.array.input_methods);
        setListAdapter(new ArrayAdapter<String>(getActivity(),
                       android.R.layout.simple_list_item_single_choice,
                       names));

        mButton = (Button)view.findViewById(R.id.next_button);
        ListView lv = getListView();

        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        if (mPos != NO_SELECTION) {
    		lv.setItemChecked(mPos, true);
    		mButton.setEnabled(true);
        } else {
        	mButton.setEnabled(false);
        }

        mButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mPos != NO_SELECTION) {
					mListener.onImportMethodSelected(mKeys[mPos]);
				}
			}
        });
	}
}
