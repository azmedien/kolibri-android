package ch.yanova.kolibri;

import org.json.JSONObject;

/**
 * Created by mmironov on 2/26/17.
 */

public interface NavigationListener {

    void onLoaded(JSONObject nav);
    boolean onFailed(Exception e);
}
