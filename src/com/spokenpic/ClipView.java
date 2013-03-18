/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.spokenpic.utils.ClipPlayer;

public class ClipView extends ImageView {
	ClipPlayer mPlayer = null;
	String audioUrl = null;
	String imageUrl = null;

	public ClipView(Context context) {
		super(context);
		init();
	}

	public ClipView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ClipView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	void init() {
		setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mPlayer != null) {
					mPlayer.show();
				}
			}
		});
	}

	/*
	void downloadImage(String uri) {
		if (mState.mImageFile != null) {
			showImage();
			return;
		}

		try {
			ImageUtils.getImageLoader().displayImage(App.getFullUrl(uri), mImageView, new ImageLoadingListener() {

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
		if (mState.mAudioFile != null) {
			play();
			return;
		}

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
					play();
				}
			});
			mState.mPendingElements++;
		} catch (Exception e) {
			App.DEBUG("Browser failed to download " + e.getMessage());
		}
	}


	public void loadAudio(String url) {
		
	}
	
	
	public void loadImage() {
		
	}
	
	*/
}
