/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.utils;

import com.spokenpic.App;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

public class LazyProgressDialog extends ProgressDialog {
	Activity mActivity;
	public LazyProgressDialog(Activity activity) {
		super(activity);
		mActivity = activity;
	}
	public static LazyProgressDialog show (Activity context, CharSequence title, CharSequence message, boolean indeterminate) {
		return show(context, title, message, indeterminate, false, null);
	}

	public static LazyProgressDialog show (Activity context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable) {
		return show(context, title, message, indeterminate, cancelable, null);
	}

	public static LazyProgressDialog show (Activity context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable, DialogInterface.OnCancelListener cancelListener) {
		LazyProgressDialog d = new LazyProgressDialog(context);
		d.setTitle(title);
		d.setMessage(message);
		d.setIndeterminate(indeterminate);
		d.setCancelable(cancelable);
		if (cancelListener != null) d.setOnCancelListener(cancelListener);
		d.show();
		return d;
	}
	
	
	public void onStart() {
		int orientation = App.getInstance().getResources().getConfiguration().orientation;
		if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if(orientation == Configuration.ORIENTATION_PORTRAIT) {
			mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		}
	}

	protected void onStop() {
		mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}
	
	@Override
	public void dismiss() {
		try {
			super.dismiss();
		} catch (Exception e) {}
	}
}
