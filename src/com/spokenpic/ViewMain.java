/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.spokenpic.utils.ImageUtils;
import com.spokenpic.utils.LoadThumbnailTask;



public class ViewMain extends SherlockActivity {
	Button mNewClipButton;
	Button mGalleryButton;
	TextView mRegisterButton;
	TextView mLoginButton;
	Button mTimelineButton;
	int mCursorPosition = -1;


	private SimpleCursorAdapter mGalleryAdapter=null;
	private Gallery mGallery;


	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.view_main);
		//sApp.getInstance().loadPreferences();

		mNewClipButton = (Button) findViewById(R.id.new_clip);
		//mGalleryButton = (Button) findViewById(R.id.galleryButton);
		mRegisterButton = (TextView) findViewById(R.id.register);
		mLoginButton = (TextView) findViewById(R.id.login);
		mTimelineButton = (Button) findViewById(R.id.timeline);

		if (App.getInstance().isDebugBuild()) {
			String version = null;
			try {
				version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
			}
			Toast.makeText(this, "Debug version: " + version + " (" + App.getInstance().getServer() + ")", Toast.LENGTH_SHORT).show();
		}




		String[] from = new String[] { "file" };
		int[] to = new int[] { R.id.galleryImage };

		mGalleryAdapter = new SimpleCursorAdapter(this, R.layout.gallery_image, null, from, to);
		mGalleryAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder(){
			/** Binds the Cursor column defined by the specified index to the specified view */
			public boolean setViewValue(View view, Cursor c, int col){
				String file = c.getString(c.getColumnIndex("file"));
				Bitmap bitmap = ImageUtils.cachedThumb(file, App.THUMB_IMAGE_SIZE, App.getInstance().getCacheDir());
				if (bitmap == null) {
					new LoadThumbnailTask().execute(view.findViewById(R.id.galleryImage), file, App.THUMB_IMAGE_SIZE, App.getInstance().getCacheDir());
				} else {
					((ImageView) view.findViewById(R.id.galleryImage)).setImageBitmap(bitmap);
				}
				// ImageUtils.getImageLoader().displayImage("file://" + file, (ImageView) view.findViewById(R.id.galleryImage), ImageUtils.getImageLoaderOptions(true, false));
				return true;
			}
		});


		mGallery = (Gallery) findViewById(R.id.gallery);
		//mGallery.setAdapter(mGalleryAdapter);

		mGallery.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View v, int position, long id) {
				//int index = mGalleryAdapter.getCursor().getColumnIndex("_id");
				Intent viewClip = new Intent(ViewMain.this, ViewClip.class);
				viewClip.putExtra("id", mGalleryAdapter.getItemId(position));
				startActivity(viewClip); 
			}
		});

		mNewClipButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(ViewMain.this, ViewClipCreator.class));
			}
		});

		mTimelineButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(ViewMain.this, ViewTimeLineBrowser.class));
			}
		});


	}

	@Override
	public void onResume() {
		super.onResume();
		App.DEBUG("Max memory: " + Runtime.getRuntime().maxMemory() + " Class: " + ((ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE)).getMemoryClass());

		invalidateOptionsMenu();
		updateButtons();
		new GalleryQuery().execute();

		/*
		mGalleryButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(SpokenPic.this, ViewGallery.class));
			}
		});
		 */

	}

	@Override
	public void onLowMemory() {
		App.getInstance().onLowMemory();
	}


	@Override
	protected void onPause(){
		super.onPause();
		Cursor cursor = mGalleryAdapter.getCursor();
		if (cursor != null) {
			mCursorPosition = cursor.getPosition();
			cursor.close();
			mGalleryAdapter.changeCursor(null);
		}
	}

	void updateButtons() {

		if (App.getInstance().isLogged()){
			mLoginButton.setVisibility(View.INVISIBLE);
			// Show user profile
			mRegisterButton.setText(Html.fromHtml(String.format("<u>" + App.getInstance().getUsername() + "</u>")));
			//mRegister.setCompoundDrawables(null, getResources().getDrawable(R.drawable.person), null, null);
			mRegisterButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(ViewMain.this, ViewWeb.class);
					intent.setData(Uri.parse(App.getFullUrl("/perfil/profile/" + App.getInstance().getKey() + "/")));
					intent.putExtra("title", App.getInstance().getUsername());
					startActivity(intent);
				}
			});
		} else {
			mLoginButton.setVisibility(View.VISIBLE);
			mLoginButton.setText(Html.fromHtml(String.format("<u>" + getString(R.string.login) + "</u>")));
			mLoginButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					startActivity(new Intent(ViewMain.this, ViewLogin.class));
				}
			});

			mRegisterButton.setText(Html.fromHtml(String.format("<u>" + getString(R.string.register) + "</u>")));
			mRegisterButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					startActivity(new Intent(ViewMain.this, ViewRegister.class));
				}
			});


		}

	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate menu resource file.
		// getMenuInflater().inflate(R.menu.main, menu);

		if (App.getInstance().isLogged()) {
			menu.add(Menu.NONE, R.string.logout, Menu.NONE, getString(R.string.logout));
			menu.add(Menu.NONE, R.string.change_pass, Menu.NONE, getString(R.string.change_pass));
			menu.add(Menu.NONE, R.string.register_another, Menu.NONE, getString(R.string.register_another));
		} else {
			menu.add(Menu.NONE, R.string.login, Menu.NONE, getString(R.string.login));
			menu.add(Menu.NONE, R.string.register, Menu.NONE, getString(R.string.register));
			// menu.add(Menu.NONE, R.string.login, Menu.NONE, getString(R.string.login));
		}
		menu.add(Menu.NONE, R.string.preferences, Menu.NONE, getString(R.string.preferences));
		menu.add(Menu.NONE, R.string.tos, Menu.NONE, getString(R.string.tos));
		menu.add(Menu.NONE, R.string.credits, Menu.NONE, getString(R.string.credits));

		// Locate MenuItem with ShareActionProvider
		//MenuItem item = menu.findItem(R.id.menu_share);

		// Fetch and store ShareActionProvider
		//mShareActionProvider = (ShareActionProvider) item.getActionProvider();

		// Return true to display menu
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection

		Intent intent;

		switch (item.getItemId()) {
		case R.string.register:
		case R.string.register_another:
			startActivity(new Intent(ViewMain.this, ViewRegister.class));
			// App.getInstance().loadPreferences();
			invalidateOptionsMenu();
			return true;
		case R.string.login:
			startActivity(new Intent(ViewMain.this, ViewLogin.class));
			return true;
		case R.string.change_pass:
			startActivity(new Intent(ViewMain.this, ViewChangePass.class));
			return true;
		case R.string.logout:
			App.getInstance().setKey(null);
			App.getInstance().store();
			updateButtons();
			invalidateOptionsMenu();
			return true;
		case R.string.preferences:
			startActivity(new Intent(ViewMain.this, ViewPreferences.class));
			return true;
		case R.string.tos:
			intent = new Intent(ViewMain.this, ViewWeb.class);
			intent.setData(Uri.parse(App.getFullUrl("/pages/legal-conditions")));
			intent.putExtra("title", getString(R.string.tos));
			startActivity(intent);
			return true;
		case R.string.credits:
			intent = new Intent(ViewMain.this, ViewWeb.class);
			intent.setData(Uri.parse(App.getFullUrl("/pages/credits")));
			intent.putExtra("title", getString(R.string.credits));
			startActivity(intent);
			return true;
		default:
			return true;
		}
	}

	// performs database query outside GUI thread
	private class GalleryQuery extends AsyncTask<Void, Void, Cursor> 
	{
		Model model = new Model();

		@Override
		protected Cursor doInBackground(Void... params)
		{
			model.open();
			Cursor cursor = model.getAllClips();
			return cursor;
		} 

		@Override
		protected void onPostExecute(Cursor result)
		{
			if (result != null) {
				mGalleryAdapter.changeCursor(result);
				mGallery.setAdapter(mGalleryAdapter);
				if (ViewMain.this.mCursorPosition > 0 && result.getCount() >= ViewMain.this.mCursorPosition && result.moveToPosition(ViewMain.this.mCursorPosition)) {
					mGallery.setSelection(ViewMain.this.mCursorPosition, false);
				}
				model.close();
			}
		} 
	}
}
