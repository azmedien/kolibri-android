package ch.yanova.kolibri;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

/**
 * Created by mmironov on 2/26/17.
 */

public final class Kolibri {

    public static void bind(View view, KolibriCoordinator coordinator) {
        view.addOnAttachStateChangeListener(new Binding(view, coordinator));
    }

    public static void bind(View view, KolibriProvider provider) {
        final KolibriCoordinator coordinator = provider.provideCoordinator(view);
        if (coordinator == null) {
            return;
        }

        View.OnAttachStateChangeListener binding = new Binding(view, coordinator);
        view.addOnAttachStateChangeListener(binding);
        // Sometimes we missed the first attach because the child's already been added.
        // Sometimes we didn't. The binding keeps track to avoid double attachment of the Coordinator,
        // and to guard against attachment to two different views simultaneously.
        binding.onViewAttachedToWindow(view);
    }

    @Nullable
    public static KolibriCoordinator getCoordinator(View view) {
        return (KolibriCoordinator) view.getTag(R.id.coordinator);
    }


    public static Intent createIntent(Uri uri) {
        final Intent res = new Intent(Intent.ACTION_VIEW);
        res.setData(uri);

        return res;
    }

    public static void notifyComponents(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
