package ch.yanova.kolibri.prototype;

import ch.yanova.kolibri.KolibriApp;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.messaging.FirebaseMessaging;
import io.fabric.sdk.android.Fabric;

/**
 * Created by mmironov on 5/10/17.
 */

public class KolibriPrototypeApp extends KolibriApp {

  @Override
  public void onCreate() {
    super.onCreate();
    Fabric.with(this, new Crashlytics());
    setFirebaseEnabled(false);

    FirebaseMessaging.getInstance().subscribeToTopic("main");
  }
}
