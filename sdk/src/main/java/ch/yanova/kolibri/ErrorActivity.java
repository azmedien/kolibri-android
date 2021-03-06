package ch.yanova.kolibri;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.afollestad.aesthetic.AestheticActivity;

public class ErrorActivity extends AestheticActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_error);

    final Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    final TextView errorMessageTextView = findViewById(R.id.error_text);

    final Intent intent = getIntent();

    if (intent != null) {
      if (intent.hasExtra(Kolibri.EXTRA_ERROR_MESSAGE)) {
        final String message = intent.getStringExtra(Kolibri.EXTRA_ERROR_MESSAGE);
        errorMessageTextView.setText(message);
      }

      if (intent.hasExtra(Intent.EXTRA_TITLE)) {
        final String title = intent.getStringExtra(Intent.EXTRA_TITLE);
        getSupportActionBar().setTitle(title);
      }
    }

    Kolibri.getInstance(this).applyRuntimeTheme(false);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void openGooglePlay(View view) {

    final String applicationId = getApplicationContext().getPackageName();

    final Uri googlePlayUri = Uri.parse("market://details?id=" + applicationId);

    final Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW, googlePlayUri);

    final PackageManager packageManager = getPackageManager();
    if (googlePlayIntent.resolveActivity(packageManager) != null) {
      startActivity(googlePlayIntent);
      finish();
    }
  }
}
