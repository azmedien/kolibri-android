package ch.yanova.kolibri.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.TypedValue;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.R;
import ch.yanova.kolibri.RuntimeConfig;

/**
 * Created by lekov on 5/18/17.
 */

public final class KolibriFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        handleMessage(this, remoteMessage);
    }

    private static final String TAG = "KolibriNotifications";

    public static final String KOLIBRI_NOTIFICATION_INTENT = "kolibri://notification";

    protected void handleMessage(Context context, RemoteMessage message) {
        if (message == null) {
            return;
        }

        if (message.getData().isEmpty()) {
            Log.w(TAG, "onReceive: Received notification without data. Skipping...");
            return;
        }

        final String title = message.getData().get("title");
        final String body = message.getData().get("body");
        final String url = message.getData().get("url");

        handleNow(context, url, title, body);
    }

    static void handleNow(Context context, String url, String title, String body) {
        final Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        final RuntimeConfig runtime = Kolibri.getInstance(context).getRuntime();

        if (runtime == null) {
            return;
        }

        final String scheme = runtime.getScheme();

        final Uri uri = Uri.parse(scheme + "://navigation");

        Intent result = Kolibri.createIntent(uri);
        result.addCategory("notification");
        result.putExtra("url", url);

        result.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, result, PendingIntent.FLAG_UPDATE_CURRENT);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(Kolibri.getInstance(context).getNotificationIcon())
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setColor(typedValue.data)
                .setTicker(String.format("%s: %s", title, body))
                .setContentIntent(pendingIntent);

        notificationManager.notify(Kolibri.getInstance(context).getNotificationIcon(), notificationBuilder.build());
    }
}
