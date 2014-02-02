package com.kevinzielnicki.mtd.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.kevinzielnicki.mtd.BaseTabActivity;
import com.kevinzielnicki.mtd.R;
import com.kevinzielnicki.mtd.helpers.Departure;
import com.kevinzielnicki.mtd.helpers.RestClient;
import com.kevinzielnicki.mtd.helpers.Stop;
import com.kevinzielnicki.mtd.routeFinding.Itinerary;
import com.kevinzielnicki.mtd.routeFinding.Itinerary.PointList;

public class MyMapActivity extends MapActivity {
	BaseTabActivity ParentActivity;
	MyLocationOverlay locOver;
	Itinerary myItinerary;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ParentActivity = (BaseTabActivity) this.getParent();

		setContentView(R.layout.map);
		

		MapView myMap = (MapView)findViewById(R.id.myMapView);
		List<Overlay> overlays = myMap.getOverlays();
		locOver = new MyLocationOverlay(this,myMap);
		overlays.add(locOver);
	}
	

	@Override
	public void onResume() {
		super.onResume();
		
		MapView myMap = (MapView)findViewById(R.id.myMapView);
		MapController control = myMap.getController();
		
		
		//GeoPoint loc = new GeoPoint(ParentActivity.loc.getLocation().getLatitude(),ParentActivity.loc.getLocation().getLongitude());
		Location loc = ParentActivity.loc.getLocation();
		if(loc != null) {
			GeoPoint geoLoc = new GeoPoint((int)(loc.getLatitude() * 1e6),(int)(loc.getLongitude() * 1e6));
			control.setCenter(geoLoc);
			
			locOver.enableMyLocation();
			locOver.enableCompass();
		}
		control.setZoom(17);     
		myMap.setBuiltInZoomControls(true);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		locOver.disableMyLocation();
		locOver.disableCompass();
		
		
	}
	
	// draw the route if it is set
	public void drawRoute(List<PointList> route) {		
		MapView myMap = (MapView)findViewById(R.id.myMapView);
		List<Overlay> overlays = myMap.getOverlays();
		
		// if the itinerary is done, and the route isn't empty, draw it now
		if(route != null) {
			GeoPoint lastPoint = null;
			int lastColor = Color.RED;
			for(PointList plist : route) {
				for(GeoPoint p : plist) {
					if(lastPoint != null) {
						RouteOverlay testRoute = new RouteOverlay(lastPoint,p,lastColor);
						overlays.add(testRoute);
					}
					lastPoint = p;
					lastColor = plist.color;
				}
			}
		}
	}
	
	// clear overlay to the base view with just the my location indicator
	public void clearOverlays() {
		MapView myMap = (MapView)findViewById(R.id.myMapView);
		List<Overlay> overlays = myMap.getOverlays();
		overlays.clear();
		overlays.add(locOver);
	}
	
	// draw all of our available stops
	public void drawStops(ArrayList<NavPoint> points) {		
		MapView myMap = (MapView)findViewById(R.id.myMapView);
		List<Overlay> overlays = myMap.getOverlays();
		MapController control = myMap.getController();
		

		Drawable walk = this.getResources().getDrawable(R.drawable.walk_dark);
		MapPoints myPoints = new MapPoints(walk,this,ParentActivity);
		if(points.size() > 0)
			control.setCenter(points.get(0).loc);
		for(NavPoint p : points) {
			//OverlayItem overlayitem =  new OverlayItem(p.loc, p.name, p.description);
			myPoints.addOverlay(p, this.getResources().getDrawable(p.resID));
		}
		overlays.add(myPoints);
	}
	
	// show a list of bus stops on the map
	public void setupOverlay(List<Stop> stopList) {
		
		// draw on a blank slate
		clearOverlays();
		
		// draw the stop points
		ArrayList<NavPoint> points = new ArrayList<NavPoint>();
		for(Stop s : stopList) {
			NavPoint myPoint = new NavPoint(s.name, s.name, Double.parseDouble(s.lat), Double.parseDouble(s.lon), R.drawable.bus_dark, s.ID, "", "", "000000"); 
			points.add(myPoint);
		}
		drawStops(points);
	}
	
	// draw the overlay given a bus departing at d from stop s, finding the bus's route
	public void setupOverlay(Departure d, Stop s) {
		
		// draw on a blank slate
		clearOverlays();
		
		// draw the stop point while waiting for the route to finish
		ArrayList<NavPoint> points = new ArrayList<NavPoint>();
		NavPoint myPoint = new NavPoint(s.name, d.name + ": Leaving at "+d.timeDeparting, Double.parseDouble(s.lat), Double.parseDouble(s.lon), R.drawable.bus_dark, s.ID, null, d.shapeID, d.color); 
		points.add(myPoint);
		drawStops(points);
		
		// find the route in an async task
		new GetRoute().execute(myPoint);
		
	}
	
	// draw the overlay for an itinerary, including all stops and the route between them
	public void setupOverlay(Itinerary itin) {
		myItinerary = itin;
		
		// draw on a blank slate
		clearOverlays();
		
		if(itin.isDone()) {
			drawRoute(myItinerary.getRoute());
			
		} else {
			// otherwise run a thread to keep checking until itinerary is done updating
			Thread updateThread = new Thread() {
				public void run () {
					int checks = 0;
					while(myItinerary.isDone() == false && checks++ < 60) {
						// sleep for 1 second
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
						if(myItinerary.retry())
							myItinerary.createPointChain();
					}
					// draw the route, if it was found
					if(myItinerary.succeeded())
						drawRouteCallback.sendEmptyMessage(0);
				}
			};
			updateThread.start();
			
		}
		
		// always draw the stop points
		drawStops(myItinerary.getPoints());
		
	}

	private Handler drawRouteCallback = new Handler () {
		public void handleMessage (Message msg) {
			// draw on a blank slate
			clearOverlays();
			
			drawRoute(myItinerary.getRoute());

			drawStops(myItinerary.getPoints());
		}
	};

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	// Get route points in the background
	private class GetRoute extends AsyncTask<NavPoint, Void, ArrayList<PointList>> {
		int status = 501;
		NavPoint myPoint;
		String msg = "Unknown Error";
		ProgressDialog myProgressDialog = null;

		protected void onPreExecute() {
			myProgressDialog = ProgressDialog.show(MyMapActivity.this,"", "Getting route...\n\n(Data provided by CUMTD)", true);
		}
		
		protected ArrayList<PointList> doInBackground(NavPoint... point) {
			myPoint = point[0];
			String method = "GetShape";
			HashMap<String,String> args = new HashMap<String,String>();
			args.put("shape_id", point[0].shapeID);
			PointList routePoints = new PointList(point[0].color);
			JSONObject result = RestClient.MTD(method, args);
			if(result == null) {
				status = 404;
				return null;
			}
			try {
				status = result.getJSONObject("status").getInt("code");
				msg = result.getJSONObject("status").getString("msg");
				if(status >= 300) {
					Log.i("nav","status failed: "+status+": "+msg);
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
				msg = "Invalid Server Response";
				return null;
			}
			ArrayList<PointList> resultList = new ArrayList<PointList>();
			resultList.add(routePoints);
			return resultList;
		}

		protected void onPostExecute(ArrayList<PointList> route) {
			myProgressDialog.dismiss();
			if(route != null) {
//				route = points;
//				routeFinalized = true;
				Log.i("nav","got route points for "+myPoint.shapeID);
				// draw on a blank slate
				clearOverlays();
				
				drawRoute(route);

				ArrayList<NavPoint> point = new ArrayList<NavPoint>();
				point.add(myPoint);
				drawStops(point);
			} else if(status == 404) {
//				retry = true;
//				Log.i("nav","retrying"+id);
				Toast.makeText(MyMapActivity.this, "Could not connect to server. Try again later.", Toast.LENGTH_SHORT).show();
			} else {
//				routeFailed = true;
//				Log.i("nav","failed to get "+id);
				Toast.makeText(MyMapActivity.this, "Could not find route. Try again later.", Toast.LENGTH_SHORT).show();
			}
		}

	}
}
