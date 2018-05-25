package com.mopub.common.privacy;

import android.app.Activity;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class PersonalInfoManagerTest {

    Activity activity;
    PersonalInfoManager subject;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).get();
    }

    @Test
    public void shouldMakeSyncRequest_withGdprAppliesNull_shouldReturnTrue() {
        boolean actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, false, null, 300000,
                null, false);

        assertThat(actual).isTrue();
    }
}
