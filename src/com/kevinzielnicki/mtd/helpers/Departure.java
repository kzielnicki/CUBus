package com.kevinzielnicki.mtd.helpers;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.kevinzielnicki.mtd.R;

public class Departure {
	public String name;
	public int timeLeft;
	public String timeDeparting;
	public String shapeID;
	public String color;  // an unparsed color of the MTD format
	
	public Departure(String name, int time, String leaving, String shapeID, String color) {
		this.name = name;
		this.timeLeft = time;
		this.timeDeparting = leaving;
		this.shapeID = shapeID;
		this.color = color;
	}
	
	

	// extend array adapter for custom views of bus stops in a list
	public static class Adapter extends ArrayAdapter<Departure> {
		private List<Departure> departures;
	
		public Adapter(Context context, int textViewResourceId,	List<Departure> users) {
			super(context, textViewResourceId, users);
			this.departures = users;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	
			View v = convertView;
	
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.departure, null);
			}
	
			Departure departure = departures.get(position);
	
			if (departure != null) {
	
				TextView busName = (TextView) v.findViewById(R.id.busName);
				TextView arrivalTime = (TextView) v.findViewById(R.id.arrivalTime);
	
				busName.setText(departure.name);
				if(departure.timeLeft == 0)
					arrivalTime.setText("DUE");
				else
					arrivalTime.setText(departure.timeLeft + " min");
//				if(departure.time < 4) {
//					arrivalTime.setTextColor(Color.RED);
//				} else {
//					arrivalTime.setTextColor(Color.WHITE);
//				}
			}
	
			return v;
	
		}
	
	}
}
