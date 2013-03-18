/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;
import com.spokenpic.net.JsonData.ClipData;
import com.spokenpic.net.JsonData.ClipDataChangeStatus;
import com.spokenpic.net.JsonData.RestResult;
import com.spokenpic.net.RestClient;
import com.spokenpic.net.RestClientFilePost;
import com.spokenpic.net.RestClientPut;
import com.spokenpic.utils.ImageUtils;



public class Model {
	public static final long UPLOAD_TIMEOUT = 1000 * 60; // 1 minute
	public static final int TYPE_AUDIO = 1;
	public static final int TYPE_IMAGE = 2;

	public static final int STATUS_INCOMPLETE = -1;
	public static final int STATUS_PUBLIC = 0;
	public static final int STATUS_PRIVATE = 1;


	private SQLiteDatabase db = null;
	private DatabaseOpenHelper dbOpenHelper = null;
	private List<MediaItem> elements = null;
	public Long mId = 0L;
	private long mServerId = 0;
	private Boolean mStored = false;
	private Date mDate = null;
	public String url = null;
	public int status = STATUS_INCOMPLETE;
	public String hashKey = null;
	public String resourceUri = null;
	public String mTitle = null;
	public int mSendStatus = 0;
	public Date mSendDate = null;

	// TODO: alert, alert, this a dirty trick for the ProgressListener
	// The whole AsyncTask must be moved inside Model
	public static long bytesToPost = 0;

	public Model() {
		init(0);
	}

	public Model(long id) {
		init(id);
	}

	private void init(long id) {
		dbOpenHelper = new DatabaseOpenHelper(App.getInstance());
		//App.getInstance().loadPreferences();
		read(id, true);
	}

	void read(long id, Boolean readElements) {
		// Read session data and elements
		if (id > 0) {
			Cursor c;
			mId = id;
			open();

			c = db.query("sessions", new String[] {"date", "url", "status", "hash_key", "resource_uri", "title", "send_status", "send_date"}, "_id = ?", new String[] {String.valueOf(mId)}, null, null, null);
			if (c.moveToFirst()) {
				mDate = new Date(c.getLong(0));
				url = c.getString(1);
				status = c.getInt(2);
				hashKey = c.getString(3);
				resourceUri = c.getString(4);
				mTitle = c.getString(5);
				mSendStatus = c.getInt(6);
				mSendDate = new Date(c.getLong(7));
				c.close();

				if (readElements) {
					c = db.query("elements", new String[] {"type", "file", "_id", "imported"}, "session = ?", new String[] {String.valueOf(mId)}, null, null, null);
					c.moveToFirst();
					while (! c.isAfterLast()) {
						add(c.getLong(2), c.getInt(0), c.getString(1), c.getInt(3) > 0);
						c.moveToNext();
					}
					c.close();
				}
			} else {
				mId = 0L;
			}
			close();
		}
	}

	public void open(Boolean writable) throws SQLException {
		db = dbOpenHelper.getWritableDatabase();
	}

	public void open() throws SQLException {
		db = dbOpenHelper.getReadableDatabase();
	}

	public void close() {
		if (db != null) {
			db.close();
		}
	}

	public void add(long id, int type, String filename, Boolean isImported) {
		MediaItem e = new MediaItem(id, type, filename, isImported);
		if (elements == null) {
			elements = new ArrayList<MediaItem>();
		}
		elements.add(e);
	}

	public long store() {
		Date date = new Date();

		ContentValues newSession = new ContentValues();

		open(true);
		if (mId == 0) {
			newSession.put("date", date.getTime());
			newSession.put("status", status);
			newSession.put("send_status", 0);
			newSession.put("send_date", 0);
			if (mTitle != null) {
				newSession.put("title", mTitle);
			}
			mId = db.insert("sessions", null, newSession);
		}

		if (mId > 0) {
			for (MediaItem e: elements) {
				e.store(db, mId);
			}
		}
		close();
		mStored = true;
		return mId;
	}

