/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.spokenpic.App;
import com.spokenpic.Model;
import com.spokenpic.R;
import com.spokenpic.audio.AudioRecorderPlayer;
import com.spokenpic.audio.AudioRecorderPlayer.AudioRecorderPlayerListener;

public class CameraPreview extends FrameLayout implements SurfaceHolder.Callback, AudioRecorderPlayerListener {
	public static final String CLIP_CREATOR_STATE = "clip_creator_state";

	protected static class MySize {
		int width, height;

		protected MySize(int w, int h) {
			this.width = w;
			this.height = h;
		}

		protected MySize() {
			width = 0;
			height = 0;
		}
	}

	static final Integer MAX_IMAGE_DIMENSION = 2800;
	static final Integer JPEG_QUALITY = 85;

	//	public static final String DEFAULT_PICTURE_FILE = App.getInstance().cacheDir.getAbsolutePath() + "/cameraPreviewPicture.jpg";


	private static final List<String> FLASH_MODES = Arrays.asList(new String[] { 
			Camera.Parameters.FLASH_MODE_AUTO,
			Camera.Parameters.FLASH_MODE_ON, 
			Camera.Parameters.FLASH_MODE_OFF 
	});

	private static final Integer[] FLASH_MODE_IMAGES = { R.drawable.flash_automatic_selector,
		R.drawable.flash_on_selector, R.drawable.flash_off_selector };


	private static final List<String> FOCUS_MODES = Arrays.asList(new String[] {
			Camera.Parameters.FOCUS_MODE_AUTO,
			Camera.Parameters.FOCUS_MODE_INFINITY,
			Camera.Parameters.FOCUS_MODE_FIXED,
			Camera.Parameters.FOCUS_MODE_MACRO 
	});

	private static final Integer[] FOCUS_MODE_IMAGES = { 
		R.drawable.focus_auto_selector,
		R.drawable.focus_infinity_selector,
		R.drawable.focus_fixed_selector,
		R.drawable.focus_macro_selector };

	Boolean mImageImported = false;

	private ImageView mSaveButton = null;
	private ImageView mGalleryButton = null;
	private ImageView mShutter = null;

	private Boolean mCameraProcessing = false;
	private Boolean mCameraReady = false;
	private Boolean mCameraEnabled = true;
	private Boolean mCameraEnabledDefault = true;
	private Boolean mCameraForbidden = false;


	public ImageView mThumbNail;
	ImageView mImageView;
	ImageView mCameraSwitch;
	ImageView mCameraFlash;
	ImageView mCameraFocus;

	RelativeLayout mainLayout;

	SurfaceView mSurfaceView;
	SurfaceHolder mHolder;

	//	protected SharedPreferences mSettings;
	private Boolean mIsPortrait = false;
	protected int mCurrentCamera = 0;
	protected int mLastOpenCamera = -1;
	protected MySize[] mPreviewSize;
	protected MySize[] mMaxPreviewSize;
	protected MySize[] mImageSize;
	protected MySize[] mMaxImageSize;

	protected MySize[] mActivePreview;
	protected MySize[] mActiveImage;

	int mCameras;
	int mCurrentFlashMode = 0;
	int mCurrentFocusMode = 0;
	Boolean mUsePanoramic = false;
	Boolean mHighBright = false;

	public Camera mCamera;
	public AudioRecorderPlayer mRecorderPlayer = null;

	//MediaPlayer mPlayer = null;
	Activity mActivity;

	Boolean mSurfaceLayoutDone = false;

	protected Boolean surfaceChanged = false;
	protected int mDisplayRotation = 0;
	protected int mPhoneRotation = -1;
	protected int mImageRotation = 0;
	public Boolean mPreviewing = false;
	public String mPictureFileName = null;
	String mLastAudioFilename = null;
	Boolean mPictureTaken = false;
	private OrientationEventListener mOrientationCallback = null;

	Camera.Parameters mCameraParameters = null;
	private boolean mSurfaceAvailable;

	public String getPictureName() {
		return App.getInstance().getMyCacheDir().getAbsolutePath() + "/preview_" + App.getInstance().getCacheSequence() + ".jpg";
	}

	public CameraPreview(Context activity) {
		super(activity);
		mActivity = (Activity) activity;
		init();
	}

	public CameraPreview(Context activity, AttributeSet attrs) {
		super(activity, attrs);
		mActivity = (Activity) activity;
		init();
	}

	public CameraPreview(Context activity, AttributeSet attrs, int defaultStyle) {
		super(activity, attrs, defaultStyle);
		mActivity = (Activity) activity;
		init();
	}

	protected void init() {
		LayoutInflater li;

		mCameras = Math.min(Camera.getNumberOfCameras(), 2); // Don't allow more than 2 cameras, it fails with HTC Evo 3D

		li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.camera, this, true);


		mShutter = (ImageView) findViewById(R.id.shutterButton);

		mSaveButton = (ImageView) findViewById(R.id.saveButton);
		mGalleryButton = (ImageView) findViewById(R.id.galleryButton);

		mainLayout = (RelativeLayout) findViewById(R.id.cameraPreviewMain);

