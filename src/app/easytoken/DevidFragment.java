/*
 * DevidFragment: Shows the device ID (for binding); allows copying/emailing it
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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DevidFragment extends Fragment {

	private void setupButtons(View v) {

		final Activity act = getActivity();
		final String deviceId = TokenInfo.getDeviceId();

		Button button = (Button)v.findViewById(R.id.copy_button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ClipboardManager clipboard = (ClipboardManager)
						act.getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("device_id", deviceId);
				clipboard.setPrimaryClip(clip);
				Toast.makeText(act.getBaseContext(), R.string.copied_entry, Toast.LENGTH_SHORT).show();
			}
		});

		button = (Button)v.findViewById(R.id.email_button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_SEND);

				i.putExtra(Intent.EXTRA_SUBJECT, "My SecurID device ID is: " + deviceId);
				i.setType("plain/text");
				try {
					startActivity(i);
				} catch (Exception e) {
					/* there might not be a handler installed for this data type */
				}
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_devid, container, false);

		TextView tv = (TextView)v.findViewById(R.id.device_id);
		tv.setText(TokenInfo.getDeviceId());

		setupButtons(v);
		return v;
	}
}
