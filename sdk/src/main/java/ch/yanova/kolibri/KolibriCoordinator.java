package ch.yanova.kolibri;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import ch.yanova.kolibri.components.KolibriComponent;
import ch.yanova.kolibri.components.KolibriFloatingActionButton;

/**
 * Created by mmironov on 2/26/17.
 */

public class KolibriCoordinator {

    private boolean attached;

    private BroadcastReceiver receiver;
    private LocalBroadcastManager manager;
    private String[] uriStrings;
    private Context context;

    KolibriCoordinator(KolibriComponent view, String... uris) {
        this.uriStrings = uris;
        this.context = ((View)view).getContext();

        if (receiver == null) {
            bindReceiver(view);
        }
    }

    final void setAttached(boolean attached) {
        this.attached = attached;
    }

    /**
     * Called when the view is attached to a Window.
     *
     * Default implementation does nothing.
     *
     * @see View#onAttachedToWindow()
     */
    void attach(final KolibriComponent view) {

        bindReceiver(view);
    }

    /**
     * Called when the view is detached from a Window.
     *
     * Default implementation does nothing.
     *
     * @see View#onDetachedFromWindow()
     */
    void detach(KolibriComponent view) {
        manager.unregisterReceiver(receiver);
    }

    private void bindReceiver(final KolibriComponent view) {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                view.handleIntent(intent);
            }
        };

        manager = LocalBroadcastManager.getInstance(context);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_VIEW);
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        final Uri[] uris = new Uri[uriStrings.length];

        for (int i = 0; i < uris.length; ++i) {
            uris[i] = Uri.parse(uriStrings[i]);

            final Uri uri = uris[i];

            filter.addDataScheme(uri.getScheme());
            filter.addDataAuthority(uri.getHost(), null);
        }

        manager.registerReceiver(receiver, filter);
    }

    /**
     * True from just before bind until just after detach.
     */
    public final boolean isAttached() {
        return attached;
    }
}
