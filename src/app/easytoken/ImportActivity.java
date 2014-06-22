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

import org.stoken.LibStoken;

import android.app.Activity;
import android.widget.Toast;

public class ImportActivity extends Activity {

	public static final String TAG = "EasyToken";

	@Override
	public void onStart() {
		super.onStart();

		TokenInfo info = TokenInfo.getDefaultToken();
		if (info != null) {
			info.delete();
			Toast.makeText(this, "Deleted token", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		LibStoken lib = new LibStoken();
		String randomToken =
				"283776995409375474030211576322767603760377747264244343664173125074574542716723244";
		if (lib.importString(randomToken) != LibStoken.SUCCESS) {
			throw new RuntimeException("importString failed");
		}
		if (lib.decryptSeed(null, null) != LibStoken.SUCCESS) {
			throw new RuntimeException("decryptSeed failed");
		}

		info = new TokenInfo(lib, null);
		info.save();
		Toast.makeText(this, "Created random token", Toast.LENGTH_SHORT).show();

		finish();
	}
}
