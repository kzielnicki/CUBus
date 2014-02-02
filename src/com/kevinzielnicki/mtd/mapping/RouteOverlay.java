package com.kevinzielnicki.mtd.mapping;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

// creates an overlay that draws a line between two geopoints
public class RouteOverlay extends Overlay {
	private GeoPoint gp1;
	private GeoPoint gp2;
	private int color;

	public RouteOverlay(GeoPoint gp1, GeoPoint gp2, int color)
	{
		this.gp1 = gp1;
		this.gp2 = gp2;
		this.color = color;
	}

	public RouteOverlay(GeoPoint gp1, GeoPoint gp2)
	{
		this(gp1, gp2, Color.RED);
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
			long when) {
		//Log.i("route","trying to draw");
		Projection projection = mapView.getProjection();
		if (shadow == false) {
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			Point point = new Point();
			projection.toPixels(gp1, point);
			paint.setColor(color);
			Point point2 = new Point();
			projection.toPixels(gp2, point2);
			paint.setStrokeWidth(7);
			paint.setAlpha(150);
			canvas.drawLine(point.x, point.y, point2.x, point2.y, paint);
		}
		return super.draw(canvas, mapView, shadow, when);
	}

}
