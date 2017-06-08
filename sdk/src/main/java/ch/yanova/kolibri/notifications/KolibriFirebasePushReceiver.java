package ch.yanova.kolibri.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriApp;
import ch.yanova.kolibri.RuntimeConfig;

/**
 * Created by lekov on 11/23/16.
 */

public class KolibriFirebasePushReceiver extends BroadcastReceiver {

    public static final String ACTION_RECEIVE = "ch.azmedien.kolibri.MESSAGE_RECEIVED";
    public static final String EXTRA_MESSAGE = "ch.azmedien.kolibri.EXTRA_MESSAGE";
    private final static Uri INTERNAL_WEBVIEW = Uri.parse("kolibri://internal/webview");

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

        final Intent result;

        if (msg.getData().containsKey("url")) {

            final String resultUrl = msg.getData().get("url");
            final Uri resultUri = Uri.parse(resultUrl);
            final RuntimeConfig config = Kolibri.getInstance(context).getRuntime();
            final String scheme;

            if (config != null) {
                scheme = config.getScheme();
            } else {
                scheme = "kolibri";
            }

            // Check if url is custom component
            if ("kolibri".equals(resultUri.getScheme()) || scheme.equals(resultUri.getScheme())) {
                result = Kolibri.createIntent(resultUri);
                final PackageManager packageManager = context.getPackageManager();
                if (intent.resolveActivity(packageManager) == null) {
                    Log.e("KolibriNotifications", "Notification received but nobody cannot handle the deeplink.");
                    abortBroadcast();
                    return;
                }
            } else {
                Uri uri = INTERNAL_WEBVIEW.buildUpon().appendQueryParameter("url", resultUrl).build();
                result = Kolibri.createIntent(uri);
            }

        } else {
            result = Kolibri.createIntent(Uri.parse("kolibri://notification"));
            result.putExtra(EXTRA_MESSAGE, msg);
        }

        result.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, result, PendingIntent.FLAG_UPDATE_CURRENT);

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
