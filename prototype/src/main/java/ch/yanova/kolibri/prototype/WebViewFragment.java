package ch.yanova.kolibri.prototype;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ch.yanova.kolibri.KolibriFragment;
import ch.yanova.kolibri.components.KolibriWebView;

/**
 * Created by lekov on 4/10/17.
 */

public class WebViewFragment extends KolibriFragment {


    private KolibriWebView webview;

    public static WebViewFragment newInstance() {

        Bundle args = new Bundle();

        WebViewFragment fragment = new WebViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.web_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webview = (KolibriWebView) view.findViewById(R.id.webview);
    }

    public KolibriWebView getWebview() {
        return webview;
    }


}
