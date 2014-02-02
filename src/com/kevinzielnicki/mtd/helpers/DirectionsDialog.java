package com.kevinzielnicki.mtd.helpers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.kevinzielnicki.mtd.BaseTabActivity;
import com.kevinzielnicki.mtd.R;

public class DirectionsDialog extends Dialog {
	public static int DIALOG_DIR_TO = 0;
	public static int DIALOG_DIR_FROM = 1;
	
	final EditText directionSearchFrom;
	final EditText directionSearchTo;
	final TimePicker directionsTimePicker;
	Button getGPS;
	
	Stop fromStop, toStop; 
	
	BaseTabActivity ParentActivity;
	
	boolean timeSet = false;

	public DirectionsDialog(final Context context, final BaseTabActivity ParentActivity) {
		super(context);

		this.ParentActivity = ParentActivity;

		setContentView(R.layout.directions_dialog);
		
		setTitle("Get Directions");

		Button cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		getGPS = (Button) findViewById(R.id.getGPS);
		getGPS.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(ParentActivity.loc.isSet()) {
					DecimalFormat f = new DecimalFormat("#.######");

					Stop fakeStop = new Stop("My GPS Location", Stop.INVALID_STOP_ID, f.format(ParentActivity.loc.getLocation().getLatitude()), f.format(ParentActivity.loc.getLocation().getLongitude()));
					setStop(fakeStop,R.id.directionsFromStopList);
				} else {
					Log.i("loc","couldn't get gps");
					Toast.makeText(context, "Location couldn't be established. Try again later.", Toast.LENGTH_SHORT).show();
				}
			}
		});

		Button search = (Button) findViewById(R.id.search);
		search.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(fromStop != null && toStop != null) {
					String time = null;
					if(timeSet) {
						int hour = directionsTimePicker.getCurrentHour();
						int minute = directionsTimePicker.getCurrentMinute();
						DecimalFormat f = new DecimalFormat("00");
						time = f.format(hour)+":"+f.format(minute);
					} 
					ParentActivity.switchToDirections(fromStop, toStop, time);

					dismiss();
				} else {
					Toast.makeText(context, "Please select your origin and destination.", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		directionSearchFrom = (EditText) findViewById(R.id.directionSearchFrom);
		
		TextWatcher searchFromWatcher = new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) { 
				ListView directionsFromStopList = (ListView) findViewById(R.id.directionsFromStopList);
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)directionsFromStopList.getLayoutParams();
				if(!s.toString().equals("")) {
					new SearchCache().execute(directionSearchFrom);
					params.height = 200;
					directionsFromStopList.setLayoutParams(params);
				} else {
					params.height = 5;
					directionsFromStopList.setLayoutParams(params);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		};


		directionSearchFrom.addTextChangedListener(searchFromWatcher);
		
		directionSearchTo = (EditText) findViewById(R.id.directionSearchTo);
		
		TextWatcher searchToWatcher = new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) { 
				ListView directionsToStopList = (ListView) findViewById(R.id.directionsToStopList);
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)directionsToStopList.getLayoutParams();
				if(!s.toString().equals("")) {
					new SearchCache().execute(directionSearchTo);
					params.height = 200;
					directionsToStopList.setLayoutParams(params);
				} else {
					params.height = 5;
					directionsToStopList.setLayoutParams(params);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		};


		directionSearchTo.addTextChangedListener(searchToWatcher);
		
		directionsTimePicker = (TimePicker) findViewById(R.id.directionsTimePicker);
		
		setTimeToNow();
	}
	
	public void setDestination(Stop s) {
		setStop(s,R.id.directionsToStopList);
	}
	
	public void setOrigin(Stop s) {
		setStop(s,R.id.directionsFromStopList);
	}
	
	private void resetLayout(LinearLayout layout) {
		layout.removeAllViews();
		if(layout.getId() == R.id.directionFromLayout) {
			directionSearchFrom.setText("");
			layout.addView(directionSearchFrom);
			layout.addView(getGPS);
			fromStop = null;
		} else if(layout.getId() == R.id.directionToLayout) {
			directionSearchTo.setText("");
			layout.addView(directionSearchTo);
			toStop = null;
		} else if(layout.getId() == R.id.directionTimeLayout) {
			Calendar c = Calendar.getInstance();
			directionsTimePicker.setCurrentHour(c.get(Calendar.HOUR_OF_DAY));
			directionsTimePicker.setCurrentMinute(c.get(Calendar.MINUTE));
			layout.addView(directionsTimePicker);
			timeSet = true;
		}
		
	}

	// set the stop that is paired with the listview id provided
	private void setStop(Stop stop, int id) {
		final LinearLayout layout;
		final ListView directionsStopList;
		

		LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View stopHolder = vi.inflate(R.layout.stop_holder, null);
		
		
		ImageView cancel = (ImageView)stopHolder.findViewById(R.id.cancel);
		cancel.setImageDrawable(getContext().getResources().getDrawable(R.drawable.close));
		cancel.setId(22);
		
		TextView selectedStop = (TextView)stopHolder.findViewById(R.id.selectedStop);
		selectedStop.setTextSize(18);
		selectedStop.setId(23);
		
		
		// decide whether we are coming or going
		if(id == R.id.directionsFromStopList) {
			fromStop = stop;
			layout = (LinearLayout) findViewById(R.id.directionFromLayout);
			selectedStop.setText("From: "+stop.toString());
			directionsStopList = (ListView) findViewById(R.id.directionsFromStopList);
		} else {
			toStop = stop;
			layout = (LinearLayout) findViewById(R.id.directionToLayout);
			selectedStop.setText("To: "+stop.toString());
			directionsStopList = (ListView) findViewById(R.id.directionsToStopList);
		}
		
		cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				resetLayout(layout);
			}
		});
		
		
		
		// finish setting up
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)directionsStopList.getLayoutParams();
		params.height = 5;
		directionsStopList.setLayoutParams(params);
		layout.removeAllViews();
		layout.addView(stopHolder);
	}
	
	public void setTimeToNow() {
		// default behavior for time picker is to not be there
		LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View stopHolder = vi.inflate(R.layout.stop_holder, null);
		final LinearLayout layout = (LinearLayout) findViewById(R.id.directionTimeLayout);
		
		
		ImageView cancelImg = (ImageView)stopHolder.findViewById(R.id.cancel);
		cancelImg.setImageDrawable(getContext().getResources().getDrawable(R.drawable.close));
		cancelImg.setId(22);
		
		TextView selectedStop = (TextView)stopHolder.findViewById(R.id.selectedStop);
		selectedStop.setTextSize(18);
		selectedStop.setId(23);
		
		selectedStop.setText("Leaving: Now");
		
		cancelImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				resetLayout(layout);
			}
		});
		
		layout.removeAllViews();
		layout.addView(stopHolder);
	}
	
	private void stopsToView(final ArrayList<Stop> stops, ListView directionsStopList) {
		directionsStopList.setAdapter(new Stop.Adapter(getContext(), R.layout.stop, stops));

		// when clicked, we should see the other tab with all the current buses
		directionsStopList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				InputMethodManager imm = (InputMethodManager)ParentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
				
				setStop((Stop)parent.getItemAtPosition(position),parent.getId());
			}
		});
		
		directionsStopList.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(AbsListView list, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onScrollStateChanged(AbsListView list, int state) {
				if(state == SCROLL_STATE_TOUCH_SCROLL) {
					InputMethodManager imm = (InputMethodManager)ParentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(list.getWindowToken(), 0);
				}
			}
			
		});
	}
	

	
	// Asynchronous task to search cached stop names
	private class SearchCache extends AsyncTask<EditText, Void, ArrayList<Stop>> {
		EditText directionSearch;
		
		@Override
		protected ArrayList<Stop> doInBackground(EditText... arg0) {
			directionSearch = arg0[0];
			String searchName = directionSearch.getText().toString();

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
			//Log.i("dir","done searching..");
			
			// if we got a null stop list, let the user know that the stops aren't done loading
//			if(stops == null) {
//				if(ParentActivity.retrievingStops)
//					Toast.makeText(getContext(), "Loading stop data, please wait.", Toast.LENGTH_SHORT).show();
//			} else
			if(stops != null) {
				if(directionSearch != null) {
					if(!directionSearch.getText().toString().equals("")) {
						//Log.i("dir","found!");
						if(directionSearch.getId() == R.id.directionSearchFrom)
							stopsToView(stops, (ListView)findViewById(R.id.directionsFromStopList));
						else
							stopsToView(stops, (ListView)findViewById(R.id.directionsToStopList));
					}
				}
			}
		}
		
	}
}
