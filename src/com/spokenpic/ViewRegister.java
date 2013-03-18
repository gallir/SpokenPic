/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;
import com.spokenpic.net.JsonData.RegisterData;
import com.spokenpic.net.JsonData.RegisterResult;
import com.spokenpic.net.JsonData.RestResult;
import com.spokenpic.net.RestClient;
import com.spokenpic.net.RestClient.RestClientListener;
import com.spokenpic.utils.LazyProgressDialog;



public class ViewRegister extends SherlockActivity implements RestClientListener {

	private ProgressDialog pd = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_register2);
		// App.getInstance().loadPreferences();
		
		getSupportActionBar().setHomeButtonEnabled(true);
		
		final EditText emailText = (EditText) findViewById(R.id.register_email);
		final EditText usernameText = (EditText) findViewById(R.id.register_username);
		final EditText passwordText = (EditText) findViewById(R.id.register_password);
		final EditText passwordText2 = (EditText) findViewById(R.id.register_password2);
		final Button registerOk = (Button) findViewById(R.id.register_ok);
		
		final TextWatcher passCheck = new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
		
			public void afterTextChanged(Editable s) {
				if (passwordText2.getText().length() > 0) {
					if (passwordText2.getText().toString().equals(passwordText.getText().toString())) {
						passwordText2.setTextColor(Color.GREEN);
					} else {
					passwordText2.setTextColor(Color.RED);
					}
				}
			}
		};

		passwordText.addTextChangedListener(passCheck);
		passwordText2.addTextChangedListener(passCheck);
		
		
		registerOk.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				pd = LazyProgressDialog.show(ViewRegister.this, null, getString(R.string.connecting), true, true);
				RegisterData data = new RegisterData();
				data.email = emailText.getText().toString();
				data.username = usernameText.getText().toString();
				data.password = passwordText.getText().toString();
				
				RestClient client = new RestClient("/api/v1/createuser/", data.toString());
				client.useSSL(true);
				client.backgroundConnect(ViewRegister.this);
				
			}
		});
	}


	public void onRestFinished(RestResult result) {
		if (pd != null) pd.dismiss();
		if (result != null) {
			if (result.ok) {
				//Log.d("Login", result.payload);
				Toast.makeText(this, getString(R.string.user_registered), Toast.LENGTH_LONG).show();
				RegisterResult data = new Gson().fromJson(result.payload, RegisterResult.class);
				App.getInstance().setUsername(data.username);
				App.getInstance().setKey(data.key);
				App.getInstance().setEmail(data.email);
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
	
	public void doLogin(View v) {
		startActivity(new Intent(this, ViewLogin.class));
		finish();
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
