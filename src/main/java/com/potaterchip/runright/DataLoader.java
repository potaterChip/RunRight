package com.potaterchip.runright;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * Created by Eric on 5/25/2014.
 */
public abstract class DataLoader<D> extends AsyncTaskLoader<D> {
    private D mData;

    public DataLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        if(mData != null) {
            deliverResult(mData);
        }else {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(D data) {
        mData = data;
        if(isStarted())
            super.deliverResult(data);
    }
}
