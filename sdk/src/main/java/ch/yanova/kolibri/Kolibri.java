package ch.yanova.kolibri;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import ch.yanova.kolibri.components.KolibriComponent;

/**
 * Created by mmironov on 2/26/17.
 */

public final class Kolibri {
    public static void bind(KolibriComponent component, String... uris) {

        if (component instanceof View) {
            View view = (View) component;
            view.addOnAttachStateChangeListener(
                    new Binding(new KolibriCoordinator(component, uris), component));
        } else if (component instanceof Activity) {

        } else if (component instanceof Fragment) {

        }
    }

    public static Intent createIntent(Uri uri) {
        final Intent res = new Intent(Intent.ACTION_VIEW);
        res.setData(uri);
        res.putExtra(KolibriCoordinator.HANDLE, true);

        return res;
    }

    public static void notifyComponents(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
