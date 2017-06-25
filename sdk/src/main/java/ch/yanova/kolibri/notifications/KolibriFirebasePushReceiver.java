package ch.yanova.kolibri.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

import java.util.List;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.RuntimeConfig;

/**
 * Created by lekov on 11/23/16.
 */

public class KolibriFirebasePushReceiver extends BroadcastReceiver {

    private static final String KOLIBRI_LINK_INTENT = "kolibri://content/link";

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

        if (msg.getData().containsKey("component")) {

            String componentUri = msg.getData().get("component");

            result = getResultIntent(componentUri);
        } else {
            result = Kolibri.createIntent(Uri.parse("kolibri://notification"));
            result.putExtra(EXTRA_MESSAGE, msg);
        }

        final PackageManager packageManager = context.getPackageManager();
        if (result.resolveActivity(packageManager) == null) {
            Log.e("KolibriNotifications", "Notification received but nobody cannot handle the deeplink.");
            abortBroadcast();
            return;
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

    private static Intent getResultIntent(String componentUri) {

        final Intent result;

        if (componentUri == null) {
            return Kolibri.getErrorIntent("Error with component");
        }

        if (componentUri.startsWith("http")) {
            componentUri = KOLIBRI_LINK_INTENT + "?url=" + componentUri;
        }

        final Uri uri = Uri.parse(componentUri);

        result = new Intent(Intent.ACTION_VIEW);
        result.setData(uri);

        if (componentUri.startsWith(KOLIBRI_LINK_INTENT)) {
            final List<String> pathSegments = uri.getPathSegments();
            final String id = pathSegments.get(pathSegments.size() - 1);
            result.putExtra(Kolibri.EXTRA_ID, id);
        }

        return result;
    }
}
