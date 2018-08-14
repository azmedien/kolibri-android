package ch.yanova.kolibri.prototype;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriSplashActivity;
import ch.yanova.kolibri.RuntimeConfig;

/**
 * Created by lekov on 2.10.17.
 */

public final class ProrotypeSplashActivity extends KolibriSplashActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setSplashImage(R.drawable.splash);
  }

  @Override
  protected void onSplashTimedOut() {

    final RuntimeConfig config = Kolibri.getInstance(this).getRuntime();

    if (Boolean.TRUE.toString().equals(config.getString("native-navigation").toLowerCase().trim())) {
      startActivity(new Intent(this, PrototypeNavigationActivity.class));
    } else {
      startActivity(new Intent(this, PrototypeActivity.class));
    }

    finish();
  }
}
