package com.kevinzielnicki.mtd;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kevinzielnicki.mtd.helpers.RestClient;
import com.kevinzielnicki.mtd.helpers.Stop;

public class SearchActivity extends Activity {
	/** Called when the activity is first created. */
	
	LinearLayout subLayout;
	TextView favHint;
	
	BaseTabActivity ParentActivity;
	
	List<Stop> currentStops = new ArrayList<Stop>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ParentActivity = (BaseTabActivity) this.getParent();

		subLayout = new LinearLayout(this);

		setContentView(R.layout.search);
		LinearLayout searchLayout = (LinearLayout)findViewById(R.id.searchLayout);

		favHint = new TextView(SearchActivity.this);
		favHint.setText("Tip: Long press a stop to save it as a favorite!");
		searchLayout.addView(favHint);
		
		//subLayout = new LinearLayout(this);
		subLayout.setOrientation(LinearLayout.VERTICAL);
		//final TextView searching = new TextView(this);
		//searching.setText("this will be search");
		searchLayout.addView(subLayout);

		final EditText search = (EditText) findViewById(R.id.searchField);

		
		// hook up the search box to find a list of stops via an asynchronous task
		// this can probably be removed because anything can be done with the textwatcher below?
		search.setOnEditorActionListener(new TextView.OnEditorActionListener() {	
			@SuppressWarnings("unchecked")
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				Log.i("Searching?",""+actionId);
				//if(actionId == EditorInfo.IME_NULL) {

				// need this to make onscreen keyboard go away for some reason
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(search.getWindowToken(), 0);

				//				TextView searching = new TextView(SearchActivity.this);
				//				searching.setText("Searching...");
				//				subLayout.removeAllViews();
				//				subLayout.addView(searching);

				HashMap<String,String> args = new HashMap<String,String>();
				args.put("method", "getStopsBySearch");
				args.put("query", v.getText().toString());
				args.put("count", "20");

				new SearchStops().execute(args);

				return true;
			}
		});


		// re-enable this if you want to do stuff that happens any time the text is changed (probably just way too slow over wireless?)
		TextWatcher searchWatcher = new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) { 
				if(!s.toString().equals("")) {
					new SearchCache().execute(s.toString());
				} else
					setToRecentStops();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		};


		search.addTextChangedListener(searchWatcher);
	}

	@Override
	public void onResume() {
		super.onResume();

		// start out by displaying a list of the recently searched stops
		//setToRecentStops(); (this is actually done automatically by searchWatcher
		
		// remove any leftover search text
		EditText search = (EditText) findViewById(R.id.searchField);
		search.setText("");


		// hook up the GPS search button
		Button searchGPS = (Button) findViewById(R.id.searchGPS);

		PackageManager pm = getBaseContext().getPackageManager();
		boolean hasGPS = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION);
		if(!hasGPS) {
			searchGPS.setVisibility(View.GONE);
		} else {

			searchGPS.setOnClickListener(new View.OnClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onClick(View v) {
					HashMap<String,String> args = new HashMap<String,String>();
					args.put("method", "getStopsByLatLon");
					args.put("count", "20");

					new SearchStops().execute(args);
				}
			});
		}

		// only show a hint if there are no favorites
		if(ParentActivity.hasFavs())
			favHint.setVisibility(View.GONE);
		else
			favHint.setVisibility(View.VISIBLE);

		//showDialog(0);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.search_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.map_stops:
				ParentActivity.switchToMap(currentStops);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	public void switchTab(int indexTabToSwitchTo){
		ParentActivity.switchTab(indexTabToSwitchTo);
	}
	
	public void setToRecentStops() {
		//Log.i("stops","Setting up the stop list");
		currentStops = ParentActivity.recentStops;
		subLayout.removeAllViews();
		TextView recentStops = new TextView(SearchActivity.this);
		recentStops.setText("My Stops:");
		recentStops.setTextSize(20);
		subLayout.addView(recentStops);
		
		ListView stopList = stopsToView(currentStops);
		subLayout.addView(stopList);
	}


	// sets a new stop as the current stop and adds it to the saved recent stop list
	public void putNewStop(Stop newStop) {
		putNewStop(newStop,false);
	}
	public void putNewStop(Stop newStop, boolean overrideFavorite) {
		ParentActivity.putNewStop(newStop, overrideFavorite);
		

		// decide whether to show the hint
		if(ParentActivity.hasFavs())
			favHint.setVisibility(View.GONE);
		else
			favHint.setVisibility(View.VISIBLE);
	}

	public ListView stopsToView(final List<Stop> stops) {
		ListView stopList = new ListView(SearchActivity.this);
		stopList.setAdapter(new Stop.Adapter(SearchActivity.this, R.layout.stop, stops));

		// when clicked, we should see the other tab with all the current buses
		stopList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(subLayout.getWindowToken(), 0);
				
				Stop newStop = (Stop)parent.getItemAtPosition(position);
				putNewStop(newStop);

				switchTab(0);
			}
		});
		
		// on a long press, make this a favorite stop
		stopList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				Stop newStop = (Stop)parent.getItemAtPosition(position);
				newStop.toggleFavorite();
				putNewStop(newStop,true);
				

				EditText search = (EditText) findViewById(R.id.searchField);
				search.setText("");
				
				//parent.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
				return true;
			}
			
		});
		
		stopList.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(AbsListView list, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onScrollStateChanged(AbsListView list, int state) {
				if(state == SCROLL_STATE_TOUCH_SCROLL) {
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(list.getWindowToken(), 0);
				}
			}
			
		});
		return stopList;
	}

	// Asynchronous task to search cached stop names
	private class SearchCache extends AsyncTask<String, Void, ArrayList<Stop>> {

		@Override
		protected ArrayList<Stop> doInBackground(String... arg0) {
			String searchName = arg0[0];

			ArrayList<Stop> favStops = new ArrayList<Stop>();
			ArrayList<Stop> stops = new ArrayList<Stop>();
			
			// try to acquire the lock for the stop list
			try {
				if(ParentActivity.stopListLock.tryLock(500, TimeUnit.MILLISECONDS)) {
					//watch out for concurrent modifications! (this should be impossible with the lock, but better to be safe
					try{
						for(Map.Entry<String,Stop> e : ParentActivity.stopList.entrySet()) {
							Stop s = e.getValue();
							if(s.matches(searchName)) {
								if(s.isFavorite) {
									favStops.add(s);
								} else {
									stops.add(s);
								}
							}
							if(stops.size() >= 30)
								break;
						}
					} catch (ConcurrentModificationException e) {
						e.printStackTrace();
					} finally {
						ParentActivity.stopListLock.unlock();
					}
				} else {
					// we failed to get the lock, return a null list
					return null;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
				

			favStops.addAll(stops);
			return favStops;
		}
		
		protected void onPostExecute(final ArrayList<Stop> stops) {
//			if(ParentActivity.retrievingStops) {
//				Toast.makeText(SearchActivity.this, "Loading stop data, please wait.", Toast.LENGTH_SHORT).show();
//			} else
			if(stops != null) {
			
				currentStops = stops;
				EditText search = (EditText) findViewById(R.id.searchField);
				
				if(!search.getText().toString().equals("")) {
					subLayout.removeAllViews();			
					ListView stopList = stopsToView(stops);
					subLayout.addView(stopList);
				}
			}
		}
		
	}
	// Asynchronous task to do searching in a separate thread
	private class SearchStops extends AsyncTask<HashMap<String,String>, Void, ArrayList<Stop>> {
		//ProgressDialog myProgressDialog = null;
		int status = 200;
		Toast myToast;

		// Show a "searching" busy animation
		protected void onPreExecute() {
			//myProgressDialog = ProgressDialog.show(SearchActivity.this,"", "Searching...\n\n(Data provided by CUMTD)", true);
			myToast = Toast.makeText(SearchActivity.this, "Searching...\n(Data provided by CUMTD)", Toast.LENGTH_LONG);
			myToast.show();
		}

		// look up stops based on provided search string
		protected ArrayList<Stop> doInBackground(HashMap<String,String>... args) {
			Log.i("search","started search");
			String method = args[0].get("method");//"GetStopsBySearch";
			args[0].remove("method");

			if(method.equals("getStopsByLatLon")) {
				if(!ParentActivity.loc.isSet()) {
					status = 1;
					return null;
				}

				DecimalFormat f = new DecimalFormat("#.######");
				//Log.i("GPS","lat: "+f.format(loc.getLatitude()));
				//Log.i("GPS","lon: "+f.format(loc.getLongitude()));

				args[0].put("lat", f.format(ParentActivity.loc.getLocation().getLatitude()));
				args[0].put("lon", f.format(ParentActivity.loc.getLocation().getLongitude()));
			}

			JSONObject result = RestClient.MTD(method, args[0]);
			if(result == null) {
				status = 500;
				return null;
			}
			try {
				status = result.getJSONObject("status").getInt("code");
				if(status >= 300)
					return null;
				JSONArray stops = result.getJSONArray("stops");
				if(stops.length() > 0) {
					final ArrayList<Stop> stopList = new ArrayList<Stop>();
					for(int i=0; i<stops.length(); ++i) {
						JSONObject stop = stops.getJSONObject(i);

		                //Log.i("Praeda","<jsonobject>\n"+stop.toString()+"\n</jsonobject>");
						// something is broken about lat/lon for stop search! this is causing search to fail:
						JSONArray points = stop.getJSONArray("stop_points");
						if(points.length() > 0) {
							JSONObject point = points.getJSONObject(0);
							Stop s = new Stop(stop.getString("stop_name"),stop.getString("stop_id"),point.getString("stop_lat"),point.getString("stop_lon"));
							if(method.equals("getStopsByLatLon")) {
								s.distanceFeet = stop.getDouble("distance");
							}
							stopList.add(s);
						} else {
							Log.i("search","Empty point list for "+stop.getString("stop_name")+" ("+stop.getString("stop_id")+")");
						}
					}

					return stopList;

				}
			} catch (JSONException e) {
				status = 500;
				e.printStackTrace();
			}

			return null;
		}

		// display a list of found stops, and hook them up to buttons that will pull up scheduled buses
		protected void onPostExecute(final ArrayList<Stop> stops) {
			currentStops = stops;
			subLayout.removeAllViews();
			if(stops == null) {
				TextView foundStops = new TextView(SearchActivity.this);
				if(status == 1) {
					foundStops.setText("Location couldn't be established. Try again later.");
				} else if(status < 300) {
					foundStops.setText("No stops found");
				} else {
					foundStops.setText("Server communication error!");
				}
				subLayout.addView(foundStops);
			} else {
				ListView stopList = stopsToView(stops);
				subLayout.addView(stopList);
			}
			//myProgressDialog.dismiss();
			myToast.cancel();
		}

	}


}