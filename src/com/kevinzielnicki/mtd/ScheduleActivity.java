package com.kevinzielnicki.mtd;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kevinzielnicki.mtd.helpers.Departure;
import com.kevinzielnicki.mtd.helpers.DirectionsDialog;
import com.kevinzielnicki.mtd.helpers.RestClient;
import com.kevinzielnicki.mtd.helpers.Stop;
import com.kevinzielnicki.mtd.routeFinding.DirectionsActivity;

public class ScheduleActivity extends Activity {
	/** Called when the activity is first created. */
	//String stopID = "IT";
	//String stopName = "Illinois Terminal";
	//String stopLat = "40.115935";
	//String stopLon = "-88.240947";
	Stop thisStop;
	LinearLayout subLayout;
	TextView stopNameTextView;
	Thread updateThread;
	boolean updateThreadGo = true; 

	BaseTabActivity ParentActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {     
		super.onCreate(savedInstanceState);
		
		ParentActivity = (BaseTabActivity) this.getParent();

		RelativeLayout myLayout = new RelativeLayout(this);
		//myLayout.setOrientation(LinearLayout.VERTICAL);
		setContentView(myLayout);

		// create stop title
		stopNameTextView = new TextView(this);
		stopNameTextView.setTextSize(23);
		stopNameTextView.setId(1);
		myLayout.addView(stopNameTextView);
		
		// create a thin grey dividing line
		View ruler = new View(this);
		ruler.setBackgroundColor(0xFF808080);
		ruler.setId(2);
		RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, 2);
		relativeParams.addRule(RelativeLayout.BELOW,1);
		myLayout.addView(ruler,relativeParams);

