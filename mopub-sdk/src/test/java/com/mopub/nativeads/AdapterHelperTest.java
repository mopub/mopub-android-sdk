package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdapterHelperTest {
    private AdapterHelper subject;
    private Activity context;
    private int contentRowCount;
    private int start;
    private int interval;

    @Before
    public void setUp() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        start = 1;
        interval = 2;
        subject = new AdapterHelper(context, start, interval);
    }

    @Test
    public void constructor_whenPassedAnApplicationContext_shouldNotThrowIllegalArgumentException() {
        new AdapterHelper(context.getApplicationContext(), start, interval);
    }

    @Test
    public void getAdView_withNullActivityContext_shouldReturnEmptyViewWithApplicationContext() {
        subject.clearContext();
        Context viewContext = subject.getAdView(null, null, mock(NativeAd.class),
                mock(ViewBinder.class)).getContext();
        assertThat(viewContext).isEqualTo(context.getApplication());
    }

    @Test
    public void adapterHelper_withContentRowCountOf10_shouldCalculateCorrectly() {
        contentRowCount = 10;

        start = 0;
        interval = 2;
        subject = new AdapterHelper(context, start, interval);

//      acacacacacacacacacac
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(20);
        validateRows(20, start, interval);
        assertThat(subject.shiftedPosition(1)).isEqualTo(0);
        assertThat(subject.shiftedPosition(3)).isEqualTo(1);
        assertThat(subject.shiftedPosition(5)).isEqualTo(2);
        assertThat(subject.shiftedPosition(7)).isEqualTo(3);
        assertThat(subject.shiftedPosition(9)).isEqualTo(4);
        assertThat(subject.shiftedPosition(11)).isEqualTo(5);
        assertThat(subject.shiftedPosition(13)).isEqualTo(6);
        assertThat(subject.shiftedPosition(15)).isEqualTo(7);
        assertThat(subject.shiftedPosition(17)).isEqualTo(8);
        assertThat(subject.shiftedPosition(19)).isEqualTo(9);

        start = 0;
        interval = 6;
        subject = new AdapterHelper(context, start, interval);

//      acccccaccccc
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(12);
        validateRows(12, start, interval);
        assertThat(subject.shiftedPosition(1)).isEqualTo(0);
        assertThat(subject.shiftedPosition(2)).isEqualTo(1);
        assertThat(subject.shiftedPosition(3)).isEqualTo(2);
        assertThat(subject.shiftedPosition(4)).isEqualTo(3);
        assertThat(subject.shiftedPosition(5)).isEqualTo(4);
        assertThat(subject.shiftedPosition(7)).isEqualTo(5);
        assertThat(subject.shiftedPosition(8)).isEqualTo(6);
        assertThat(subject.shiftedPosition(9)).isEqualTo(7);
        assertThat(subject.shiftedPosition(10)).isEqualTo(8);
        assertThat(subject.shiftedPosition(11)).isEqualTo(9);

        start = 0;
        interval = 11;
        subject = new AdapterHelper(context, start, interval);

//      acccccccccc
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(11);
        validateRows(11, start, interval);
        assertThat(subject.shiftedPosition(1)).isEqualTo(0);
        assertThat(subject.shiftedPosition(5)).isEqualTo(4);
        assertThat(subject.shiftedPosition(10)).isEqualTo(9);

        start = 0;
        interval = 4;
        subject = new AdapterHelper(context, start, interval);

//      acccacccacccac
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(14);
        validateRows(14, start, interval);

        start = 5;
        interval = 6;
        subject = new AdapterHelper(context, start, interval);

//      cccccaccccc
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(11);
        validateRows(11, start, interval);
        assertThat(subject.shiftedPosition(4)).isEqualTo(4);
        assertThat(subject.shiftedPosition(6)).isEqualTo(5);
        assertThat(subject.shiftedPosition(10)).isEqualTo(9);

        start = 5;
        interval = 5;
        subject = new AdapterHelper(context, start, interval);

//      cccccaccccac
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(12);
        validateRows(12, start, interval);
        assertThat(subject.shiftedPosition(6)).isEqualTo(5);
        assertThat(subject.shiftedPosition(11)).isEqualTo(9);

        start = 3;
        interval = 4;
        subject = new AdapterHelper(context, start, interval);

//      cccacccacccac
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(13);
        validateRows(13, start, interval);
        assertThat(subject.shiftedPosition(0)).isEqualTo(0);
        assertThat(subject.shiftedPosition(2)).isEqualTo(2);
        assertThat(subject.shiftedPosition(12)).isEqualTo(9);

        start = 10;
        interval = 100;
        subject = new AdapterHelper(context, start, interval);

//      cccccccccc
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(10);
        validateRows(10, start, interval);
        assertThat(subject.shiftedPosition(3)).isEqualTo(3);
        assertThat(subject.shiftedPosition(7)).isEqualTo(7);
        assertThat(subject.shiftedPosition(9)).isEqualTo(9);

        start = 0;
        interval = 10;
        subject = new AdapterHelper(context, start, interval);

//      acccccccccac
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(12);
        validateRows(12, start, interval);
        assertThat(subject.shiftedPosition(1)).isEqualTo(0);
        assertThat(subject.shiftedPosition(9)).isEqualTo(8);
        assertThat(subject.shiftedPosition(11)).isEqualTo(9);
    }

    @Test
    public void adapterHelper_withContentRowCountOf1_shouldCalculateCorrectly() {
        contentRowCount = 1;
        start = 0;
        interval = 2;
        subject = new AdapterHelper(context, start, interval);

//      ac
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(2);
        validateRows(2, start, interval);
        assertThat(subject.shiftedPosition(1)).isEqualTo(0);

        // We can still query for large numbers since the calculation assumes an infinite list
        assertThat(subject.shiftedPosition(1001)).isEqualTo(500);

        start = 1;
        interval = 2;
        subject = new AdapterHelper(context, start, interval);

//      c
        assertThat(subject.shiftedCount(contentRowCount)).isEqualTo(1);
        validateRows(1, start, interval);
        assertThat(subject.shiftedPosition(0)).isEqualTo(0);
    }

    private void validateRows(int totalRows, int start, int interval) {
        for (int i = 0; i < totalRows; ++i) {
            if (i == start) {
                assertThat(subject.isAdPosition(i)).isTrue();
            } else if (i > start && ((i - start) % (interval) == 0)) {
                assertThat(subject.isAdPosition(i)).isTrue();
            } else {
                assertThat(subject.isAdPosition(i)).isFalse();
            }
        }
    }
}
