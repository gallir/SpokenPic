/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.google.gson.Gson;
import com.spokenpic.net.JsonData.ClipData;
import com.spokenpic.net.JsonData.ClipDataChangeStatus;
import com.spokenpic.net.JsonData.RestResult;
import com.spokenpic.net.RestClient;
import com.spokenpic.net.RestClient.RestClientListener;
import com.spokenpic.net.RestClientDelete;
import com.spokenpic.net.RestClientPut;
import com.spokenpic.utils.ClipPlayer;
import com.spokenpic.utils.ImageUtils;
import com.spokenpic.utils.LazyProgressDialog;

/**
 * @author gallir
 *
 */
public class ViewClip extends SherlockActivity {
	final int VOICE_RECOGNITION = 100;

	static final String TAG = "ViewImage";
	private ImageView mImageView;
	private MenuItem mWebButton;
	private MenuItem mUploadButton;
	private MenuItem mPrivateButton;
	private MenuItem mShareButton;
	private EditText mTitleEdit;
	private ImageView mSpeechButtton;
	private ImageView mCancelButtton;
	private ClipPlayer mPlayer;
	private Model mModel;
	private String imageFileName = null;
	private String audioFileName = null;
	private long mSessionId;
	private ShareActionProvider mShareActionProvider;
	private boolean mReturnToCreator = false;
	private String mPreviousTitle = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.view_image);

		//App.getInstance().loadPreferences();

		getSupportActionBar().setHomeButtonEnabled(true);
		//getSupportActionBar().setDisplayShowTitleEnabled(false);

		Bundle intentExtras = getIntent().getExtras();

		mImageView = (ImageView) findViewById(R.id.imageView);
		mImageView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mTitleEdit.hasFocus()) {
					mTitleEdit.clearFocus();
					// hide virtual keyboard
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mTitleEdit.getWindowToken(), 0);
				} else {
					if (mPlayer != null) {
						mPlayer.showHide();
					}
				}
			}
		});

		mTitleEdit = (EditText) findViewById(R.id.editTitle);
		mSpeechButtton = (ImageView) findViewById(R.id.micButton);
		mCancelButtton = (ImageView) findViewById(R.id.cancelButton);
		mCancelButtton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String tmp;
				tmp = mTitleEdit.getText().toString();
				if (mModel.mTitle != null && ! mModel.mTitle.equals(tmp)) {
					mTitleEdit.setText(mModel.mTitle);

				} else if (mPreviousTitle != null && ! mPreviousTitle.equals(tmp)){
					mTitleEdit.setText(mPreviousTitle);
				}
				mPreviousTitle = tmp;
			}
		});


		if (intentExtras != null) {
			mSessionId = intentExtras.getLong("id");
			if (mSessionId <= 0) {
				finish();
				return;
			}

			mModel = new Model(mSessionId);
			if (mModel.mId <= 0) { // Not found
				finish();
				return;
			}

			imageFileName = mModel.getImage();
			audioFileName = mModel.getAudio();

			if (imageFileName == null || ! (new File(imageFileName)).canRead()) {
				// Bad clip, delete it and finish
				mModel.delete();
				finish();
			}

			if (intentExtras.getLong("new") > 0) {
				// It a new clip, do a ping
				RestClient c = new RestClient("/ping/");
				c.backgroundConnect(null);
			}
			mReturnToCreator  = intentExtras.getBoolean("return_to_creator", false);
		}

		if (imageFileName != null) {
			// new LoadThumbnailTask().execute(mImageView, imageFileName, App.LARGE_IMAGE_SIZE);
			ImageUtils.getImageLoader().displayImage("file://"+imageFileName, mImageView, ImageUtils.getImageLoaderOptions(false, false));
		}


		mSpeechButtton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mTitleEdit.clearFocus();
				Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
				intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.a_short_title));
				intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
				try {
					startActivityForResult(intent, VOICE_RECOGNITION);
				} catch (ActivityNotFoundException e) {
					AlertDialog.Builder builder = new AlertDialog.Builder(ViewClip.this);
					builder.setTitle("Not available");
					builder.setMessage("No speech recognition installed. " + "Would you like to install it?");
					builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent marketIntent = new Intent(Intent.ACTION_VIEW);
							marketIntent.setData(Uri.parse("market://details?id=com.google.android.voicesearch"));
						}
					});
					builder.setNegativeButton(getString(R.string.cancel), null);
					builder.create().show();
				}
			}
		});

		mTitleEdit.setText(mModel.mTitle);

		mTitleEdit.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void afterTextChanged(Editable s) {
				mCancelButtton.setVisibility(View.VISIBLE);
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (audioFileName != null) {
			mPlayer = new ClipPlayer(this);
			mPlayer.setAnchorView(mImageView);
			mPlayer.setAudioSource(audioFileName);
			mPlayer.prepare();
		}

		if (mPreviousTitle == null || mModel.mTitle == null || mTitleEdit.getText().toString().equals(mModel.mTitle)) {
			mCancelButtton.setVisibility(View.INVISIBLE);
		} else {
			mCancelButtton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mPlayer != null) mPlayer.release();
		checkTitleChanged();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK && mReturnToCreator)) {
			// The creator was called from the launcher, call it the same way
			Intent intent = new Intent(ViewClip.this, ViewClipCreator.class);
			intent.setAction("android.intent.action.MAIN");
			startActivity(intent);
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNITION && resultCode == RESULT_OK) {
			ArrayList<String> results;
			results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			/*
			float[] confidence;
			String confidenceExtra = RecognizerIntent.EXTRA_CONFIDENCE_SCORES;
			confidence = data.getFloatArrayExtra(confidenceExtra);
			 */
			if (results.size() > 0) {
				mPreviousTitle = mTitleEdit.getText().toString();
				mTitleEdit.setText(results.get(0));
				invalidateOptionsMenu();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	void uploadClip() {
		mUploadButton.setEnabled(false);
		checkTitleChanged();

		if (App.getInstance().getKey() == null) {
			Toast toast = Toast.makeText(ViewClip.this, getString(R.string.must_authenticate), Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER | Gravity.TOP, 0, 20);
			toast.show();
			mUploadButton.setEnabled(true);
		} else {
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("background_upload", false) ) {
				Toast toast = Toast.makeText(ViewClip.this, ViewClip.this.getString(R.string.uploading_clip), Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.CENTER | Gravity.TOP, 0, 20);
				toast.show();
				Handler uploaderHandler = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						mUploadButton.setEnabled(true);
						if (msg.arg1 == 0) {
							Toast toast = Toast.makeText(ViewClip.this, ViewClip.this.getString(R.string.clip_uploaded), Toast.LENGTH_SHORT);
							toast.setGravity(Gravity.CENTER | Gravity.TOP, 0, 20);
							toast.show();
							mModel.read(mModel.mId, false);
							invalidateOptionsMenu();
						} else {
							Toast.makeText(ViewClip.this, ViewClip.this.getString(R.string.connection_error), Toast.LENGTH_LONG).show();
						}
					}
				};

				Intent intent = new Intent(this, ClipUploaderService.class);
				intent.putExtra("clip_id", mModel.mId);
				intent.putExtra(ClipUploaderService.EXTRA_MESSENGER, new Messenger(uploaderHandler));
				startService(intent);
				App.DEBUG("Uploading in background");
			} else {
				ProcessFiles task = new ProcessFiles(ViewClip.this);
				task.execute(mModel);
			}
		}
	}

	void showStatus() {
		if (mModel.status == Model.STATUS_PRIVATE) {
			mPrivateButton.setIcon(R.drawable.ab_secure);
		} else {
			mPrivateButton.setIcon(R.drawable.ab_not_secure);
		}
	}

	void changeStatus() {
		int newStatus;
		if (mModel.status != Model.STATUS_PRIVATE) {
			newStatus = Model.STATUS_PRIVATE;
		} else {
			newStatus = Model.STATUS_PUBLIC;
		}

		if (mModel.resourceUri == null || mModel.resourceUri.length() == 0) {
			mModel.status = newStatus;
			mModel.update();
			invalidateOptionsMenu();
			showStatus();
			return;
		}

		ClipDataChangeStatus statusData = new ClipDataChangeStatus();
		statusData.hash_key = mModel.hashKey;
		statusData.status = newStatus;
		RestClientPut client = new RestClientPut(mModel.resourceUri, statusData.toString());
		client.backgroundConnect(new RestClientListener() {
			public void onRestFinished(RestResult result) {
				if (result != null && result.ok) {
					ClipData data = new Gson().fromJson(result.payload, ClipData.class);
					mModel.status = data.status;
					mModel.update();
					invalidateOptionsMenu();
				}
			}
		});
	}

	void askToDelete() {
		AlertDialog alertDialog = new AlertDialog.Builder(ViewClip.this).create();
		alertDialog.setMessage(getString(R.string.ask_delete));
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				delete();
				// Leave few milliseconds to give time to delete from the db
				new Handler().postDelayed(
						new Runnable() {
							public void run() {
								try {
									finish();
								} catch (Exception e) {};
							}
						},
						200);
				return;
			}
		});

		alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				return;
			} });

		alertDialog.show();
	}

	void checkTitleChanged(){
		if (! mTitleEdit.getText().toString().equals(mModel.mTitle)) {
			mPreviousTitle = mModel.mTitle;
			mModel.mTitle = mTitleEdit.getText().toString();
			mModel.update();
		}
	}

	void delete() {
		mModel.delete();

		if (mModel.resourceUri != null && mModel.resourceUri.length() > 0
				&& PreferenceManager.getDefaultSharedPreferences(App.getInstance()).getBoolean("delete_uploaded", false)) {
			// Delete from the server
			RestClientDelete c = new RestClientDelete(mModel.resourceUri);
			c.backgroundConnect(new RestClientListener() {
				public void onRestFinished(RestResult r) {
					if (r == null || (!r.ok && r.httpCode != 404)) {
						if (r != null) {
							Log.d("Delete clip", "Failed connection: " + r.httpCode);
						}
						Toast.makeText(App.getInstance(), R.string.connection_error, Toast.LENGTH_LONG).show();
					}
				}
			});
		}
	}

	@Override
	public void onLowMemory() {
		App.getInstance().onLowMemory();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.viewclip, menu);

		mWebButton = menu.findItem(R.id.item_web);
		mUploadButton = menu.findItem(R.id.item_upload);
		mPrivateButton = menu.findItem(R.id.item_private);
		mShareButton = menu.findItem(R.id.item_share);

		setShareProviderIntent();
		if (mModel.url == null) {
			mWebButton.setVisible(false);
			if (mModel.isUploading()) {
				mUploadButton.setVisible(false);
			} else {
				mUploadButton.setVisible(true);
			}
			mShareButton.setVisible(false);
			//mPrivateButton.setVisible(false);
		} else {
			mWebButton.setVisible(true);
			mUploadButton.setVisible(false);
			mShareButton.setVisible(true);
			//mPrivateButton.setVisible(true);
		}

		showStatus();
		if (! App.getInstance().isLogged()) {
			menu.add(Menu.NONE, R.string.login, Menu.NONE, getString(R.string.login));
		}
		menu.add(Menu.NONE, R.string.preferences, Menu.NONE, getString(R.string.preferences));


		return true;
	}

	private void setShareProviderIntent() {
		mShareActionProvider = (ShareActionProvider) mShareButton.getActionProvider();
		//mShareActionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);

		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		String extraText = "";
		if (mTitleEdit.getText().toString().length() > 0) {
			extraText += mTitleEdit.getText().toString() + ": ";
		}
		extraText += mModel.getUrl();
		shareIntent.putExtra(Intent.EXTRA_TEXT, extraText);
		mShareActionProvider.setShareIntent(shareIntent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in action bar clicked; go home
			Intent intent = new Intent(this, ViewMain.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
			//case R.id.item_share:

		case R.id.item_private:
			changeStatus();
			return true;

		case R.id.item_upload:
			uploadClip();
			return true;

		case R.id.item_web:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mModel.getUrl()));
			startActivity(browserIntent);
			return true;

		case R.id.item_delete:
			askToDelete();
			return true;

		case R.string.login:
			startActivity(new Intent(this, ViewLogin.class));
			return true;

		case R.string.preferences:
			startActivity(new Intent(this, ViewPreferences.class));

		default:
			return super.onOptionsItemSelected(item);
		}
	}



	static class ProcessFiles extends AsyncTask<Model, Integer, Boolean> {
		LazyProgressDialog pd = null;
		ViewClip activity = null;

		public ProcessFiles(ViewClip activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			pd = LazyProgressDialog.show(activity, null, activity.getString(R.string.uploading_clip), true, true);
		}

		@Override
		protected Boolean doInBackground(Model... models) {
			return models[0].postToServer();
		}

		@Override
		protected void onPostExecute(final Boolean result) {
			pd.dismiss();
			if (activity != null && ! activity.isFinishing()) {
				activity.mUploadButton.setEnabled(true);
			}
			if (result) {
				Toast.makeText(activity, activity.getString(R.string.clip_uploaded), Toast.LENGTH_LONG).show();
				if (activity != null && ! activity.isFinishing()) {
					activity.invalidateOptionsMenu();
					/*
					activity.finish();
					activity.startActivity(activity.getIntent());
					 */
				}
			} else {
				Toast.makeText(activity, activity.getString(R.string.connection_error), Toast.LENGTH_LONG).show();
			}
		}
	}

}