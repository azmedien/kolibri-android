package ch.yanova.kolibri.components;

import android.content.Context;
import android.net.Uri;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by mmironov on 3/3/17.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({Uri.class})
public class HandleUrlTargetTest {

    public static final String URL_HOST = "www.wildeisen.ch";
    public static final String URL_AMP_HOST = "www.amp.wildeisen.ch";

    public static final String PARAM_TARGET = "kolibri-target";
    public static final String TARGET_SELF = "_self";
    public static final String TARGET_INTERNAL = "_internal";
    public static final String TARGET_EXTERNAL = "_external";

    @Mock
    Context context;

    @Spy
    KolibriWebViewClient webViewClient = new KolibriWebViewClient();

    Uri uri;

    @Before
    public void runBeforeTests() throws Exception {
        PowerMockito.mockStatic(Uri.class);
        uri = mock(Uri.class);
        PowerMockito.when(Uri.class, "parse", anyString()).thenReturn(uri);
    }

    @Test
    public void selfTargetTest() {
        when(uri.getHost()).thenReturn(URL_HOST);
        when(uri.getQueryParameter(PARAM_TARGET)).thenReturn(TARGET_SELF);
        doReturn(true).when(webViewClient).shouldHandleInternal();
        Assert.assertFalse(webViewClient.handleUri(context, Uri.parse("")));
    }

    @Test
    public void internalTargetTest() {
        when(uri.getHost()).thenReturn(URL_HOST);
        when(uri.getQueryParameter(PARAM_TARGET)).thenReturn(TARGET_INTERNAL);
        doReturn(true).when(webViewClient).shouldHandleInternal();
        Assert.assertTrue(webViewClient.handleUri(context, Uri.parse("")));
    }

    @Test
    public void externalTargetTest() {
        when(uri.getHost()).thenReturn(URL_HOST);
        when(uri.getQueryParameter(PARAM_TARGET)).thenReturn(TARGET_EXTERNAL);
        doReturn(true).when(webViewClient).shouldHandleInternal();
        Assert.assertTrue(webViewClient.handleUri(context, Uri.parse("")));
    }

    @Test
    public void noTargetAmp() {
        when(uri.getHost()).thenReturn(URL_AMP_HOST);
        when(uri.getQueryParameter(PARAM_TARGET)).thenReturn(null);
        doReturn(true).when(webViewClient).shouldHandleInternal();
        Assert.assertFalse(webViewClient.handleUri(context, Uri.parse("")));
    }

    @Test
    public void noTargetHtml() {
        when(uri.getHost()).thenReturn(URL_HOST);
        when(uri.getQueryParameter(PARAM_TARGET)).thenReturn(null);
        doReturn(true).when(webViewClient).shouldHandleInternal();
        Assert.assertTrue(webViewClient.handleUri(context, Uri.parse("")));
    }
}
