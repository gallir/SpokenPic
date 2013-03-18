/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.AsyncTask;

import com.spokenpic.App;
import com.spokenpic.net.FileDownloader.FileDownloaderListener;


public class InputStreamDownloader {
	Downloader mTask;
	InputStream mIs;
	FileDownloaderListener mListener;
	String mFilename;
	
	/*
	static public void download(InputStream is, String outputFile, FileDownloaderListener listener) {
		Downloader task = new Downloader(listener);
		task.execute(is, outputFile);
		return;
	}
	*/
	
	public InputStreamDownloader(InputStream is, String outputFile, FileDownloaderListener listener) {
		mTask = new Downloader(listener);
		mIs = is;
		mListener = listener;
		mFilename = outputFile;
	}

	public void execute() {
		mTask.execute(mIs, mFilename);
	}

	public void cancel() {
		mTask.listener = null;
		mTask.cancel(true);
	}

	static class Downloader extends AsyncTask<Object, Void, File> {
		public FileDownloaderListener listener;

		public Downloader(FileDownloaderListener listener) {
			this.listener = listener;
		}

		@Override
		protected File doInBackground(Object... params) {
			InputStream in = (FileInputStream) params[0];
			String outputFile = (String) params[1];

			File inFile = null;

			App.DEBUG("FileInputStream downloading " + in.toString());
			try {
				inFile = new File(outputFile);

				byte[] buffer = new byte[4096];
				int len;

				OutputStream out = new FileOutputStream(inFile);

				while ((len = in.read(buffer)) != -1) {
					out.write(buffer, 0, len);
					if (isCancelled()) return null;
				}

				in.close();
				out.close();

			} catch (Exception e) {
				inFile = null;
			}

			return inFile;
		}

		@Override
		protected void onPostExecute(File file) {
			try {
				if (listener != null) {
					listener.onDownLoadFinished(file);
				}
			} catch (Exception e) {
				App.DEBUG("Error in InputStreamDownload: " + e.getMessage());
			}
		}
	};
}
