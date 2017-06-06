package ch.yanova.kolibri;

import android.support.annotation.NonNull;
import android.view.View;

final class Binding implements View.OnAttachStateChangeListener {

    private KolibriCoordinator coordinator;
    private View view;
    private View attached;

    Binding(@NonNull View view, @NonNull KolibriCoordinator coordinator) {
        this.coordinator = coordinator;
        this.view = view;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull View v) {
        if (v != attached) {
            attached = v;
            if (coordinator.isAttached()) {
                throw new IllegalStateException(
                        "Coordinator " + coordinator + " is already attached to a View");
            }
            coordinator.setAttached(true);
            coordinator.attach(view);
            view.setTag(R.id.coordinator, coordinator);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull View v) {
        if (v == attached) {
            attached = null;
            coordinator.detach(view);
            coordinator.setAttached(false);
            view.setTag(R.id.coordinator, null);
        }
    }
}