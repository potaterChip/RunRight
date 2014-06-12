package com.potaterchip.runright;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Date;

/**
 * Created by Eric on 4/21/2014.
 */
public class RunFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {
        @Override
        protected void onLocationReceived(Context context, Location loc) {
            //if(!mRunManager.isTrackingRun(mRun))
              //  return;
            mLastLocation = loc;
            if(mLocationCursor != null) {
                mLocationCursor.requery();
            }
            if(isVisible())
                updateUI();
        }

        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        }
    };
    private Run mRun;
    private Location mLastLocation;
    private Button mStartButton, mStopButton, mMapButton;
    private TextView mStartedTextView, mLatitudeTextView,
    mLongitudeTextView, mAltitudeTextView, mDurationTextView;
    private RunManager mRunManager;
    private GoogleMap mGoogleMap;
    private static final String TAG = "RunFragment";
    private static final String ARG_RUN_ID = "RUN_ID";
    private static final int LOAD_RUN = 0;
    private static final int LOAD_LOCATION = 1;
    private FragmentActivity myContext;
    private RunDatabaseHelper.LocationCursor mLocationCursor;

    @Override
    public void onAttach(Activity activity) {
        myContext = (FragmentActivity)activity;
        super.onAttach(activity);
    }

    public static RunFragment newInstance(long runId) {
        Bundle args = new Bundle();
        args.putLong(ARG_RUN_ID, runId);
        RunFragment rf = new RunFragment();
        rf.setArguments(args);
        return rf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); //retain fragment instance across Activity recreation
        mRunManager = RunManager.get(getActivity());

        Bundle args = getArguments();
        if(args != null) {
            long runId = args.getLong(ARG_RUN_ID, -1);
            if(runId != -1) {
                LoaderManager lm = getLoaderManager();
                lm.initLoader(LOAD_RUN, args, new RunLoaderCallbacks());
                lm.initLoader(LOAD_LOCATION, args, new LocationLoaderCallbacks());
            }
        }

        //initMapView();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_run, container, false);

        mStartedTextView = (TextView)view.findViewById(R.id.run_startedTextView);
        mLatitudeTextView = (TextView)view.findViewById(R.id.run_latitudeTextView);
        mLongitudeTextView = (TextView)view.findViewById(R.id.run_longitudeTextView);
        mAltitudeTextView = (TextView)view.findViewById(R.id.run_altitudeTextView);
        mDurationTextView = (TextView)view.findViewById(R.id.run_durationTextView);

        mStartButton = (Button)view.findViewById(R.id.run_startButton);
        final RunFragment fragSaved = this;
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRun == null) {
                    mRun = mRunManager.startNewRun();
                } else {
                    mRunManager.startTrackingRun(mRun);
                }
                ((RunActivity) getActivity()).startRunNotification();
                if(mLocationCursor == null) {
                    Bundle args = new Bundle();
                    args.putLong(ARG_RUN_ID, mRun.getId());
                    LoaderManager lm = getLoaderManager();
                    lm.initLoader(LOAD_LOCATION, args, fragSaved);
                }
                updateUI();
            }
        });
        mStopButton = (Button)view.findViewById(R.id.run_stopButton);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRunManager.stopRun();
                ((RunActivity)getActivity()).stopRunNotification();
                updateUI();
            }
        });

        mMapButton = (Button)view.findViewById(R.id.run_mapButton);
        mMapButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), RunMapActivity.class);
                i.putExtra(RunMapActivity.EXTRA_RUN_ID, mRun.getId());
                startActivity(i);
            }
        });

        mGoogleMap = ((SupportMapFragment)getFragmentManager().findFragmentById(R.id.running_map)).getMap();
        mGoogleMap.setMyLocationEnabled(true);
        mRunManager.startLocationUpdates();
        updateUI();

        return view;
    }

    private void updateUI() {
        boolean started = mRunManager.isTrackingRun();
        boolean trackingThisRun = mRunManager.isTrackingRun(mRun);

        if(mRun != null) {
            mStartedTextView.setText(mRun.getStartDate().toString());
        }
        int durationSeconds = 0;
        if(mRun != null && mLastLocation != null) {
            durationSeconds = mRun.getDurationSeconds(mLastLocation.getTime());
            mLatitudeTextView.setText(Double.toString(mLastLocation.getLatitude()));
            mLongitudeTextView.setText(Double.toString(mLastLocation.getLongitude()));
            mAltitudeTextView.setText(Double.toString(mLastLocation.getAltitude()));
            mMapButton.setEnabled(true);
        }else {
            mMapButton.setEnabled(false);
        }

        if(mLastLocation != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 17.4f));

            if(started && mLocationCursor != null) {
                //draw the lines
                drawLinesOnMap();
            }
        }

        mDurationTextView.setText(Run.formatDuration(durationSeconds));

        mStartButton.setEnabled(!trackingThisRun);
        mStopButton.setEnabled(started && trackingThisRun);

    }

    private void drawLinesOnMap() {

        mGoogleMap.clear();
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

            line.add(latLng);
            builder.include(latLng);

            mLocationCursor.moveToNext();
        }

        // Add the polyline to the map
        mGoogleMap.addPolyline(line);
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        long runId = args.getLong(ARG_RUN_ID, -1);
        return new LocationListCursorLoader(getActivity(), runId);
    }



    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mLocationCursor = (RunDatabaseHelper.LocationCursor)data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Stop using the data
        mLocationCursor.close();
        mLocationCursor = null;
    }

    private class RunLoaderCallbacks implements LoaderManager.LoaderCallbacks<Run> {
        @Override
        public Loader<Run> onCreateLoader(int id, Bundle args) {
            return new RunLoader(getActivity(), args.getLong(ARG_RUN_ID));
        }

        @Override
        public void onLoadFinished(Loader<Run> loader, Run data) {
            mRun = data;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Run> loader) {

        }
    }

    private class LocationLoaderCallbacks implements LoaderManager.LoaderCallbacks<Location> {
        @Override
        public Loader<Location> onCreateLoader(int id, Bundle args) {
            return new LastLocationLoader(getActivity(), args.getLong(ARG_RUN_ID));
        }

        @Override
        public void onLoadFinished(Loader<Location> loader, Location data) {
            mLastLocation = data;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Location> loader) {
            // Do nothing.
        }
    }
}
