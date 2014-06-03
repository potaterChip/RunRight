package com.potaterchip.runright;

import android.content.Context;
import android.location.Location;

/**
 * Created by Eric on 5/25/2014.
 */
public class LastLocationLoader extends DataLoader<Location> {

    private long mRunId;

    public LastLocationLoader(Context context, long runId) {
        super(context);
        mRunId = runId;
    }

    @Override
    public Location loadInBackground() {
        return RunManager.get(getContext()).getLastLocationForRun(mRunId);
    }
}
