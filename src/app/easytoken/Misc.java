/*
 * Misc: helpful utility functions
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class Misc {

	public static final String TAG = "EasyToken";

	private static final int BUFLEN = 65536;

	private static String readAndClose(Reader reader)
			throws UnsupportedEncodingException, IOException {
		StringWriter sw = new StringWriter();
    	char[] buffer = new char[BUFLEN];

    	while (true) {
    		int len = reader.read(buffer);
    		if (len == -1) {
    			break;
    		}
    		sw.write(buffer, 0, len);
    	}
		reader.close();
    	return sw.toString();
	}

	public static String readStringFromFile(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			return readAndClose(reader);
		} catch (IOException e) {
			Log.e(TAG, "Misc: readStringFromFile exception", e);
		}
		return null;
	}

	public static String readStringFromUri(Context context, Uri uri) {
		try {
			InputStream fs = context.getContentResolver().openInputStream(uri);
			byte buffer[] = new byte[BUFLEN];

			try {
				int len = fs.read(buffer);
				byte newbuf[] = new byte[len];
				System.arraycopy(buffer, 0, newbuf, 0, len);
				return new String(newbuf);
			} finally {
				fs.close();
			}
		} catch (Exception e) {
			Log.e(TAG, "error reading from content provider", e);
		}
		return null;
	}
}
