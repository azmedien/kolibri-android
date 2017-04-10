package ch.yanova.kolibri;

/**
 * Created by mmironov on 2/26/17.
 */

interface RuntimeListener {

    void onLoaded(Kolibri.Runtime runtime);
    boolean onFailed(Exception e);
}
