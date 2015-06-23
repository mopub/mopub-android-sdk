package com.mopub.nativeads;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;

import java.util.Iterator;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class ClickDestinationResolutionListenerTest {

    private Activity context;
    private Iterator mockIterator;
    private SpinningProgressView mockSpinningProgressView;
    private ClickDestinationResolutionListener subject;

    @Before
    public void setUp() throws Exception {
        context = spy(Robolectric.buildActivity(Activity.class).create().get());
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getPackageName()).thenReturn("testPackageName");
        mockIterator = mock(Iterator.class);
        mockSpinningProgressView = mock(SpinningProgressView.class);

        subject = new ClickDestinationResolutionListener(context, mockIterator,
                mockSpinningProgressView);
    }

    @Test
    public void onSuccess_withMoPubNativeBrowserUrl_shouldOpenExternalBrowserAndHideProgressView() {
        subject.onSuccess("mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.example.com");

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getDataString()).isEqualTo("https://www.example.com");
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        verify(mockSpinningProgressView).removeFromRoot();
    }

    @Test
    public void onSuccess_withMalformedMoPubNativeBrowserUrl_shouldHideProgressViewButNotOpenExternalBrowser() {
        // url2 is the wrong query parameter
        subject.onSuccess("mopubnativebrowser://navigate?url2=https%3A%2F%2Fwww.example.com");

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
        verify(mockSpinningProgressView).removeFromRoot();
    }

    @Test
    public void onSuccess_withDeepLinkThatIsResolvable_shouldStartActionViewIntent_shouldRemoveSpinningProgressView() {
        String deepLinkUrl = "appscheme://host";
        Robolectric.packageManager.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse(deepLinkUrl)), new ResolveInfo());

        subject.onSuccess(deepLinkUrl);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getDataString()).isEqualTo("appscheme://host");
        verify(mockSpinningProgressView).removeFromRoot();
    }

    @Test
    public void onSuccess_withDeepLinkThatIsUnresolvable_shouldNotStartNewIntent_shouldRemoveSpinningProgressView() {
        String deepLinkUrl = "appscheme://host";
        // don't add any relevant ResolveInfos to the Robolectric packageManager

        subject.onSuccess(deepLinkUrl);

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
        verify(mockSpinningProgressView).removeFromRoot();
    }

    @Test
    public void onSuccess_withAppStoreUrl_shouldStartAppStoreIntent_shouldRemoveSpinningProgressView() {
        final String appStoreUrl = "play.google.com/";
        Robolectric.packageManager.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse(appStoreUrl)), new ResolveInfo());

        subject.onSuccess(appStoreUrl);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getDataString()).isEqualTo(appStoreUrl);
        verify(mockSpinningProgressView).removeFromRoot();
    }

    @Test
    public void onSuccess_withHttpUrl_showOpenInMoPubBrowser_shouldRemoveSpinningProgressViewFromRoot() {
        String httpUrl = "http://www.dotcom.com";

        subject.onSuccess(httpUrl);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startActivity(intentCaptor.capture());

        Intent intent = intentCaptor.getValue();

        assertThat(intent.getComponent().getPackageName()).isEqualTo("testPackageName");
        assertThat(intent.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(intent.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(httpUrl);
        verify(mockSpinningProgressView).removeFromRoot();
    }

    @Test
    public void onSuccess_withHttpsUrl_showOpenInMoPubBrowser_shouldRemoveSpinningProgressViewFromRoot() {
        String httpsUrl = "https://www.comdot.com";

        subject.onSuccess(httpsUrl);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startActivity(intentCaptor.capture());

        Intent intent = intentCaptor.getValue();

        assertThat(intent.getComponent().getPackageName()).isEqualTo("testPackageName");
        assertThat(intent.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(intent.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(httpsUrl);
        verify(mockSpinningProgressView).removeFromRoot();
    }

    @Test
    public void onSuccess_withAboutBlankUrl_shouldFailSilently_shouldRemoveSpinningProgressView() {
        String url = "about:blank";

        subject.onSuccess(url);

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
        verify(mockSpinningProgressView).removeFromRoot();
    }
}
