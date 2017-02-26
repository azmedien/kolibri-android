package ch.yanova.kolibri;

import android.app.Activity;
import android.app.Fragment;
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
                    new Binding(new KolibriCoordinator(view.getContext(), uris), component));
        } else if (component instanceof Activity) {

        } else if (component instanceof Fragment) {

        }
    }
}
