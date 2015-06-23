package com.mopub.mobileads.util.vast;

import com.mopub.mobileads.VastAbsoluteProgressTracker;
import com.mopub.mobileads.VastFractionalProgressTracker;

import org.junit.Test;

import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;

public class VastVideoConfigurationTest {

    @Test
    public void testAddFractionalTrackers_multipleTimes_shouldBeSorted() throws Exception {
        ArrayList<VastFractionalProgressTracker> testSet1 = new ArrayList<VastFractionalProgressTracker>();
        testSet1.add(new VastFractionalProgressTracker("test1a", 0.24f));
        testSet1.add(new VastFractionalProgressTracker("test1b", 0.5f));
        testSet1.add(new VastFractionalProgressTracker("test1c", 0.91f));

        ArrayList<VastFractionalProgressTracker> testSet2 = new ArrayList<VastFractionalProgressTracker>();
        testSet2.add(new VastFractionalProgressTracker("test2a", 0.14f));
        testSet2.add(new VastFractionalProgressTracker("test2b", 0.6f));
        testSet2.add(new VastFractionalProgressTracker("test2c", 0.71f));

        VastVideoConfiguration subject = new VastVideoConfiguration();

        subject.addFractionalTrackers(testSet1);
        subject.addFractionalTrackers(testSet2);

        assertThat(subject.getFractionalTrackers()).isSorted();
    }

    @Test
    public void testAddAbsoluteTrackers_multipleTimes_shouldBesSorted() throws Exception {
        ArrayList<VastAbsoluteProgressTracker> testSet1 = new ArrayList<VastAbsoluteProgressTracker>();
        testSet1.add(new VastAbsoluteProgressTracker("test1a", 1000));
        testSet1.add(new VastAbsoluteProgressTracker("test1b", 10000));
        testSet1.add(new VastAbsoluteProgressTracker("test1c", 50000));

        ArrayList<VastAbsoluteProgressTracker> testSet2 = new ArrayList<VastAbsoluteProgressTracker>();
        testSet2.add(new VastAbsoluteProgressTracker("test2a", 1100));
        testSet2.add(new VastAbsoluteProgressTracker("test2b", 9000));
        testSet2.add(new VastAbsoluteProgressTracker("test2c", 62000));

        VastVideoConfiguration subject = new VastVideoConfiguration();

        subject.addAbsoluteTrackers(testSet1);
        subject.addAbsoluteTrackers(testSet2);

        assertThat(subject.getAbsoluteTrackers()).isSorted();
    }
}
