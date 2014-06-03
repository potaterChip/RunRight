package com.potaterchip.runright;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

/**
 * Created by Eric on 5/22/2014.
 */
public class RunNotificationService extends IntentService {
    private static final String TAG = "RunNotificationService";

    RunNotificationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Notification notification = new NotificationCompat.Builder(this)
                .setTicker("Ongoing run")
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle("Run Title")
                .setContentText("Running...")
                .setOngoing(true)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, notification);
    }
}
