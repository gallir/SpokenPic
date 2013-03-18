/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class ViewWeb extends SherlockActivity {
	WebView mWebView;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web_profile);
		//App.getInstance().loadPreferences();
		getSupportActionBar().setHomeButtonEnabled(true);

		mWebView = (WebView) findViewById(R.id.web_profile);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new WebProfileClient());
		
		Intent intent = getIntent();
		Uri uri = intent.getData();

		String title = null;
		if (intent.getExtras() != null) {
			title = intent.getExtras().getString("title");
		}

		if (title == null) {
			title = "Web";
		}
		
		mWebView.loadUrl(uri.toString());
		
		setTitle(title);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
			// mWebView.goBack();
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	private class WebProfileClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in action bar clicked; go home
			Intent intent = new Intent(this, ViewMain.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
			//case R.id.item_share:

		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
