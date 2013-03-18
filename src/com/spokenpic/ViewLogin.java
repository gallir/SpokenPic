/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;
import com.spokenpic.net.JsonData.LoginResult;
import com.spokenpic.net.JsonData.RegisterData;
import com.spokenpic.net.JsonData.RestResult;
import com.spokenpic.net.RestClient;
import com.spokenpic.net.RestClient.RestClientListener;
import com.spokenpic.utils.LazyProgressDialog;

public class ViewLogin extends SherlockActivity implements RestClientListener {
	private ProgressDialog pd = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_login2);
		//App.getInstance().loadPreferences();
		getSupportActionBar().setHomeButtonEnabled(true);

		final EditText usernameText = (EditText) findViewById(R.id.login_username);
		final EditText passwordText = (EditText) findViewById(R.id.login_password);
		final Button loginOk = (Button) findViewById(R.id.login_ok);

		if(App.getInstance().getUsername() != null) {
			usernameText.setText(App.getInstance().getUsername());
		}

		loginOk.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				pd = LazyProgressDialog.show(ViewLogin.this, null, getString(R.string.connecting), true, true);
				RegisterData data = new RegisterData();
				data.username = usernameText.getText().toString();
				data.password = passwordText.getText().toString();

				RestClient client = new RestClient("/api/v1/login/", data.toString());
				client.useSSL(true);
				client.backgroundConnect(ViewLogin.this);

			}
		});

	}

	public void onRestFinished(RestResult result) {
		if (pd != null) pd.dismiss();
		if (result != null) {
			if (result.ok) {
				//Log.d("Login", result.payload);
				LoginResult data = new Gson().fromJson(result.payload, LoginResult.class);
				App.getInstance().setUsername(data.username);
				App.getInstance().setKey(data.key);
				App.getInstance().setPk(data.pk);
				App.getInstance().store();
				finish();
			} else {
				Toast.makeText(this, getString(R.string.authentication_error), Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_LONG).show();
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
