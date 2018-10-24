package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.crashlytics.android.Crashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriApp;
import ch.yanova.kolibri.KolibriException;
import ch.yanova.kolibri.RuntimeConfig;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static ch.yanova.kolibri.Kolibri.TARGET_EXTERNAL;
import static ch.yanova.kolibri.Kolibri.TARGET_INTERNAL;
import static ch.yanova.kolibri.Kolibri.TARGET_SELF;

/** Created by mmironov on 2/26/17. */
public class KolibriWebView extends WebView {

  public static String UAStringPrefix = "";
  private static final String GET_HTML_STRING =
      "javascript:window.GetHtml.processHTML('<head>'+document.getElementsByTagName('head')[0].innerHTML+'</head>');";
  public final String TAG = KolibriWebView.class.getSimpleName();

  private List<KolibriWebViewClient> webClients;
  private List<KolibriWebChromeClient> webChromeClients;
  private RuntimeConfig config;
  private boolean proxyFailure;

  private Intent intent;

  private boolean shouldHandleInternal;
  private boolean clearHistory;

  public KolibriWebView(Context context) {
    super(context);
    init();
  }

  public KolibriWebView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public KolibriWebView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {

    if (!isInEditMode()) {

      final InternalWebViewClient internalWebViewClient = new InternalWebViewClient();
      final InternalChromeClient internalChromeClient = new InternalChromeClient();

      webClients = new ArrayList<>();
      webChromeClients = new ArrayList<>();

      super.setWebViewClient(internalWebViewClient);
      super.setWebChromeClient(internalChromeClient);

      setVerticalScrollBarEnabled(false);
      setHorizontalScrollBarEnabled(false);

      if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
      }

      getSettings().setJavaScriptEnabled(true);
      getSettings().setAppCacheEnabled(true);
      getSettings().setDomStorageEnabled(true);
      getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

      // Load http thumbnails and assets over a secure https webpages
      if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
        getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
      }

      if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
        // chromium, enable hardware acceleration
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        if (0 != (getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
          WebView.setWebContentsDebuggingEnabled(true);
        }
      } else {
        // older android version, disable hardware acceleration
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
      }

      clearHistory = false;

      try {
        config = Kolibri.getInstance(getContext()).getRuntime();
      } catch (KolibriException e) {
        e.printStackTrace();
      }

