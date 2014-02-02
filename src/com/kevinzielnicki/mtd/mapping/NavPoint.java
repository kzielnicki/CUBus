package com.kevinzielnicki.mtd.mapping;

import java.text.DecimalFormat;

import android.graphics.Color;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;
import com.kevinzielnicki.mtd.helpers.Stop;

public class NavPoint extends OverlayItem {
//	public static final int WALK_POINT = 0;
//	public static final int BUS_POINT = 1;
//	public static final int DEST_POINT = 2;

	public GeoPoint loc;
	int resID;
	String name;
	String description;
	public String startID;
	public String stopID;
	public String shapeID;
	public int color;
	double lat, lon; 
	
	public NavPoint(String name, String desc, double lat, double lon, int res) {
		super(new GeoPoint((int)(lat * 1e6),(int)(lon * 1e6)),name,desc);
		this.name = name;
		this.description = desc;
		this.lat = lat;
		this.lon = lon;
		loc = new GeoPoint((int)(lat * 1e6),(int)(lon * 1e6));
		this.resID = res;
		startID = null;
		stopID = null;
		shapeID = null;
		color = Color.RED;
	}
	
	public NavPoint(String name, String desc, double lat, double lon, int res, String startID, String stopID, String shapeID, String color) {
		this(name, desc, lat,lon,res);
		this.startID = startID;
		this.stopID = stopID;
		this.shapeID = shapeID;
		

		color = "#"+color;
		int parsedColor = Color.RED;
		try {
			parsedColor = Color.parseColor(color);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		// not all colors are equally visible, convert some to darker versions
		float hsv[] = new float[3];
		Color.colorToHSV(parsedColor, hsv);
		if((hsv[0] < 70 && hsv[0] > 50) || hsv[1] < 50)
			hsv[2] *= 0.5f;
		this.color = Color.HSVToColor(hsv);
	}
	
	public boolean hasRoute() {
		return startID != null && stopID != null && shapeID != null;
	}
	
	public boolean isStop() {
		return startID != null;
	}
	
	// convert this NavPoint to a stop
	public Stop toStop() {
		DecimalFormat f = new DecimalFormat("#.######");
		String latS = f.format(lat);
		String lonS = f.format(lon);
		return new Stop(name, startID, latS, lonS);
	}
}