	public Boolean postToServer() {
		int tries;
		final int maxTries = 3;
		int remainingOps = 2; // Initial and close operations

		if (mServerId > 0) {
			// Already uploaded, return true
			updateSendStatus(0);
			return true;
		}

		if (isUploading()) return false;

		if (elements != null) {
			remainingOps += elements.size();
		}
		updateSendStatus(remainingOps);

		// First stage: create the clip
		ClipData clip = new ClipData();
		clip.session_id = mId;
		clip.status = STATUS_INCOMPLETE;
		clip.lang = Locale.getDefault().getLanguage();
		if (mTitle != null && mTitle.length() > 0 ) {
			clip.title = mTitle;
		}

		RestClient c = new RestClient("/api/v1/clip/", clip.toString());
		RestResult r = c.connect();


		if (r== null || ! r.ok || r.payload == null) {
			updateSendStatus(-1);
			return false;
		}
		remainingOps--;
		updateSendStatus(remainingOps);

		ClipData data = new Gson().fromJson(r.payload, ClipData.class);

		// Din't get the hash key
		if (data.hash_key == null) {
			updateSendStatus(-1);
			return false;
		}

		hashKey = data.hash_key;
		if (data.resource_uri != null) resourceUri = data.resource_uri;
		if (data.permalink != null) url = App.getFullUrl(data.permalink);

		// Second stage: upload image and audio
		ImageUtils.clearImageLoaderCache();
		for (MediaItem e: elements) {
			if (! e.postToServer()) {
				updateSendStatus(-1);
				return false;
			}
			remainingOps--;
			updateSendStatus(remainingOps);
		}

		// Third stage: Change the status to "public"
		// TODO: must use preferences
		if (status != STATUS_PRIVATE) {
			status= STATUS_PUBLIC; // Clip public
		}


		clip.status = status;

		tries = 0;
		while (tries < maxTries) {
			RestClientPut complete = new RestClientPut(resourceUri, clip.toString());
			r = complete.connect();
			if (r== null || ! r.ok || r.payload == null) {
				App.DEBUG("Error uploading clip : " + resourceUri + " tries: " + tries);
				tries++;
			} else {
				break;
			}
		}
		if (tries >= maxTries) {
			updateSendStatus(-1);
			return false;
		}


		data = new Gson().fromJson(r.payload, ClipData.class);
		if (data.id > 0) mServerId = data.id;
		remainingOps--;
		mSendStatus = 0;
		updateSendStatus(mSendStatus);
		update();
		return true;
	}

	public Cursor getAllImages() 
	{
		return db.rawQuery("select sessions._id, image.file, datetime(date/1000, 'unixepoch', 'localtime') as date " +
				"from sessions, elements as image where " +
				"image.session = sessions._id and image.type = " + Model.TYPE_IMAGE +
				" order by sessions._id desc", null);
	}

	public Cursor getAllClips() 
	{
		return db.rawQuery("select sessions._id, image.file " +
				"from sessions, elements as image where " +
				"image.session = sessions._id and image.type = " + Model.TYPE_IMAGE +
				" order by sessions._id desc", null);
	}

	public String getAudio() {
		try { // The file might be already deleted
			for (MediaItem e: elements) {
				if (e.mType == Model.TYPE_AUDIO) return e.mFilename;
			}
		} catch (Exception e) {}
		return null;
	}

	public String getImage() {
		try { // The file might be already deleted
			for (MediaItem e: elements) {
				if (e.mType == Model.TYPE_IMAGE) return e.mFilename;
			}
		} catch (Exception e) {}
		return null;
	}

	public String getUrl() {
		String url = this.url;
		if (status == Model.STATUS_PRIVATE) {
			url += hashKey + "/";
		}
		return url;
	}

	public synchronized Boolean isUploading() {
		Date now = new Date();
		return mSendStatus > 0 && now.getTime() - mSendDate.getTime() < UPLOAD_TIMEOUT; 
	}

	public synchronized Boolean updateSendStatus(int newSendStatus) {
		if (mId <= 0) return false;
		open(true);
		String strFilter = "_id=" + mId;
		ContentValues args = new ContentValues();
		args.put("send_status", newSendStatus);
		args.put("send_date", new Date().getTime());
		db.update("sessions", args, strFilter, null);
		close();
		return true;
	}

