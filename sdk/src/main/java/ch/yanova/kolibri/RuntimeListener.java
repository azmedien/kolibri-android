package ch.yanova.kolibri;

/**
 * Created by mmironov on 2/26/17.
 */

interface RuntimeListener {

    void onLoaded(RuntimeConfig runtime, boolean isFresh);
    boolean onFailed(Exception e);
}
