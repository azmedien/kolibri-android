package ch.yanova.kolibri.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by lekov on 11/23/16.
 */

public class KolibriFirebasePushReceiver extends BroadcastReceiver {

    public static final String ACTION_RECEIVE = "ch.azmedien.kolibri.MESSAGE_RECEIVED";
    public static final String EXTRA_MESSAGE = "ch.azmedien.kolibri.EXTRA_MESSAGE";

    @Override
    public void onReceive(Context context, Intent intent) {
        final RemoteMessage msg = intent.getParcelableExtra(EXTRA_MESSAGE);

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

        String componentUri = msg.getData().get("component");

        KolibriFirebaseMessagingService.handleNow(context, componentUri, title, body);
    }
}
