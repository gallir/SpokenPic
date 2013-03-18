/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.spokenpic.net.FileDownloader.FileDownloaderListener;
import com.spokenpic.net.InputStreamDownloader;
import com.spokenpic.utils.CameraPreview;
import com.spokenpic.utils.CameraPreview.ClipCreatorState;
import com.spokenpic.utils.ImageUtils;
import com.spokenpic.utils.LazyProgressDialog;


public class ViewClipCreator extends SherlockActivity implements CameraPreview.CameraPreviewCallback {

	static final int INTENT_RESULT_GALLERY = 0;

	private CameraPreview mPreview = null;
	Boolean mStartedFromLauncher;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		ActionBar ab;
		if ( ( ab = getSupportActionBar()) != null) {
			ab.hide();
		}
		//App.getInstance().loadPreferences();

		setContentView(R.layout.view_clip_creator);
		mPreview = (CameraPreview) findViewById(R.id.cameraPreview);


		Intent intent = getIntent();
		String action = intent.getAction();
		if (action != null && action.equals("android.intent.action.MAIN")) {
			mStartedFromLauncher = true;
		} else {
			mStartedFromLauncher = false;
		}
	}

	
	@Override
	public void onLowMemory() {
		App.getInstance().onLowMemory();
	}


	public void onSaveClip(Model session) {
		final LazyProgressDialog dialog = LazyProgressDialog.show(this, null, App.getInstance().getString(R.string.saving), true, false);
		dialog.show();

		long clipId = session.store();

		mPreview.onPause(); // Stop all camera related stuff
		mPreview.setVisibility(View.GONE);

		
		Intent viewClip = new Intent(this, ViewClip.class);		
		viewClip.putExtra("id", clipId);
		viewClip.putExtra("new", clipId);
		if (mStartedFromLauncher) {
			viewClip.putExtra("return_to_creator", mStartedFromLauncher);
		}
		startActivity(viewClip);
		dialog.dismiss();
		finish();
	}

	public void onPictureTaken(String filename) {
	}


	@Override
	public Object onRetainNonConfigurationInstance() {
		return mPreview.getState();
	}

	protected void onRestoreInstanceState (Bundle savedInstanceState) {
		ClipCreatorState state;
		state = (ClipCreatorState) savedInstanceState.getSerializable(CameraPreview.CLIP_CREATOR_STATE);
		if (state != null) {
			mPreview.restoreState(state);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(CameraPreview.CLIP_CREATOR_STATE, mPreview.getState());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();

		int orientation = getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		}

		if (mPreview != null) mPreview.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mPreview != null) mPreview.onPause();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

	@Override
	public void onStop(){
		super.onStop();
		if (mPreview != null) mPreview.onStop();
	}



	public void onPickFromGallery() {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType("image/*");
		startActivityForResult(intent, INTENT_RESULT_GALLERY); 
	}


	// All of the f*ucking crap is due to the integration of Picasa in the Gallery.
	// But those images have to be downloaded in a background thread,
	// which interfere with the thread that open the camera(trillions of race conditions).
	// Furthermore, the user has to be able to cancel the download, so we have to be able
	// to cancel the background thread and re-init the camera.
	//
	// All of that for a lack of a proper integration with the API:
	// For god sake, block and annoy the user in YOUR f*cking activity, don't
	// throw your shit all over the place.
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//if (resultCode == Activity.RESULT_CANCELED) return;
		if (resultCode != Activity.RESULT_OK) {
			return;
		}

		switch (requestCode) {
		case INTENT_RESULT_GALLERY:
			Uri uri = data.getData();
			if (uri == null) return;

			String filename = ImageUtils.getFilenameFromUri(uri);

			if (filename != null) {
				mPreview.setImage(filename, true);
				return;
			}

			// this is not gallery provider, go deeper into the shit
			final InputStreamDownloader downloader;
			final InputStream is;

			try {
				is = getContentResolver().openInputStream(uri);
			} catch (FileNotFoundException e) {
				Toast.makeText(getApplicationContext(), getString(R.string.gallery_error), Toast.LENGTH_LONG).show();
				// mPreview.returnToCamera();
				return;
			}

			mPreview.setImageDelayed();

			final LazyProgressDialog dialog = LazyProgressDialog.show(this, null, getString(R.string.downloading_image), true, true);

			downloader = new InputStreamDownloader(is, mPreview.getPictureName(), new FileDownloaderListener() {
				public void onDownLoadFinished(File file) {
					dialog.dismiss();
					if (file == null) {
						Toast.makeText(getApplicationContext(), getString(R.string.gallery_error), Toast.LENGTH_LONG).show();
						mPreview.returnToCamera();
						return;
					}
					mPreview.setImage(file.getAbsolutePath(), false);
				}
			});

			dialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					downloader.cancel();
					mPreview.cancelImageDelayed();
					App.DEBUG("Download canceled");
				}
			});

			downloader.execute();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, R.string.preferences, Menu.NONE, getString(R.string.preferences));
		return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.string.preferences:
			startActivity(new Intent(this, ViewPreferences.class));
		default:
			return true;
		}
	}

	/*
	@Override
	public void onAttachedToWindow() {
		// To solve ugly gradients
	    super.onAttachedToWindow();
	    Window window = getWindow();
	    window.setFormat(PixelFormat.RGBA_8888);
	}
	 */
}
