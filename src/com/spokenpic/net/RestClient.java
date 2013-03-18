/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.net;

import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;
import android.util.Log;

import com.spokenpic.App;
import com.spokenpic.net.JsonData.RestResult;

public class RestClient {
	//protected DefaultHttpClient mClient;
	protected String uri = null;
	protected String data = null;
	protected RestClientListener listener = null;
//	protected Context activity;
	protected Boolean useSSL = false;

	public RestClient(String uri){
		this.uri = uri;
	}

	public RestClient(String uri, String data) {
		this.uri = uri;
		this.data = data;
	}

	public void useSSL(Boolean ssl) {
		useSSL = ssl;
	}

	protected String getScheme() {
		// TODO: check which server
		if (useSSL && App.getInstance().getUseSSL()) {
			return "https://";
		} else return "http://";
	}

	protected String getSchemeServer() {
		return getScheme() + App.getInstance().getServer();
	}

	public void backgroundConnect(RestClientListener listener) {
		// Create a background thread
		this.listener = listener;
		new RestTask(this).execute();
	}

	public RestResult connect() {
		// Do all operations in the same thread
		return process();
	}

	protected void finished(RestResult result) {
		if (listener != null){
			try {
				listener.onRestFinished(result);
			} catch (Exception e) {
				Log.d("Rest Client", "Error " + e.getMessage());
			}
		}
	}

	protected void setupHttp(HttpMessage m) {
		if (App.getInstance().getKey() != null) {
			m.setHeader("Authorization", "ApiKey " + App.getInstance().getUsername() + ":" + App.getInstance().getKey());
		}
	}

	protected RestResult doGet() {
		HttpGet httpGet = new HttpGet(getSchemeServer()+this.uri);
		setupHttp(httpGet);
		try {
			httpGet.setHeader("Content-type", "application/json");
			HttpResponse httpResponse = HttpManager.execute(httpGet);
			return returnResponse(httpResponse);
		} catch (Exception e) {
			Log.d("RestClient", "Error doGet " + this.uri + ": " + e.toString());
			return errorResponse("Fatal error during GET: " + this.uri);
		}
	}

	protected RestResult doPost() {
		HttpPost httpPost = new HttpPost(getSchemeServer()+this.uri);
		setupHttp(httpPost);
		try {
			StringEntity data = new StringEntity(this.data, "UTF-8");
			data.setChunked(false);
			httpPost.setEntity(data);
			httpPost.setHeader("Content-type", "application/json");
			HttpResponse httpResponse = HttpManager.execute(httpPost);
			return returnResponse(httpResponse);
		} catch (Exception e) {
			Log.d("RestClient", "Error doPost " + e.toString());
			return errorResponse("Fatal error during POST");
		}
	}

	protected RestResult errorResponse(String message) {
		RestResult result = new RestResult();
		result.ok = false;
		result.httpCode = 0;
		result.payload = message;
		return result;
	}

	protected RestResult returnResponse(HttpResponse httpResponse) {
		try {
			//mClient.close();
			RestResult result = new RestResult();
			result.httpCode = httpResponse.getStatusLine().getStatusCode();
			if (httpResponse.getEntity() != null) {
				result.payload = EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8);
				//Log.d("HTTP", "Got response: " + result.payload);
			}
			if (result.httpCode < 200 || result.httpCode > 299) {
				result.ok = false;
				if (result.payload == null || result.payload.length() == 0) {
					if (result.httpCode >= 400 && result.httpCode < 500) {
						result.payload = "Not found";
					} else {
						result.payload = "Server error";
					}
				}
				App.DEBUG("HTTP Status: " + result.httpCode + " Body: "+ result.payload.substring(0, Math.min(50, result.payload.length()) - 1 ));
				return result;
			} else {
				//App.DEBUG(result.payload);
				result.ok = true;
			}
			return result;
		} catch (Exception e) {
			Log.d("RestClient", "Error returnResponse " + e.toString());
			return errorResponse("Fatal error in response");
		}
	}

	protected RestResult process() {
		if (data != null) {
			return doPost();
		} else {
			return doGet();
		}
	}

	public static interface RestClientListener {
		void onRestFinished(RestResult result);
	}

	private class RestTask extends AsyncTask<Void, Void, RestResult> {
		RestClient rest = null;

		public RestTask(RestClient rest) {
			this.rest = rest;
		}

		@Override
		protected RestResult doInBackground(Void... params) {
			return process();
		}

		@Override
		protected void onPostExecute(RestResult result){
			try {
				rest.finished(result);
			} catch (Exception e) {
				Log.d("RestClient", "Error onPostExecute " + e.toString());
			}
		}
	}
}
