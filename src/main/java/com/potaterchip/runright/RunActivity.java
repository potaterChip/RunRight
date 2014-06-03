package com.potaterchip.runright;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class RunActivity extends SingleFragmentActivity {

    public static final String EXTRA_RUN_ID =
            "com.potaterchip.runright.run_id";
    private long mRunId;

    @Override
    protected Fragment createFragment() {

         mRunId = getIntent().getLongExtra(EXTRA_RUN_ID, -1);


            if (mRunId != -1) {
                return RunFragment.newInstance(mRunId);

            } else {
                return new RunFragment();
            }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void startRunNotification() {
        Intent runIntent = new Intent(this, RunActivity.class);
        runIntent.putExtra(EXTRA_RUN_ID, mRunId);
        runIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(runIntent);

        PendingIntent pendingRunIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new NotificationCompat.Builder(this)
                .setTicker("Ongoing run")
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle("Run Title")
                .setContentText("Running...")
                .setContentIntent(pendingRunIntent)
                .setOngoing(true)
                .build();

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, notification);

    }

    public void stopRunNotification() {
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

}