      if (config != null) {
        UAStringPrefix = config.getScheme() + "-android / ";
      }
      UAStringPrefix += "Kolibri / Android-universal /";
      getSettings().setUserAgentString(UAStringPrefix + " " + getSettings().getUserAgentString());
    }
  }

  @Override
  public final void setWebViewClient(WebViewClient client) {
    throw new KolibriException(
        "Cannot preset WebView client. Use #setKolibriWebViewClient instead.");
  }

  @Override
  public final void setWebChromeClient(WebChromeClient client) {
    throw new KolibriException(
        "Cannot preset WebChrome client. Use #setKolibriWebChromeClient instead.");
  }

  public void addKolibriWebViewClient(@NonNull KolibriWebViewClient client) {
    if (client != null) {
      webClients.add(client);
    }
  }

  public void addKolibriWebChromeClient(@NonNull KolibriWebChromeClient client) {
    if (client != null) {
      webChromeClients.add(client);
    }
  }

  public Intent getIntent() {
    return intent;
  }

  public void setIntent(Intent intent) {
    this.intent = intent;
  }

  private boolean handleInNewView(String target) {
    if (target == null) {
      target = TARGET_SELF;
    }

    if (TARGET_INTERNAL.equals(target) && !shouldHandleInternal) {
      target = TARGET_SELF;
    }

    return !TARGET_SELF.equals(target);
  }

  public boolean handleUri(Uri link) {

    final Context context = getContext();
    final String target = Kolibri.getInstance(getContext()).getTarget(link);

    final boolean handleInNewView = handleInNewView(target);
    boolean handleCustomTarget = false;

    if (handleInNewView) {

      for (KolibriWebViewClient webClient : webClients) {
        if (webClient.onCustomTarget(link, target)) {
          handleCustomTarget = true;
          break;
        }
      }

      if (!handleCustomTarget) {

        final String appScheme = Kolibri.getInstance(context).getRuntime().getScheme();

        Intent linkIntent =
            target.equals(TARGET_INTERNAL)
                ? new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(String.format("%s://internal/webview?url=%s", appScheme, link)))
                : new Intent(Intent.ACTION_VIEW, link);

        final Intent kolibriIntent = getIntent();

        if (kolibriIntent != null) {
          linkIntent.putExtras(kolibriIntent);
        }

        final PackageManager packageManager = context.getPackageManager();
        if (linkIntent.resolveActivity(packageManager) != null) {
          context.startActivity(linkIntent);
        } else {
          final String scheme = link.getScheme();

          if (scheme.equals(appScheme)) {
            Kolibri.notifyComponents(context, Kolibri.createIntent(link));
          }
        }
      }
    } else {
      // If in proxy mode, load from proxy and tell the webview, actually we handle it
      if (config.inProxyMode()) {
        try {
          loadFromProxy(link.toString());
          return true;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    return handleInNewView;
  }

  /**
   *
   * Load url
   *
   * <p>
   *   Unless we are in proxy mode we delegate the loading to the webivew.
   *   In case we are using proxy we try to load the HTML from there
   * </p>
   *
   * @see #loadFromProxy(String)
   *
   * @param url URL to be loaded on the webview
   */
  @Override
  public void loadUrl(final String url) {

    if (!config.inProxyMode() || GET_HTML_STRING.equals(url)) {
      super.loadUrl(url);
      return;
    }

    // Prevents flood the proxy and recursively load once page was failed
    if (proxyFailure) {
      Log.w("KolibriWebView", String.format("Once failed to be processed trough the proxy, falling back to default load [url = %s]", url));
      proxyFailure = false;
      super.loadUrl(url);
      return;
    }

    try {
      loadFromProxy(url);
    } catch (Exception ex) {
      Log.e("KolibriWebView", String.format("Cannot load from proxy, fallback to normal load [url = %s]: ", url), ex);
      super.loadUrl(url);
    }
  }

  /**
   *
   * Load URL trough the proxy.
   *
   * <p>
   *   Loading an URL trough the proxy will return a HTML with stripped
   *   navigation, header and footer elements of the page.
   * </p>
   *
   * @param url URL to be loaded trough the proxy
   * @throws JSONException when there's error with the JSON parsing
   */
  private void loadFromProxy(final String url) throws JSONException {
    final String proxyUrl = config.getProxy();
    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    final JSONObject json = new JSONObject();
    final JSONObject data = new JSONObject();
    data.put("url", url);
    json.put("data", data);

    final OkHttpClient client = new OkHttpClient();
    final RequestBody body = RequestBody.create(JSON, json.toString());
    final Request request = new Request.Builder()
        .url(proxyUrl)
        .header("User-Agent", getSettings().getUserAgentString())
        .post(body).build();

    for (KolibriWebViewClient webClient : webClients) {
      webClient.onPageStarted(this, url, null);
    }

    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(@NonNull Call call, @NonNull IOException e) {
                post(
                    new Runnable() {
                      @Override
                      public void run() {
                        proxyFailure = true;
                        loadUrl(url);
                      }
                    });
              }

              @Override
              public void onResponse(@NonNull Call call, @NonNull final Response response)
                  throws IOException {

                final String html = response.body().string();

                post(
                    new Runnable() {
                      @Override
                      public void run() {
                        if (response.isSuccessful()) {
                          loadDataWithBaseURL(url, html, "text/html", "UTF-8", null);
                        } else {
                          proxyFailure = true;
                          loadUrl(url);
                        }
                      }
                    });
              }
            });
  }

  public boolean shouldHandleInternal() {
    return shouldHandleInternal;
  }

  public void setHandleInternal(boolean handleInternal) {
    this.shouldHandleInternal = handleInternal;
  }

  public boolean shouldClearHistory() {
    return clearHistory;
  }

  public void setClearHistory(boolean clearHistory) {
    this.clearHistory = clearHistory;
  }

  private class InternalChromeClient extends WebChromeClient {

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
      super.onProgressChanged(view, newProgress);
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && newProgress == 100) {
        final String url = view.getUrl();
        if (!"about:blank".equals(url) && url != null) {
          final Uri link = Uri.parse(url);
          final String target = Kolibri.getInstance(view.getContext()).getTarget(link);

          // Skip external targets when reporting to netmetrix
          if (!TARGET_EXTERNAL.equals(target)) {
            KolibriApp.getInstance().logEvent(null, link.toString());
          }
        }
      }
      for (KolibriWebChromeClient webChromeClient : webChromeClients) {
        webChromeClient.onProgressChanged(view, newProgress);
      }
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
      super.onShowCustomView(view, callback);
      for (KolibriWebChromeClient webChromeClient : webChromeClients) {
        webChromeClient.onShowCustomView(view, callback);
      }
    }

    @Override
    public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
      super.onShowCustomView(view, requestedOrientation, callback);
      for (KolibriWebChromeClient webChromeClient : webChromeClients) {
        webChromeClient.onShowCustomView(view, requestedOrientation, callback);
      }
    }

    @Override
    public void onHideCustomView() {
      super.onHideCustomView();
      for (KolibriWebChromeClient webChromeClient : webChromeClients) {
        webChromeClient.onHideCustomView();
      }
    }

    @Override
    public View getVideoLoadingProgressView() {

      for (KolibriWebChromeClient webChromeClient : webChromeClients) {
        if (webChromeClient.getVideoLoadingProgressView() != null) {
          return webChromeClient.getVideoLoadingProgressView();
        }
      }

      return super.getVideoLoadingProgressView();
    }

    @Override
    public void getVisitedHistory(ValueCallback<String[]> callback) {
      super.getVisitedHistory(callback);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onShowFileChooser(
        WebView webView,
        ValueCallback<Uri[]> filePathCallback,
        FileChooserParams fileChooserParams) {

      for (KolibriWebChromeClient webChromeClient : webChromeClients) {
        if (webChromeClient.onShowFileChooser(webView, filePathCallback, fileChooserParams)) {
          return true;
        }
      }

      return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
    }
  }

  private class InternalWebViewClient extends WebViewClient {

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      Uri link = Uri.parse(url);
      return handleUri(link);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
      Uri link = request.getUrl();
      return handleUri(link);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onPageCommitVisible(WebView view, String url) {
      super.onPageCommitVisible(view, url);

      if ("about:blank".equals(url)) {
        return;
      }

      final Uri link = Uri.parse(url);
      final String target = Kolibri.getInstance(view.getContext()).getTarget(link);

      // Skip external targets when reporting to netmetrix
      if (!TARGET_EXTERNAL.equals(target)) {
        KolibriApp.getInstance().logEvent(null, link.toString());
      }

      for (KolibriWebViewClient webClient : webClients) {
        webClient.onPageCommitVisible(view, url);
      }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
      super.onPageFinished(view, url);

      if (shouldClearHistory()) {
        setClearHistory(false);
        clearHistory();
      }

      view.loadUrl(GET_HTML_STRING);

      for (KolibriWebViewClient webClient : webClients) {
        webClient.onPageFinished(view, url);
      }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      super.onPageStarted(view, url, favicon);

      for (KolibriWebViewClient webClient : webClients) {
        webClient.onPageStarted(view, url, favicon);
      }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
      super.onReceivedError(view, request, error);
      Crashlytics.log(
          5,
          "KolibriWebView",
          "onReceivedError() called with: request = [" + request + "], error = [" + error + "]");

      // We ignore errors regarding assets loading,
      // so in this case we check if the request url and webview url are same
      if (request.getUrl().toString().equals(view.getUrl()) || request.isForMainFrame()) {
        for (KolibriWebViewClient webClient : webClients) {
          webClient.onReceivedError(view, request, error);
        }
      }
    }

    @Override
    public void onReceivedError(
        WebView view, int errorCode, String description, String failingUrl) {
      super.onReceivedError(view, errorCode, description, failingUrl);
      Crashlytics.log(
          5,
          "KolibriWebView",
          "onReceivedError() called with: errorCode = ["
              + errorCode
              + "], description = ["
              + description
              + "], failingUrl = ["
              + failingUrl
              + "]");

      for (KolibriWebViewClient webClient : webClients) {
        webClient.onReceivedError(view, errorCode, description, failingUrl);
      }
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

      Map<String, String> params = new HashMap<>();

      for (KolibriWebViewClient webClient : webClients) {
        params.putAll(webClient.onRequestExtraParams());
      }

      handler.proceed(params.get("username"), params.get("password"));
    }
  }
}