	public Boolean update() {
		if (mId <= 0) return false;

		open(true);
		String strFilter = "_id=" + mId;
		ContentValues args = new ContentValues();
		args.put("status", status);
		args.put("url", url);
		args.put("server_id", mServerId);
		args.put("hash_key", hashKey);
		args.put("resource_uri", resourceUri);
		args.put("title", mTitle);
		args.put("send_status", mSendStatus);
		args.put("send_date", mSendDate.getTime());
		db.update("sessions", args, strFilter, null);
		close();
		return true;
	}

	public void delete() {
		if (mId <= 0) return;
		open(true);

		int n;
		n = db.delete("sessions", "_id=?", new String[] { mId.toString() });
		n = db.delete("elements", "session=?", new String[] { mId.toString() });
		close();

		new AsyncTask<Model, Void, Void>() {

			@Override
			protected Void doInBackground(Model... params) {
				Model model = params[0];
				for (MediaItem e: model.elements) {
					if (! e.mImported) {
						File file = new File(e.mFilename);
						boolean deleted = file.delete();
						if (deleted) {
							App.DEBUG("Deleted: " + e.mId +" "+ e.mFilename);
						}
						e.notifyGallery();
					}
				}
				return null;
			}

		}.execute(this);
	}



	private class MediaItem {
		public Long mId;
		public int mType;
		public String mFilename;
		public long mStart = 0;
		public long mDuration = 0;
		public Boolean mImported = false;

		public MediaItem (long id, int type, String filename, Boolean isImported) {
			mId = id;
			mType = type;
			mFilename = filename;
			mDuration = 0;
			mImported = isImported;
		}

		public String mime(){
			String mime = null;

			String extension = MimeTypeMap.getFileExtensionFromUrl(mFilename);
			if (extension != null && extension.length() > 0 ) {
				mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			}

			if (mime == null || mime.length() <= 0) {
				switch (mType) {
				case Model.TYPE_IMAGE: mime = "image/jpeg"; break;
				case Model.TYPE_AUDIO: mime = "audio/ogg"; break;
				default: mime = "application/octet-stream";
				}
			}
			return mime;
		}

		public Boolean store(SQLiteDatabase db, long sessionId) {
			File destinationDirectory;

			ContentValues newElement = new ContentValues();

			if (mFilename == null) return false;

			if (! mImported) {
				File originalFile = new File(mFilename);
				destinationDirectory = App.getInstance().getMyCacheDir();
				switch (mType) {
				case Model.TYPE_IMAGE:
					destinationDirectory = App.getInstance().getPicturesStoragePublicDirectory();
					break;
				case Model.TYPE_AUDIO:
					destinationDirectory = App.getInstance().getAudioStoragePublicDirectory();
					break;
				default:
					destinationDirectory = Environment.getDataDirectory();
				}

				App.DEBUG("Model saving to: " + destinationDirectory.toString());

				String newName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "__" + sessionId + "_" + originalFile.getName();
				File newFile = new File(destinationDirectory, newName);

				if (! originalFile.renameTo(newFile)) {
					// try to copy
					try {
						InputStream in = new FileInputStream(originalFile);
						OutputStream out = new FileOutputStream(newFile);

						byte[] buf = new byte[4096];
						int len;
						while((len = in.read(buf)) > 0 ) {
							out.write(buf, 0, len);
						}
						in.close();
						out.close();
						originalFile.delete();
					} catch (IOException e) {
						Log.d(App.getInstance().getString(R.string.album_name), "failed to rename and copy " + originalFile.getAbsolutePath() + " to " + newFile.getAbsolutePath());
						return false;
					}
				}
				mFilename = newFile.getAbsolutePath();
			}


			newElement.put("type", mType);
			newElement.put("session", sessionId);
			newElement.put("file", mFilename);
			newElement.put("imported", mImported);

			if (mType == Model.TYPE_AUDIO) {
				MediaPlayer mp = new MediaPlayer();
				try {
					mp.setDataSource(mFilename);
					mp.prepare();
					mDuration = mp.getDuration();
					newElement.put("duration", (float) mDuration/1000);
					mp.release();
				} catch (Exception e) {
				}
			}
			mId = db.insert("elements", null, newElement);
			if (! mImported) {
				notifyGallery();
			}
			return true;
		}

