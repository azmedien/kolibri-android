package ch.yanova.kolibri.components;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.modules.junit4.PowerMockRunner;

import static ch.yanova.kolibri.components.KolibriWebViewClient.TARGET_EXTERNAL;
import static ch.yanova.kolibri.components.KolibriWebViewClient.TARGET_INTERNAL;
import static ch.yanova.kolibri.components.KolibriWebViewClient.TARGET_SELF;
import static org.mockito.Mockito.doReturn;

/**
 * Created by mmironov on 3/3/17.
 */

@RunWith(PowerMockRunner.class)
public class KolibriUrlTargetTest {

    @Mock
    Context context;

    @Mock
    KolibriWebView webView;

    @Spy
    KolibriWebViewClient webViewClient = new KolibriWebViewClient();

    @Test
    public void selfTargetTest() {
        doReturn(true).when(webViewClient).shouldHandleInternal();
        Assert.assertFalse(webViewClient.handleAsSelf(TARGET_SELF));
    }

    @Test
    public void internalTargetTest() {
        doReturn(true).when(webViewClient).shouldHandleInternal();
        Assert.assertTrue(webViewClient.handleAsSelf(TARGET_INTERNAL));
    }

    @Test
    public void externalTargetTest() {
        doReturn(true).when(webViewClient).shouldHandleInternal();
        Assert.assertTrue(webViewClient.handleAsSelf(TARGET_EXTERNAL));
    }

    @Test
    public void noTarget() {
        doReturn(true).when(webViewClient).shouldHandleInternal();
        Assert.assertFalse(webViewClient.handleAsSelf(null));
    }
}
