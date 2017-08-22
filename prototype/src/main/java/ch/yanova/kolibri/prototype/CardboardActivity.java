package ch.yanova.kolibri.prototype;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;

import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;

import java.io.File;
import java.io.IOException;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.RuntimeConfig;
import ch.yanova.kolibri.TintUtils;

import static ch.yanova.kolibri.RuntimeConfig.THEME_COLOR_PRIMARY;
import static ch.yanova.kolibri.RuntimeConfig.THEME_COLOR_PRIMARY_DARK;
import static ch.yanova.kolibri.RuntimeConfig.getMaterialPalette;

public class CardboardActivity extends AppCompatActivity {

    protected VrVideoView videoWidgetView;

    private Toolbar toolbar;
    private View progress;

    private DialogFragment errorDialog;

    private VrVideoView.Options videoOptions = new VrVideoView.Options();
    boolean tryDefaultFormat;
    private Uri uri;

    public static Intent createIntent(Context context, Uri video) {
        Intent i = new Intent(context, CardboardActivity.class);
        i.setData(video);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardboard);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progress = findViewById(R.id.progress);
        errorDialog = ErrorDialog.newInstance("Video cannot be played.");

        videoWidgetView = (VrVideoView) findViewById(R.id.video_view);
        videoWidgetView.setInfoButtonEnabled(false);
        videoWidgetView.setEventListener(new VrVideoEventListener() {
            @Override
            public void onLoadSuccess() {
                super.onLoadSuccess();
                progress.setVisibility(View.GONE);
            }

            @Override
            public void onLoadError(String errorMessage) {
                super.onLoadError(errorMessage);

                if (!tryDefaultFormat) {
                    tryDefaultFormat = true;
                    videoOptions.inputFormat = VrVideoView.Options.FORMAT_DEFAULT;
                    startVideo();
                }
            }
        });

        uri = getIntent().getData();

        final String type = getMimeType(this, uri);

        videoOptions.inputFormat = "mp4".equals(type) ? VrVideoView.Options.FORMAT_DEFAULT : VrVideoView.Options.FORMAT_HLS;
        videoOptions.inputType = VrVideoView.Options.TYPE_MONO;

        setupStyling();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoWidgetView.pauseRendering();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoWidgetView.resumeRendering();
    }

    @Override
    protected void onDestroy() {
        videoWidgetView.shutdown();
        super.onDestroy();
    }

    private void startVideo() {
        try {
            progress.setVisibility(View.VISIBLE);
            videoWidgetView.loadVideo(uri, videoOptions);
        } catch (IOException e) {
            if (tryDefaultFormat) {
                errorDialog.show(getSupportFragmentManager(), "dialog");
            }
        }
    }

    private static String getMimeType(Context context, Uri uri) {
        String extension;

        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());

        }

        return extension;
    }

    private void setupStyling() {

        final RuntimeConfig configuration = Kolibri.getInstance(this).getRuntime();

        if (configuration == null) {
            return;
        }

        final RuntimeConfig.Styling styling = configuration.getStyling();

        if (styling.hasPaletteColor(RuntimeConfig.Styling.OVERRIDES_TOOLBAR_BACKGROUND)) {
            final int toolbarBackgroud = styling.getPaletteColor(RuntimeConfig.Styling.OVERRIDES_TOOLBAR_BACKGROUND);
            final int[] palette = getMaterialPalette(String.format("#%06X", 0xFFFFFF & toolbarBackgroud));

            TintUtils.tintToolbar(this, toolbar, palette[THEME_COLOR_PRIMARY], palette[THEME_COLOR_PRIMARY_DARK], false);
        } else {
            TintUtils.tintToolbar(this, toolbar, styling.getPrimary(), styling.getPrimaryDark(), false);
        }

        if (styling.hasPaletteColor(RuntimeConfig.Styling.OVERRIDES_TOOLBAR_TEXT)) {
            toolbar.setTitleTextColor(styling.getPaletteColor(RuntimeConfig.Styling.OVERRIDES_TOOLBAR_TEXT));
            toolbar.setSubtitle(styling.getPaletteColor(RuntimeConfig.Styling.OVERRIDES_TOOLBAR_TEXT));
        }

        TintUtils.tintProgressBar((ProgressBar) progress);
    }

    public static class ErrorDialog extends AppCompatDialogFragment {

        public static ErrorDialog newInstance(String message) {
            ErrorDialog frag = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString("message", message);
            frag.setArguments(args);
            return frag;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String message = getArguments().getString("message");

            return new AlertDialog.Builder(getActivity())
                    .setTitle("Error")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {

                        }
                    })
                    .create();
        }
    }
}