		// create a linear sublayout inside the main layout, surrounded by a scroll layout
		//ScrollView sv = new ScrollView(this);
		relativeParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		relativeParams.addRule(RelativeLayout.BELOW,2);
		relativeParams.addRule(RelativeLayout.ABOVE,3);
		subLayout = new LinearLayout(this);
		subLayout.setOrientation(LinearLayout.VERTICAL);
		//sv.addView(subLayout);
		//myLayout.addView(sv,relativeParams);
		myLayout.addView(subLayout,relativeParams);

//		relativeParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//		relativeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//		relativeParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//
//		Button directionsTo = new Button(this);
//		directionsTo.setId(3);
//		directionsTo.setText("Bus Route to Here");
//		myLayout.addView(directionsTo,relativeParams);
//		directionsTo.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				//showDialog(DirectionsDialog.DIALOG_DIR_TO);
//
//				DirectionsDialog dialog = new DirectionsDialog(ScheduleActivity.this,ParentActivity);
//				dialog.setDestination(thisStop);
//				dialog.show();
//			}
//		});
//
//
//		relativeParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//		relativeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//		relativeParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//
//		Button directionsFrom = new Button(this);
//		directionsFrom.setText("Bus Route from Here");
//		myLayout.addView(directionsFrom,relativeParams);
//		directionsFrom.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				//showDialog(DirectionsDialog.DIALOG_DIR_FROM);
//				
//				DirectionsDialog dialog = new DirectionsDialog(ScheduleActivity.this,ParentActivity);
//				dialog.setOrigin(thisStop);
//				dialog.show();
//			}
//		});



	}

	@Override
	public void onResume() {
		super.onResume();

		// Load preferences
		SharedPreferences settings = getSharedPreferences(BaseTabActivity.PREFS_NAME, 0);
		String stopID = settings.getString("stopID", "");
		String stopName = settings.getString("stopName", "");
		String stopLat = settings.getString("stopLat", "");
		String stopLon = settings.getString("stopLon", "");

		thisStop = new Stop(stopName,stopID,stopLat,stopLon);

		Log.i("HASDKL","resumed!!!");


		updateThreadGo = true;
		updateThread = new Thread() {
			public void run () {
				while(updateThreadGo == true) {
					// do stuff
					uiCallback.sendEmptyMessage(0);
					// sleep for 3 seconds
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		updateThread.start();

		//android.R.drawable.ic_search_category_default
	}

	@Override
	public void onPause() {
		super.onPause();

		updateThreadGo = false;
		updateThread.interrupt();
	}

//	protected Dialog onCreateDialog(final int id) {
//		Stop thisStop = new Stop(stopName,stopID,stopLat,stopLon);
//		DirectionsDialog dialog = new DirectionsDialog(this,ParentActivity);
//		if(id == DirectionsDialog.DIALOG_DIR_FROM)
//			dialog.setOrigin(thisStop);
//		else
//			dialog.setDestination(thisStop);
//		return dialog;
//	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.schedule_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		DirectionsDialog dialog;
	    switch (item.getItemId()) {
	        case R.id.route_from_here:
				dialog = new DirectionsDialog(ScheduleActivity.this,ParentActivity);
				dialog.setOrigin(thisStop);
				dialog.show();
	            return true;
	        case R.id.route_to_here:
				dialog = new DirectionsDialog(ScheduleActivity.this,ParentActivity);
				dialog.setDestination(thisStop);
				dialog.show();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	public ListView departuresToView(final List<Departure> departures) {
		ListView stopList = new ListView(this);
		stopList.setAdapter(new Departure.Adapter(this, R.layout.departure, departures));

		// when clicked, plot this stop on the map (if it is a real route)
		stopList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Departure d = (Departure)parent.getItemAtPosition(position);
				if(d.shapeID != null)
					ParentActivity.switchToMap(d, thisStop);
			}
		});
		
//		// on a long press, make this a favorite stop
//		stopList.setOnItemLongClickListener(new OnItemLongClickListener() {
//
//			@Override
//			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//				return false;
//			}
//			
//		});
		return stopList;
	}

	private Handler uiCallback = new Handler () {
		@SuppressWarnings("unchecked")
		public void handleMessage (Message msg) {
			HashMap<String,String> args = new HashMap<String,String>();
			args.put("stop_id", thisStop.ID);
			new UpdateArrivals().execute(args);
		}
	};

	// Update arriving buses in the background
	private class UpdateArrivals extends AsyncTask<HashMap<String,String>, Void, List<Departure>> {
		//ProgressDialog myProgressDialog = null;
		int status = 200;
		Toast myToast;

		protected void onPreExecute() {
			//myProgressDialog = ProgressDialog.show(ScheduleActivity.this,"", "Updating...\n\n(Data provided by CUMTD)", true);
			myToast = Toast.makeText(ScheduleActivity.this, "Updating...\n(Data provided by CUMTD)", Toast.LENGTH_LONG);
			myToast.show();
		}

		protected List<Departure> doInBackground(HashMap<String,String>... args) {
			String method = "GetDeparturesByStop";
			JSONObject result = RestClient.MTD(method, args[0]);
			if(result == null) {
				status = 500;
				return null;
			}
			try {
				status = result.getJSONObject("status").getInt("code");
				if(status >= 300)
					return null;
				JSONArray departures = result.getJSONArray("departures");
				ArrayList<Departure> departureList = new ArrayList<Departure>();
				if(departures.length() > 0) {
					for(int i=0; i<departures.length(); ++i) {
						JSONObject bus = departures.getJSONObject(i);
						//Log.i("Praeda","<jsonobject>\n"+bus.toString()+"\n</jsonobject>");
						String shapeID = null;
						String color = "000000";
						try {
							shapeID = bus.getJSONObject("trip").getString("shape_id");
							color = bus.getJSONObject("route").getString("route_color");
						} catch (JSONException e) {
							status = 500;
							e.printStackTrace();
						}


						String timeF;
						try {
							Date parsedTime = DirectionsActivity.dateIn.parse(bus.getString("expected"));
							timeF = DirectionsActivity.dateOut.format(parsedTime);
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							timeF = "Unknown";
						}
						departureList.add(new Departure(bus.getString("headsign"), bus.getInt("expected_mins"), timeF, shapeID, color));
						//departureText.put(bus.getInt("expected_mins"), bus.getString("headsign"));
					}
					return departureList;
				}
			} catch (JSONException e) {
				status = 500;
				e.printStackTrace();
			}

			return null;
		}

		protected void onPostExecute(List<Departure> departures) {
			subLayout.removeAllViews();
			stopNameTextView.setText(thisStop.name);
			if(departures == null) {
				TextView nextStop = new TextView(ScheduleActivity.this);
				if(status < 300) {
					nextStop.setText("There are no buses scheduled");
				} else {
					nextStop.setText("Server communication error!");
				}
				subLayout.addView(nextStop);
			} else {

				ListView departureList = departuresToView(departures);
				subLayout.addView(departureList);
			}
			//myProgressDialog.dismiss();
			myToast.cancel();
		}

	}
}