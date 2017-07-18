package ch.yanova.kolibri.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
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

/**
 * Created by lekov on 7/12/17.
 */

public final class InternalNotificationsReceiver extends KolibriNotificationsReceiver {

    private static final String TAG = "KolibriNotifications";

    private static final String KOLIBRI_NOTIFICATION_INTENT = "kolibri://notification";
    private static final String KOLIBRI_ID_INTENT = "kolibri://navigation";

    @Override
    protected void onMessageReceived(Context context, RemoteMessage message) {
        if (message == null) {
            return;
        }

        if (message.getData().isEmpty()) {
            Log.w(TAG, "onReceive: Received notification without data. Skipping...");
            return;
        }

        final String title = message.getData().get("title");
        final String body = message.getData().get("body");
        final String componentUri = message.getData().get("component");

        handleNow(context, componentUri, title, body);
        abortBroadcast();
    }

    static void handleNow(Context context, String componentUri, String title, String body) {
        final Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent result;

        if (componentUri != null) {
            result = getResultIntent(context, componentUri);
        } else {
            result = Kolibri.createIntent(Uri.parse("kolibri://notification"));
        }

        final PackageManager packageManager = context.getPackageManager();
        if (result.resolveActivity(packageManager) == null) {

            if (result.hasExtra(Kolibri.EXTRA_ID)) {
                final String id = result.getStringExtra(Kolibri.EXTRA_ID);

                if (Kolibri.getInstance(context).getRuntime().getNavigation().hasItem(id)) {
                    final String query = result.getData().getQuery();

                    String modifiedUri = KOLIBRI_NOTIFICATION_INTENT;
                    if (query != null) {
                        modifiedUri += "?" + query;
                    }

                    result = Kolibri.createIntent(Uri.parse(modifiedUri));
                    result.putExtra(Kolibri.EXTRA_ID, id);
                }
            } else {
                Log.e("KolibriNotifications", "Notification received but nobody cannot handle the deeplink.");
                result = Kolibri.getErrorIntent(context, "Content of this type cannot be open.");
            }

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
    }

    private static Intent getResultIntent(Context context, String componentUri) {

        final Intent result = new Intent(Intent.ACTION_VIEW);

        if (componentUri == null) {
            return Kolibri.getErrorIntent(context, "Error with component");
        }

        Uri uri;
        if (componentUri.startsWith("http")) {
            componentUri = KOLIBRI_NOTIFICATION_INTENT + "?url=" + componentUri;
            uri = Uri.parse(componentUri);
        } else if (componentUri.startsWith(KOLIBRI_ID_INTENT)) {
            uri = Uri.parse(componentUri);
            final List<String> pathSegments = uri.getPathSegments();

            if (pathSegments == null || pathSegments.size() <= 0) {
                return Kolibri.getErrorIntent(context, "Content of this type cannot be open.");
            }
            final String id = pathSegments.get(pathSegments.size() - 1);
            result.putExtra(Kolibri.EXTRA_ID, id);
        } else {
            uri = Uri.parse(componentUri);
        }

        result.setData(uri);

        return result;
    }
}
