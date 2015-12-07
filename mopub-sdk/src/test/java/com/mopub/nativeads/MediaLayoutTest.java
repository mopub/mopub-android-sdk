package com.mopub.nativeads;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MediaLayoutTest {

    MediaLayout spySubject;
    Context context;

    @Before
    public void setup() {
        context = ShadowApplication.getInstance().getApplicationContext();
        spySubject = spy(new MediaLayout(context));
        spySubject.setLayoutParams(new RelativeLayout.LayoutParams(300, 300));
        when(spySubject.getMeasuredHeight()).thenReturn(300);
        when(spySubject.getMeasuredWidth()).thenReturn(300);
    }

    @Test
    @SuppressLint("WrongCall") // onMeasure should not ordinarily be called in application code
    public void onMeasure_exactWidth_flexibleHeight_shouldBe16By9() {
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(500, MeasureSpec.AT_MOST);

        spySubject.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final ViewGroup.LayoutParams params = spySubject.getLayoutParams();

        assertThat(params.width).isEqualTo(500);
        assertThat(params.height).isEqualTo((int)(500 * 9f / 16));
    }

    @Test
    @SuppressLint("WrongCall") // onMeasure should not ordinarily be called in application code
    public void onMeasure_flexibleWidth_flexibleHeight_shouldBe16By9() {
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(500, MeasureSpec.AT_MOST);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(500, MeasureSpec.AT_MOST);

        spySubject.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final ViewGroup.LayoutParams params = spySubject.getLayoutParams();

        assertThat(params.width).isEqualTo(300);
        assertThat(params.height).isEqualTo((int)(300 * 9f / 16));
    }

    @Test
    @SuppressLint("WrongCall") // onMeasure should not ordinarily be called in application code
    public void onMeasure_flexibleWidth_ExactHeight_shouldBe16By9() {
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(700, MeasureSpec.AT_MOST);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY);

        spySubject.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final ViewGroup.LayoutParams params = spySubject.getLayoutParams();

        assertThat(params.height).isEqualTo(150);
        assertThat(params.width).isEqualTo((int)(150 * 16f / 9));
    }

    @Test
    @SuppressLint("WrongCall")
    public void onMeasure_exactWidth_exactHeight_heightShouldBeSmaller() {
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY);

        spySubject.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final ViewGroup.LayoutParams params = spySubject.getLayoutParams();

        assertThat(params.width).isEqualTo(500);
        assertThat(params.height).isEqualTo((int)(500 * 9f / 16));
    }
}
