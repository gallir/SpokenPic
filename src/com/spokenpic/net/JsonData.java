/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.net;

import java.util.List;

import com.google.gson.Gson;

public class JsonData {

	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	public static class RestResult {
		public int httpCode = 0;
		public String payload = null;
		public Boolean ok = false;
	}

	public static class RegisterData extends JsonData {
		public String email;
		public String username;
		public String password;
	}

	public static class RegisterResult extends JsonData {
		public String email;
		public String key;
		public String username;
		public String password;
		public Integer pk = null; // An integer key
	}

	public static class LoginData extends JsonData {
		public String username;
		public String password;
	}

	public static class ChangePassData extends JsonData {
		public String old_password;
		public String password;
	}

	public static class LoginResult extends JsonData {
		public String email;
		public String key;
		public String username;
		public String password;
		public Integer pk = null; // An integer key

	}

	public static class ClipDataXXX extends JsonData {
		public long session_id;
		public String created;
		public int duration;
		public int status;
		public String lang; // Locale.getDefault().getLanguage()
	}

	public static class ClipDataChangeStatus extends JsonData {
		public String hash_key;
		public int status;
	}

	public static class ClipData extends JsonData {
		public Long session_id;
		public String lang; // Locale.getDefault().getLanguage()
		public String user;
		public Long user_id;
		public String created;
		public Integer duration;
		public Integer status;
		public String title;
		public String hash_key;
		public Long id;
		public Float latitude;
		public Float longitude;
		public String resource_uri;
		public String permalink;
		public String send_date;
		public String updated;
		public String image;
		public String thumbnail;
		public String sound;
		public String mp3_sound;
		public String ogg_sound;
		public Long score;
		public Boolean adult_content = false;
		public Boolean has_voted = false;
		public Integer votes = 0;
		
	}
	
	public static class ClipTimeLineResult extends JsonData {
		public class Meta {
			public int limit;
			public String next;
			public String previous;
			public int total_count;
		}
		public Meta meta;
		public List<ClipData> objects;
	}

	public static class FollowData extends JsonData {
		public long to_user;
	}
}
