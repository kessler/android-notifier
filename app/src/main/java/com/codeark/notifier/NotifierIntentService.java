package com.codeark.notifier;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.inject.Inject;

import roboguice.service.RoboIntentService;
import roboguice.util.Ln;

/**
 *
 */
public class NotifierIntentService extends RoboIntentService {
    @Inject
    private GcmService gcm;

    @Inject
    private NotificationManager notifications;

    @Inject
    private SharedPreferences prefs;

    public NotifierIntentService() {
        super("CodearkIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();

        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        Ln.i("NotifierIntentService got intent call: " + intent.getAction());
        if (messageType == null) {

            return;
        }

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            String notificationText = null;
            String title = "";

            switch (messageType) {
                case GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR:
                    notificationText = "Send error: " + extras.toString();
                    break;

                case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
                    notificationText = "Deleted messages on server: " +
                            extras.toString();
                    break;

                case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE:
                    notificationText = extras.getString("message");
                    title = extras.getString("title");
                    break;

                default:
                    Ln.e("invalid message type " + messageType);
                    break;
            }

            if (notificationText != null) {
                sendNotification(notificationText, title);


//                if (title.isEmpty()) {
//                    mainActivity.appendAsync(notificationText);
//                } else {
//                    mainActivity.appendAsync(title + ": " + notificationText);
//                }
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        NotifierBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String message, String title) {
        String notificationSoundData = prefs.getString("notification_sound", "");

        Uri notificationSoundUri;
        if (notificationSoundData.isEmpty()) {
            notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            notificationSoundUri = Uri.parse(notificationSoundData);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSound(notificationSoundUri)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setContentText(message);

        mBuilder.setContentIntent(contentIntent);
        notifications.notify(1, mBuilder.build());
    }
}
