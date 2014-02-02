package com.kevinzielnicki.mtd.routeFinding;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kevinzielnicki.mtd.BaseTabActivity;
import com.kevinzielnicki.mtd.R;
import com.kevinzielnicki.mtd.helpers.DirectionsDialog;
import com.kevinzielnicki.mtd.helpers.RestClient;
import com.kevinzielnicki.mtd.helpers.Stop;
import com.kevinzielnicki.mtd.mapping.NavPoint;

public class DirectionsActivity  extends Activity {
	/** Called when the activity is first created. */
	LinearLayout subLayout;

	BaseTabActivity ParentActivity;
	
	//ArrayList<ArrayList<NavPoint>> allRoutes;
	ArrayList<Itinerary> itineraryList;

	public static final DateFormat dateIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
	public static final DateFormat dateOut = new SimpleDateFormat("h:mm", Locale.ENGLISH);
	
	Stop start;
	Stop end;
	
	boolean directionSet = false;
	
	DirectionsDialog myDirDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {     
		super.onCreate(savedInstanceState);
		
		ParentActivity = (BaseTabActivity) this.getParent();

		setContentView(R.layout.dirctions_activity);
		
		subLayout = (LinearLayout) findViewById(R.id.subLayout);

		myDirDialog = new DirectionsDialog(DirectionsActivity.this,ParentActivity);
		
//		Button dirButton = (Button) findViewById(R.id.newDirections);
//		dirButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				myDirDialog.show();
//			}
//		});
	
	}

	@Override
	public void onResume() {
		super.onResume();
		if(!directionSet) {
			myDirDialog.show();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.directions_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.new_directions:
				myDirDialog.show();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@SuppressWarnings("unchecked")
	public void directionSearch(Stop start, Stop end, String time) {
		this.start = start;
		this.end = end;
		myDirDialog.setOrigin(start);
		myDirDialog.setDestination(end);
		if(time == null)
			myDirDialog.setTimeToNow();
		myDirDialog.dismiss();
		directionSet = true;

		HashMap<String,String> args = new HashMap<String,String>();
		args.put("method", "GetPlannedTripsByLatLon");

		args.put("origin_lat",  start.lat);
		args.put("origin_lon", start.lon);
		args.put("destination_lat", end.lat);
		args.put("destination_lon", end.lon);
		if(time != null)
			args.put("time", time);
		
		new GetDirections().execute(args);
	}

	public ListView itinerariesToView() {
		ListView stopList = new ListView(this);
		stopList.setAdapter(new Itinerary.Adapter(this, R.layout.stop, itineraryList));  //using R.layout.stop is a hack! could cause problems?

		// when clicked, switch to the map view with the correct stuff drawn in
		stopList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ParentActivity.switchToMap(itineraryList.get(position));
			}
		});
		
