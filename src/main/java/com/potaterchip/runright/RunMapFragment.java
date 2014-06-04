package com.potaterchip.runright;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Date;

/**
 * Created by Eric on 5/25/2014.
 */

public class RunMapFragment extends SupportMapFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String ARG_RUN_ID = "RUN_ID";
    private static final int LOAD_LOCATIONS = 0;
    private boolean mZoomedInOnce = false;
    private GoogleMap mGoogleMap;
    private RunDatabaseHelper.LocationCursor mLocationCursor;

    private RunManager mRunManager;
    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {
        @Override
        protected void onLocationReceived(Context context, Location loc) {

            if(!mRunManager.isTrackingRun(mRun))
                return;

            if(isVisible()) {
                mLocationCursor.requery();
                updateUI(loc);
            }
        }

        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        }
    };
    private Run mRun;


    public static RunMapFragment newInstance(long runId) {
        Bundle args = new Bundle();
        args.putLong(ARG_RUN_ID, runId);
        RunMapFragment rf = new RunMapFragment();
        rf.setArguments(args);
        return rf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRunManager = RunManager.get(getActivity());
        Bundle args = getArguments();
        if(args != null) {
            long runId = args.getLong(ARG_RUN_ID, -1);
            mRun = mRunManager.getRun(runId);
            if(runId != -1) {
                LoaderManager lm = getLoaderManager();
                lm.initLoader(LOAD_LOCATIONS, args, this);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        // Stash a reference to the GoogleMap
        mGoogleMap = getMap();
        // Show the user's location
        mGoogleMap.setMyLocationEnabled(true);
        //mGoogleMap.setLocationSource(this);
        //mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(8.0f));


        return v;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        long runId = args.getLong(ARG_RUN_ID, -1);
        return new LocationListCursorLoader(getActivity(), runId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mLocationCursor = (RunDatabaseHelper.LocationCursor)data;
        updateUI(null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Stop using the data
        mLocationCursor.close();
        mLocationCursor = null;
    }

    private void updateUI(Location loc) {
        if(mGoogleMap == null || mLocationCursor == null) {
            return;
        }

        if(loc != null) {
            mGoogleMap.clear();
        }

        // Setup an overlay on the map for this run's locations
        // Create a polyline with all all of the points
        PolylineOptions line = new PolylineOptions();
        // Also create a LatLongBounds so you can zoom to fit
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // Iterate over the locations
        mLocationCursor.moveToFirst();

        while(!mLocationCursor.isAfterLast()) {
            Location location = mLocationCursor.getLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            Resources r = getResources();

            //If this is the first location, add a marker for it
            if(mLocationCursor.isFirst()) {
                String startDate = new Date(location.getTime()).toString();
                MarkerOptions startMarkerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(r.getString(R.string.run_start))
                        .snippet(r.getString(R.string.run_started_at_format, startDate));
                mGoogleMap.addMarker(startMarkerOptions);
            }else if(mLocationCursor.isLast()) {
                // If this is the last location, and not also the first, add a marker
                // but only if like the location passed in isn't null
                if(loc == null) {
                    setLastLocation(location, latLng, r);
                }
            }

            line.add(latLng);
            builder.include(latLng);

            mLocationCursor.moveToNext();
        }

        if(loc != null) {
            LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
            Resources r = getResources();
            setLastLocation(loc, latLng, r);

            line.add(latLng);
            builder.include(latLng);
        }

        // Add the polyline to the map
        mGoogleMap.addPolyline(line);
        // Make the map zoom to show the track, with some padding
        // Use the size the current display in pixels as a bounding box
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        // Construct a movement instruction for the map camera
        LatLngBounds latLngBounds = builder.build();

        //TODO look at this
        //http://stackoverflow.com/questions/23041604/how-to-stop-zooming-in-google-map-after-updating-current-location-every-second
        // may help keep the zoom level proper
        CameraUpdate movement = CameraUpdateFactory.newLatLngBounds(latLngBounds,
                display.getWidth(), display.getHeight(), 15);
        float somethign = mGoogleMap.getCameraPosition().zoom;
        mGoogleMap.moveCamera(movement);

    }

    private void setLastLocation(Location location, LatLng latLng, Resources r) {
        String endDate = new Date(location.getTime()).toString();
        MarkerOptions finishMarkerOptions = new MarkerOptions()
                .position(latLng)
                .title(r.getString(R.string.run_finish))
                .snippet(r.getString(R.string.run_finished_at_format, endDate));
        mGoogleMap.addMarker(finishMarkerOptions);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mLocationReceiver,
                new IntentFilter(RunManager.ACTION_LOCATION));
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mLocationReceiver);
        super.onStop();
    }
}
