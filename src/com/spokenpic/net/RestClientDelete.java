/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.net;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;

import android.util.Log;

import com.spokenpic.net.JsonData.RestResult;

public class RestClientDelete extends RestClient {
	
	public RestClientDelete(String uri) {
		super(uri);
	}
	
	@Override
	protected RestResult doGet() {
		HttpDelete httpDelete = new HttpDelete(getSchemeServer()+this.uri);
		setupHttp(httpDelete);
		try {
			HttpResponse httpResponse = HttpManager.execute(httpDelete);
			return returnResponse(httpResponse);
		} catch (Exception e) {
			Log.d("RestClientDelete", "Error doGet (delete) " + e.toString());
			return errorResponse("Fatal error during DELETE");
		}
	}
}