		mImageView = (ImageView) findViewById(R.id.imageView);
		mSurfaceView = (SurfaceView) findViewById(R.id.cameraSurface);
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mThumbNail = (ImageView) findViewById(R.id.cameraThumb);
		mThumbNail.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				((ImageView) v).setImageResource(android.R.color.transparent);
			}
		});

		mCameraSwitch = (ImageView) findViewById(R.id.cameraSwitch);
		mCameraFlash = (ImageView) findViewById(R.id.cameraFlash);
		mCameraFocus = (ImageView) findViewById(R.id.cameraFocus);

		mCameraSwitch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (! mCameraReady || mCamera == null) return;
				mCurrentCamera = (mCurrentCamera + 1) % mCameras;
				mSurfaceLayoutDone = false;
				setCamera();
				restoreControlButtons();
			}
		});

		mCameraFlash.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mCamera != null && mCameraReady) {
					setFlash(mCurrentFlashMode + 1);
					mCamera.setParameters(mCameraParameters);
				}
			}
		});


		// TODO: camera focus
		mCameraFocus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mCamera != null && mCameraReady) {
					setFocus(mCurrentFocusMode + 1);
					mCamera.setParameters(mCameraParameters);
				}
			}
		});


		mShutter.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mCamera == null && mImageView.getVisibility() == View.VISIBLE) {
					returnToCamera(); // Because an image was imported before
				} else if (mPreviewing) {
					takePicture();
				} else if (mCameraReady){
					startPreview();
				}
			}
		});

		mGalleryButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				((CameraPreviewCallback) mActivity).onPickFromGallery();
			}
		});

		mSaveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onSaveClip();
			}
		});

		//reset();
	}

	public void onResume() {
		App.DEBUG("onResume");

		if (mCameras < 1) {
			mCameraEnabled = false;
		}

		if (mCameraForbidden) {
			forbidCamera();
		}
		mCameraReady = false;
		mCameraProcessing = false;

		if (! mCameraForbidden) {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mIsPortrait = false;
			} else {
				mIsPortrait = true;
			}

			// Restore previous state and thumbnail
			if (! mCameraEnabled && mPictureTaken && mPictureFileName != null && mImageView.getVisibility() != View.VISIBLE) {
				App.DEBUG("onResume: calling setImage");
				setImage(mPictureFileName, mImageImported);
			} else {
				// Camera was not released
				if (mCamera != null ) {
					// App.DEBUG("Restart preview from onResume");
					startPreview();
				} else if (mSurfaceAvailable) {
					// App.DEBUG("setCamera() from onResume");
					setCamera();
				}
				if (mPictureTaken && mPictureFileName != null) {
					setTopThumb();
				}
			}
			readPreferences();
		}

		if (mRecorderPlayer == null) {
			post(new Runnable() {
				public void run() {
					int quality = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mActivity).getString("audio_quality", "0"));
					/* NOTE: Use vorbis always
					Boolean useVorbis = PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean("use_vorbis", true);
					mRecorderPlayer = new AudioRecorderPlayer(CameraPreview.this, useVorbis, AudioFormat.CHANNEL_IN_MONO, quality);
					*/
					mRecorderPlayer = new AudioRecorderPlayer(CameraPreview.this, true, AudioFormat.CHANNEL_IN_MONO, quality);
					mRecorderPlayer.setPlayButton((ImageView) findViewById(R.id.playButton));
					mRecorderPlayer.setRecordButton((ImageView) findViewById(R.id.recordButton));
					mRecorderPlayer.setAudioProgressCounter((TextView) findViewById(R.id.progressCounter));
					if (mLastAudioFilename != null) {
						mRecorderPlayer.setAudioFilename(mLastAudioFilename);
					}
					mRecorderPlayer.restoreControlButtons();
				}
			});
		}

		setOrientationCallback();
		restoreControlButtons();

		if (Build.VERSION.SDK_INT >= 11) {
			// Hide soft keys
			setSystemUiVisibility(SYSTEM_UI_FLAG_LOW_PROFILE);
		}

	}

	public void onPause() {
		App.DEBUG("onPause");
		stopPreview();
		//getHandler().removeCallbacksAndMessages(null);
		//releaseCamera();
		if (mRecorderPlayer != null) {
			mRecorderPlayer.onPause();
		}
		if (mOrientationCallback != null) {
			mOrientationCallback.disable();
			mOrientationCallback = null;
		}
	}

	public void onStop() {
		App.DEBUG("onStop");
		releaseCamera();
		storePreferences();
		if (mRecorderPlayer != null) {
			mRecorderPlayer.onStop();
		}
	}

	protected void readPreferences() {
		if (mSurfaceView.getVisibility() == GONE || mCameras < 1) return;

		//mSettings = mActivity.getSharedPreferences(PREFS_NAME, 0);
		String versionName = getVersionName();
		SharedPreferences preferences = App.getInstance().getPreferences();
		String storedVersionName = preferences.getString("versionName", "");
		mUsePanoramic = preferences.getBoolean("camera_panoramic", false);
		mHighBright = preferences.getBoolean("high_bright", true);

		// AppPreferences.DEBUG("Init cameras, panoramic: " + mUsePanoramic);


		if (mCameras < 2) {
			mCameraSwitch.setVisibility(INVISIBLE);
		}

		mCurrentCamera = preferences.getInt("currentCamera", 0);
		mCurrentFlashMode = preferences.getInt("currentFlashMode", 0);
		mCurrentFocusMode = preferences.getInt("currentFocusMode", 0);

		mPreviewSize = new MySize[mCameras];
		mMaxPreviewSize = new MySize[mCameras];
		mImageSize = new MySize[mCameras];
		mMaxImageSize = new MySize[mCameras];

		if (versionName != null && storedVersionName != null && versionName.equals(storedVersionName)) {
			String orientation_key;
			if (mIsPortrait)
				orientation_key = "portrait";
			else
				orientation_key = "landscape";

			for (int c = 0; c < mCameras; c++) {
				mPreviewSize[c] = new MySize(preferences.getInt("cameraPreviewSizeW_" + orientation_key
						+ "_" + c, 0), preferences.getInt("cameraPreviewSizeH_" + orientation_key + "_"
								+ c, 0));
				mMaxPreviewSize[c] = new MySize(preferences.getInt("cameraMaxPreviewSizeW_"
						+ orientation_key + "_" + c, 0), preferences.getInt("cameraMaxPreviewSizeH_"
								+ orientation_key + "_" + c, 0));

				mImageSize[c] = new MySize(preferences.getInt("cameraImageSizeW_" + orientation_key
						+ "_" + c, 0), preferences.getInt("cameraImageSizeH_" + orientation_key + "_"
								+ c, 0));
				mMaxImageSize[c] = new MySize(preferences.getInt("cameraMaxImageSizeW_"
						+ orientation_key + "_" + c, 0), preferences.getInt("cameraMaxImageSizeH_"
								+ orientation_key + "_" + c, 0));
			}
		} else {
			App.DEBUG("Ignoring previous cameras' sizes " + versionName + "/" + storedVersionName);
			for (int c = 0; c < mCameras; c++) {
				mPreviewSize[c] = new MySize();
				mImageSize[c] = new MySize();

				mMaxPreviewSize[c] = new MySize();
				mMaxImageSize[c] = new MySize();
			}
		}

		if (mUsePanoramic) {
			mActivePreview = mMaxPreviewSize;
			mActiveImage = mMaxImageSize;
		} else {
			mActivePreview = mPreviewSize;
			mActiveImage = mImageSize;
		}
	}

	protected void storePreferences() {
		if (mSurfaceView.getVisibility() == GONE || mCameras < 1) return;

		SharedPreferences.Editor editor = App.getInstance().getPreferences().edit();
		editor.putInt("currentCamera", mCurrentCamera);
		editor.putInt("currentFlashMode", mCurrentFlashMode);
		editor.putInt("currentFocusMode", mCurrentFocusMode);
		editor.putString("versionName", getVersionName());

		String orientation;
		if (mIsPortrait)
			orientation = "portrait";
		else
			orientation = "landscape";

		for (int c = 0; c < mCameras; c++) {
			editor.putInt("cameraPreviewSizeW_" + orientation + "_" + c, mPreviewSize[c].width);
			editor.putInt("cameraPreviewSizeH_" + orientation + "_" + c, mPreviewSize[c].height);
			editor.putInt("cameraMaxPreviewSizeW_" + orientation + "_" + c, mMaxPreviewSize[c].width);
			editor.putInt("cameraMaxPreviewSizeH_" + orientation + "_" + c, mMaxPreviewSize[c].height);

			editor.putInt("cameraImageSizeW_" + orientation + "_" + c, mImageSize[c].width);
			editor.putInt("cameraImageSizeH_" + orientation + "_" + c, mImageSize[c].height);
			editor.putInt("cameraMaxImageSizeW_" + orientation + "_" + c, mMaxImageSize[c].width);
			editor.putInt("cameraMaxImageSizeH_" + orientation + "_" + c, mMaxImageSize[c].height);
		}
		editor.commit();
	}

	public void startPreview() {
		if (mCameraForbidden || mCameras < 1) return;

		try {
			mCamera.startPreview();
			mPreviewing = true;
			mCameraReady = true;
		} catch (Exception e) {
			App.DEBUG("Error in startPreview: " + e.getMessage());
			App.criticalErrorAlert(mActivity, R.string.camera_init_error, true);
			return;
		}

		if (mHighBright) {
			// Increase brightness
			post(new Runnable() {
				public void run() {
					try {
						WindowManager.LayoutParams layout = mActivity.getWindow().getAttributes();
						layout.screenBrightness = 1F;
						mActivity.getWindow().setAttributes(layout);
					} catch (Exception e) {};
				}
			});
		}
		restoreControlButtons();
	}

	public void stopPreview() {
		if (mCamera != null && mPreviewing) {
			mCamera.stopPreview();
		}
		mPreviewing = false;
		//restoreControlButtons();
	}

	protected Boolean getPreviewAndImageSize(int w, int h) {
		if (w == 0 || h == 0)
			return false; // We have no size to compare

		if (mPreviewSize[mCurrentCamera] != null && mPreviewSize[mCurrentCamera].width > 0
				&& mPreviewSize[mCurrentCamera].height > 0) {
			return true; // It is already calculated
		}

		Camera camera;

		if (mCamera == null) {
			camera = openCamera(mCurrentCamera);
		} else {
			camera = mCamera;
		}

		try {
			Camera.Parameters parameters = camera.getParameters();

			List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
			List<Camera.Size> supportedSizes = parameters.getSupportedPictureSizes();

			Set<Long> supportedAspects = new HashSet<Long>();
			for (Camera.Size s : supportedSizes) {
				supportedAspects.add(Math.round(((double) s.width / s.height) * 100));
				//AppPreferences.DEBUG("Aspect: adding " +  Math.round(((double) s.width / s.height) * 100));
			}

			getOptimalPreviewSize(previewSizes, w, h, supportedAspects);
			// AppPreferences.DEBUG("Preview Size: " + mPreviewSize[mCurrentCamera].width + "/" + mPreviewSize[mCurrentCamera].height);
			// AppPreferences.DEBUG("Max preview Size: " + mMaxPreviewSize[mCurrentCamera].width + "/" + mMaxPreviewSize[mCurrentCamera].height);

			if (mImageSize[mCurrentCamera].width == 0 || mImageSize[mCurrentCamera].height == 0) {
				Camera.Size s = getOptimalImageSize(supportedSizes, mPreviewSize[mCurrentCamera]);
				mImageSize[mCurrentCamera].width = s.width;
				mImageSize[mCurrentCamera].height = s.height;
				// AppPreferences.DEBUG("Image Sizes: " + mImageSize[mCurrentCamera].width + "/" + mImageSize[mCurrentCamera].height);
			}
			if (mMaxImageSize[mCurrentCamera].width == 0 || mMaxImageSize[mCurrentCamera].height == 0) {
				Camera.Size s = getOptimalImageSize(supportedSizes, mMaxPreviewSize[mCurrentCamera]);
				mMaxImageSize[mCurrentCamera].width = s.width;
				mMaxImageSize[mCurrentCamera].height = s.height;
				// AppPreferences.DEBUG("Max image size: " + mMaxImageSize[mCurrentCamera].width + "/" + mMaxImageSize[mCurrentCamera].height);
			}
		} catch (Exception e) {
			App.DEBUG("Camera Error: "+ e.getMessage());
			if (camera != null) {
				camera.release();
			}
			App.criticalErrorAlert(mActivity, R.string.camera_init_error, true);
			return false;
		}
		if (mCamera == null)
			camera.release(); // mCamera was not initialized, release our own
		return true;
	}

	protected MySize getRotatedSize(MySize s) {
		if (mIsPortrait) {
			MySize r = new MySize(s.height, s.width);
			return r;
		}
		return s;

	}

	Camera openCamera(int which) {
		if (mCamera != null && which == mLastOpenCamera) {
			App.DEBUG("openCamera(), same camera: " + which);
			return mCamera;
		}

		Camera camera;
		
		int tries = 0;
		while (tries < 3) {
			try {
				camera = Camera.open(which);
				mLastOpenCamera = which;
				return camera;
			} catch (RuntimeException e) {
				App.DEBUG("openCamera error: " + e.getMessage());
			}
			tries++;
			try {
				Thread.sleep(200);
			} catch (Exception e) {}
		}
		return null;
	}

	void releaseCamera() {
		App.DEBUG("releaseCamera");
		try {
			if (mCamera != null) {
				stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.release();
			}
		} catch (Exception e) {
			Log.d("Camera", "Error releasing: " + e.getMessage());
		}
		mCamera = null;
	}

	public void setCamera() {
		App.DEBUG("setCamera()");
		if (mCameras < 1) return;
		if (mCamera != null && mLastOpenCamera != mCurrentCamera) {
			releaseCamera();
		}

		mCameraReady = false;
		new AsyncTask<Integer, Void, Camera>() {
			int currentCamera = 0;

			@Override
			protected Camera doInBackground(Integer... params) {
				currentCamera = params[0];
				return openCamera(params[0]);
			}

			@Override
			protected void onPostExecute(Camera camera) {
				if (camera == null) {
					App.criticalErrorAlert(mActivity, R.string.camera_error, true);
				}
				mCurrentCamera = currentCamera;
				mCamera = camera;
				try {
					mCamera.setPreviewDisplay(mHolder);
				} catch (Exception exception) {
					App.DEBUG("Exception caused by setPreviewDisplay: " + exception.getMessage());
					return;
				}

				if (!mSurfaceLayoutDone) {
					getPreviewAndImageSize(getWidth(), getHeight());
					requestLayout();
				}
				try {
					setCameraParameters();
				} catch (RuntimeException e) {
					App.DEBUG("Exception caused by setCameraParameters: " + e.getMessage());
					return;
				}
				mCameraReady = true;
				mCameraEnabled = true;
				mPhoneRotation = -1; // Force to recalculate rotation
				startPreview();
			}
		}.execute(mCurrentCamera);
	}

	protected void setCameraParameters() {
		CameraInfo info = new android.hardware.Camera.CameraInfo();

		Camera.getCameraInfo(mCurrentCamera, info);
		int previewRotation;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			previewRotation = (info.orientation + mDisplayRotation) % 360;
			previewRotation = (360 - previewRotation) % 360; // compensate the mirror
			//mCameraRotation = (info.orientation + degrees) % 360;  // <- bad in examples (http://goo.gl/kmpQH)
		} else { // back-facing
			previewRotation = (info.orientation - mDisplayRotation + 360) % 360;
		}

		try {
			Method downPolymorphic = mCamera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });
			if (downPolymorphic != null)
				downPolymorphic.invoke(mCamera, new Object[] { previewRotation });
		} catch (Exception e1) {}

		// configure and set the camera parameters
		mCameraParameters = mCamera.getParameters();

		mCameraParameters.setPreviewSize(mActivePreview[mCurrentCamera].width, mActivePreview[mCurrentCamera].height);
		mCameraParameters.setPictureSize(mActiveImage[mCurrentCamera].width, mActiveImage[mCurrentCamera].height);

		mCameraParameters.setPictureFormat(ImageFormat.JPEG);
		mCameraParameters.setJpegQuality(JPEG_QUALITY);

		List<String> feature;
		setFlash(mCurrentFlashMode); // Use the last stored value
		setFocus(mCurrentFocusMode);

		feature = mCameraParameters.getSupportedSceneModes();
		if (feature != null && feature.contains(Camera.Parameters.SCENE_MODE_AUTO))
			mCameraParameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
		feature = mCameraParameters.getSupportedSceneModes();
		if (feature != null && feature.contains(Camera.Parameters.WHITE_BALANCE_AUTO))
			mCameraParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);

		mCamera.setParameters(mCameraParameters);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (mSurfaceView.getVisibility() == View.GONE || mCameras < 1) return;

		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);

		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);

		if (mActivePreview[mCurrentCamera] == null || mActivePreview[mCurrentCamera].width == 0
				|| heightMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.AT_MOST) {
			getPreviewAndImageSize(width, height);
			MySize s = getRotatedSize(mPreviewSize[mCurrentCamera]);

			if (heightMode == MeasureSpec.AT_MOST) {
				height = (int) Math.max(s.height, Math.round(MeasureSpec.getSize(heightMeasureSpec) * 0.8)); // Don't let the button panels grow too big
			}
			if (widthMode == MeasureSpec.AT_MOST) {
				width = (int) Math.max(s.width, Math.round(MeasureSpec.getSize(widthMeasureSpec) * 0.8)); // Don't let the button panels grow too big
			}
		}
		setMeasuredDimension(width, height);
		// AppPreferences.DEBUG("OnMeasure: set preview size: " + width + "/" + height);
	}

	protected int resolveRecomendedSize(int spec) {
		int mode = MeasureSpec.getMode(spec);
		int size = MeasureSpec.getSize(spec);

		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else {
			return 1000;
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (changed) {
			super.onLayout(changed, l, t, r, b);
		}
		// if (mCameraForbiden) return;
		if (mSurfaceView.getVisibility() != View.GONE) {
			surfaceLayout(changed, l, t, r, b);
		}
	}

	protected void surfaceLayout(boolean changed, int l, int t, int r, int b) {
		//AppPreferences.DEBUG("surfaceLayout: " + mPreviewSize[mCurrentCamera].toString() + " LayoutDone: " + mSurfaceLayoutDone);
		if (mCameras < 1 || mPreviewSize[mCurrentCamera] == null || (mSurfaceLayoutDone && ! changed))
			return;
		final int width = r - l;
		final int height = b - t;

		MySize s = getRotatedSize(mActivePreview[mCurrentCamera]);
		// AppPreferences.DEBUG("Surface Layout: " + s.width + "/" + s.height);
		int left, top, bottom, right;

		try {
			if (width * s.height > height * s.width) {
				final int scaledChildWidth = s.width * height / s.height;
				left = (width - scaledChildWidth) / 2;
				top = 0;
				right = (width + scaledChildWidth) / 2;
				bottom = height;
			} else {
				// Center in height
				final int scaledChildHeight = s.height * width / s.width;
				left = 0;
				top = (height - scaledChildHeight) / 2;
				right = width;
				bottom = (height + scaledChildHeight) / 2;
			}
		} catch (ArithmeticException e){
			left = l;
			top = t;
			right = r;
			bottom = b;
		}

		mSurfaceView.layout(left, top, right, bottom);
		//mThumbNail.layout(0, 0, s.width / 4, s.height / 4);

		mSurfaceLayoutDone = true;
		//App.DEBUG("Layout done");
	}

	public void surfaceCreated(SurfaceHolder holder) {
		mSurfaceAvailable = true;
		App.DEBUG("Surface created");
		if (!isInEditMode() && mCameraEnabled) {
			setCamera();
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		App.DEBUG("Surface changed");
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		mSurfaceAvailable = false;
		App.DEBUG("Surface destroyed");
		if (mCamera != null && !isInEditMode()) {
			releaseCamera();
		}
	}

	private Boolean getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h, Set<Long> aspects) {
		if (mIsPortrait) {
			int tmp;
			tmp = w;
			w = h;
			h = tmp;
		}

		// AppPreferences.DEBUG("getOptimalPreviewSize  Measure: " + w + " " + h);

		int area, maxArea = 0, maxSubArea = 0;

		if (sizes == null) {
			App.DEBUG("PreviewSize Null value");
			return null;
		}

		Boolean found = false;
		// Try to find an size match aspect ratio and size
		for (Camera.Size s : sizes) {
			if (s.width > w || s.height > h)
				continue;
			area = s.width * s.height;
			Long aspect = Math.round(((double) s.width / s.height) * 100);
			//AppPreferences.DEBUG("Preview: " + s.width + "/" + s.height + " aspect " +  aspect);
			if (area > maxArea && aspects.contains(aspect) && aspect < 141 && aspect > 110) {
				maxArea = area;
				found = true;
				mPreviewSize[mCurrentCamera].width = s.width;
				mPreviewSize[mCurrentCamera].height = s.height;
				App.DEBUG("Optimal selected " + s.width + " " + s.height + " Aspect: " + aspect);
			}
			if (area > maxSubArea && aspects.contains(aspect)) {
				maxSubArea = area;
				mMaxPreviewSize[mCurrentCamera].width = s.width;
				mMaxPreviewSize[mCurrentCamera].height = s.height;
				App.DEBUG("Max preview selected " + s.width + " " + s.height + " Aspect: " + aspect);
			}
		}
		if (!found) {
			mPreviewSize[mCurrentCamera] = mMaxPreviewSize[mCurrentCamera];
		}
		return found;
	}

	private Camera.Size getOptimalImageSize(List<Camera.Size> sizes, MySize previewSize) {
		double ASPECT_TOLERANCE = 0.05;
		double targetRatio = (double) previewSize.width / previewSize.height;
		if (sizes == null)
			return null;

		Camera.Size optimalSize = null;

		int area, maxArea = 0;

		// Try to find an size match aspect ratio and size
		while (optimalSize == null) {
			for (Camera.Size size : sizes) {
				double ratio = (double) size.width / size.height;
				area = size.width * size.height;
				if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
					continue;
				if (size.height <= MAX_IMAGE_DIMENSION && size.width <= MAX_IMAGE_DIMENSION && area > maxArea) {
					maxArea = area;
					optimalSize = size;
				}
			}
			ASPECT_TOLERANCE *= 2;
		}
		return optimalSize;
	}

	public void takePicture() {
		if (mCameraProcessing || ! mPreviewing)
			return;
		mCameraProcessing = true;
		// AppPreferences.DEBUG("Take picture");
		// TODO: getFocusMode

		String mode = mCameraParameters.getFocusMode();
		if (mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || mode.equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
			mCamera.autoFocus(autofocusCallback);
		} else {
			mCamera.takePicture(null, null, pictureCallback);
		}
		restoreControlButtons();
	}

	Camera.AutoFocusCallback autofocusCallback = new Camera.AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera) {
			// AppPreferences.DEBUG("AutoFocus");
			mCamera.takePicture(null, null, pictureCallback);
		}
	};

	Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
		// called when the user takes a picture
		public void onPictureTaken(byte[] imageData, Camera c) {
			mPreviewing = false;
			// AppPreferences.DEBUG("PictureTaken");
			try {
				mPictureFileName = getPictureName();
				FileOutputStream outStream = new FileOutputStream(new File(mPictureFileName));
				outStream.write(imageData);
				outStream.flush();
				outStream.close();
				mPictureTaken = true;
				mImageImported = false;
				setTopThumb();
			} catch (Exception e) {
				Toast message = Toast.makeText(mActivity, "Error saving", Toast.LENGTH_SHORT);
				message.show();
			}

			postDelayed(new Runnable() {
				public void run() {
					try {
						mCameraProcessing = false;
						restoreControlButtons();
					} catch (Exception e) {
						mCameraProcessing = false;
					}
				}
			}, 200); // Milliseconds to allow a new photo

			// Restart preview in few seconds
			postDelayed(new Runnable() {
				public void run() {
					try {
						if (mCamera != null 
								// Make sure we are not paused or stopped
								&& mCameraReady
								&& mOrientationCallback != null
								&& mCameraEnabled
								&& mSurfaceAvailable) {
							startPreview();
						}
					} catch (Exception e) {
						//mPreviewing = true;
					}
				}
			}, 2500); // Milliseconds to restart
		}
	};

	public void setTopThumb() {
		// new LoadThumbnailTask().execute(mThumbNail, imageFileName, 200);
		// TODO: the camera needs to be released for changing the surface
		// A solution is to create another view on top
		//new ImageToSurface().execute(mHolder, imageFileName, 500);
		try {
			if (mPictureFileName != null) {
				File file = new File(mPictureFileName);
				if (file.exists()) {
					new FillThumbnail().execute(mThumbNail, VISIBLE, App.THUMB_IMAGE_SIZE, mPictureFileName, (360 - mPhoneRotation - mDisplayRotation) % 360);
				}
			}
		} catch (Exception e) {}
	}

	static class FillThumbnail extends AsyncTask<Object, Void, Bitmap> {
		ImageView view;
		Integer size;
		Integer rotation;
		Integer visible;

		@Override
		protected Bitmap doInBackground(Object... objects) {
			view = (ImageView) objects[0];
			visible = (Integer) objects[1];
			size = (Integer) objects[2];
			String fileName = (String) objects[3];
			rotation = (Integer) objects[4];
			return ImageUtils.rotate(ImageUtils.thumb(fileName, size), rotation);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			view.setImageBitmap(result);
			view.setVisibility(visible);
		}
	}

	private Boolean setFocus(int modeIndex) {
		// WARNING: 
		// Contract: The caller is responsible for calling mCamera.setParameters(mCameraParameters)
		int modesSize = FOCUS_MODES.size();
		modeIndex = modeIndex % modesSize;


		// Eliminate duplicated entries (seen in some cameras)
		Set<String> uniques =  new HashSet<String>();
		for (String f: mCameraParameters.getSupportedFocusModes() ) {
			// Do we allow this mode?
			if (FOCUS_MODES.contains(f)) {
				uniques.add(f);
			}
		}

		if (uniques.size() < 2) {
			mCameraFocus.setImageResource(android.R.color.transparent);
			mCameraFocus.setEnabled(false);
			return false;
		}


		int c = 0;
		while (!uniques.contains(FOCUS_MODES.get(modeIndex)) && c < modesSize) {
			modeIndex = (modeIndex + 1) % modesSize;
			c++;
		}

		if (c < modesSize) {
			mCurrentFocusMode = modeIndex;
			mCameraParameters.setFocusMode(FOCUS_MODES.get(mCurrentFocusMode));
			mCameraFocus.setImageResource(FOCUS_MODE_IMAGES[mCurrentFocusMode]);
			mCameraFocus.setEnabled(true);
			return true;
		}
		mCurrentFocusMode = 0;
		return false;
	}


	private Boolean setFlash(int modeIndex) {
		// WARNING: 
		// Contract: The caller is responsible for calling mCamera.setParameters(mCameraParameters)

		int modesSize = FLASH_MODES.size();
		List<String> features = mCameraParameters.getSupportedFlashModes();
		if (features == null || features.size() < 2) {
			mCameraFlash.setImageResource(android.R.color.transparent);
			return false;
		}
		;

		int c = 0;
		modeIndex = modeIndex % modesSize;
		while (!features.contains(FLASH_MODES.get(modeIndex)) && c < modesSize) {
			modeIndex = (modeIndex + 1) % modesSize;
			c++;
		}

		if (c < modesSize) {
			mCurrentFlashMode = modeIndex;
			mCameraParameters.setFlashMode(FLASH_MODES.get(mCurrentFlashMode));
			mCameraFlash.setImageResource(FLASH_MODE_IMAGES[mCurrentFlashMode]);
			return true;
		}
		return false;
	}

	private String getVersionName() {
		try {
			PackageInfo manager = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0);
			return manager.versionName;
		} catch (NameNotFoundException e1) {
			return null;
		}
	}


	public void restoreControlButtons() {
		if (mRecorderPlayer != null) {
			mRecorderPlayer.restoreControlButtons();
		}

		if (!mCameraProcessing && (mCameraReady || mImageView.getVisibility() == View.VISIBLE)) {
			mShutter.setClickable(true);
			mShutter.setAlpha(255);
		} else {
			mShutter.setClickable(false);
			mShutter.setAlpha(64);
		}

		restoreSaveButton();
	}

	public void restoreSaveButton() {
		if (mPictureTaken && !mCameraProcessing && (mRecorderPlayer == null || ! mRecorderPlayer.recordingAudio())) {
			mSaveButton.setClickable(true);
			mSaveButton.setAlpha(255);
		} else {
			mSaveButton.setClickable(false);
			mSaveButton.setAlpha(64);
		}
	}

	void setOrientationCallback() {

		int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
			mDisplayRotation = 0;
			break;
		case Surface.ROTATION_90:
			mDisplayRotation = 90;
			break;
		case Surface.ROTATION_180:
			mDisplayRotation = 180;
			break;
		case Surface.ROTATION_270:
			mDisplayRotation = 270;
			break;
		}

		if (mOrientationCallback == null) {
			mOrientationCallback = new OrientationEventListener(mActivity, SensorManager.SENSOR_DELAY_NORMAL) {
				// Example from Google http://goo.gl/EPKYy
				int animationPrevious = 0;
				int animationCurrent = 0;

				@Override
				public void onOrientationChanged(int orientation) {
					if (orientation == ORIENTATION_UNKNOWN || mCameraProcessing)
						return;
					orientation = ((orientation + 45) / 90 * 90) % 360;
					if (orientation == mPhoneRotation) {
						return;
					}
					mPhoneRotation = orientation;
					if (mCamera != null && mCameraReady) {
						if (mCameraParameters == null) {
							mCameraParameters = mCamera.getParameters();
						}
						android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
						android.hardware.Camera.getCameraInfo(mCurrentCamera, info);
						if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
							mImageRotation = (info.orientation - orientation + 360) % 360;
						} else { // back-facing camera
							mImageRotation = (info.orientation + orientation) % 360;
						}
						mCameraParameters.setRotation(mImageRotation);
						try {
							mCamera.setParameters(mCameraParameters);
						} catch (Exception e) {}
					}

					animationCurrent = (360 - mPhoneRotation - mDisplayRotation) % 360;
					if (animationPrevious == animationCurrent) {
						return;
					}

					if (Math.abs(animationCurrent - animationPrevious) > 180) {
						if (animationCurrent > animationPrevious) {
							animationCurrent -= 360;
						} else {
							animationCurrent += 360;
						}
					}

					Animation switchersAnimation = new RotateAnimation(animationPrevious, animationCurrent,
							Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
					switchersAnimation.setDuration(250);
					switchersAnimation.setRepeatCount(0);
					switchersAnimation.setFillAfter(true);
					switchersAnimation.setFillEnabled(true);
					if (mCameraFlash.getVisibility() == View.VISIBLE)
						mCameraFlash.startAnimation(switchersAnimation);
					if (mCameraSwitch.getVisibility() == View.VISIBLE)
						mCameraSwitch.startAnimation(switchersAnimation);
					if (mCameraFocus.getVisibility() == View.VISIBLE)
						mCameraFocus.startAnimation(switchersAnimation);

					Animation buttonsAnimation = new RotateAnimation(animationPrevious, animationCurrent,
							Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
					buttonsAnimation.setDuration(250);
					buttonsAnimation.setRepeatCount(0);
					buttonsAnimation.setFillAfter(true);
					buttonsAnimation.setFillEnabled(true);
					if (mRecorderPlayer != null) {
						mRecorderPlayer.startAnimation(animationPrevious, animationCurrent);
					}

					mSaveButton.startAnimation(buttonsAnimation);
					if (mGalleryButton.getVisibility() == View.VISIBLE)
						mGalleryButton.startAnimation(buttonsAnimation);

					Animation shutterAnimation = new RotateAnimation(animationPrevious, animationCurrent,
							Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
					shutterAnimation.setDuration(250);
					shutterAnimation.setRepeatCount(0);
					shutterAnimation.setFillAfter(true);
					shutterAnimation.setFillEnabled(true);
					if (mShutter.getVisibility() == View.VISIBLE) {
						mShutter.startAnimation(shutterAnimation);
					}
					animationPrevious = (360 + animationCurrent) % 360;
				}
			};
		}
		if (mOrientationCallback.canDetectOrientation()) {
			mOrientationCallback.enable();
		} else {
			Toast.makeText(mActivity, mActivity.getString(R.string.rotation_disabled), Toast.LENGTH_LONG).show();
		}
	}

	public Boolean isImageImported() {
		return mImageImported;
	}

	public void setImageDelayed() {
		// The image will be downloaded in background
		mCameraEnabledDefault = mCameraEnabled;
		mCameraEnabled = false;
	}

	public void cancelImageDelayed() {
		// The image will be downloaded in background
		if (mCameraEnabledDefault && ! mCameraEnabled) {
			returnToCamera();
		} else {
			mCameraEnabled = mCameraEnabledDefault;
		}
	}

	public void forbidCamera() {
		mShutter.setVisibility(INVISIBLE);
		mSurfaceView.setVisibility(GONE);
		mGalleryButton.setVisibility(INVISIBLE);
		mCameraForbidden = true;
	}

	public void setShareImage(String filename, Boolean imported) {
		forbidCamera();
		setImage(filename, imported);
	}

	public void setImage(String filename, Boolean isImported) {
		//App.DEBUG("setImage");
		// If isImported, the original file is never deleted

		// AppPreferences.DEBUG("setImage: " + filename);
		mCameraEnabled = false;
		mImageImported = isImported;
		// Stop and release the camera
		releaseCamera();

		mImageView.setVisibility(VISIBLE);
		if (mSurfaceView.getVisibility() == VISIBLE) {
			mSurfaceView.setVisibility(INVISIBLE);
		}
		mCameraSwitch.setVisibility(INVISIBLE);
		mCameraFlash.setVisibility(INVISIBLE);
		mCameraFocus.setVisibility(INVISIBLE);
		mPictureTaken = true;
		mPictureFileName = filename;

		new LoadThumbnailTask().execute(mImageView, mPictureFileName, App.LARGE_IMAGE_SIZE, App.getInstance().getMyCacheDir());
		restoreControlButtons();
	}

	public void returnToCamera() {
		mImageView.setVisibility(INVISIBLE);
		mImageView.setImageDrawable(null);
		mSurfaceView.setVisibility(VISIBLE);
		mCameraSwitch.setVisibility(VISIBLE);
		mCameraFlash.setVisibility(VISIBLE);
		mCameraFocus.setVisibility(VISIBLE);
		setCamera();
		setTopThumb();
	}

	/*
	public void reset() {
		mPictureTaken = mImageImported = mCameraForbidden = false;
		mRecorderPlayer = null;
		mThumbNail.setImageBitmap(null);
		mPictureFileName = null;
		mImageView.setVisibility(INVISIBLE);
		mSurfaceView.setVisibility(VISIBLE);

	}
	 */

	public ClipCreatorState getState() {
		ClipCreatorState state = new ClipCreatorState();
		state.imageCacheFileName = mPictureFileName;
		if (mRecorderPlayer != null) {
			state.audioCacheFileName = mRecorderPlayer.getAudioFilename();
		} else {
			state.audioCacheFileName = null;
		}
		state.imageCaptured = mPictureTaken;
		state.imageImported = mImageImported;
		state.viewCamera = mCameraEnabled;
		state.cameraForbidden = mCameraForbidden;
		return state;
	}

	public void restoreState(ClipCreatorState state){
		// AppPreferences.DEBUG("restoreState()" + state.imageCacheFileName + " " + state.imageCaptured);

		mPictureFileName = state.imageCacheFileName;

		if (mRecorderPlayer != null) {
			mRecorderPlayer.setAudioFilename(state.audioCacheFileName);
			mLastAudioFilename = null;
		} else {
			mLastAudioFilename = state.audioCacheFileName;
		}

		mPictureTaken = state.imageCaptured;
		mImageImported = state.imageImported;
		mCameraEnabled = state.viewCamera;
		mCameraForbidden = state.cameraForbidden;
	}

	public void onSaveClip() {
		Model session = new Model();

		if (mRecorderPlayer.getAudioFilename() != null) {
			session.add(0, Model.TYPE_AUDIO, mRecorderPlayer.getAudioFilename(), false);
			App.DEBUG("Adding audio: " + mRecorderPlayer.getAudioFilename());
		}
		session.add(0, Model.TYPE_IMAGE, mPictureFileName, isImageImported());
		App.DEBUG("Adding image: " + mPictureFileName);
		((CameraPreviewCallback) mActivity).onSaveClip(session);
	}

	public interface CameraPreviewCallback {
		public void onPictureTaken(String s);
		public void onSaveClip(Model session);
		public void onPickFromGallery();
	}

	public static class ClipCreatorState implements Serializable {
		public String imageCacheFileName = null;
		public String audioCacheFileName = null;
		public Boolean imageCaptured = false;
		public Boolean imageImported = false;
		public Boolean viewCamera = true;
		public Boolean cameraForbidden = true;
	}

	public void onAudioStatusChanged(int stated) {
		restoreSaveButton();
	}
}
