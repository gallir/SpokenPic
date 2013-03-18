/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.utils;

import com.google.gson.Gson;




public class PostResult {
	public long session_id;
	public Boolean error;
	public String message;
	public String url;
	
	public String getErrorMessage() {
		String[] fields = message.split("#");
		if (fields.length == 2) return fields[1];
		return null;
	}
	
	static public PostResult fromJson(String json) {
		if (json == null) return null;
		Gson gson = new Gson();
		return gson.fromJson(json, PostResult.class);
	}
	
	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
