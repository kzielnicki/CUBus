package com.kevinzielnicki.mtd.helpers;

import java.text.DecimalFormat;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kevinzielnicki.mtd.R;


// simple class to define a bus stop
public class Stop implements Comparable<Stop> {
	public static final String INVALID_STOP_ID = "XXX_NO_SUCH_STOP";
	public String name;
	public String ID;
	public String lat,lon;
	public double distanceFeet;
	public boolean isFavorite;
	
	public Stop(String myName, String myID, String myLat, String myLon, boolean myFav) {
		name = myName;
		ID = myID;
		lat = myLat;
		lon = myLon;
		distanceFeet = 0;
		isFavorite = myFav;
	}
	
	public Stop(String myName, String myID, String myLat, String myLon) {
		this(myName, myID, myLat, myLon, false);
	}
	
	public boolean equals(Stop s) {
		return s.ID.equals(this.ID);
	}
	
	@Override
	public String toString() {
		if(distanceFeet == 0) {
			return /*(isFavorite?"(F) ":"(N) ")+*/name;
		} else {
			double distanceMiles = distanceFeet / 5280;
			DecimalFormat df = new DecimalFormat("#.#");
			return df.format(distanceMiles)+" mi - " + name;
		}
	}
	
	public void toggleFavorite() {
		isFavorite = !isFavorite;
	}

	@Override
	public int compareTo(Stop another) {
		return this.name.compareTo(another.name);
	}
	
	// return true if this stop matches "searchName"
	public boolean matches(String searchName) {
		searchName = searchName.toLowerCase();
		String testName = name.toLowerCase();
		String[] subStrings = searchName.split("\\s");
		for(String s : subStrings) {
			if(!testName.contains(s))
				return false;
		}
		
		return true;
	}
	
	

	// extend array adapter for custom views of bus stops in a list
	public static class Adapter extends ArrayAdapter<Stop> {
		private List<Stop> stops;
	
		public Adapter(Context context, int textViewResourceId,	List<Stop> users) {
			super(context, textViewResourceId, users);
			this.stops = users;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	
			View v = convertView;
	
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.stop, null);
			}
	
			Stop stop = stops.get(position);
	
			if (stop != null) {
	
				TextView stopName = (TextView) v.findViewById(R.id.stopText);
				ImageView stopFavorite = (ImageView) v.findViewById(R.id.stopFavorite);
	
				stopName.setText(stop.toString());
	
				if (!stop.isFavorite) {
					stopFavorite.setVisibility(View.GONE);
					stopName.setPadding(15, 10, 10, 10);
				} else {
					stopFavorite.setVisibility(View.VISIBLE);
					stopName.setPadding(0, 10, 10, 10);
				}
	
			}
	
			return v;
	
		}
	
	}
}
