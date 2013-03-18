/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.audio;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

public abstract class AudioEncoderBase {
	public static enum State {
		INITIALIZING, READY, RECORDING, ERROR, STOPPED
	};
	protected String fPath = null;

	protected State state;

	protected short nChannels;
	final protected int mSampleRate = 16000;
	final protected short mBitsPerSample = 16;
	protected int mBufferSize;
	protected int mAudioSource;
	final protected int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
	protected int mUserQuality;
	protected int mChannelsConfiguration; // AudioFormat.CHANNEL_IN_MONO...

	abstract String getDefaultExtension();
	abstract Boolean onSetOutputfile(String filaname);
	abstract Boolean onPrepare();
	abstract Boolean onRelease();
	abstract Boolean onReset();
	abstract Boolean onStart() throws Exception;
	abstract Boolean onStop();

	public AudioEncoderBase(int chanConfig, int userConfig) {
		mChannelsConfiguration = chanConfig; // AudioFormat.CHANNEL_IN_MONO
		mUserQuality = userConfig;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mAudioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
		} else {
			mAudioSource = MediaRecorder.AudioSource.DEFAULT;
		}

		if (mChannelsConfiguration == AudioFormat.CHANNEL_IN_MONO) {
			nChannels = 1;
		} else {
			nChannels = 2;
		}
		fPath = null;
		state = State.INITIALIZING;
	}

	public String getFileName() {
		return fPath;
	}

	public State getState() {
		return state;
	}

	public Boolean prepare() {
		try {
			if (state == State.INITIALIZING) {
				if (onPrepare()) {
					state = State.READY;
					return true;
				} else {
					Log.e(this.getClass().getName(), "prepare() method called on uninitialized recorder");
					state = State.ERROR;
				}
			} else {
				Log.e(this.getClass().getName(), "prepare() method called on illegal state");
				state = State.ERROR;
			}
			return false;
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "prepare() error: " + e.getMessage());
			state = State.ERROR;
		}
		release();
		return false;
	}

	public void release() {
		if (state == State.RECORDING) {
			stop();
		}
		onRelease();
		state = State.INITIALIZING;
	}

	public void reset() {
		try {
			if (state != State.ERROR) {
				fPath = null; // Reset file path
				onReset();
				state = State.INITIALIZING;
			}
		} catch (Exception e) {
			Log.e(this.getClass().getName(), e.getMessage());
			state = State.ERROR;
		}
	}

	public void start() {
		if (state == State.READY) {
			state = State.RECORDING;
			try {
				onStart();
			} catch (Exception e) {
				Log.e(this.getClass().getName(), "onStart() failed: " + e.getMessage());
				state = State.ERROR;
			}
		} else {
			Log.e(this.getClass().getName(), "start() called on illegal state");
			state = State.ERROR;
		}
	}

	public void stop() {
		if (state == State.RECORDING) {
			onStop();
			state = State.STOPPED;
		} else if (state != State.STOPPED) {
			Log.e(this.getClass().getName(), "stop() called on illegal state");
			state = State.ERROR;
		}
	}

	public void setOutputFile(String argPath) {
		try {
			if (state == State.INITIALIZING) {
				fPath = argPath + getDefaultExtension();
				onSetOutputfile(fPath);
			}
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "Error occured while setting output path: " + e.getMessage());
			state = State.ERROR;
		}
	}

}

