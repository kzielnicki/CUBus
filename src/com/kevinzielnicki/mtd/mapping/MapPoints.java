package com.kevinzielnicki.mtd.mapping;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.kevinzielnicki.mtd.BaseTabActivity;

public class MapPoints extends ItemizedOverlay<NavPoint> {

	private ArrayList<NavPoint> mapOverlays = new ArrayList<NavPoint>();

	private Context context;
	
	private BaseTabActivity baseParent;

	public MapPoints(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	public MapPoints(Drawable defaultMarker, Context context, BaseTabActivity parent) {
		this(defaultMarker);
		this.context = context;
		baseParent = parent;
	}

	@Override
	protected NavPoint createItem(int i) {
		return mapOverlays.get(i);
	}

	@Override
	public int size() {
		return mapOverlays.size();
	}

	@Override
	protected boolean onTap(int index) {
		final NavPoint item = mapOverlays.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		if(item.isStop()) {
			dialog.setPositiveButton("View Schedule", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		                baseParent.switchToSchedule(item.toStop());
		           }
		       });
		}
		
		dialog.setNeutralButton("OK", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                dialog.cancel();
	           }
	       });
		
		if(!item.getTitle().equals(item.getSnippet()))
			dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();
		return true;
	}

	public void addOverlay(NavPoint overlay) {
		mapOverlays.add(overlay);
		this.populate();
	}

	public void addOverlay(NavPoint overlay, Drawable marker) {
		overlay.setMarker(boundCenterBottom(marker));
		mapOverlays.add(overlay);
		this.populate();
	}

}

