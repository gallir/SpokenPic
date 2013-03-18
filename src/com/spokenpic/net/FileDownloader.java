/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic.net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;

public class FileDownloader {

	static public void download(URL url, String outputFile, FileDownloaderListener listener) {
		Downloader task = new Downloader(listener);
		task.execute(url, outputFile);
		return;
	}


	static class Downloader extends AsyncTask<Object, Void, File> {
		FileDownloaderListener listener;

		public Downloader(FileDownloaderListener listener) {
			this.listener = listener;
		}

		@Override
		protected File doInBackground(Object... params) {
			URL url = (URL) params[0];
			String outputFile = (String) params[1];

			HttpURLConnection urlConnection = null;
			File inFile = null;

			//App.DEBUG("File downloading " + url);
			try {
				inFile = new File(outputFile);

				urlConnection = (HttpURLConnection) url.openConnection();

				byte[] buffer = new byte[4096];
				int len;

				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				OutputStream out = new FileOutputStream(inFile);

				while ((len = in.read(buffer)) != -1) {
					out.write(buffer, 0, len);
				}

				in.close();
				out.close();

			} catch (Exception e) {
				inFile = null;
			}

			if (urlConnection != null) urlConnection.disconnect();
			return inFile;
		}

		@Override
		protected void onPostExecute(File file) {
			listener.onDownLoadFinished(file);
		}

	};



	static public interface FileDownloaderListener {
		void onDownLoadFinished(File file);
	}
}
