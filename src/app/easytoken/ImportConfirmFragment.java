/*
 * ImportConfirmFragment: confirm that the user wants to overwrite current token
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

import org.stoken.LibStoken;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class ImportConfirmFragment extends Fragment {

	public static final String PFX = "app.easytoken.";
	public static final String ARG_OLD_TOKEN = PFX + "old_token";
	public static final String ARG_NEW_TOKEN = PFX + "new_token";

	private OnConfirmDoneListener mListener;

	public interface OnConfirmDoneListener {
		public void onConfirmDone(boolean accepted);
	}

	private void populateTokenInfo(View v, String s, int labelRes, int snRes, int expRes) {
		LibStoken lib = new LibStoken();
		String sn = "ERROR", exp = "ERROR";

		if (lib.importString(s) == LibStoken.SUCCESS &&
				lib.decryptSeed(null, null) == LibStoken.SUCCESS) {

			LibStoken.StokenInfo info = lib.getInfo();
			sn = this.getString(labelRes) + " " + info.serial;

			DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
			exp = this.getString(R.string.exp_date) + " " + df.format(info.unixExpDate * 1000);
		}
		lib.destroy();

		TextView tv = (TextView)v.findViewById(snRes);
		tv.setText(sn);

		tv = (TextView)v.findViewById(expRes);
		tv.setText(exp);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
		View v = inflater.inflate(R.layout.fragment_import_confirm, container, false);

		Bundle args = getArguments();

		populateTokenInfo(v, args.getString(ARG_OLD_TOKEN), R.string.current_token,
				R.id.old_token_sn, R.id.old_token_exp_date);
		populateTokenInfo(v, args.getString(ARG_NEW_TOKEN), R.string.replacement_token,
				R.id.new_token_sn, R.id.new_token_exp_date);

        Button button = (Button)v.findViewById(R.id.yes_button);
        button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mListener.onConfirmDone(true);
			}
        });

        button = (Button)v.findViewById(R.id.no_button);
        button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mListener.onConfirmDone(false);
			}
        });

		return v;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mListener = (OnConfirmDoneListener)activity;
	}
}
