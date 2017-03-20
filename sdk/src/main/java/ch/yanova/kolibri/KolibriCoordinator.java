package ch.yanova.kolibri;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import java.util.List;

/**
 * Created by mmironov on 2/26/17.
 */

public abstract class KolibriCoordinator {

    private boolean attached;

    private BroadcastReceiver receiver;
    private LocalBroadcastManager manager;
    private Context context;

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
    void attach(final View view) {

        if (!isAttached()) {
            context = view.getContext();
            bindReceiver();
            setAttached(true);
        }

    }

    /**
     * Called when the view is detached from a Window.
     *
     * Default implementation does nothing.
     *
     * @see View#onDetachedFromWindow()
     */
    void detach(View view) {
        manager.unregisterReceiver(receiver);
        attached = false;
    }

    private void bindReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                handleIntent(intent);
            }
        };

        manager = LocalBroadcastManager.getInstance(context);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_VIEW);
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        final Uri[] uris = new Uri[kolibriUris().length];

        for (int i = 0; i < uris.length; ++i) {
            uris[i] = Uri.parse(kolibriUris()[i]);

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

    public abstract void handleIntent(Intent intent);

    public abstract String[] kolibriUris();
}
