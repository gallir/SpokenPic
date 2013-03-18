/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.audio;

import java.io.IOException;
import java.nio.ShortBuffer;

import android.util.Log;

import com.ideaheap.io.VorbisFileOutputStream;
import com.ideaheap.io.VorbisInfo;

public class AudioEncoderVorbis extends AudioEncoderRecord {
	
	VorbisFileOutputStream fWriter;
	VorbisInfo mConfig;
	
	// TODO: add internal buffering
	ShortBuffer mBigBuffer;
	final int BUFFER_CAPACITY = 16536;

	public AudioEncoderVorbis(int chanConfig, int userConfig) {
		super(chanConfig, userConfig);
	}

	@Override
	Boolean prepareEncoder() throws IOException {
		mConfig = new VorbisInfo();
		mConfig.channels = nChannels;
		mConfig.sampleRate = mSampleRate;
		mConfig.coder = 1; // 0 = Use ABR, 1 = VBR
		switch (mUserQuality) {
		case 1:
			mConfig.quality = 0.0f;
			mConfig.bitRate = 22000;
			break;
		case 2:
			mConfig.quality = 0.2f;
			mConfig.bitRate = 26000;
			break;
		case 3:
			mConfig.quality = 0.4f;
			mConfig.bitRate = 32000;
			break;
		default:
			mConfig.quality = -0.1f;
			mConfig.bitRate = 20000;
		}
		return true;
	}

	@Override
	Boolean startRecording() throws IOException {
		fWriter = new VorbisFileOutputStream(fPath, mConfig);
		return true; // we already have the output stream
	}

	@Override
	Boolean writeBuffer(short[] buffer, int len) {
		try {
			fWriter.write(buffer, 0, len);
		} catch (IOException e) {
			try {
				fWriter.close();
			} catch (IOException e1) {}
			Log.e(this.getClass().getName(), "writeBuffer() failed: " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	Boolean finishEncoder() {
		return true; // we already closed the file
	}

	@Override
	String getDefaultExtension() {
		return ".ogg";
	}

	@Override
	Boolean onSetOutputfile(String filename) {
		return null;
	}


	@Override
	Boolean stopRecording() {
		try {
			fWriter.close();
		} catch (IOException e) {
			Log.e(this.getClass().getName(), "finishEncoder() failed: " + e.getMessage());
			return false;
		}
		return true;
	}

}
