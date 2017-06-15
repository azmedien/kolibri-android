package ch.yanova.kolibri;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by mmironov on 6/16/17.
 */

public abstract class KolibriSplashActivity extends AppCompatActivity
                        implements RuntimeListener {

    private long minimumDisplayTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        hideSystemUI();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        minimumDisplayTime = 1000;

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
        ((ViewGroup)findViewById(ch.yanova.kolibri.R.id.splash_root)).addView(view);
    }

    @Override
    public void onLoaded(RuntimeConfig runtime) {
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

    public void setSplashImage(@DrawableRes int resId) {
        ((ImageView)findViewById(R.id.splash)).setImageResource(resId);
    }
}
