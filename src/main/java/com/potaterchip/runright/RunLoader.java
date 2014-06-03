package com.potaterchip.runright;

import android.content.Context;

/**
 * Created by Eric on 5/25/2014.
 */
public class RunLoader extends DataLoader<Run>{
    private long mRunId;

    public RunLoader (Context context, long runId) {
        super(context);
        mRunId = runId;
    }

    @Override
    public Run loadInBackground() {
        return RunManager.get(getContext()).getRun(mRunId);
    }
}
