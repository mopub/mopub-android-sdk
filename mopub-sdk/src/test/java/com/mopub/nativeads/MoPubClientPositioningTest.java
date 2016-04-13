package com.mopub.nativeads;

import com.mopub.common.Preconditions.NoThrow;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;

import org.fest.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubClientPositioningTest {

    private MoPubClientPositioning subject;

    @Before
    public void setup() {
        subject = MoPubNativeAdPositioning.clientPositioning();
    }

    @Test
    public void addFixedPositionsOutOfOrder_shouldBeSorted() {
        subject.addFixedPosition(27);
        subject.addFixedPosition(31);
        subject.addFixedPosition(17);
        subject.addFixedPosition(7);
        subject.addFixedPosition(56);

        assertThat(subject.getFixedPositions())
                .isEqualTo(Lists.newArrayList(7, 17, 27, 31, 56));
    }

    @Test
    public void setRepeatingEnabled_shouldHaveRightInterval() {
        subject.addFixedPosition(10);
        subject.enableRepeatingPositions(5);

        assertThat(subject.getRepeatingInterval()).isEqualTo(5);
        assertThat(subject.getFixedPositions()).isEqualTo(Lists.newArrayList(10));
    }

    @Test
    public void setNoRepeat_shouldReturnNoRepeat() {
        subject.enableRepeatingPositions(5);
        subject.enableRepeatingPositions(NO_REPEAT);

        assertThat(subject.getRepeatingInterval()).isEqualTo(NO_REPEAT);
    }

    @Test
    public void setFixedPositionTwice_shouldReturnOnlyOne() {
        subject.addFixedPosition(7);
        subject.addFixedPosition(7);

        assertThat(subject.getFixedPositions().size()).isEqualTo(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setInvalidFixedPosition_strictMode_shouldThrow() {
        NoThrow.setStrictMode(true);
        subject.addFixedPosition(-3);
    }

    @Test
    public void setInvalidFixedPosition_releaseMode_shouldNotAddPosition() {
        NoThrow.setStrictMode(false);
        subject.addFixedPosition(-3);

        assertThat(subject.getFixedPositions().size()).isEqualTo(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setInvalidRepeatingInterval_strictMode_shouldThrow() {
        NoThrow.setStrictMode(true);
        subject.enableRepeatingPositions(1);
    }

    @Test
    public void setInvalidRepeatingInterval_releaseMode_shouldClearRepeatingInterval() {
        NoThrow.setStrictMode(false);
        subject.enableRepeatingPositions(0);

        assertThat(subject.getRepeatingInterval()).isEqualTo(NO_REPEAT);
    }
}