package ch.yanova.kolibri;

import android.content.Intent;
import android.net.Uri;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class UriUtils {

  public static final String KEY_UTM_SOURCE = "utm_source";
  public static final String KEY_UTM_MEDIUM = "utm_medium";
  public static final String UTM_ANDROID_APP = "android-app";
  public static final String UTM_PUSH = "push";

  /**
   * Appends the UTM parameters needed to track that the
   * URL was loaded from a push notification.
   * If the url already has one of the parameters,
   * this parameter is not appended again.
   * @param url The url, to which the constants will be appended
   * @return The url with the appended parameters.
   */
  public static String appendUtmParameters(String url) {

    final Uri uri = Uri.parse(url);
    final Uri.Builder builder = uri.buildUpon();

    if (uri.getQueryParameter(KEY_UTM_MEDIUM) == null) {
      builder.appendQueryParameter(KEY_UTM_MEDIUM, UTM_ANDROID_APP);
    }

    if (uri.getQueryParameter(KEY_UTM_SOURCE) == null) {
      builder.appendQueryParameter(KEY_UTM_SOURCE, UTM_PUSH);
    }

    return builder.build().toString();
  }

  /**
   * Appends the UTM parameters needed to track that the
   * url in the intent was loaded from a push notification.
   * If the url already has one of the parameters,
   * this parameter is not appended again.
   * @param intent The intent that is being searched for the url.
   * This is a Kolibri specific intent that keeps the URL in question
   * as a query value with key "url" in the intent's data part.
   */
  public static void appendUtmParameters(@NotNull Intent intent) {

    String url = intent.getData().getQueryParameter("url");

    if (url != null) {
      url = appendUtmParameters(url);
      final Uri newUrlData = replaceUriParameter(intent.getData(), "url", url);
      intent.setData(newUrlData);
    }
  }

  public static Uri replaceUriParameter(Uri uri, String key, String newValue) {
    final Set<String> params = uri.getQueryParameterNames();
    final Uri.Builder newUri = uri.buildUpon().clearQuery();
    for (String param : params) {
      newUri.appendQueryParameter(param,
          param.equals(key) ? newValue : uri.getQueryParameter(param));
    }

    return newUri.build();
  }
}
