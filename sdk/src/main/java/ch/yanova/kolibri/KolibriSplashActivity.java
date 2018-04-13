package ch.yanova.kolibri;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import ch.yanova.kolibri.RuntimeConfig.Navigation;
import com.afollestad.aesthetic.AestheticActivity;

/**
 * Created by mmironov on 6/16/17.
 */

public abstract class KolibriSplashActivity extends AestheticActivity implements RuntimeListener {

  private long minimumDisplayTime;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    hideSystemUI();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);
    minimumDisplayTime = 2000;

    final RuntimeConfig runtime = Kolibri.getInstance(this).getRuntimeConfigFromCache();
    if (runtime != null) {
      findViewById(R.id.splash).postDelayed(new Runnable() {
        @Override
        public void run() {
          onSplashTimedOut();
        }
      }, minimumDisplayTime);
    }

    Kolibri.getInstance(this).loadRuntimeConfiguration(this);
  }

  private void hideSystemUI() {
    getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_FULLSCREEN); // hide status bar
  }

  protected abstract void onSplashTimedOut();

  public void attachToRoot(View view) {
    ((ViewGroup) findViewById(R.id.splash_root)).addView(view);
  }

  public void setSplashImage(@DrawableRes int resId) {
    ((ImageView) findViewById(R.id.splash)).setImageResource(resId);
  }

  @Override
  public void onLoaded(RuntimeConfig runtime, boolean isFresh) {
    Kolibri.getInstance(this).applyRuntimeTheme(false);
    findViewById(R.id.splash).postDelayed(new Runnable() {
      @Override
      public void run() {
        onSplashTimedOut();
      }
    }, minimumDisplayTime);
  }

  @Override
  public boolean onFailed(Exception e) {
    findViewById(R.id.splash).postDelayed(new Runnable() {
      @Override
      public void run() {
        onSplashTimedOut();
      }
    }, minimumDisplayTime);

    return true;
  }
}
