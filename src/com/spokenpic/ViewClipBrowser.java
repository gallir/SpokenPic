/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.spokenpic.net.FileDownloader;
import com.spokenpic.net.FileDownloader.FileDownloaderListener;
import com.spokenpic.net.JsonData.ClipData;
import com.spokenpic.net.JsonData.FollowData;
import com.spokenpic.net.JsonData.RestResult;
import com.spokenpic.net.RestClient;
import com.spokenpic.net.RestClient.RestClientListener;
import com.spokenpic.net.RestClientDelete;
import com.spokenpic.net.RestClientPut;
import com.spokenpic.utils.ClipPlayer;
import com.spokenpic.utils.ImageUtils;

public class ViewClipBrowser extends SherlockActivity {

	static class PersistentData {
		String mBaseCache = null;
		File mAudioFile = null;
		File mImageFile = null;
		int mPendingElements = 0;
		ClipData mClipData = null;
	}

	ImageView mImageView;
	ClipPlayer mPlayer;
	PersistentData mState;
	TextView mAuthorView;
	TextView mDateView;
	TextView mTitleView;
	RelativeLayout mClipFrame;
	Button mFollowButton;
	ImageView mVote;

	Boolean mFollowing = false;
	Boolean mVoted = false;
	TextView mVotes;
	MenuItem mWebItem;
	MenuItem mShareItem;
	MenuItem mDeleteItem;
	MenuItem mPrivateButton;


