package ch.yanova.kolibri.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by mmironov on 4/22/17.
 */

public class NetworkUtils {

  public static boolean isConnectedToInternet(Context context) {
    final ConnectivityManager cm = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

    return activeNetwork != null &&
        activeNetwork.isConnectedOrConnecting();
  }
}
