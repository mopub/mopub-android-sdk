package com.mopub.common.util;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class UtilsTest {
    @Test
    public void generateUniqueId_withMultipleInvocations_shouldReturnUniqueValues() throws Exception {
        final int expectedIdCount = 100;

        Set<Long> ids = new HashSet<Long>(expectedIdCount);
        for (int i = 0; i < expectedIdCount; i++) {
            final long id = Utils.generateUniqueId();
            ids.add(id);
        }

        assertThat(ids).hasSize(expectedIdCount);
    }
}
