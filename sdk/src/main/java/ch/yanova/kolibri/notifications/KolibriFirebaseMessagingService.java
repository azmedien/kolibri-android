package ch.yanova.kolibri.notifications;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by lekov on 5/18/17.
 */

public class KolibriFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Intent messageReceived = new Intent(KolibriFirebasePushReceiver.ACTION_RECEIVE);
        messageReceived.putExtra(KolibriFirebasePushReceiver.EXTRA_MESSAGE, remoteMessage);

        getApplicationContext().sendOrderedBroadcast(messageReceived, null);
    }
}
