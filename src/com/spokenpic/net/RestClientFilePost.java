/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.net;

import java.io.File;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;

import android.util.Log;

import com.spokenpic.net.JsonData.RestResult;

public class RestClientFilePost extends RestClient {
	protected String mime = null;

	public RestClientFilePost(String uri, String filename) {
		super(uri, filename);
	}

	public RestClientFilePost(String uri, String filename, String mime) {
		super(uri, filename);
		this.mime = mime;
	}

	@Override
	protected RestResult doPost() {
		HttpPost httpPost = new HttpPost(getSchemeServer()+this.uri);
		setupHttp(httpPost);
		try {
			FileEntity e = new FileEntity(new File(data), mime);
			httpPost.setEntity(e);

			HttpResponse httpResponse = HttpManager.execute(httpPost);
			return returnResponse(httpResponse);
		} catch (Exception e) {
			Log.d("RestClientFilePost", "Error doPost " + e.toString());
			return errorResponse("Fatal error during file POST");
		}
	}

}
