package ch.yanova.kolibri.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

import ch.yanova.kolibri.Kolibri;

/**
 * Created by lekov on 11/23/16.
 */

public class KolibriFirebasePushReceiver extends BroadcastReceiver {

    public static final String ACTION_RECEIVE = "ch.azmedien.kolibri.MESSAGE_RECEIVED";
    public static final String EXTRA_MESSAGE = "ch.azmedien.kolibri.EXTRA_MESSAGE";

    @Override
    public void onReceive(Context context, Intent intent) {
        final RemoteMessage msg = intent.getParcelableExtra(EXTRA_MESSAGE);
        final Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        String title;
        String body;

        if (msg == null) {
            return;
        }

        if (msg.getNotification() != null) {
            title = msg.getNotification().getTitle();
            body = msg.getNotification().getBody();
        } else {
            title = msg.getData().get("title");
            body = msg.getData().get("body");
        }

        final Uri parse = Uri.parse("kolibri://internal/webview");
        final Intent content;

        if (msg.getData().containsKey("url")) {
            Uri urlUri = parse.buildUpon().appendQueryParameter("url", msg.getData().get("url")).build();
            content = Kolibri.createIntent(urlUri);
        } else {
            content = Kolibri.createIntent(Uri.parse("kolibri://notification"));
            content.putExtra(EXTRA_MESSAGE, msg);
        }

        content.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, content, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(Kolibri.getInstance(context).getNotificationIcon())
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setTicker(String.format("%s: %s", title, body))
                .setContentIntent(pendingIntent);

        notificationManager.notify(Kolibri.getInstance(context).getNotificationIcon(), notificationBuilder.build());

        abortBroadcast();
    }
}
