package ch.yanova.kolibri;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

/**
 * Created by mmironov on 2/26/17.
 */

public abstract class KolibriCoordinator<T extends View> {

  protected T view;
  private boolean attached;
  private BroadcastReceiver receiver;
  private LocalBroadcastManager manager;
  private Context context;

  /**
   * Called when the view is attached to a Window.
   * <p>
   * Default implementation does nothing.
   *
   * @see View#onAttachedToWindow()
   */
  protected void attach(final T view) {
    context = view.getContext();
    this.view = view;
    bindReceiver(view);
  }

  /**
   * Called when the view is detached from a Window.
   * <p>
   * Default implementation does nothing.
   *
   * @see View#onDetachedFromWindow()
   */
  protected void detach(T view) {
    manager.unregisterReceiver(receiver);
  }

  private void bindReceiver(final T view) {
    receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {

        handleIntent(view, intent);
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

  final void setAttached(boolean attached) {
    this.attached = attached;
  }

  protected abstract void handleIntent(T view, Intent intent);

  protected abstract String[] kolibriUris();
}
