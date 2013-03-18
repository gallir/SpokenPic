/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.audio;

import android.media.MediaRecorder;
import android.util.Log;

public class AudioEncoderInternal extends AudioEncoderBase {
	private MediaRecorder mRecorder = null;

	public AudioEncoderInternal(int chanConfig, int userConfig) {
		super(chanConfig, userConfig);
		try {
			mRecorder = new MediaRecorder();

			setCompressUserConfig();

		} catch (Exception e) {
			Log.e(this.getClass().getName(), "Error occured while initializing recording" + e.getMessage());
			state = State.ERROR;
		}
	}

	@Override
	String getDefaultExtension() {
		return ".3gp";
	}

	@Override
	Boolean onPrepare() {
		try {
			mRecorder.prepare();
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "Error occured while preparing recording: " + e.getMessage());
			state = State.ERROR;
			return false;
		}
		state = State.READY;
		return true;
	}

	@Override
	Boolean onRelease() {
		if (mRecorder != null) {
			mRecorder.release();
			return true;
		}
		return false;
	}

	@Override
	Boolean onReset() {
		if (mRecorder != null) {
			mRecorder.release();
		}
		mRecorder = new MediaRecorder();
		setCompressUserConfig();
		return null;
	}

	@Override
	Boolean onStart() {
		mRecorder.start();
		return true;
	}

	@Override
	Boolean onStop() {
		mRecorder.stop();
		return true;
	}

	void setCompressUserConfig() {
		mRecorder.setAudioSource(mAudioSource);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

		switch (mUserQuality) {
		case 1: // Default
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
			break;
		case 2: //Better
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
			mRecorder.setAudioSamplingRate(mSampleRate);
			mRecorder.setAudioEncodingBitRate(16);
			break;
		case 3: // High
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
			mRecorder.setAudioSamplingRate(mSampleRate);
			mRecorder.setAudioEncodingBitRate(24);
			break;
		default: //Very high
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		}
	}

	@Override
	Boolean onSetOutputfile(String filename) {
		mRecorder.setOutputFile(filename);
		return true;
	}

}