		return stopList;
	}
	

	// Asynchronous task to do searching in a separate thread
	private class GetDirections extends AsyncTask<HashMap<String,String>, Void, View> {
		//ProgressDialog myProgressDialog = null;
		int status = 200;
		String msg = "";
		Toast myToast;

		// Show a "searching" busy animation
		protected void onPreExecute() {
			itineraryList = new ArrayList<Itinerary>();
			//myProgressDialog = ProgressDialog.show(SearchActivity.this,"", "Searching...\n\n(Data provided by CUMTD)", true);
			myToast = Toast.makeText(DirectionsActivity.this, "Searching...\n(Data provided by CUMTD)", Toast.LENGTH_LONG);
			myToast.show();
		}

		// look up stops based on provided search string
		protected View doInBackground(HashMap<String,String>... args) {
			String method = args[0].get("method");//"GetStopsBySearch";
			args[0].remove("method");
			
			// first, see if we have to search our original stop
			if(args[0].containsKey("query")) {
				String query = args[0].get("query");
				args[0].remove("query");
				HashMap<String,String> stopArgs = new HashMap<String,String>();
				stopArgs.put("query", query);
				stopArgs.put("count", "1");
				JSONObject result = RestClient.MTD("GetStopsBySearch",stopArgs);
				if(result == null) {
					status = 499;
					return null;
				}
				try {
					status = result.getJSONObject("status").getInt("code");
					msg = result.getJSONObject("status").getString("msg");
					if(status >= 300)
						return null;
					JSONArray stops = result.getJSONArray("stops");
					if(stops.length() > 0) {
						JSONObject stop = stops.getJSONObject(0);
						JSONObject point = stop.getJSONArray("stop_points").getJSONObject(0);
						if(args[0].containsKey("destination_lat")) {
							args[0].put("origin_lat", point.getString("stop_lat"));
							args[0].put("origin_lon", point.getString("stop_lon"));
						} else {
							args[0].put("destination_lat", point.getString("stop_lat"));
							args[0].put("destination_lon", point.getString("stop_lon"));
						}
					} else {
						return null;
					}
				} catch (JSONException e) {
					status = 501;
					msg = "Invalid server response";
					e.printStackTrace();
					return null;
				}
			}

			args[0].put("max_walk", "0.75");
			JSONObject result = RestClient.MTD(method, args[0]);
			if(result == null) {
				status = 404;
				msg = "Could not connect to server";
				return null;
			}
			try {
				status = result.getJSONObject("status").getInt("code");
				msg = result.getJSONObject("status").getString("msg");
				if(status >= 300)
					return null;
				JSONArray itineraries = result.getJSONArray("itineraries");
				if(itineraries.length() > 0) {
					for(int it=0; it<itineraries.length(); ++it) {
						JSONObject itinerary = itineraries.getJSONObject(it);
						
						String travelTime = itinerary.getString("travel_time");

						JSONArray legs = itinerary.getJSONArray("legs");
						JSONObject end = null;
						Itinerary myItinerary = new Itinerary(it, travelTime, DirectionsActivity.this);
						for(int i=0; i<legs.length(); ++i) {
							
							JSONObject leg = legs.getJSONObject(i);
							String type = leg.getString("type");
							if(type.equalsIgnoreCase("walk")) {
								JSONObject walk = leg.getJSONObject("walk");
								String dist = walk.getString("distance");
								
								JSONObject begin = walk.getJSONObject("begin");
								double lat = begin.getDouble("lat");
								double lon = begin.getDouble("lon");
								String origin = begin.getString("name");
								if(origin.startsWith("40"))  // catches GPS coordinates instead of real name
									origin = "Start";
								String time = begin.getString("time");
								Date parsedTime = dateIn.parse(time);
								
								end = walk.getJSONObject("end");
								String dest = end.getString("name");
								if(dest.startsWith("40"))  // catches GPS coordinates instead of real name
									dest = "destination";
								String timeF = dateOut.format(parsedTime);
								String text = "Walk "+dist+" mi to "+dest;
								myItinerary.addItem(timeF, R.drawable.walk, text);
								myItinerary.addPoint(new NavPoint(origin, timeF+" - "+text, lat, lon, R.drawable.walk_dark));
							} else {
								JSONArray services = leg.getJSONArray("services");
								for(int j=0; j<services.length(); ++j) {
									JSONObject service = services.getJSONObject(0);
									JSONObject route = service.getJSONObject("route");
									String dir = service.getJSONObject("trip").getString("direction");
									String name = route.getString("route_short_name")+" "+dir+" "+route.getString("route_long_name");
		
									JSONObject begin = service.getJSONObject("begin");
									double lat = begin.getDouble("lat");
									double lon = begin.getDouble("lon");
									String originName = begin.getString("name");
									String time = begin.getString("time");
									Date parsedTime = dateIn.parse(time);
									
									end = service.getJSONObject("end");
									String dest = end.getString("name");
									
									String startID = begin.getString("stop_id");
									String stopID = end.getString("stop_id");
									String shapeID = service.getJSONObject("trip").getString("shape_id");

									String color = route.getString("route_color");

									String timeF = dateOut.format(parsedTime);
									String text;
									if(j==0)
										text = "Board "+name+" to "+dest;
									else
										text = "Continue on "+name+" to "+dest;
									myItinerary.addItem(timeF, R.drawable.bus, text);
									myItinerary.addPoint(new NavPoint(originName, timeF+" - "+text, lat, lon, R.drawable.bus_dark, startID, stopID, shapeID, color));
								}
								
								// if the next leg exists and is also a bus, add a wait for transfer line
								if(i+1 < legs.length()) {
									if(legs.getJSONObject(i+1).getString("type").equalsIgnoreCase("service")) {
										String loc = legs.getJSONObject(i+1).getJSONArray("services").getJSONObject(0).getJSONObject("begin").getString("name");
										Date parsedTime = dateIn.parse(end.getString("time"));
										myItinerary.addItem(dateOut.format(parsedTime), R.drawable.wait, "Wait for transfer at "+loc);
									}
								}
							}
						}
						String time = end.getString("time");
						double lat = end.getDouble("lat");
						double lon = end.getDouble("lon");
						Date parsedTime = dateIn.parse(time); 
						String text = "Arrive at destination";
						String timeF = dateOut.format(parsedTime);
						myItinerary.addItem(timeF, R.drawable.arrive, text);
						myItinerary.addPoint(new NavPoint("End", timeF+" - "+text, lat, lon, R.drawable.arrive_dark));
						
						myItinerary.createPointChain();
						itineraryList.add(myItinerary);
					}

					return itinerariesToView();

				}
			} catch (JSONException e) {
				msg = "Invalid server response";
				status = 501;
				e.printStackTrace();
			} catch (ParseException e) {
				msg = "Invalid server response";
				status = 502;
				e.printStackTrace();
			}

			return null;
		}

		// display a list of found stops, and hook them up to buttons that will pull up scheduled buses
		protected void onPostExecute(final View resultView) {
			subLayout.removeAllViews();
			if(resultView == null) {
				TextView foundStops = new TextView(DirectionsActivity.this);
				if(status == 1) {
					foundStops.setText("Location couldn't be established. Try again later.");
				} else if(status < 300) {
					foundStops.setText("Could not plan trip: "+msg);
				} else {
					foundStops.setText("Server error: "+msg+" ("+status+")");
				}
				subLayout.addView(foundStops);
			} else {
				// if they've never used the map feature, give them a hint to try it out
				SharedPreferences settings = getSharedPreferences(BaseTabActivity.PREFS_NAME,0);
				boolean hasTriedMap = settings.getBoolean("hasTriedMap", false);
				if(!hasTriedMap) {
					TextView mapHint = new TextView(DirectionsActivity.this);
					mapHint.setText("Tip: Select an itinerary to see it on the map!");
					subLayout.addView(mapHint);
				}
				
				subLayout.addView(resultView);
			}

			//myProgressDialog.dismiss();
			myToast.cancel();
			
			if(itineraryList.size() > 0)
				ParentActivity.setMapRoute(itineraryList.get(0));
		}

	}
}
