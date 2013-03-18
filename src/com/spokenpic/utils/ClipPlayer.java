/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.utils;

import java.io.FileInputStream;

import com.spokenpic.App;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;


public class ClipPlayer implements OnPreparedListener, MediaController.MediaPlayerControl{
	MediaPlayer mPlayer;
	MediaController mController;
	String audioFilename;
	Boolean mPrepared = false;
	
	public ClipPlayer(Activity activity) {
		mController = new MediaController(activity);
		mController.setMediaPlayer(this);

		mPlayer = new MediaPlayer();
		mPlayer.setOnPreparedListener(this);
	}

	public void setAudioSource(String audioFile) {
		
		try {
			FileInputStream fileInputStream = new FileInputStream(audioFile);
			mPlayer.setDataSource(fileInputStream.getFD());
			fileInputStream.close();
			audioFilename = audioFile;
		} catch (Exception e) {
			Log.d("ClipPlayer", "Error: " + e.getMessage());
		}
		
	}

	public void setAnchorView(View view) {
		mController.setAnchorView(view);
	}
	
	public void onPrepared(MediaPlayer mp) {
		mPrepared = true;
		Handler handler = new Handler();
		handler.post(new Runnable() {
			public void run() {
				show();
			}
		});
	}

	public void prepare() {
		try {
			mPlayer.prepare();
		} catch (Exception e) {
			Log.d("ClipPlayer", "Error: " + e.getMessage());
		}
	}

	public void start() {
		if (mPrepared) {
			mPlayer.start();
		}
	}

	public void seekTo(int pos) {
		if (mPrepared) {
			mPlayer.seekTo(pos);
		}
	}

	public void pause() {
		if (mPlayer.isPlaying()) {
			mPlayer.pause();
		}
	}

	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}

	public int getDuration() {
		return mPlayer.getDuration();
	}

	public int getCurrentPosition() {
		return mPlayer.getCurrentPosition();
	}

	public int getBufferPercentage() {
		return 0;
	}

	public boolean canSeekForward() {
		return false;
	}

	public boolean canSeekBackward() {
		return false;
	}

	public boolean canPause() {
		return true;
	}

	public void show() {
		if (mPrepared) {
			mController.setEnabled(true);
			mController.show();
		}
	}

	public void showHide() {
		App.DEBUG("showHide()");
		if (mPrepared && ! mController.isShowing()) {
			mController.show();
		} else {
			mController.hide();
		}
	}

	public void stop() {
		if (isPlaying()) {
			mPlayer.stop();
		}
	}
	public void reset() {
		mPlayer.reset();
	}
	
	public void release() {
		mPrepared = false;
		mPlayer.release();
		if (mController.isShowing()) {
			mController.hide();
		}
	}
}
