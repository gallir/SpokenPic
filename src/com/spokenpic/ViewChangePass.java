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
import com.spokenpic.net.JsonData.ChangePassData;
import com.spokenpic.net.JsonData.LoginResult;
import com.spokenpic.net.JsonData.RestResult;
import com.spokenpic.net.RestClient.RestClientListener;
import com.spokenpic.net.RestClientPut;
import com.spokenpic.utils.LazyProgressDialog;

public class ViewChangePass extends SherlockActivity implements RestClientListener {
	private ProgressDialog pd = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_changepass2);
		
		getSupportActionBar().setHomeButtonEnabled(true);
		//App.getInstance().loadPreferences();

		if (App.getInstance().getKey() == null) {
			finish();
			return;
		}
		final EditText passwordText = (EditText) findViewById(R.id.password);
		final EditText newPasswordText = (EditText) findViewById(R.id.new_password);
		final EditText newPasswordText2 = (EditText) findViewById(R.id.new_password2);
		final Button changeOk = (Button) findViewById(R.id.change_pass_ok);

		final TextWatcher passCheck = new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void afterTextChanged(Editable s) {
				if (newPasswordText2.getText().length() > 0) {
					if (newPasswordText2.getText().toString().equals(newPasswordText.getText().toString())) {
						newPasswordText2.setTextColor(Color.GREEN);
					} else {
						newPasswordText2.setTextColor(Color.RED);
					}
				}
			}
		};

		newPasswordText.addTextChangedListener(passCheck);
		newPasswordText2.addTextChangedListener(passCheck);

		changeOk.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				pd = LazyProgressDialog.show(ViewChangePass.this, null, getString(R.string.connecting), true, true);
				ChangePassData data = new ChangePassData();
				data.old_password = passwordText.getText().toString();
				data.password = newPasswordText.getText().toString();

				//Log.d("Changepass", "PUT: " + "/api/v1/changepassword/" + AppPreferences.getPk() + "/"  + " ---> " + data.toString());
				RestClientPut client = new RestClientPut("/api/v1/changepassword/" + App.getInstance().getPk() + "/", data.toString());
				client.useSSL(true);
				client.backgroundConnect(ViewChangePass.this);
			}
		});

	}

	public void onRestFinished(RestResult result) {
		if (pd != null) pd.dismiss();
		if (result != null) {
			if (result.ok) {
				//Log.d("Changepass", result.payload);
				LoginResult data = new Gson().fromJson(result.payload, LoginResult.class);
				App.getInstance().setKey(data.key);
				App.getInstance().setPk(data.pk);
				App.getInstance().store();
				finish();
			} else {
				Toast.makeText(this, "Error: " + result.payload, Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, "Connection error", Toast.LENGTH_LONG).show();
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
