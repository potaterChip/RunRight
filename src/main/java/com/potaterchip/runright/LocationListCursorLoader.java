package com.potaterchip.runright;

import android.content.Context;
import android.database.Cursor;

/**
 * Created by Eric on 6/2/2014.
 */
public class LocationListCursorLoader extends SQLiteCursorLoader {

    private long mRunId;

    public LocationListCursorLoader(Context c, long runId) {
        super(c);
        mRunId = runId;
    }

    @Override
    protected Cursor loadCursor() {
        return RunManager.get(getContext()).queryLocationsForRun(mRunId);
    }


}
