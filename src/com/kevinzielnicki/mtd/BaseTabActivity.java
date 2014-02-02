package com.kevinzielnicki.mtd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TabHost;

import com.kevinzielnicki.mtd.helpers.Departure;
import com.kevinzielnicki.mtd.helpers.RestClient;
import com.kevinzielnicki.mtd.helpers.Stop;
import com.kevinzielnicki.mtd.mapping.MyMapActivity;
import com.kevinzielnicki.mtd.routeFinding.DirectionsActivity;
import com.kevinzielnicki.mtd.routeFinding.Itinerary;

public class BaseTabActivity extends TabActivity {
	public static final int MAX_SAVED_STOPS = 30;
	private static String STOP_DATA_FILE = "stopData";
	private String stopCacheID = "";

	public static String PREFS_NAME = "MTDprefs";
	public LocationMonitor loc = new LocationMonitor();
	//public TreeMap<String, Stop> stopListUnsynced = new TreeMap<String,Stop>();
	public SortedMap<String,Stop> stopList;
	public Lock stopListLock;
	public boolean retrievingStops = false;
	public List<Stop> recentStops;
	private int numFavs = 0;

	private final LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			loc.updateLocation(location);
		}

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub

		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		stopList = Collections.synchronizedSortedMap(new TreeMap<String,Stop>());
		
		//ConcurrentMap testList = new ConcurrentMap();
		stopListLock = new ReentrantLock(true);


		SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
		if(!settings.contains("stopID")) {
			// initialize the default stop as Illinois Terminal
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("stopName", "Illinois Terminal");
			editor.putString("stopID", "IT");
			editor.putString("stopLat", "40.115935");
			editor.putString("stopLon", "-88.240947");

			// initialize list of recently searched stops
			editor.putInt("recentStops", 1);
			editor.putString("recentName0", "Illinois Terminal");
			editor.putString("recentID0", "IT");
			editor.putString("recentLat0", "40.115935");
			editor.putString("recentLon0", "-88.240947");
			editor.commit();
		}
		
		recentStops = getRecentStops();
		
		
		setContentView(R.layout.main);




		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost();  // The activity TabHost
		TabHost.TabSpec spec;  // Reusable TabSpec for each tab
		Intent intent;  // Reusable Intent for each tab

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, ScheduleActivity.class);

		// Initialize a TabSpec for each tab and add it to the TabHost
		//	    LinearLayout icon = new LinearLayout(this);
		//	    TextView label = new TextView(this);
		//	    label.setText("Schedule");
		//	    icon.addView(label);
		spec = tabHost.newTabSpec("Schedule").setIndicator("Schedule",
				res.getDrawable(R.drawable.schedule)).setContent(intent);
		tabHost.addTab(spec);

		// Do the same for the other tabs
		intent = new Intent().setClass(this, SearchActivity.class);
		spec = tabHost.newTabSpec("Search").setIndicator("Search",
				res.getDrawable(R.drawable.search)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, DirectionsActivity.class);
		spec = tabHost.newTabSpec("Directions").setIndicator("Directions",
				res.getDrawable(R.drawable.directions))
				.setContent(intent);
		tabHost.addTab(spec);
		
		intent = new Intent().setClass(this, MyMapActivity.class);
		spec = tabHost.newTabSpec("Map").setIndicator("Map",
				res.getDrawable(R.drawable.map)).setContent(intent);
		tabHost.addTab(spec);



		tabHost.setCurrentTabByTag("Search");

		getAllStops();
	}


	@Override
	public void onResume() {
		super.onResume();

		// start polling GPS if available
		//PackageManager pm = getBaseContext().getPackageManager();
		//boolean hasGPS = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION);
		//if(hasGPS) {
			LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
			String provider = locationManager.getBestProvider(criteria, true);
			if(provider != null) {
				locationManager.requestLocationUpdates(provider, 60000, 100, locationListener);
				loc.updateLocation(locationManager.getLastKnownLocation(provider));
			}
			criteria = new Criteria();
			criteria.setPowerRequirement(Criteria.POWER_LOW);
			provider = locationManager.getBestProvider(criteria, true);
			if(provider != null) {
				locationManager.requestLocationUpdates(provider, 30000, 100, locationListener);
			}
		//}
	}


	@Override
	public void onPause() {
		super.onPause();

		// stop GPS when view goes away
		LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(locationListener);

	}


	public void switchTab(int tab){
		//		// need this to make onscreen keyboard go away for some reason
		//		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		//		imm.hideSoftInputFromWindow(this.findViewById(android.R.id.content).getRootView().getWindowToken(), 0);

		getTabHost().setCurrentTab(tab);
	}
	
	public void switchToSchedule(Stop s) {
		putNewStop(s,false);
		TabHost tabHost = getTabHost();  // The activity TabHost

		SharedPreferences settings = getSharedPreferences(BaseTabActivity.PREFS_NAME,0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("stopName", s.name);
		editor.putString("stopID", s.ID);
    	editor.putString("stopLat", s.lat);
    	editor.putString("stopLon", s.lon);
    	editor.commit();
    	
		tabHost.setCurrentTabByTag("Schedule");
	}

	public void switchToDirections(Stop start, Stop end, String time) {
//		// first, create the directions tab if it doesn't exist yet
		TabHost tabHost = getTabHost();  // The activity TabHost

		tabHost.setCurrentTabByTag("Directions");
		DirectionsActivity currentActivity = (DirectionsActivity)getCurrentActivity();
		currentActivity.directionSearch(start,end, time);
	}
	
	// draw a list of stops
	public void switchToMap(List<Stop> stopList) {
		TabHost tabHost = getTabHost();  // The activity TabHost
		
		tabHost.setCurrentTabByTag("Map");
		MyMapActivity currentActivity = (MyMapActivity)getCurrentActivity();
		currentActivity.setupOverlay(stopList);
	}
	
	// draw a map for a route
	public void switchToMap(Departure d, Stop s) {
		TabHost tabHost = getTabHost();  // The activity TabHost
//		}
		
		tabHost.setCurrentTabByTag("Map");
		MyMapActivity currentActivity = (MyMapActivity)getCurrentActivity();
		currentActivity.setupOverlay(d,s);
		

		SharedPreferences settings = getSharedPreferences(BaseTabActivity.PREFS_NAME,0);
		boolean hasTriedMapRoute = settings.getBoolean("hasTriedMapRoute", false);
		if(!hasTriedMapRoute) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("hasTriedMapRoute", true);
			editor.commit();
		}
		
	}
	
	// draw a map for an itinerary
	public void switchToMap(Itinerary it) {
		TabHost tabHost = getTabHost();  // The activity TabHost
		
		tabHost.setCurrentTabByTag("Map");
		MyMapActivity currentActivity = (MyMapActivity)getCurrentActivity();
		currentActivity.setupOverlay(it);
		

		SharedPreferences settings = getSharedPreferences(BaseTabActivity.PREFS_NAME,0);
		boolean hasTriedMap = settings.getBoolean("hasTriedMap", false);
		if(!hasTriedMap) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("hasTriedMap", true);
			editor.commit();
		}
	}
	
	public void setMapRoute(Itinerary it) {
		MyMapActivity mapActivity = (MyMapActivity)getLocalActivityManager().getActivity("Map");
		if (mapActivity != null)
			mapActivity.setupOverlay(it);
	}

	private ArrayList<Stop> getRecentStops() {
		ArrayList<Stop> favStops = new ArrayList<Stop>();
		ArrayList<Stop> stops = new ArrayList<Stop>();
		SharedPreferences settings = getSharedPreferences(BaseTabActivity.PREFS_NAME,0);
		int nStops = settings.getInt("recentStops", 0);
		//Log.i("stops","getting the last "+nStops);
		for(int i=0; i<nStops; ++i) {
			String stopName = settings.getString("recentName"+i, null);
			String stopID = settings.getString("recentID"+i, null);
			String stopLat = settings.getString("recentLat"+i, null);
			String stopLon = settings.getString("recentLon"+i, null);
			boolean stopFav = settings.getBoolean("recentFavorite"+i, false);
			if(stopFav) {
				Stop s = new Stop(stopName,stopID,stopLat,stopLon,stopFav);
				favStops.add(s);
				//markFavoriteStop(s);
			} else {
				stops.add(new Stop(stopName,stopID,stopLat,stopLon,stopFav));
			}
		}
		
		numFavs = favStops.size();
		Collections.sort(favStops);
		favStops.addAll(stops);

		return favStops;
	}
	

	public void putNewStop(Stop newStop, boolean overrideFavorite) {
		ArrayList<Stop> newStops = new ArrayList<Stop>();
		if(overrideFavorite)
			markFavoriteStop(newStop);
		newStop.distanceFeet = 0;
		newStops.add(newStop);
		for(Stop s : recentStops) {
			if(!s.equals(newStop)) {
				newStops.add(s);

			} else if(s.isFavorite && !overrideFavorite) {
				newStop.isFavorite = true;
			}
			if(newStops.size() >= MAX_SAVED_STOPS)
				break;
		}

		// sort the favs stops in front
		ArrayList<Stop> favStops = new ArrayList<Stop>();
		ArrayList<Stop> stops = new ArrayList<Stop>();
		for(Stop s : newStops) {
			if(s.isFavorite) {
				favStops.add(s);
			} else {
				stops.add(s);
			}
		}
		numFavs = favStops.size();
		Collections.sort(favStops);
		favStops.addAll(stops);
		recentStops = favStops;
		//Log.i("favs","num favs = "+numFavs);

		SharedPreferences settings = getSharedPreferences(BaseTabActivity.PREFS_NAME,0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("stopName", newStop.name);
		editor.putString("stopID", newStop.ID);
    	editor.putString("stopLat", newStop.lat);
    	editor.putString("stopLon", newStop.lon);
		int i=0;
		for(Stop s : recentStops) {
			editor.putString("recentName"+i, s.name);
			editor.putString("recentID"+i, s.ID);
			editor.putString("recentLat"+i, s.lat);
			editor.putString("recentLon"+i, s.lon);
	    	editor.putBoolean("recentFavorite"+i, s.isFavorite);
			i++;
		}

		editor.putInt("recentStops", newStops.size());
		editor.commit();
	}
	
	public boolean hasFavs() {
		return numFavs > 0;
	}
	
	private void markFavoriteStop(Stop s) {
		stopListLock.lock();
		Stop n = stopList.get(s.name);
		if(n != null) {
			n.isFavorite = s.isFavorite;
		}
		stopListLock.unlock();
	}
	
	private void markAllFavorites() {
		for (Stop s : recentStops) {
			if(s.isFavorite)
				markFavoriteStop(s);
		}
	}

	// query the API for the list of all stops in a new thread, using cached data if available
	private void getAllStops() {
		loadCachedStops();
		if(stopList.size() == 0)
			retrievingStops = true;
		
		Thread updateThread = new Thread() {
			public void run () {
				HashMap<String,String> args = new HashMap<String,String>();
				args.put("changeset_id", stopCacheID);

				//Log.i("whoa","getting stops");
				JSONObject result = RestClient.MTD("GetStops", args);
				//int status = 500;
				if(result == null) {
					//status = 500;
				} else {
					
					// get a lock on the stop list, don't clear it until we're done getting stops
					stopListLock.lock();
					try {
						if(result.getBoolean("new_changeset") == true) {
							retrievingStops = true;
							//Log.i("whoa","updating stops");
							stopList.clear();
							//status = result.getJSONObject("status").getInt("code");
							JSONArray stops = result.getJSONArray("stops");
							if(stops.length() > 0) {
								for(int i=0; i<stops.length(); ++i) {
									JSONObject stop = stops.getJSONObject(i);
	
									JSONObject point = stop.getJSONArray("stop_points").getJSONObject(0);
									Stop s = new Stop(stop.getString("stop_name"),stop.getString("stop_id"),point.getString("stop_lat"),point.getString("stop_lon"));
	
									//stopList.add(s);
									stopList.put(s.name, s);
								}
								stopCacheID = result.getString("changeset_id");
								saveCachedStops();
								markAllFavorites();
							}
						} else {
							//Log.i("whoa","reusing cached stops");
						}
					} catch (JSONException e) {
						//status = 500;
						e.printStackTrace();
					} finally {
						stopListLock.unlock();  // we're done changing the stop list
					}
				}
				//Log.i("whoa",stopList.toString());
				retrievingStops = false;
			}
		};
		updateThread.start();
	}

	private void loadCachedStops() {
		FileInputStream fstream;
		try {
			fstream = openFileInput(STOP_DATA_FILE);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			stopCacheID = br.readLine();
			String strLine;
			stopList.clear();
			while ((strLine = br.readLine()) != null)   {
				String name = strLine;
				String id = br.readLine();
				String lat = br.readLine();
				String lon = br.readLine();
				Stop s = new Stop(name,id,lat,lon);
				//stopList.add(s);
				stopList.put(s.name, s);
			}
			br.close();
			markAllFavorites();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void saveCachedStops() {
		FileOutputStream fos;
		try {
			//Log.i("file",getFilesDir().toString());
			fos = openFileOutput(STOP_DATA_FILE, Context.MODE_WORLD_READABLE);
			DataOutputStream out = new DataOutputStream(fos);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));

			bw.write(stopCacheID);
			bw.newLine();
			
			for(Map.Entry<String,Stop> e : stopList.entrySet()) {
				Stop s = e.getValue();
				bw.write(s.name);
				bw.newLine();
				bw.write(s.ID);
				bw.newLine();
				bw.write(s.lat);
				bw.newLine();
				bw.write(s.lon);
				bw.newLine();
			}
			
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
