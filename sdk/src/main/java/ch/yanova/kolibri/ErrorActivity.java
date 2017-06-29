package ch.yanova.kolibri;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

public class ErrorActivity extends AppCompatActivity {

    private TextView errorMessageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        errorMessageTextView = (TextView) findViewById(R.id.error_text);

        final Intent intent = getIntent();

        if (intent != null && intent.hasExtra(Kolibri.EXTRA_ERROR_MESSAGE)) {
            final String message = intent.getStringExtra(Kolibri.EXTRA_ERROR_MESSAGE);
            errorMessageTextView.setText(message);
        }
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
}