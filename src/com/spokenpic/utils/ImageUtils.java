/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.nostra13.universalimageloader.cache.disc.impl.FileCountLimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LRULimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.download.URLConnectionImageDownloader;
import com.spokenpic.App;

public class ImageUtils {
	static int sequence = 0;
	static public String scale(String inFileName, String outDirName, Integer maxSize) {
		String outFileName = null;

		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(inFileName, o);

		Integer max = Math.max(o.outHeight, o.outWidth);
		double scale = (double) max/maxSize;

		int sample = (int) Math.max(1,  Math.floor(scale)); 

		FileInputStream inFileStream;

		try {
			inFileStream = new FileInputStream(inFileName);
		} catch (FileNotFoundException e) {
			return null;
		}

		//Decode with inSampleSize
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize=sample;

		Bitmap bitmap;
		try {
			bitmap = BitmapFactory.decodeStream(inFileStream, null, o2);
		} catch (OutOfMemoryError e) {
			return null;
		}

		if (bitmap != null) {
			Bitmap scaledBitmap = null;
			if (scale > 1 && sample * 1.2d < scale) {
				App.DEBUG("Rescaling, scale, sample: " + scale + ", " + sample);
				int tries = 0;
				while (scaledBitmap == null && tries < 2) {
					try {
						scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) Math.round(o.outWidth/scale), (int) Math.round(o.outHeight/scale), true);
					} catch (OutOfMemoryError e) {
						scaledBitmap = null;
						if (tries < 1) {
							System.gc();
						} else {
							App.getInstance().onLowMemory();
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e1) {}
					}
					tries++;
				}

				if (scaledBitmap != null) {
					bitmap.recycle();
					bitmap = scaledBitmap;
				}
			}
			bitmap = rotateByExif(bitmap, inFileName);
			sequence = (sequence + 1) % 5;
			outFileName = outDirName + "/"+sequence+"_scaled.jpg";
			try {
				bitmap.compress(CompressFormat.JPEG, 70, new FileOutputStream(outFileName));
			} catch (FileNotFoundException e) {
				outFileName = null;
			} finally {
				bitmap.recycle();
			}
		}
		return outFileName;
	}
	static public Bitmap thumb(String fileName, int maxSize) {
		return thumb(fileName, maxSize, null);
	}

	static public Bitmap cachedThumb(String fileName, int maxSize, File cacheDir) {
		Bitmap bitmap;
		File thumbNail = null;

		File original = new File(fileName);
		try {
			thumbNail = new File(cacheDir, "thumb_"+maxSize+"_"+original.getName());
			bitmap = BitmapFactory.decodeFile(thumbNail.getAbsolutePath());
			if (bitmap != null) {
				return bitmap;
			}
		} catch (Exception e) {}
		return null;
	}

	static public Bitmap thumb(String fileName, int maxSize, File cacheDir) {
		File thumbNail = null;
		File original;
		Bitmap bitmap;

		if (fileName == null) {
			App.DEBUG("LoadThumbnail: null fileName");
			return null;
		}

		original = new File(fileName);

		bitmap = cachedThumb(fileName, maxSize, cacheDir);
		if (bitmap != null) {
			return bitmap;
		}

		try {
			if (cacheDir != null) {
				thumbNail = new File(cacheDir, "thumb_"+maxSize+"_"+original.getName());
			}

			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(fileName, o);

			Integer max = Math.max(o.outHeight, o.outWidth);
			double scale = (double) max/maxSize;

			int sample = (int) Math.max(1,  Math.floor(scale)); 

			//Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize=sample;
			// AppPreferences.DEBUG("Thumb scale " + fileName + " = " + scale + " w: " + o.outWidth + " h: "  + o.outHeight + " Max: " + maxSize);
			bitmap = BitmapFactory.decodeStream(new FileInputStream(fileName), null, o2);

			if (bitmap == null) return null;

			bitmap = rotateByExif(bitmap, fileName);
			if (thumbNail != null) {
				try {
					bitmap.compress(CompressFormat.JPEG, 85, new FileOutputStream(thumbNail));
				} catch (FileNotFoundException e) {
				}
			}
			return bitmap;
		} catch (FileNotFoundException e) {
			Log.d("thumbnail", "FileNotFoundException");
			return null;
		}
	}
	static public Bitmap rotate(Bitmap image, int rotation) {
		if (rotation == 0) return image;
		Matrix matrix = new Matrix();
		matrix.postRotate(rotation);
		return Bitmap.createBitmap(image, 0, 0, 
				image.getWidth(), image.getHeight(), 
				matrix, true);
	}

	static public Bitmap rotateByExif(Bitmap bitmap, String filename) {
		int rotation = getExifRotation(filename);
		return rotate(bitmap, rotation);
	}

	static public int getExifRotation(String fileName) {
		ExifInterface exif;
		try {
			exif = new ExifInterface(fileName);
		} catch (IOException e) {
			return 0;
		}
		int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
		switch (orientation) {
		case ExifInterface.ORIENTATION_ROTATE_90:
			return 90;
		case ExifInterface.ORIENTATION_ROTATE_180:
			return 180;
		case ExifInterface.ORIENTATION_ROTATE_270:
			return 270;
		default:
			return 0;
		}

	}

	static public String getFilenameFromUri(Uri uri) {
		String[] filePathColumn = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME };
		Cursor cursor = App.getInstance().getContentResolver().query(uri, filePathColumn, null, null, null);
		cursor.moveToFirst();

		String fileName = null;

		int columnIndex;
		columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
		if (columnIndex != -1 ) {
			fileName = cursor.getString(columnIndex);
		}
		cursor.close();

		if (fileName != null && new File(fileName).canRead()) {
			return fileName;
		}

		return null;
	}

	static ImageLoader imageLoader;

	static public ImageLoader getImageLoader() {
		if (imageLoader == null) {
			imageLoader = createImageLoader();
			App.DEBUG("Creating ImageLoader");
		}
		return imageLoader;
	}


	static public DisplayImageOptions getImageLoaderOptions(Boolean fromMemory, Boolean fromDisc) {
		if (fromMemory && fromDisc) {
			return new DisplayImageOptions.Builder()
			.cacheInMemory()
			.exifRotate()
			.imageScaleType(ImageScaleType.EXACT)
			.build();
		} else if (fromDisc) {
			return new DisplayImageOptions.Builder()
			.cacheOnDisc()
			.imageScaleType(ImageScaleType.EXACT)
			.build();
		} else if (fromMemory) {
			return new DisplayImageOptions.Builder()
			.cacheInMemory()
			.exifRotate() // Not cache from disc, so we assume is local and must be rotated
			.imageScaleType(ImageScaleType.EXACT)
			.build();
		} else {
			return new DisplayImageOptions.Builder()
			.exifRotate()
			.imageScaleType(ImageScaleType.EXACT)
			.build();
		}
	}

	static ImageLoader createImageLoader() {
		int width;
		int height;

		WindowManager wm = (WindowManager) App.getInstance().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

		width = display.getWidth();
		height = display.getHeight();

		if (height > width) {
			int tmp;
			tmp = width;
			width = height;
			height = tmp;
		}

		File dir =  new File(App.getInstance().getMyCacheDir().getAbsolutePath() + "/.universaldownloader");
		dir.mkdir();

		/** TODO: it doesn't work in API < 13
			Point size = new Point();
			display.getSize(size);
			width = size.x;
			height = size.y;
		 **/


		// Get singleton instance of ImageLoader
		ImageLoader imageLoader = ImageLoader.getInstance();
		DisplayImageOptions options = new DisplayImageOptions.Builder()
		.cacheInMemory()
		//.cacheOnDisc()
		//.decodingType(DecodingType.FAST)
		.imageScaleType(ImageScaleType.POWER_OF_2)
		.build();

		long memoryClass = ((ActivityManager) App.getInstance().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		int megaBytes;

		if (memoryClass < 32) {
			megaBytes = 1;
		} else if (memoryClass <= 48) {
			megaBytes = 2;
		} else {
			megaBytes = 4;
		}
		App.DEBUG("Memory for images cache: " + megaBytes);

		// Create configuration for ImageLoader (all options are optional)
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(App.getInstance())
		.memoryCache(new LRULimitedMemoryCache(megaBytes * 1024 * 1024))
		.memoryCacheExtraOptions(Math.min((int) (width * 0.9), 800), Math.min((int) (height * 0.9), 800))
		//.discCacheExtraOptions(Math.min((int) (width * 0.9), 600), Math.min((int) (height * 0.9), 900), CompressFormat.JPEG, 85)
		.threadPoolSize(4)
		.threadPriority(Thread.MIN_PRIORITY + 2)
		.denyCacheImageMultipleSizesInMemory()
		.offOutOfMemoryHandling()
		.discCache(new FileCountLimitedDiscCache(dir, 2000)) // You can pass your own disc cache implementation
		.discCacheFileNameGenerator(new HashCodeFileNameGenerator())
		.defaultDisplayImageOptions(options)
		.imageDownloader(new URLConnectionImageDownloader(10 * 1000, 20 * 1000))
		.build();

		// Initialize ImageLoader with created configuration. Do it once.
		imageLoader.init(config);

		return imageLoader;

	}

	static public void clearImageLoaderCache() {
		if (imageLoader != null) {
			imageLoader.clearMemoryCache();
			System.gc();
		}
	}

	static public void clearImageLoader() {
		if (imageLoader != null) {
			imageLoader.clearMemoryCache();
			imageLoader.stop();
			imageLoader = null;
			System.gc();
		}
	}

}
