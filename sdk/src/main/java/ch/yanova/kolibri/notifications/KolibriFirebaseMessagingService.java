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

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;

import ch.yanova.kolibri.Kolibri;

/**
 * Created by lekov on 5/18/17.
 */

public class KolibriFirebaseMessagingService extends FirebaseMessagingService {

    private static final String KOLIBRI_LINK_INTENT = "kolibri://content/link";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
//        super.onMessageReceived(remoteMessage);
//        Intent messageReceived = new Intent(KolibriFirebasePushReceiver.ACTION_RECEIVE);
//        messageReceived.putExtra(KolibriFirebasePushReceiver.EXTRA_MESSAGE, remoteMessage);
//
//        getApplicationContext().sendOrderedBroadcast(messageReceived, null);
    }

    @Override
    public void handleIntent(Intent intent) {
        super.handleIntent(intent);

        handleNow(intent);

        String token = FirebaseInstanceId.getInstance().getToken();
    }

    private void handleNow(Intent intent) {
        final Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final String componentUri = intent.getStringExtra("component");
        final String title = intent.getStringExtra("title");
        final String body = intent.getStringExtra("body");

        final Intent result;

        if (componentUri != null) {

            result = getResultIntent(this, componentUri);
        } else {
            result = Kolibri.createIntent(Uri.parse("kolibri://notification"));
        }

        final PackageManager packageManager = getPackageManager();
        if (result.resolveActivity(packageManager) == null) {
            Log.e("KolibriNotifications", "Notification received but nobody cannot handle the deeplink.");
            return;
        }

        result.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, result, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(Kolibri.getInstance(this).getNotificationIcon())
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setTicker(String.format("%s: %s", title, body))
                .setContentIntent(pendingIntent);

        notificationManager.notify(Kolibri.getInstance(this).getNotificationIcon(), notificationBuilder.build());
    }

    private static Intent getResultIntent(Context context, String componentUri) {

        final Intent result = new Intent(Intent.ACTION_VIEW);

        if (componentUri == null) {
            return Kolibri.getErrorIntent(context, "Error with component");
        }

        Uri uri = null;
        if (componentUri.startsWith("http")) {
            componentUri = KOLIBRI_LINK_INTENT + "?url=" + componentUri;
            uri = Uri.parse(componentUri);
        } else if (componentUri.startsWith(KOLIBRI_LINK_INTENT)) {
            uri = Uri.parse(componentUri);
            final List<String> pathSegments = uri.getPathSegments();
            final String id = pathSegments.get(pathSegments.size() - 1);
            result.putExtra(Kolibri.EXTRA_ID, id);
        }

        result.setData(uri);

        return result;
    }
}
