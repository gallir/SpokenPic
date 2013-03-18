/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.net;

import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.spokenpic.App;
import com.spokenpic.Model;
import com.spokenpic.R;
import com.spokenpic.net.JsonData.ClipData;
import com.spokenpic.net.JsonData.ClipTimeLineResult;
import com.spokenpic.net.JsonData.RestResult;
import com.spokenpic.utils.ImageUtils;

public class TimeLineAdapter extends BaseAdapter {
	final static public int TIMELINE_ALL = 0;
	final static public int TIMELINE_USER = 1;
	final static public int TIMELINE_AUTHENTICATED = 2;
	final static public int TIMELINE_FOLLOWING = 3;

	final int ITEMS_PER_DOWNLOAD = 20;
	final int MAX_ITEM = 1000;

	Boolean mNotMoreItems = false;
	int mPendingItems = 0;


	Handler mHandler;
	ClipTimeLineResult mData;
	List<ClipData> mObjects;
	LayoutInflater mInflater;
	ImageLoader mLoader;
	TimeLineListener mListener;

	String mUsername;
	int mType;
	String mBaseUri;
	int mInvalidClips = 0;
	
	final DisplayImageOptions mDisplayOptions = ImageUtils.getImageLoaderOptions(true, true);


	public TimeLineAdapter(TimeLineListener app) {
		init(app, TIMELINE_ALL, null);
	}

	public TimeLineAdapter(TimeLineListener app, int type, String user) {
		init(app, type, user);
	}

	protected void init(TimeLineListener app, int type, String user) {
		mListener = app;
		mObjects = new ArrayList<ClipData>();
		mInflater = LayoutInflater.from(App.getInstance());
		mHandler = new Handler();
		mLoader = ImageUtils.getImageLoader();
		mType = type;
		mUsername = user;
		switch (mType) {
		case TIMELINE_AUTHENTICATED:
			mBaseUri = "/api/v1/clip/";
			break;
		case TIMELINE_FOLLOWING:
			mBaseUri = "/api/v1/friendstimeline/";
			break;
		case TIMELINE_ALL:
		case TIMELINE_USER:
			if (App.getInstance().isLogged()) {
				mBaseUri = "/api/v1/timeline/"; // The same by now, it doesn't work for all cases
			} else {
				mBaseUri = "/api/v1/timeline/";
			}
			break;
		}
		mHandler.post(new Runnable() {
			public void run() {
				fillObjects(ITEMS_PER_DOWNLOAD*2);
			}
		});

	}

	public void addAll(List<ClipData> objects) {
		mObjects.addAll(objects);
	}

	public int getCount() {
		return mObjects.size();
	}

	public Object getItem(int position) {
		return mObjects.get(position);
	}

	public long getItemId(int position) {
		return mObjects.get(position).id;
	}

	// create a new ImageView for each item referenced by the Adapter
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.grid_image, null);
			holder = new ViewHolder();
			holder.image = (ImageView) convertView.findViewById(R.id.gridImage);
			holder.author = (TextView) convertView.findViewById(R.id.gridAuthor);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
			holder.image.setImageDrawable(null);
		}

		ClipData item = mObjects.get(position);


		String url;
		if (item.thumbnail != null && item.thumbnail.length() > 0) {
			url = item.thumbnail;
		} else if (item.image != null && item.image.length() > 0){
			url = item.image;
		} else {
			return convertView;
		}

		holder.author.setText(item.user);
		holder.url = App.getFullUrl(url);
		holder.position = position;
		holder.resource_uri = item.resource_uri;
		holder.id = item.id;

		mHandler.postDelayed(new CheckedDownload(convertView, item.id, position), 50);
		//mLoader.displayImage(holder.url, holder.image, mDisplayOptions);

		if (position > mObjects.size() + mPendingItems -  ITEMS_PER_DOWNLOAD/2 
				&& mObjects.size() < MAX_ITEM && ! mNotMoreItems) {
			fillObjects(ITEMS_PER_DOWNLOAD);
		}
		return convertView;
	}

	class CheckedDownload implements Runnable {
		View v;
		long id;
		long position;
		protected CheckedDownload(View v, long id, long position) {
			this.v = v;
			this.id = id;
			this.position = position;
		}
		public void run() {
			ViewHolder holder = (ViewHolder) v.getTag();
			if (holder.position == position) {
				try {
					mLoader.displayImage(holder.url, holder.image, mDisplayOptions);
				} catch (Exception e) {};
			}
		}
	}

	protected void fillObjects(int count) {
		if (mNotMoreItems) return;
		mPendingItems += count;
		mListener.onStartDownloading();

		new AsyncTask<Integer, Void, Integer>() {
			List<ClipData> objects = new ArrayList<ClipData>();
			String nextUri;

			@Override
			protected Integer doInBackground(Integer... params) {
				int count = params[0];


				Uri.Builder uri = new Uri.Builder();
				uri.path(mBaseUri);
				uri.appendQueryParameter("limit", Integer.toString(count));
				uri.appendQueryParameter("offset", Integer.toString(mObjects.size()+mInvalidClips));

				if (mType != TIMELINE_AUTHENTICATED) {
					uri.appendQueryParameter("status__in", Integer.toString(Model.STATUS_PUBLIC)); // Show only those in public
				} else {
					uri.appendQueryParameter("status__in", Model.STATUS_PRIVATE + "," + Model.STATUS_PUBLIC); // Show only those in public or private
				}

				if (mType == TIMELINE_USER && mUsername != null) {
					uri.appendQueryParameter("user__username", mUsername);
				}
				nextUri = uri.toString();

				Boolean finished = false;
				while (objects.size() < count && nextUri != null && ! finished) {
					// AppPreferences.DEBUG("Downloading: " + nextUri);
					RestClient client = new RestClient(nextUri);
					RestResult result = client.connect();
					if (result.ok) {
						ClipTimeLineResult timeline = new Gson().fromJson(result.payload, ClipTimeLineResult.class);
						if (timeline.objects.size() > 0) {
							// Add only valid objects, to avoid errors in the API
							for (ClipData r: timeline.objects) {
								if (r.user != null && r.image != null) {
									objects.add(r);
								} else {
									mInvalidClips++;
								}
							}
						}
						nextUri = timeline.meta.next;
					} else {
						App.DEBUG("Failed to get: "+ uri);
						finished = true;
					}
				}
				return params[0];
			}

			@Override
			protected void onPostExecute(Integer count){
				//setSupportProgressBarIndeterminateVisibility(false);
				mPendingItems -= count;
				mListener.onStopDownloading();
				if (objects.size() > 0) {
					mObjects.addAll(objects);
					mListener.onNewItems(mObjects.size());
				}
				if (nextUri == null) {
					mNotMoreItems = true;
				}
			}
		}.execute(count);
	}

	public interface TimeLineListener {
		void onNewItems(int count);
		void onStartDownloading();
		void onStopDownloading();
	}

	public class ViewHolder {
		public ImageView image;
		public TextView author;
		public long position;
		public String url;
		public String resource_uri;
		public long id;
	}
}

