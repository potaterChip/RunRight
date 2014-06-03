package com.potaterchip.runright;

import android.content.Context;
import android.location.Location;

/**
 * Created by Eric on 5/9/2014.
 */
public class TrackingLocationReceiver extends LocationReceiver {
    @Override
    protected void onLocationReceived(Context context, Location loc) {
        RunManager.get(context).insertLocation(loc);
    }
}
