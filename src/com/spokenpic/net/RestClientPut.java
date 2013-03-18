/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.net;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;

import android.util.Log;

import com.spokenpic.net.JsonData.RestResult;

public class RestClientPut extends RestClient {

	public RestClientPut(String uri, String data) {
		super(uri, data);
	}

	@Override
	protected RestResult doPost() {
		HttpPut httpPut = new HttpPut(getSchemeServer()+this.uri);
		setupHttp(httpPut);
		try {
			StringEntity data = new StringEntity(this.data, "UTF-8");
			data.setChunked(false);
			httpPut.setEntity(data);
			httpPut.setHeader("Content-type", "application/json");
			HttpResponse httpResponse = HttpManager.execute(httpPut);
			return returnResponse(httpResponse);
		} catch (Exception e) {
			Log.d("RestClientFilePUT", "Error doPost: " + e.toString());
			return errorResponse("Fatal error during PUT");
		}
	}

}
