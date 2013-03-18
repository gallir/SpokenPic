/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.spokenpic.App;
import com.spokenpic.R;

public class AudioRecorderPlayer {
	public static final String DEFAULT_AUDIO_FILE = App.getInstance().getMyCacheDir().getAbsolutePath() + "/audio";

	AudioEncoderBase mRecorder = null;
	MediaPlayer mPlayer = null;
	Boolean mRecordingAudio = false;


	private AudioRecorderPlayerListener mListener = null;
	private ImageView mRecordButton = null;
	private ImageView mPlayButton = null;
	private ProgressBar mAudioProgressBar = null;
	private TextView mAudioProgressCounter = null;

	public String mAudioFilename = null;

	public Boolean recordingAudio() {
		return mRecordingAudio;
	}

	public String audioFileName() {
		return mAudioFilename;
	}

	public void setRecordButton(ImageView mRecordButton) {
		this.mRecordButton = mRecordButton;
		mRecordButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onRecord();
			}
		});
	}

	public void setPlayButton(ImageView mPlayButton) {
		this.mPlayButton = mPlayButton;
		mPlayButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onPlay();
			}
		});
	}

	public void setAudioProgressBar(ProgressBar mAudioProgressBar) {
		this.mAudioProgressBar = mAudioProgressBar;
	}

	public void setAudioProgressCounter(TextView mAudioProgressCounter) {
		this.mAudioProgressCounter = mAudioProgressCounter;
	}

	public String getAudioFilename() {
		return mAudioFilename;
	}

	public void setAudioFilename(String mAudioFilename) {
		this.mAudioFilename = mAudioFilename;
	}


	public AudioRecorderPlayer(AudioRecorderPlayerListener listener, Boolean useVorbis, int audioFormat, int quality) {
		mListener = listener;
		if (useVorbis) {
			mRecorder = new AudioEncoderVorbis(AudioFormat.CHANNEL_IN_MONO, quality);
		} else {
			mRecorder = new AudioEncoderInternal(AudioFormat.CHANNEL_IN_MONO, quality);
		}
	}


	public void onPause() {
		try {
			if (mPlayer != null) {
				stopPlaying();
			}
		} catch (Exception e) {}

		try {
			if (mRecorder != null) {
				stopRecording();
				mRecorder.release();
			}
		} catch (Exception e) {}

	}

	public void onStop() {
		if (mRecorder != null) {
			mRecorder.release();
		}
		
		if (mPlayer != null) {
			mPlayer.release();
		}
	}

	void onDestroy() {
		mListener = null;
	}

	void onPlay() {
		if (mPlayer == null) {
			startPlaying();
		} else {
			stopPlaying();
		}
	}

	void onRecord() {
		if (!mRecordingAudio) {
			startRecording();
		} else {
			stopRecording();
		}
	}

	private void startPlaying() {
		if (mAudioFilename == null) return;
		mPlayer = new MediaPlayer();
		File input = new File(mAudioFilename);

		try {
			if (App.getInstance().isDebugBuild()) {
				long size = input.length();
				Toast.makeText(App.getInstance(), new File(mAudioFilename).getName() + " size: " + size + " quality: " + App.getInstance().getPreferences().getString("audio_quality", ""), Toast.LENGTH_LONG).show();
			}

			mPlayer.setDataSource(new FileInputStream(input).getFD());
			mPlayer.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer mp) {
					stopPlaying();
				}
			});
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException e) {
			App.DEBUG("Player prepare failed: " + e.getMessage() + " (" + mAudioFilename + ")");
			stopPlaying();
		}
		restoreControlButtons();
		mListener.onAudioStatusChanged(0);
	}

	private void stopPlaying() {
		try {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
			restoreControlButtons();
			mListener.onAudioStatusChanged(0);
		} catch (Exception r) {}
	}

	private void startRecording() {
		try {
			initRecorder();
			mRecorder.start();
			mRecordingAudio = true;
			if (mAudioProgressBar != null) {
				mAudioProgressBar.setVisibility(View.VISIBLE);
			}
			if (mAudioProgressCounter != null) {
				mAudioProgressCounter.setVisibility(View.VISIBLE);
			}
			//Toast.makeText(mActivity, mActivity.getString(R.string.speak_now), Toast.LENGTH_SHORT).show();
			mRecordProgress.execute(30000L);
		} catch (Exception e) {
			mRecordingAudio = false;
			if (mAudioProgressBar != null) {
				mAudioProgressBar.setVisibility(View.INVISIBLE);
			}
			if (mAudioProgressCounter != null) {
				mAudioProgressCounter.setText(null);
				//mAudioProgressCounter.setVisibility(View.INVISIBLE);
			}
			Toast.makeText(App.getInstance(), App.getInstance().getString(R.string.mediarecord_error), Toast.LENGTH_SHORT);
			//criticalErrorAlert(mActivity.getString(R.string.mediarecord_error), false);
			App.DEBUG("startRecording failed" + " reason: " + e.getMessage());
		}
		mListener.onAudioStatusChanged(0);
		restoreControlButtons();
	}

	private void stopRecording() {
		if (mRecordProgress != null) {
			mRecordProgress.cancel(true);
			if (mAudioProgressBar != null) {
				mAudioProgressBar.setVisibility(View.INVISIBLE);
			}
			if (mAudioProgressCounter != null) {
				mAudioProgressCounter.setText(null);
				//mAudioProgressCounter.setVisibility(View.INVISIBLE);
			}
		}
		try {
			if (mRecordingAudio) {
				mRecorder.stop();
			}
			mRecorder.reset();
		} catch (Exception e) {
			App.DEBUG("stopRecording failed: " + e.getMessage());
		}
		mRecordingAudio = false;
		mListener.onAudioStatusChanged(0);
		restoreControlButtons();
	}

	private AsyncTask<Long, Long, Void> mRecordProgress;
	
	private void initRecorder() throws Exception {
		mRecorder.setOutputFile(DEFAULT_AUDIO_FILE);
		mAudioFilename = mRecorder.getFileName();
		if (! mRecorder.prepare()) {
			throw new Exception("AudioRecord initialization failed");
		}

		if (mRecorder.getState() == AudioEncoderBase.State.ERROR) {
			mAudioFilename = null;
			Toast.makeText(App.getInstance(), App.getInstance().getString(R.string.mediarecord_error), Toast.LENGTH_LONG).show();
			throw new Exception("AudioRecord initialization failed");
		}

		// Todo: control the progress bar
		mRecordProgress = new AsyncTask<Long, Long, Void>() {
			private long time;
			private long duration;
			long passed = 0;

			@Override
			protected void onPreExecute() {
				time = System.currentTimeMillis();
				if (mAudioProgressBar != null) {
					mAudioProgressBar.setProgress(100);
				}
			}

			@Override
			protected Void doInBackground(Long... params) {
				duration = params[0];

				while (duration - passed > 0 && !isCancelled()) {
					passed = System.currentTimeMillis() - time;
					try {
						publishProgress(passed);
						Thread.sleep(200);
					} catch (InterruptedException e) {}
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Long... value) {
				try {
					long passed = value[0];
					if (mAudioProgressBar != null) {
						int progress = (int) (((float) (duration - passed) / (float) duration) * 100);
						mAudioProgressBar.setProgress(progress);
					}
					
					if (mAudioProgressCounter != null) {
						float remaining = (duration - passed) / 1000.0f;
						String time = String.format("%03.1f", remaining);
						mAudioProgressCounter.setText(time);
					}
				} catch (Exception e) {}
			}

			@Override
			protected void onPostExecute(Void result) {
				stopRecording();
			}
		};
	}

	Boolean wasRecording = false;
	Boolean wasPlaying = false;
	long previousAudioFileSize = 0;

	void playEnabledMonitor() {
		if (mAudioFilename != null && ! mRecordingAudio && mPlayer == null) {
			long fileSize = new File(mAudioFilename).length();
			if (fileSize > 0 && fileSize == previousAudioFileSize) {
				mPlayButton.setClickable(true);
				mPlayButton.setAlpha(255);
				previousAudioFileSize = 0;
				return;
			}
			previousAudioFileSize = fileSize;
		}
		if (! mRecordingAudio && mPlayer == null) {
			new Handler().postDelayed(new Runnable() {
				public void run() {
					playEnabledMonitor();
				}
			}, 200);
		}
	}

	public void restoreControlButtons() {
		final Resources r = App.getInstance().getResources();
		// Check recording status
		if (mRecordingAudio) {
			if (! wasRecording) {
				mRecordButton.setImageDrawable(r.getDrawable(R.drawable.stop_selector));
				mPlayButton.setClickable(false);
				mPlayButton.setAlpha(64);
				if (mAudioProgressBar != null) {
					mAudioProgressBar.setVisibility(View.VISIBLE);
				}
				if (mAudioProgressCounter != null) {
					mAudioProgressCounter.setVisibility(View.VISIBLE);
				}
				wasRecording = true;
			} else {
				mPlayButton.setClickable(false);
				mPlayButton.setAlpha(64);
			}
		} else {
			if (mAudioProgressBar != null) {
				mAudioProgressBar.setVisibility(View.INVISIBLE);
			}
			if (mAudioProgressCounter != null) {
				mAudioProgressCounter.setText(null);
				//mAudioProgressCounter.setVisibility(View.INVISIBLE);
			}
			if (wasRecording) {
				wasRecording = false;
				mRecordButton.setImageDrawable(r.getDrawable(R.drawable.mic_selector));
				mRecordButton.setAlpha(64);
				mRecordButton.setClickable(false);
				mPlayButton.setImageDrawable(r.getDrawable(R.drawable.play_selector));
				playEnabledMonitor();
				new Handler().postDelayed(new Runnable() {
					public void run() {
						if (! mRecordingAudio && mPlayer == null) {
							mRecordButton.setAlpha(255);
							mRecordButton.setClickable(true);
						}
					}
				}, 1000);
			}
		}

		if (mPlayer != null) { // It's playing
			if (! wasPlaying) {
				wasPlaying = true;
				mPlayButton.setImageDrawable(r.getDrawable(R.drawable.stop2_selector));
				mRecordButton.setClickable(false);
				mRecordButton.setAlpha(64);
			}
		} else {
			if (wasPlaying) {
				wasPlaying = false;
				mPlayButton.setImageDrawable(r.getDrawable(R.drawable.play_selector));
				new Handler().postDelayed(new Runnable() {
					public void run() {
						if (! mRecordingAudio && mPlayer == null) {
							mRecordButton.setClickable(true);
							mRecordButton.setAlpha(255);
						}
					};
				}, 250);
			} else if (mAudioFilename == null) {
				mPlayButton.setClickable(false);
				mPlayButton.setAlpha(64);
			}
		}
	}

	public void startAnimation(int previous, int current) {
		Animation animation = new RotateAnimation(previous, current,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		animation.setDuration(250);
		animation.setRepeatCount(0);
		animation.setFillAfter(true);
		animation.setFillEnabled(true);

		mRecordButton.startAnimation(animation);
		mPlayButton.startAnimation(animation);
	}
	
	public static interface AudioRecorderPlayerListener {
		public void onAudioStatusChanged(int stated);
	}
}
