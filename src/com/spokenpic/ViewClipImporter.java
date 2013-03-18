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

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.spokenpic.net.FileDownloader.FileDownloaderListener;
import com.spokenpic.net.InputStreamDownloader;
import com.spokenpic.utils.CameraPreview;
import com.spokenpic.utils.CameraPreview.CameraPreviewCallback;
import com.spokenpic.utils.CameraPreview.ClipCreatorState;
import com.spokenpic.utils.ImageUtils;
import com.spokenpic.utils.LazyProgressDialog;

public class ViewClipImporter extends SherlockActivity implements CameraPreviewCallback {
	private CameraPreview mPreview = null;


	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		/*
		if (App.getInstance().isDebugBuild()) {
			StrictMode.enableDefaults();
		}
		*/
		
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		ActionBar ab;
		if ( ( ab = getSupportActionBar()) != null) {
			ab.hide();
		}
		//App.getInstance().loadPreferences();

		setContentView(R.layout.view_clip_creator);
		mPreview = (CameraPreview) findViewById(R.id.cameraPreview);
		mPreview.forbidCamera();
		checkShareIntent();

	}

	void checkShareIntent() {
		// Check if it's a share intent
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
			Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			String filename = ImageUtils.getFilenameFromUri(uri);
			
			if (filename != null) {
				mPreview.setShareImage(filename, true);
			} else {

				final InputStreamDownloader downloader;
				final InputStream is;
				
				final LazyProgressDialog dialog = LazyProgressDialog.show(this, null, getString(R.string.downloading_image), true, true);
				try {
					is = getContentResolver().openInputStream(uri);
				} catch (FileNotFoundException e) {
					App.criticalErrorAlert(this, R.string.gallery_error, true);
					return;
				}

				downloader = new InputStreamDownloader(is, mPreview.getPictureName(), new FileDownloaderListener() {
					public void onDownLoadFinished(File file) {
						dialog.dismiss();
						if (file == null) {
							App.criticalErrorAlert(ViewClipImporter.this, R.string.gallery_error, true);
							return;
						}
						mPreview.setShareImage(file.getAbsolutePath(), false);
					}
				});

				dialog.setOnCancelListener(new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						downloader.cancel();
						finish();
					}
				});
				downloader.execute();
			}
			
		}
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
		if (mPreview != null) mPreview.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mPreview != null) mPreview.onPause();
	}

	@Override
	public void onStop(){
		super.onStop();
		if (mPreview != null) mPreview.onStop();
	}

	public void onPictureTaken(String s) {
	}

	public void onPickFromGallery() {
	}

	public void onSaveClip(Model session) {
		final LazyProgressDialog dialog = LazyProgressDialog.show(this, null, App.getInstance().getString(R.string.saving), true, false);
		dialog.show();

		long clipId = session.store();

		Intent viewClip = new Intent(this, ViewClip.class);
		viewClip.putExtra("id", clipId);
		viewClip.putExtra("new", clipId);
		startActivity(viewClip);
		dialog.dismiss();
		finish();
	}

}
