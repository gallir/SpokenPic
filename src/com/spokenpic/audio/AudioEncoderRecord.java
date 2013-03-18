/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.audio;

import java.io.IOException;

import android.media.AudioRecord;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.spokenpic.App;

public abstract class AudioEncoderRecord extends AudioEncoderBase {
	final static int WORKER_QUIT = -1;
	final static int WORKER_RESET = 0;
	final static int WORKER_INIT_ENCODER = 1;
	final static int WORKER_START_REC = 2;
	final static int WORKER_READ = 3;
	final static int WORKER_STOP_REC = 4;
	final static int WORKER_RELEASE = 5;

	final static int TIME_INTERVAL = 100; // In milliseconds

	short mBuffer[];
	int mChunkSize;
	WorkerThread mThread = null;
	Handler mThreadHandler;
	protected AudioRecord mRecorder;

	abstract Boolean prepareEncoder() throws IOException;
	abstract Boolean startRecording() throws IOException;
	abstract Boolean writeBuffer(short buffer[], int len) throws IOException;
	abstract Boolean stopRecording();
	abstract Boolean finishEncoder();


	public AudioEncoderRecord(int chanConfig, int userConfig) {
		super(chanConfig, userConfig);
		int minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelsConfiguration, mAudioFormat);
		minBufferSize = minBufferSize/2; // We use short[]
		int elementsPerSecond = bytesPerSecond() / 2; // Short array, so 8 bits per byte, and 2 bytes
		mChunkSize = elementsPerSecond * TIME_INTERVAL / 1000;
		mBufferSize = Math.max(mChunkSize, minBufferSize);
		App.DEBUG("Buffer size: " + mBufferSize + " ChunkSize: " + mChunkSize + " MinBufferSize: " + minBufferSize + " Elements/s: " + elementsPerSecond);
		mBuffer = new short[mBufferSize];
		initWorker();
	}

	public int bytesPerSecond() {
		return nChannels * mSampleRate * mBitsPerSample / 8;
		
	}
	
	void initWorker() {
		try {
			if (mThread != null && mThread.isAlive()) return;
			mThread = new WorkerThread("encoder", this);
			//mThread.setPriority(Thread.NORM_PRIORITY - 2);
			mThread.setDaemon(true);
			mThread.start();
			mThreadHandler = mThread.getHandler();
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "onPrepare() error creating thread: " + e.getMessage());
			release();
		}
	}

	void sendMessageToWorker(int what) {
		try {
			mThreadHandler.sendEmptyMessage(what);
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "Error sending message to worker: " + e.getMessage());
		}
	}

	@Override
	Boolean onPrepare() {
		initWorker();
		try {
			mRecorder = new AudioRecord(mAudioSource, mSampleRate, mChannelsConfiguration, mAudioFormat, mBufferSize * 2); // We use short
			if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
				sendMessageToWorker(WORKER_RELEASE);
				mRecorder.release();
				mRecorder = null;
				throw new Exception("AudioRecord initialization failed");
			}
			sendMessageToWorker(WORKER_INIT_ENCODER);
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "onPrepare() error initializing recorder: " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	Boolean onRelease() {
		if (mThread != null && !mThread.isAlive()) {
			mThread = null;
		} else {
			sendMessageToWorker(WORKER_RELEASE);
		}
		return true;
	}

	@Override
	Boolean onReset() {
		sendMessageToWorker(WORKER_RESET);
		return null;
	}

	@Override
	Boolean onStart() {
		try {
			sendMessageToWorker(WORKER_START_REC);
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "onStart() error creating thread and intializing recorder: " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	Boolean onStop() {
		sendMessageToWorker(WORKER_STOP_REC);
		return true;
	}

	protected static class WorkerThread extends HandlerThread {

		private Handler mHandler;
		AudioEncoderRecord mEncoder;
		Message check;
		int lastOrder = 0;
		Boolean working = false;
		long startTime;
		long stopTime;
		long chunks;
		long bytes;
		Boolean mustRelease = false;


		Runnable readMore = new Runnable() {
			public void run() {
				mHandler.sendEmptyMessage(WORKER_READ);
			}
		};

		public WorkerThread(String name, AudioEncoderRecord encoder) {
			super(name);
			mEncoder = encoder;
		}

		public Handler getHandler() {
			return mHandler;
		}

		void stopIt() {
			readAndSendBuffer();
			/*
			stopTime = new java.util.Date().getTime();
			long expectedBytes = (stopTime - startTime) * bytesPerSecond() / 1000;

			while (bytes < expectedBytes) {
				readAndSendBuffer();
			}*/

			try {
				mEncoder.mRecorder.stop();
			} catch (Exception e) {
				Log.e("WORKER_STOP", "Error stoping audiorecord: " + e.getMessage());
			}
			mEncoder.stopRecording();
			working = false;

		}

		int readAndSendBuffer() {
			int r;
			try {
				r = mEncoder.mRecorder.read(mEncoder.mBuffer, 0, mEncoder.mChunkSize);
				if (r > 0) {
					chunks++;
					bytes += r*2; // We use shorts
					mEncoder.writeBuffer(mEncoder.mBuffer, r);
				}
			} catch (Exception e) {
				Log.e(this.getClass().getName(), "error calling writeBuffer: " + e.getMessage());
				return 0;
			}
			return r;
		}

		@Override
		public void start() {
			super.start();
			App.DEBUG("Thread started: " + getName());
			final Looper looper = getLooper();
			mHandler = new Handler(looper) {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case WORKER_QUIT:
						if (working) {
							stopIt();
						}
						break;
					case WORKER_RELEASE:
						if (working) {
							stopIt();
						}
						if (mEncoder.mRecorder != null) {
							mEncoder.mRecorder.release();
							mEncoder.mRecorder = null;
						}
						//looper.quit();
						break;
					case WORKER_RESET:
						if (working) {
							stopIt();
						}
						break;
					case WORKER_INIT_ENCODER:
						working = true;
						try {
							mEncoder.prepareEncoder();
						} catch (IOException e) {
							mEncoder.state = AudioEncoderBase.State.ERROR;
							Log.e(this.getClass().getName(), "initEncoder() error from worker thread");
						}
						break;
					case WORKER_START_REC:
						// startTime = new java.util.Date().getTime();
						try {
							mEncoder.mRecorder.startRecording();
							mEncoder.startRecording();
						} catch (Exception e) {
							Log.e("WORKER_STOP", "Error: " + e.getMessage());
							
						}
						chunks = 0;
						bytes = 0;
						mEncoder.mRecorder.read(mEncoder.mBuffer, 0, mEncoder.mChunkSize); // Discard first chunk
						working = true;
						break;
					case WORKER_STOP_REC:
						stopIt();
						break;
					case WORKER_READ:
						if (! working)
							return;
						if (readAndSendBuffer() <= 0) {
							working = false;
						} else {
							chunks++;
						}
						break;
					}
					lastOrder = msg.what;
					if (working) post(readMore);
				}
			};
		}
	}
}
