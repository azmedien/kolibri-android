package ch.yanova.kolibri;

import android.support.v4.app.Fragment;

/**
 * Created by lekov on 4/2/17.
 */

public interface KolibriInitializeListener {
    void onPreInitialize();
    Fragment onPostInitialize();
    void onBindComponents();
    void onNavigationInitialize();
}
