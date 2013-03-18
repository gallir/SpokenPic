/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */


package com.spokenpic.utils;

import java.io.File;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * @author gallir
 *
 */
public class LoadThumbnailTask extends AsyncTask<Object, Object, Bitmap> {
	ImageView imageView;

	@Override
	protected Bitmap doInBackground(Object... params)
	{
			String fileName;
			int maxSize;
			File cacheDir = null;

			imageView = (ImageView) params[0];
			fileName = (String) params[1];
			maxSize = (Integer) params[2];
			if (params.length >= 4) {
				cacheDir = (File) params[3];
			}
			return ImageUtils.thumb(fileName, maxSize, cacheDir);

	}

	@Override
	protected void onPostExecute(Bitmap result)
	{
		super.onPostExecute(result);
		try {
			imageView.setImageBitmap(result);
		} catch (Exception e) {}
	}
}
