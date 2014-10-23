package com.mopub.mobileads;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import org.fest.assertions.api.ANDROID;
import org.junit.Ignore;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLocalBroadcastManager;

import static com.mopub.common.util.Drawables.INTERSTITIAL_CLOSE_BUTTON_NORMAL;
import static com.mopub.common.util.Drawables.INTERSTITIAL_CLOSE_BUTTON_PRESSED;
import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.getHtmlInterstitialIntentFilter;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

@Ignore
public class BaseInterstitialActivityTest {
    public static final String EXPECTED_SOURCE = "expected source";

    protected BaseInterstitialActivity subject;
    protected BroadcastReceiver broadcastReceiver;
    protected AdConfiguration adConfiguration;
    protected long testBroadcastIdentifier;

    public void setup() {
        broadcastReceiver = mock(BroadcastReceiver.class);
        testBroadcastIdentifier = 2222;
    }

    @Test
    public void onCreate_shouldBroadcastInterstitialShow() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, testBroadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver, getHtmlInterstitialIntentFilter());

        subject.onCreate(null);

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    @Test
    public void onCreate_shouldCreateView() throws Exception {
        subject.onCreate(null);

        View adView = getContentView(subject).getChildAt(0);

        assertThat(adView).isNotNull();
    }

    @Test
    public void onCreate_shouldShowInterstitialCloseButton() throws Exception {
        subject.onCreate(null);

        ImageButton closeButton = getCloseButton();

        Robolectric.clickOn(closeButton);

        ANDROID.assertThat(subject).isFinishing();
    }

    @Test
    public void onCreate_shouldMakeCloseButtonVisible() throws Exception {
        subject.onCreate(null);

        ImageButton closeButton = getCloseButton();

        ANDROID.assertThat(closeButton).isVisible();
        StateListDrawable states = (StateListDrawable) closeButton.getDrawable();

        int[] unpressedState = new int[] {-android.R.attr.state_pressed};
        assertThat(shadowOf(states).getDrawableForState(unpressedState))
                .isEqualTo(INTERSTITIAL_CLOSE_BUTTON_NORMAL.decodeImage(new Activity()));
        int[] pressedState = new int[] {android.R.attr.state_pressed};
        assertThat(shadowOf(states).getDrawableForState(pressedState))
                .isEqualTo(INTERSTITIAL_CLOSE_BUTTON_PRESSED.decodeImage(new Activity()));
    }

    @Test
    public void canShowAndHideTheCloseButton() throws Exception {
        subject.onCreate(null);
        ANDROID.assertThat(getCloseButton()).isVisible();

        subject.hideInterstitialCloseButton();
        ANDROID.assertThat(getCloseButton()).isInvisible();

        subject.showInterstitialCloseButton();
        ANDROID.assertThat(getCloseButton()).isVisible();
    }

    @Test
    public void onDestroy_shouldCleanUpContentView() throws Exception {
        subject.onCreate(null);
        subject.onDestroy();

        assertThat(getContentView(subject).getChildCount()).isEqualTo(0);
    }

    @Test
    public void onDestroy_shouldBroadcastInterstitialDismiss() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, testBroadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver, getHtmlInterstitialIntentFilter());

        subject.onCreate(null);
        subject.onDestroy();

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    @Test
    public void getAdConfiguration_shouldReturnAdConfigurationFromIntent() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(AD_CONFIGURATION_KEY, adConfiguration);

        subject.onCreate(null);
        subject.setIntent(intent);

        assertThat(subject.getAdConfiguration()).isNotNull();
    }

    @Test
    public void getAdConfiguration_withMissingOrWrongAdConfiguration_shouldReturnNull() throws Exception {
        Intent intent = new Intent();
        // This intent is missing an AdConfiguration extra.

        subject.onCreate(null);
        subject.setIntent(intent);

        assertThat(subject.getAdConfiguration()).isNull();
    }

    protected ImageButton getCloseButton() {
        return (ImageButton) getContentView(subject).getChildAt(1);
    }

    protected RelativeLayout getContentView(BaseInterstitialActivity subject) {
        return (RelativeLayout) ((ViewGroup) subject.findViewById(android.R.id.content)).getChildAt(0);
    }

    protected void resetMockedView(View view) {
        reset(view);
        stub(view.getLayoutParams()).toReturn(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
    }
}