		private void notifyGallery() {
			if (mType == Model.TYPE_IMAGE) {
				Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
				mediaScanIntent.setData(Uri.fromFile(new File(mFilename)));
				App.getInstance().sendBroadcast(mediaScanIntent);
			}
		}

		private Boolean postToServer(){
			String postUri = "/api/v1/post/";
			String query = "";
			String filename;

			File file = null;

			if (mFilename == null) return false;
			filename = mFilename;

			switch (mType) {
			case Model.TYPE_AUDIO:
				postUri += "audio/";
				if (mDuration > 0) {
					query += "duration=" + String.valueOf(mDuration);
				}
				break;
			case Model.TYPE_IMAGE:
				postUri += "img/";
				int size = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(App.getInstance()).getString("image_quality", "900"));
				App.DEBUG("Uploaded image size: " + size);
				String scaledImage = ImageUtils.scale(mFilename, App.getInstance().getMyCacheDir().getAbsolutePath(), size);
				if (scaledImage != null) {
					filename = scaledImage;
				}
				break;
			}
			file = new File(filename);
			if ( file != null && file.canRead()) {
				String uri = postUri + hashKey + "/";
				if (query.length() > 0) {
					uri += "?" + query;
				}
				RestClientFilePost c = new RestClientFilePost(uri, filename, mime());
				RestResult r = c.connect();

				if (r == null || ! r.ok || r.payload == null) {
					return false;
				}
				return true;
			}
			return false;
		}
	}


	public static class DatabaseOpenHelper extends SQLiteOpenHelper 
	{
		private static final String DATABASE_NAME = "db";
		private static final int DATABASE_VERSION = 8;

		public DatabaseOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{
			db.execSQL("create table sessions (_id integer primary key autoincrement, date unsigned big int, url text, title text, server_id unsigned integer, status integer, send_status integer default 0, send_date big int default 0, hash_key text, resource_uri text, note text)");
			db.execSQL("create table elements (_id integer primary key autoincrement, session integer references sessions (id), type integer not null, start unsigned integer default 0, duration unsigned integer default 0, file TEXT not null, imported boolean default false)");
			db.execSQL("create index elements_session on elements (session)");
			db.execSQL("create index type_id on elements (type, _id)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{
			if (oldVersion > 0 && oldVersion < 4) {
				try {
					db.execSQL("alter table sessions add hash_key text");
				} catch (SQLiteException e) {}
				try {
					db.execSQL("alter table sessions add resource_uri text");
				} catch (SQLiteException e) {}
			}

			if (oldVersion > 0 && oldVersion < 5) {
				try {
					db.execSQL("alter table elements add imported boolean default false");
				} catch (SQLiteException e) {}
			}

			if (oldVersion > 0 && oldVersion < 7) {
				try {
					db.execSQL("alter table elements rename to tmp_elements");
					db.execSQL("create table elements (_id integer primary key autoincrement, session integer references sessions (id), type integer not null, start unsigned integer default 0, duration unsigned integer default 0, file TEXT not null, imported boolean default false)");
					db.execSQL("insert into elements (_id, session, type, start, duration, file, imported) select _id, session, type, start, duration, file, imported from tmp_elements");
					db.execSQL("drop table tmp_elements");
					db.execSQL("create index type_id on elements (type, _id)");
				} catch (SQLiteException e) {}
			}

			if (oldVersion > 0 && oldVersion < 8) {
				try {
					db.execSQL("alter table sessions add title text");
					db.execSQL("alter table sessions add send_status integer default 0");
					db.execSQL("alter table sessions add send_date big int default 0");
				} catch (SQLiteException e) {}
			}

		} 
	}
}
