/**
 * SpokenPic Java source code 
 * @author Ricardo Galli (gallir@gmail.com)
 * @copyright: Menéame & APSL, 2012
 * @license: GPL3
 */

package com.spokenpic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.spokenpic.net.TimeLineAdapter;
import com.spokenpic.net.TimeLineAdapter.TimeLineListener;

public class ViewTimeLineBrowser extends SherlockActivity implements ActionBar.TabListener, TimeLineListener {

	final int TAB_ALL = 0;
	final int TAB_MYCLIPS = 1;
	final int TAB_USER = 2;
	final int TAB_FOLLOWING = 3;

	int mTabSelected;

	Tab mUserTab = null;

	//	List<ClipDataResult> mObjects;
	GridView mGridview;
	String mUsername;


	TimeLineAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		//requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		//getSupportActionBar().hide();

		getSupportActionBar().setHomeButtonEnabled(true);
		//App.getInstance().loadPreferences();

		setContentView(R.layout.view_timeline_browser);


		Intent intent = getIntent();
		Uri intentData = intent.getData();


		// Try to get the username
		if (intent.getExtras() != null) {
			mUsername = intent.getExtras().getString("username");
		}
		
		// Check if it was an intent from a link
		if (mUsername == null && intentData != null && intentData.getPath() != null) {
			Pattern p = Pattern.compile(".*?/clips/(\\w+)/");
			Matcher m = p.matcher( intentData.getPath() );
			if(m.find()) {
				mUsername = m.group(1);
			}
		}


		
		
		
		mGridview = (GridView) findViewById(R.id.timelineGrid);

		ActionBar.Tab tab;
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		tab = getSupportActionBar().newTab();
		tab.setText(getString(R.string.all_clips));
		tab.setTabListener(this);
		getSupportActionBar().addTab(tab);
		mTabSelected = TAB_ALL;


		if (App.getInstance().isLogged()) {
			tab = getSupportActionBar().newTab();
			tab.setText(getString(R.string.following));
			tab.setTabListener(this);
			getSupportActionBar().addTab(tab);

			tab = getSupportActionBar().newTab();
			tab.setText(getString(R.string.my_clips));
			tab.setTabListener(this);
			getSupportActionBar().addTab(tab);
			mTabSelected = TAB_MYCLIPS;
			if (mUsername != null && mUsername.equals(App.getInstance().getUsername())) {
				tab.select();
			}
		}

		if (mUsername != null && ( ! App.getInstance().isLogged() || ! mUsername.equals(App.getInstance().getUsername())) ) {
			mUserTab = getSupportActionBar().newTab();
			mUserTab.setText(mUsername);
			mUserTab.setTabListener(this);
			getSupportActionBar().addTab(mUserTab);
			mTabSelected = TAB_USER;
			mUserTab.select();
		}
	}

	public void onTabReselected(Tab tab, FragmentTransaction transaction) {
		mGridview.setSelection(Math.min(1, mAdapter.getCount()));
		new Handler().post(new Runnable() {
			public void run() {
				mGridview.smoothScrollToPosition(0);
			}
		});
	}


	public void onTabSelected(Tab tab, FragmentTransaction transaction) {
		String selected = tab.getText().toString();
		int type;

		//App.DEBUG("onTabCalled: " + selected);

		if (selected.equals(getString(R.string.all_clips))) {
			type = TimeLineAdapter.TIMELINE_ALL;
			mTabSelected = TAB_ALL;
		} else if (selected.equals(getString(R.string.following))) {
			type = TimeLineAdapter.TIMELINE_FOLLOWING;
			mTabSelected = TAB_FOLLOWING;
		} else if (selected.equals(getString(R.string.my_clips))) {
			type = TimeLineAdapter.TIMELINE_AUTHENTICATED;
			mTabSelected = TAB_MYCLIPS;
		} else if (mUsername != null) {
			if (App.getInstance().isLogged() && mUsername.equals(App.getInstance().getUsername())) {
				type = TimeLineAdapter.TIMELINE_AUTHENTICATED;
				mTabSelected = TAB_MYCLIPS;
			} else {
				type = TimeLineAdapter.TIMELINE_USER;
				mTabSelected = TAB_USER;
			}
		} else {
			type = TimeLineAdapter.TIMELINE_ALL;
			mTabSelected = TAB_ALL;
		}

		if (mUserTab != null && mTabSelected != TAB_USER) {
			getSupportActionBar().removeTab(mUserTab);
			mUserTab = null;
		}

		mAdapter = new TimeLineAdapter(ViewTimeLineBrowser.this, type, mUsername);
		mGridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Intent viewImage = new Intent(ViewTimeLineBrowser.this, ViewClipBrowser.class);
				viewImage.putExtra("resource_uri", ((TimeLineAdapter.ViewHolder)v.getTag()).resource_uri);
				viewImage.putExtra("clip_id", ((TimeLineAdapter.ViewHolder)v.getTag()).id);
				viewImage.putExtra("username", ((TimeLineAdapter.ViewHolder)v.getTag()).author.getText());
				startActivity(viewImage); 
			}
		});


		mGridview.setAdapter(mAdapter);
	}

	public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
	}

	@Override
	public void onResume() {
		super.onResume();
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in action bar clicked; go home
			Intent intent = new Intent(this, ViewMain.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
			//case R.id.item_share:

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onNewItems(int count) {
		if (mGridview.getFirstVisiblePosition() == 0 || mGridview.getLastVisiblePosition() > count - 4) {
			mGridview.invalidateViews();
		}
	}

	public void onStartDownloading() {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	public void onStopDownloading() {
		setSupportProgressBarIndeterminateVisibility(false);
	}

}
