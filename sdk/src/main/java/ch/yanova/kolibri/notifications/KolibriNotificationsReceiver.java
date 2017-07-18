package ch.yanova.kolibri.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by lekov on 11/23/16.
 */

public abstract class KolibriNotificationsReceiver extends BroadcastReceiver {

    public static final String ACTION_RECEIVE = "ch.azmedien.kolibri.action.MESSAGE_RECEIVED";
    public static final String EXTRA_MESSAGE = "ch.azmedien.kolibri.extra.MESSAGE";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (ACTION_RECEIVE.equals(intent.getAction())) {
            if (intent.hasExtra(EXTRA_MESSAGE)) {
                onMessageReceived(context, (RemoteMessage) intent.getParcelableExtra(EXTRA_MESSAGE));
            }
        }
    }

    protected abstract void onMessageReceived(Context context, RemoteMessage message);
}
