/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.spokenpic.utils.ImageUtils;

public class App extends Application {
	static {
		System.loadLibrary("ogg");
		System.loadLibrary("vorbis");
		System.loadLibrary("vorbis-stream");
	}

	final static String PREFS_NAME = "spokenpic";
	public final static int LARGE_IMAGE_SIZE = 600;
	public final static int MEDIUM_IMAGE_SIZE = 200;
	public final static int THUMB_IMAGE_SIZE = 120;


	private static App sInstance;

	Boolean isDebug = null;
	String username;
	Integer pk;
	String key;

	ImageLoader imageLoader;
	public SharedPreferences preferences = null;
	protected String server = null;
	protected Boolean useSSL = false;


	private Integer cacheSequence = 0;
	private File mPicturesDir = null;
	private File mAudioDir = null;


	public static App getInstance() {
		return sInstance;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;

		PackageManager pm = getPackageManager();
		try {
			PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
			isDebug = ((pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
		} catch (Exception e) {
			isDebug = false;
		}

		if (isDebug) {
			if (server == null) server = getString(R.string.dev_server);
			useSSL = false;
		} else {
			if (server == null) server = getString(R.string.main_server);
			useSSL = getResources().getBoolean(R.bool.use_ssl);
		}

		System.loadLibrary("ogg");
		System.loadLibrary("vorbis");
		System.loadLibrary("vorbis-stream");

		loadPreferences();
		enableHttpResponseCache();
	}

	@Override
	public void onLowMemory() {
		Log.d("App", "onLowMemory() called");
		ImageUtils.clearImageLoader();
	}

	public File getMyExternalDir(String type) {
		File dir = null;
		if (type != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			dir = getExternalFilesDir(type);
		}

		if (dir == null) {
			dir = getDir(type, Context.MODE_WORLD_READABLE);
		}

		return dir;
	}

	public File getMyCacheDir() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return getExternalCacheDir();
		} else {
			return getCacheDir();
		}
	}

	public File getPicturesStoragePublicDirectory() {
		if (mPicturesDir != null) {
			return mPicturesDir;
		}

		File dir = null;
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			try {
				dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getString(R.string.album_name));
				if (! dir.exists() && ! dir.mkdirs() ) {
					Log.d(App.getInstance().getString(R.string.album_name), "failed to create directory " + dir.getAbsolutePath());
					dir = null;
				}
			} catch (Exception e) {
				dir = null;
			}
		}

		if (dir == null) {
			dir = getMyExternalDir(Environment.DIRECTORY_PICTURES);
		}
		mPicturesDir = dir;
		return dir;

	}

	public File getAudioStoragePublicDirectory() {
		if (mAudioDir != null) {
			return mAudioDir;
		}
		File dir = null;
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			try {
				dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS), getString(R.string.album_name));
				if (! dir.exists() && ! dir.mkdirs() ) {
					Log.d(App.getInstance().getString(R.string.album_name), "failed to create directory " + dir.getAbsolutePath());
					dir = null;
				}
			} catch (Exception e) {
				dir = null;
			}
		}

		if (dir == null) {
			dir = getMyExternalDir(Environment.DIRECTORY_PODCASTS);
		}
		mAudioDir = dir;
		return dir;

	}

	public Integer getPk() {
		return pk;
	}
	public void setPk(Integer pk) {
		this.pk = pk;
	}

	protected String email;

	public int getCacheSequence() {
		cacheSequence = (cacheSequence + 1) % 200;
		return cacheSequence;
	}

	public String getCacheName() {
		String name = "app_cache_" + getCacheSequence();
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getKey() {
		return key != null && key.length() > 0 ? key : null;
	}

	public SharedPreferences getPreferences() {
		return preferences;
	}

	public String getServer() {
		return server;
	}

	public String getUsername() {
		return username != null && username.length() > 0 ? username : null;
	}

	public Boolean getUseSSL() {
		return useSSL;
	}

	public boolean isDebugBuild()
	{
		return isDebug;
	}

	private void loadPreferences() {
		App.DEBUG("**** Loading preferences");
		preferences = PreferenceManager.getDefaultSharedPreferences(sInstance);
		username = preferences.getString("username", null);
		key = preferences.getString("key", null);
		pk = preferences.getInt("pk", 0);
		email = preferences.getString("email", null);
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setUseSSL(Boolean useSSL) {
		this.useSSL = useSSL;
	}

	public void store() {
		if (preferences == null) return;
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("username", username);
		editor.putString("key", key);
		editor.putInt("pk", pk);
		editor.putString("email", email);
		editor.putString("server", server);
		editor.commit();
	}

	public Boolean isLogged() {
		return key != null && pk != null && key.length() > 0 && pk > 0;
	}

	static public void DEBUG(String m) {
		if (! sInstance.isDebug) return;

		try {
			Log.d("SPOKEN DEBUG", m);
		} catch (Exception e) {
			Log.e("SPOKEN DEBUG", "Error in DEBUG: " + e.getMessage() );
		}
	}

	static public void criticalErrorAlert(final Activity activity, int resId, final Boolean finish) {
		AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.setMessage(activity.getString(resId));
		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (finish) {
					activity.finish();
				}
			}
		});
		alertDialog.show();
	}

	static public String getFullUrl(String url) {
		if (! url.matches("^https*://.*")) {
			return "http://" + App.getInstance().getServer()+ url;
		} else {
			return url;
		}
	}

	private void enableHttpResponseCache() {
		// TODO: check it doesn't fail in ICS
		try {
			long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
			File httpCacheDir = new File(getCacheDir(), "http");
			Class.forName("android.net.http.HttpResponseCache")
			.getMethod("install", File.class, long.class)
			.invoke(null, httpCacheDir, httpCacheSize);
		} catch (Exception httpResponseCacheNotAvailable) {}
	}

}
