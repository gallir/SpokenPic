/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

public class ClipUploaderService extends IntentService {
	public static final String EXTRA_MESSENGER="ClipUploaderService.EXTRA_MESSENGER";
	private long mId = 0;
	private Model mModel = null;
	Message mMess = null;

	public ClipUploaderService() {
		super("Clip uploader");
	}

	public ClipUploaderService(String name) {
		super(name);
	}


	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onLowMemory() {
		App.getInstance().onLowMemory();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		int result = 0;
		Bundle extras=intent.getExtras();

		if (extras != null) {
			mId = intent.getExtras().getLong("clip_id");
		}
		mModel = new Model(mId);
		if (mModel.mId <= 0 ) {
			result = 1;
		} else {
			if (! mModel.postToServer()) {
				result = 1;
				App.DEBUG("Error uploading clip " + mId);
			} else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}
		}


		if (extras!=null) {
			Messenger messenger=(Messenger)extras.get(EXTRA_MESSENGER);
			if (messenger != null) {
				Message msg=Message.obtain();
				msg.arg1=result;
				try {
					messenger.send(msg);
				} catch (android.os.RemoteException e1) {
					App.DEBUG(getClass().getName() +  " Exception sending message: " + e1);
				}
			}
		}
	}
}