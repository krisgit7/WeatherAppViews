package com.example.weatherappviews.location;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.weatherappviews.MainActivity;
import com.example.weatherappviews.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.TimeUnit;

public class ForegroundLocationService extends Service {

    private static final String PACKAGE_NAME = "com.example.weatherappviews";
    public static final String ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            PACKAGE_NAME + ".action.FOREGROUND_ONLY_LOCATION_BROADCAST";
    public static final String EXTRA_LOCATION = PACKAGE_NAME + ".extra.LOCATION";
    private static final String EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION = PACKAGE_NAME + ".extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION";
    private static final int NOTIFICATION_ID = 12345678;
    private static final String NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01";

    private boolean configurationChange;
    private boolean serviceRunningInForeground;
    private final LocalBinder localBinder = new LocalBinder();
    private NotificationManager notificationManager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(TimeUnit.SECONDS.toMillis(60));
        locationRequest.setFastestInterval(TimeUnit.SECONDS.toMillis(30));
        locationRequest.setMaxWaitTime(TimeUnit.SECONDS.toMillis(2));
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();

                Intent intent = new Intent(ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST);
                intent.putExtra(EXTRA_LOCATION, currentLocation);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                if (serviceRunningInForeground) {
                    notificationManager.notify(
                            NOTIFICATION_ID,
                            generateNotification(currentLocation)
                    );
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean cancelLocationTrackingFromNotification =
                intent.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false);

        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocationUpdates();
            stopSelf();
        }
        // Tells the system not to recreate the service after it's been killed.
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        serviceRunningInForeground = false;
        configurationChange = false;
        return localBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        serviceRunningInForeground = false;
        configurationChange = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // MainActivity (client) leaves foreground, so service needs to become a foreground service
        // to maintain the 'while-in-use' label.
        // NOTE: If this method is called due to a configuration change in MainActivity,
        // we do nothing.
        if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
            Notification notification = generateNotification(currentLocation);
            startForeground(NOTIFICATION_ID, notification);
            serviceRunningInForeground = true;
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configurationChange = true;
    }

    @SuppressLint("MissingPermission")
    public void subscribeToLocationUpdates() {
        SharedPreferenceUtil.saveLocationTrackingPref(this, true);

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(new Intent(getApplicationContext(), ForegroundLocationService.class));

        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (Exception exception) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, false);
        }
    }

    public void unsubscribeToLocationUpdates() {
        try {
            Task<Void> removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            removeTask.addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        stopSelf();
                    }
                }
            });
            SharedPreferenceUtil.saveLocationTrackingPref(this, false);
        } catch (Exception exception) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, true);
        }
    }

    private Notification generateNotification(Location location) {
        String mainNotificationText = location != null ? location.toString() : getString(R.string.no_location_text);
        String titleText = getString(R.string.app_name);

        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                .bigText(mainNotificationText)
                .setBigContentTitle(titleText);

        Intent launchActivityIntent = new Intent(this, MainActivity.class);
        Intent cancelIntent = new Intent(this, ForegroundLocationService.class);

        PendingIntent servicePendingIntent = PendingIntent.getService(
                this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        PendingIntent activityPendingIntent = PendingIntent.getActivity(
                this, 0, launchActivityIntent, 0
        );

        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID);

        return notificationCompatBuilder
                .setStyle(bigTextStyle)
                .setContentTitle(titleText)
                .setContentText(mainNotificationText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(
                        R.drawable.ic_launch,
                        getString(R.string.launch_activity),
                        activityPendingIntent
                )
                .addAction(
                        R.drawable.ic_cancel,
                        getString(R.string.stop_location_updates_button_text),
                        servicePendingIntent
                )
                .build();
    }

    public class LocalBinder extends Binder {
        public final ForegroundLocationService service = ForegroundLocationService.this;
    }


}
