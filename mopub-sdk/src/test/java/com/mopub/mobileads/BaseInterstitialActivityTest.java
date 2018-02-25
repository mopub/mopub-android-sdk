package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class BaseInterstitialActivityTest {
    private BaseInterstitialActivity subject;
    private long broadcastIdentifier;

    // Make a concrete version of the abstract class for testing purposes.
    private static class TestInterstitialActivity extends BaseInterstitialActivity {
        View view;

        @Override
        public View getAdView() {
            if (view == null) {
                view = new View(this);
            }
            return view;
        }
    }

    @Before
    public void setup() {
        broadcastIdentifier = 2222;
    }

    @Test
    public void onCreate_shouldCreateView() throws Exception {
        subject = Robolectric.buildActivity(TestInterstitialActivity.class).create().get();
        View adView = getContentView(subject).getChildAt(0);

        assertThat(adView).isNotNull();
    }

    @Test
    public void onDestroy_shouldCleanUpContentView() throws Exception {
        subject = Robolectric.buildActivity(TestInterstitialActivity.class).create().destroy().get();

        assertThat(getContentView(subject).getChildCount()).isEqualTo(0);
    }

    @Test
    public void getBroadcastIdentifier_shouldReturnBroadcastIdFromIntent() throws Exception {
        Context context = Robolectric.buildActivity(Activity.class).create().get();
        Intent intent = new Intent(context, TestInterstitialActivity.class);
        intent.putExtra(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);

        subject = Robolectric.buildActivity(TestInterstitialActivity.class, intent)
                .create().get();
        assertThat(subject.getBroadcastIdentifier()).isEqualTo(2222L);
    }

    @Test
    public void getBroadcastIdentifier_withMissingBroadCastId_shouldReturnNull() throws Exception {
        Context context = Robolectric.buildActivity(Activity.class).create().get();
        Intent intent = new Intent(context, TestInterstitialActivity.class);
        // This intent is missing a broadcastidentifier extra.

        subject = Robolectric.buildActivity(TestInterstitialActivity.class, intent)
                .create().get();

        assertThat(subject.getBroadcastIdentifier()).isNull();
    }

    protected FrameLayout getContentView(BaseInterstitialActivity subject) {
        return subject.getCloseableLayout();
    }
}