	@Override
	public Object onRetainNonConfigurationInstance() {
		return mState;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		//requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		//getSupportActionBar().hide();

		setContentView(R.layout.view_clip_browser);
		//App.getInstance().loadPreferences();

		getSupportActionBar().setHomeButtonEnabled(true);

		mImageView = (ImageView) findViewById(R.id.image);
		mClipFrame = (RelativeLayout) findViewById(R.id.clipFrame);
		mFollowButton = (Button) findViewById(R.id.userFollow);
		mVote = (ImageView) findViewById(R.id.clipVote);
		mVotes = (TextView) findViewById(R.id.clipVotes);



		// They are shown after the image has been downloaded
		mClipFrame.setVisibility(View.INVISIBLE);
		mFollowButton.setVisibility(View.INVISIBLE);


		mImageView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (ViewClipBrowser.this.mPlayer != null) {
					ViewClipBrowser.this.mPlayer.show();
				}
			}
		});

		mFollowButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				followUnfollow();
			}
		});

		mAuthorView = (TextView) findViewById(R.id.authorView);
		mDateView = (TextView) findViewById(R.id.dateView);
		mDateView.setText(null);
		mTitleView = (TextView) findViewById(R.id.titleView);
		mTitleView.setText(null);

		//		mAuthorView.setVisibility(View.INVISIBLE);
		//		mDateView.setVisibility(View.INVISIBLE);

		mState = (PersistentData) getLastNonConfigurationInstance();
		if (mState == null) {
			mState = new PersistentData();
			mState.mBaseCache = App.getInstance().getMyCacheDir()+"/"+((App) getApplicationContext()).getCacheName();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		mPlayer = new ClipPlayer(this);
		mPlayer.setAnchorView(findViewById(R.id.browserClipLayout));

		if (mState.mClipData == null) {
			getClipData();
		} else {
			checkAndDownload();
		}
	}

	void getClipData() {
		// To get the data from the link
		Intent intent = getIntent();
		Uri intentData = intent.getData();


		String uri = null;
		long clip_id = 0;
		String clip_user = null;

		if (intent.getExtras() != null) {
			//uri = intent.getExtras().getString("resource_uri");
			clip_id = intent.getExtras().getLong("clip_id");
			clip_user = intent.getExtras().getString("username");
		}

		if (App.getInstance().isLogged()) {
			if (App.getInstance().getUsername().equals(clip_user)) {
				uri = "/api/v1/clip/";
			} else {
				uri = "/api/v1/auth/timeline/";
			}
		} else {
			uri = "/api/v1/timeline/";
		}


		if (clip_id > 0) {
			uri += clip_id + "/";
		} else {
			Pattern p = Pattern.compile(".*?/clip/([0-9]+/)(([^/]+/))*");
			Matcher m = p.matcher( intentData.getPath() );

			if(m.find()) {
				uri += m.group(1);
				if (m.groupCount() >= 3 && m.group(3) != null) {
					uri += m.group(3);
				}
			} else {
				Toast.makeText(this, R.string.bad_url, Toast.LENGTH_LONG).show();
				finish();
			}
		}

		setSupportProgressBarIndeterminateVisibility(true);
		//url = new URL(data.getScheme(), data.getHost(), data.getPath());
		RestClient client = new RestClient(uri);
		client.backgroundConnect(new RestClientListener() {
			public void onRestFinished(RestResult result) {
				if (result.ok) {
					mState.mClipData = new Gson().fromJson(result.payload, ClipData.class);
					checkAndDownload();
					invalidateOptionsMenu();
				} else {
					AlertDialog alertDialog = new AlertDialog.Builder(ViewClipBrowser.this).create();
					alertDialog.setMessage(getString(R.string.clip_not_found));
					alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						} 
					});
					alertDialog.show();
					setSupportProgressBarIndeterminateVisibility(false);
					App.DEBUG("Failed to get: "+ result.payload);
				}
			}
		});
	}

	void checkAndDownload() {
		if (mState.mClipData.user != null) {
			mAuthorView.setText(Html.fromHtml(String.format("<u>"+mState.mClipData.user+"</u>")));
			mAuthorView.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Intent timeline = new Intent(ViewClipBrowser.this, ViewTimeLineBrowser.class);
					timeline.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					timeline.putExtra("username", mState.mClipData.user);
					startActivity(timeline); 
					finish();
				}
			});
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS").parse(mState.mClipData.send_date);
				mDateView.setText(DateFormat.getDateInstance().format(date));
			} catch (Exception e) {
				App.DEBUG("Parse date error: " + e.getMessage());
			}

			if (mState.mClipData.title != null && mState.mClipData.title.length() > 0) {
				mTitleView.setText(mState.mClipData.title);
			}

		}
		if (mState.mClipData.image != null) {
			downloadImage(mState.mClipData.image);
		}
		if (mState.mClipData.sound != null) {
			downloadSound(mState.mClipData.sound);
		}
		checkReady();
	}

	public void checkReady () {
		if (mState.mPendingElements <= 0) {
			setSupportProgressBarIndeterminateVisibility(false);
			//getSupportActionBar().hide();
			showClip();
		}
	}

	void showClip() {
		mClipFrame.setVisibility(View.VISIBLE);
		if (! mState.mClipData.user.equals(App.getInstance().getUsername()) &&  mState.mClipData.user_id > 0) {
			getFollowStatus(mState.mClipData.user_id);
		}
		getVoteStatus();
		play();
	}


	void getFollowStatus (long id) {
		final String uri = "/api/v1/friend/" + id + "/";
		RestClient client = new RestClient(uri);
		client.backgroundConnect(new RestClientListener() {
			public void onRestFinished(RestResult result) {
				if (result.ok) {
					mFollowing = true;
				} else {
					mFollowing = false;
				}
				mFollowButton.setVisibility(View.VISIBLE);
				setFollowButtonText();
			}
		});
	}

	void followUnfollow() {
		long id = mState.mClipData.user_id;
		if (! mFollowing) {
			final String uri = "/api/v1/friend/";
			FollowData post = new FollowData();
			post.to_user = id;
			RestClient client = new RestClient(uri, post.toString());
			client.backgroundConnect(new RestClientListener() {
				public void onRestFinished(RestResult result) {
					if (! result.ok) {
						mFollowing = ! mFollowing;
						setFollowButtonText();
					}
				}
			});
		} else {
			final String uri = "/api/v1/friend/" + id + "/";
			RestClientDelete client = new RestClientDelete(uri);
			client.backgroundConnect(new RestClientListener() {
				public void onRestFinished(RestResult result) {
					if (! result.ok) {
						//App.DEBUG("failed to DELETE " + uri + ": " + result.payload);
						mFollowing = ! mFollowing;
						setFollowButtonText();
					}
				}
			});
		}

		mFollowing = ! mFollowing;
		setFollowButtonText();
	}

	void setFollowButtonText() {
		if (mFollowing) {
			mFollowButton.setText(getString(R.string.unfollow));
		} else {
			mFollowButton.setText(getString(R.string.follow));
		}
	}

	void getVoteStatus () {
		mVote.setVisibility(View.VISIBLE);
		if (App.getInstance().isLogged() && ! App.getInstance().getUsername().equals(mState.mClipData.user) ) {
			mVoted = mState.mClipData.has_voted;
			mVote.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (mVoted) return;
					mVoted = true;
					setVote();
					final String uri = "/api/v1/auth/timeline/vote/" + mState.mClipData.id + "/";
					RestClientPut client = new RestClientPut(uri, "{\"score\": 1}");
					client.backgroundConnect(new RestClientListener() {
						public void onRestFinished(RestResult result) {
							if (result.ok) {
								ClipData data = new Gson().fromJson(result.payload, ClipData.class);
								mState.mClipData.votes = data.votes;
							} else {
								mVoted = false;
							}
							setVote();
						}
					});
				}
			});

		} else {
			mVoted = false;
		}
		setVote();
	}

	void setVote() {
		mVotes.setText(Integer.toString(mState.mClipData.votes));
		if (mVoted) {
			mVote.setImageResource(R.drawable.plus_on);
		} else {
			mVote.setImageResource(R.drawable.plus);
		}
	}

	public void play() {
		if (mState.mAudioFile == null) return;
		mPlayer.setAudioSource(mState.mAudioFile.getAbsolutePath());
		mPlayer.prepare();
		//mPlayer.start();
	}

	void downloadImage(String uri) {
		try {
			ImageUtils.getImageLoader().displayImage(App.getFullUrl(uri), mImageView, ImageUtils.getImageLoaderOptions(false, true), new ImageLoadingListener() {
				public void onLoadingComplete() {
					mState.mPendingElements--;
					checkReady();
				}
				public void onLoadingCancelled() {
				}
				public void onLoadingFailed(FailReason arg0) {
				}
				public void onLoadingStarted() {
				}
			});
			mState.mPendingElements++;
		} catch (Exception e) {
			App.DEBUG("Browser failed to download " + e.getMessage());
		}
	}

	void downloadSound(String uri) {
		try {
			final String fileCache = mState.mBaseCache + "_sound";
			FileDownloader.download(new URL(App.getFullUrl(uri)), fileCache, new FileDownloaderListener() {
				public void onDownLoadFinished(File file) {
					mState.mPendingElements--;
					checkReady();
					if (file == null) {
						App.DEBUG("Browser Failed to download " + fileCache);
						return;
					}
					mState.mAudioFile = file;
				}
			});
			mState.mPendingElements++;
		} catch (Exception e) {
			App.DEBUG("Browser failed to download " + e.getMessage());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mPlayer.release();
	}

	void askToDelete() {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setMessage(getString(R.string.ask_delete));
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				delete();
			}
		});

		alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				return;
			} });

		alertDialog.show();
	}

	void delete() {
		if (mState.mClipData.id > 0) {
			// Delete from the server
			RestClientDelete c = new RestClientDelete("/api/v1/clip/"+mState.mClipData.id+"/");
			c.backgroundConnect(new RestClientListener() {
				public void onRestFinished(RestResult r) {
					if (r == null || !r.ok) {
						if (r != null) {
							Log.d("Delete clip", "Failed connection: " + r.httpCode);
						}
						AlertDialog alertDialog = new AlertDialog.Builder(ViewClipBrowser.this).create();
						alertDialog.setMessage(getString(R.string.connection_error));
						alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								return;
							} 
						});
						alertDialog.show();
					} else {
						AlertDialog alertDialog = new AlertDialog.Builder(ViewClipBrowser.this).create();
						alertDialog.setMessage(getString(R.string.clip_deleted));
						alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", (Message) null);
						alertDialog.show();
						mState.mClipData.id = 0L;
						invalidateOptionsMenu();
					}
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.view_clip_browser, menu);

		mWebItem = menu.findItem(R.id.item_web);
		mShareItem = menu.findItem(R.id.item_share);
		mDeleteItem = menu.findItem(R.id.item_delete);

		if (mState.mClipData != null && mState.mClipData.permalink != null) {
			mWebItem.setVisible(true);
			mShareItem.setVisible(true);
			ShareActionProvider share = (ShareActionProvider) mShareItem.getActionProvider();
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			String text = "";
			if (mState.mClipData.title != null && mState.mClipData.title.length() > 0) {
				text += mState.mClipData.title + " ";
			}
			text += App.getFullUrl(mState.mClipData.permalink);
			shareIntent.putExtra(Intent.EXTRA_TEXT, text);
			share.setShareIntent(shareIntent);
		} else {
			mWebItem.setVisible(false);
			mShareItem.setVisible(false);
		}

		if (App.getInstance().isLogged()
				&& mState.mClipData != null
				&& mState.mClipData.id > 0 
				&& App.getInstance().getUsername().equals(mState.mClipData.user)) {
			mDeleteItem.setVisible(true);

		} else {
			mDeleteItem.setVisible(false);
		}

		if (! App.getInstance().isLogged()) {
			menu.add(Menu.NONE, R.string.login, Menu.NONE, getString(R.string.login));
		}
		menu.add(Menu.NONE, R.string.preferences, Menu.NONE, getString(R.string.preferences));

		return true;
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

		case R.id.item_web:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(App.getFullUrl(mState.mClipData.permalink)));
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

}
