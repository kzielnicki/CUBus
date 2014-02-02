package com.kevinzielnicki.mtd.routeFinding;


import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.kevinzielnicki.mtd.helpers.RestClient;
import com.kevinzielnicki.mtd.mapping.NavPoint;

public class Itinerary {
	ArrayList<Item> displayItems;
	public ArrayList<NavPoint> stops;
	private ArrayList<PointList> route;
	boolean routeFinalized = false;
	boolean routeFailed = false;
	boolean retry = false;
	int id;
	String travelTime;
	DirectionsActivity parentActivity;
	
	public Itinerary(int id, String time, DirectionsActivity parent) {
		this.id = id;
		travelTime = time;
		parentActivity = parent;
		displayItems = new ArrayList<Item>();
		stops = new ArrayList<NavPoint>();
		route = new ArrayList<PointList>();
	}
	
	public void addItem(String time, int resID, String text) {
		displayItems.add(new Item(time, resID, text));
	}
	
	public void addPoint(NavPoint np) {
		stops.add(np);
	}
	
	// to call after all items have been added to the itinerary, create a list of geopoints from stop to stop
	@SuppressWarnings("unchecked")
	public void createPointChain() {
		retry = false;
		new GetRoute().execute(stops);
	}
	
	public boolean isDone() {
		return routeFinalized || routeFailed;
	}
	
	public boolean succeeded() {
		return routeFinalized;
	}

	public boolean retry() {
		return retry;
	}
	
	public ArrayList<PointList> getRoute() {
		if(!routeFinalized)
			return null;
		else
			return route;
	}
	
	public ArrayList<NavPoint> getPoints() {
		return stops;
	}
	
	public View toView() {
		LinearLayout myView = new LinearLayout(parentActivity);
		myView.setOrientation(LinearLayout.VERTICAL);
		TextView label = new TextView(parentActivity);
		label.setText("Itinerary "+(id+1)+" ("+travelTime+" min)");
		label.setTextSize(23);
		myView.addView(label);
		
		for(Item i : displayItems) {
			myView.addView(i.toView());
		}
		
		return myView;
	}
	
	
	
	// Get route points in the background
	private class GetRoute extends AsyncTask<ArrayList<NavPoint>, Void, ArrayList<PointList>> {
		int status = 501;

		protected ArrayList<PointList> doInBackground(ArrayList<NavPoint>... points) {
			
			String method = "GetShapeBetweenStops";
			HashMap<String,String> args = new HashMap<String,String>();
			ArrayList<PointList> allPoints = new ArrayList<PointList>();
			for(NavPoint p : points[0]) {
				PointList routePoints = new PointList(p.color);
				if(p.hasRoute()) {
					args.put("begin_stop_id", p.startID);
					args.put("end_stop_id", p.stopID);
					args.put("shape_id", p.shapeID);
					Log.i("route",p.shapeID);
					JSONObject result = RestClient.MTD(method, args);
					if(result == null) {
						status = 404;
						return null;
					}
					try {
						status = result.getJSONObject("status").getInt("code");
						if(status >= 300) {
							Log.i("nav","status failed: "+status+": "+result.getJSONObject("status").getString("msg"));
							return null;
						}
						JSONArray shape = result.getJSONArray("shapes");
						for(int i=0; i<shape.length(); ++i) {
							JSONObject shapePoint = shape.getJSONObject(i);
							double lat = shapePoint.getDouble("shape_pt_lat");
							double lon = shapePoint.getDouble("shape_pt_lon");
							GeoPoint loc = new GeoPoint((int)(lat * 1e6),(int)(lon * 1e6));
							routePoints.add(loc);
						}
					} catch (JSONException e) {
						e.printStackTrace();
						return null;
					}

				} else {
					routePoints.add(p.loc);
				}
				allPoints.add(routePoints);
			}
			return allPoints;
		}

		protected void onPostExecute(ArrayList<PointList> points) {
			if(points != null) {
				route = points;
				routeFinalized = true;
				Log.i("nav","got route points for "+id);
			} else if(status == 404) {
				retry = true;
				Log.i("nav","retrying"+id);
			} else {
				routeFailed = true;
				Log.i("nav","failed to get "+id);
			}
		}

	}
	
	// subclass for list of points, just a wrapper around an arraylist that also gives a color
	@SuppressWarnings("serial")
	public static class PointList extends ArrayList<GeoPoint> {
		public int color;
		
		public PointList(int color) {
			super();
			this.color = color;
		}
	}
	
	// subclass for each line on the itinerary
	public class Item {
		String time;
		int resID;
		String text;
		
		public Item(String tm, int res, String txt) {
			time = tm;
			resID = res;
			text = txt;
		}
		
		public View toView() {

			LinearLayout legView = new LinearLayout(parentActivity);
			
			TextView timeView = new TextView(parentActivity);
			timeView.setText(time);
			timeView.setTextSize(15);
			legView.addView(timeView);
			
			ImageView image = new ImageView(parentActivity);
			image.setImageResource(resID);
			legView.addView(image);
			
			TextView textView = new TextView(parentActivity);
			textView.setText(text);
			textView.setTextSize(15);
			legView.addView(textView);
			
			return legView;
		}
	}

	

	// extend array adapter for custom views of itineraries in a list
	public static class Adapter extends ArrayAdapter<Itinerary> {
		private ArrayList<Itinerary> itins;
	
		public Adapter(Context context, int textViewResourceId,	ArrayList<Itinerary> users) {
			super(context, textViewResourceId, users);
			this.itins = users;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	
			View v = convertView;
	
			if (v == null) {
				//LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = new LinearLayout(this.getContext());
			}
	
			Itinerary it = itins.get(position);
	
			if (it != null) {
				v = it.toView();
			}
	
			return v;
	
		}
	
	}


}
