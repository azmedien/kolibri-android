package ch.yanova.kolibri.notifications;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by lekov on 5/18/17.
 */

public final class KolibriFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        final Intent messageReceived = new Intent(KolibriNotificationsReceiver.ACTION_RECEIVE);
        messageReceived.putExtra(KolibriNotificationsReceiver.EXTRA_MESSAGE, remoteMessage);

        getApplicationContext().sendOrderedBroadcast(messageReceived, null);
    }

}
