package ch.yanova.kolibri.prototype;

import ch.yanova.kolibri.KolibriApp;

/**
 * Created by mmironov on 5/10/17.
 */

public class KolibriPrototypeApp extends KolibriApp {

    @Override
    public void onCreate() {
        super.onCreate();
        setFirebaseEnabled(false);
    }
}
