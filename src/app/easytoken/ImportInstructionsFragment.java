/*
 * ImportInstructionsFragment: display instructions/errors to the user
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class ImportInstructionsFragment extends Fragment {

	public static final String TAG = "EasyToken";

	public static final String PFX = "app.easytoken.";
	public static final String ARG_INPUT_METHOD = PFX + "input_method";

	private void setHtml(TextView tv, int resId) {

		String in = getString(resId);
		in = TextUtils.htmlEncode(in).replace("\n", "<br>");

		// match markdown-formatted links: [link text](http://foo.bar.com)
		// replace with: <a href="http://foo.bar.com">link text</a>
		StringBuilder out = new StringBuilder();
		Pattern p = Pattern.compile("\\[(.+?)\\]\\((\\S+?)\\)");
		Matcher m;

		while (true) {
			m = p.matcher(in);
			if (!m.find()) {
				break;
			}
			out.append(in.substring(0, m.start()));
			out.append("<a href=\"" + m.group(2) + "\">");
			out.append(m.group(1));
			out.append("</a>");
			in = in.substring(m.end());
		}

		out.append(in);
		tv.setText(Html.fromHtml(out.toString()));
		tv.setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_import_instructions_uri, container, false);

		Bundle b = getArguments();
		TextView tv = (TextView)v.findViewById(R.id.import_help);

		String method = b.getString(ARG_INPUT_METHOD);
		if (method.equals("uri")) {
			setHtml(tv, R.string.import_uri_help);

			tv = (TextView)v.findViewById(R.id.import_examples);
			tv.setHorizontallyScrolling(true);
			tv.setMovementMethod(new ScrollingMovementMethod());
			setHtml(tv, R.string.import_uri_examples);
		}

        Button button = (Button)v.findViewById(R.id.ok_button);
        button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getActivity().finish();
			}
        });

		return v;
	}
}
