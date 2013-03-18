/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.MenuItem;
import com.spokenpic.utils.LoadThumbnailTask;




public class ViewGallery extends SherlockListActivity {
	private ListView galleryListView;
	private SimpleCursorAdapter galleryAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		getSupportActionBar().setHomeButtonEnabled(true);

		galleryListView = getListView();
		galleryListView.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) 
			{
				/*
				Toast toast = Toast.makeText(getApplicationContext(), view.getId() + " " + position + " " + id, Toast.LENGTH_LONG);
				toast.show();
				 */
				Intent viewImage = new Intent(ViewGallery.this, ViewClip.class);

				// pass the selected session ID as an extra with the Intent
				viewImage.putExtra("id", id);
				startActivity(viewImage); 
			}
		});

		// map each contact's name to a TextView in the ListView layout
		String[] from = new String[] { "date", "file" };
		int[] to = new int[] { R.id.galleryDate, R.id.galleryImage };
		galleryAdapter = new SimpleCursorAdapter(ViewGallery.this, R.layout.gallery_item, null, from, to);

		galleryAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder(){
			/** Binds the Cursor column defined by the specified index to the specified view */
			public boolean setViewValue(View view, Cursor c, int col){
				if(view.getId() == R.id.galleryImage){
					((ImageView) view).setImageBitmap(null);
					String file = c.getString(c.getColumnIndex("file"));
					new LoadThumbnailTask().execute(view, file, App.MEDIUM_IMAGE_SIZE, App.getInstance().getMyCacheDir());
					return true;
				}
				return false;
			}
		});
		setListAdapter(galleryAdapter);
		//startManagingCursor(galleryAdapter.getCursor());
		
	}

	@Override
	protected void onResume() 
	{
		super.onResume(); // call super's onResume method
		new GetImagesTask().execute((Object[]) null);
	}

	@Override
	protected void onStop() 
	{
		Cursor cursor = galleryAdapter.getCursor();
		if (cursor != null) 
			cursor.deactivate();

		galleryAdapter.changeCursor(null);
		super.onStop();
	}

	// performs database query outside GUI thread
	private class GetImagesTask extends AsyncTask<Object, Object, Cursor> 
	{
		Model databaseConnector = new Model();

		@Override
		protected Cursor doInBackground(Object... params)
		{
			databaseConnector.open();
			return databaseConnector.getAllImages(); 
		} 

		@Override
		protected void onPostExecute(Cursor result)
		{
			galleryAdapter.changeCursor(result);
			databaseConnector.close();
		} 
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, ViewMain.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
