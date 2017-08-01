package ch.yanova.kolibri.prototype;

import android.app.Dialog;
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
import android.view.View;
import android.widget.ProgressBar;

import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;

import java.io.IOException;

public class CardboardActivity extends AppCompatActivity {

    protected VrVideoView videoWidgetView;

    private View progress;
    private View overlay;

    private DialogFragment errorDialog;
    private VrVideoView.Options videoOptions = new VrVideoView.Options();

    boolean tryDefaultFormat;

    public static Intent createIntent(Context context, Uri video) {
        Intent i = new Intent(context, CardboardActivity.class);
        i.setData(video);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardboard);

        progress = findViewById(R.id.progress);
        overlay = findViewById(R.id.overlay);
        errorDialog = ErrorDialog.newInstance("Video cannot be played.");

        videoWidgetView = (VrVideoView) findViewById(R.id.video_view);
        videoWidgetView.setEventListener(new VrVideoEventListener() {
            @Override
            public void onLoadSuccess() {
                super.onLoadSuccess();
                progress.setVisibility(View.GONE);
                overlay.setVisibility(View.GONE);
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
    }

    @Override
    protected void onStart() {
        super.onStart();

        videoOptions.inputFormat = VrVideoView.Options.FORMAT_HLS;
        videoOptions.inputType = VrVideoView.Options.TYPE_MONO;
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
            overlay.setVisibility(View.VISIBLE);
            videoWidgetView.loadVideo(getIntent().getData(), videoOptions);
        } catch (IOException e) {
            if (tryDefaultFormat) {
                errorDialog.show(getSupportFragmentManager(), "dialog");
            }
        }
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
