

package com.mopub.nativeads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mopub.nativeads.PlacementData.NOT_FOUND;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class PlacementDataTest {
    private PlacementData noAds;
    private PlacementData adAt0;
    private PlacementData adAt1;
    private PlacementData adsAt01;
    private PlacementData adsAt1234;
    private PlacementData adsAt14;
    private PlacementData adsRepeating;
    private PlacementData adsAt15repeating;

    @Mock private NativeAd mMockNativeAd;
    @Mock private NativeAd mMockNativeAd2;
    @Mock private NativeAd mMockNativeAd3;
    @Mock private NativeAd mMockNativeAd4;

    @Before
    public void setup() {
        noAds = PlacementData.fromAdPositioning(MoPubNativeAdPositioning.clientPositioning());
        adAt0 = PlacementData.fromAdPositioning(MoPubNativeAdPositioning.clientPositioning()
                .addFixedPosition(0));
        adAt1 = PlacementData.fromAdPositioning(MoPubNativeAdPositioning.clientPositioning()
                .addFixedPosition(1));
        adsAt01 = PlacementData.fromAdPositioning(MoPubNativeAdPositioning.clientPositioning()
                .addFixedPosition(0)
                .addFixedPosition(1));
        adsAt14 = PlacementData.fromAdPositioning(MoPubNativeAdPositioning.clientPositioning()
                .addFixedPosition(1)
                .addFixedPosition(4));
        adsRepeating = PlacementData.fromAdPositioning(MoPubNativeAdPositioning.clientPositioning()
                .enableRepeatingPositions(3));
        adsAt15repeating = PlacementData.fromAdPositioning(MoPubNativeAdPositioning.clientPositioning()
                .addFixedPosition(1)
                .addFixedPosition(5)
                .enableRepeatingPositions(3));
        adsAt1234 = PlacementData.fromAdPositioning(MoPubNativeAdPositioning.clientPositioning()
                .addFixedPosition(1)
                .addFixedPosition(2)
                .addFixedPosition(3)
                .addFixedPosition(4));
    }

    @Test
    public void initialState_isDesired() {
        assertThat(noAds.shouldPlaceAd(0)).isFalse();
        assertThat(noAds.shouldPlaceAd(1)).isFalse();
        assertThat(noAds.shouldPlaceAd(2)).isFalse();
        assertThat(noAds.shouldPlaceAd(3)).isFalse();
        assertThat(noAds.shouldPlaceAd(4)).isFalse();

        assertThat(adAt0.shouldPlaceAd(0)).isTrue();
        assertThat(adAt0.shouldPlaceAd(1)).isFalse();
        assertThat(adAt0.shouldPlaceAd(2)).isFalse();
        assertThat(adAt0.shouldPlaceAd(3)).isFalse();
        assertThat(adAt0.shouldPlaceAd(4)).isFalse();

        assertThat(adAt1.shouldPlaceAd(0)).isFalse();
        assertThat(adAt1.shouldPlaceAd(1)).isTrue();
        assertThat(adAt1.shouldPlaceAd(2)).isFalse();
        assertThat(adAt1.shouldPlaceAd(3)).isFalse();
        assertThat(adAt1.shouldPlaceAd(4)).isFalse();

        assertThat(adsAt01.shouldPlaceAd(0)).isTrue();
        assertThat(adsAt01.shouldPlaceAd(1)).isFalse();
        assertThat(adsAt01.shouldPlaceAd(2)).isFalse();
        assertThat(adsAt01.shouldPlaceAd(3)).isFalse();
        assertThat(adsAt01.shouldPlaceAd(4)).isFalse();

        assertThat(adsAt14.shouldPlaceAd(0)).isFalse();
        assertThat(adsAt14.shouldPlaceAd(1)).isTrue();
        assertThat(adsAt14.shouldPlaceAd(2)).isFalse();
        assertThat(adsAt14.shouldPlaceAd(3)).isTrue();
        assertThat(adsAt14.shouldPlaceAd(4)).isFalse();

        assertThat(adsRepeating.shouldPlaceAd(0)).isFalse();
        assertThat(adsRepeating.shouldPlaceAd(1)).isFalse();
        assertThat(adsRepeating.shouldPlaceAd(2)).isTrue();
        assertThat(adsRepeating.shouldPlaceAd(3)).isFalse();
        assertThat(adsRepeating.shouldPlaceAd(4)).isTrue();
        assertThat(adsRepeating.shouldPlaceAd(5)).isFalse();
        assertThat(adsRepeating.shouldPlaceAd(6)).isTrue();

        assertThat(adsAt15repeating.shouldPlaceAd(0)).isFalse();
        assertThat(adsAt15repeating.shouldPlaceAd(1)).isTrue();
        assertThat(adsAt15repeating.shouldPlaceAd(2)).isFalse();
        assertThat(adsAt15repeating.shouldPlaceAd(3)).isFalse();
        assertThat(adsAt15repeating.shouldPlaceAd(4)).isTrue();
        assertThat(adsAt15repeating.shouldPlaceAd(5)).isFalse();
        assertThat(adsAt15repeating.shouldPlaceAd(6)).isTrue();
        assertThat(adsAt15repeating.shouldPlaceAd(7)).isFalse();
        assertThat(adsAt15repeating.shouldPlaceAd(8)).isTrue();

        assertThat(adsAt1234.shouldPlaceAd(0)).isFalse();
        assertThat(adsAt1234.shouldPlaceAd(1)).isTrue();
        assertThat(adsAt1234.shouldPlaceAd(2)).isFalse();
        assertThat(adsAt1234.shouldPlaceAd(3)).isFalse();
    }

    @Test
    public void initialState_nextInsertionPosition() {
        assertThat(noAds.nextInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(noAds.nextInsertionPosition(1)).isEqualTo(NOT_FOUND);

        assertThat(adAt0.nextInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(adAt0.nextInsertionPosition(1)).isEqualTo(NOT_FOUND);

        assertThat(adAt1.nextInsertionPosition(0)).isEqualTo(1);
        assertThat(adAt1.nextInsertionPosition(1)).isEqualTo(NOT_FOUND);
        assertThat(adAt1.nextInsertionPosition(2)).isEqualTo(NOT_FOUND);

        assertThat(adsAt01.nextInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(adsAt01.nextInsertionPosition(1)).isEqualTo(NOT_FOUND);
        assertThat(adsAt01.nextInsertionPosition(2)).isEqualTo(NOT_FOUND);

        assertThat(adsAt14.nextInsertionPosition(0)).isEqualTo(1);
        assertThat(adsAt14.nextInsertionPosition(1)).isEqualTo(3);
        assertThat(adsAt14.nextInsertionPosition(2)).isEqualTo(3);
        assertThat(adsAt14.nextInsertionPosition(3)).isEqualTo(NOT_FOUND);
        assertThat(adsAt14.nextInsertionPosition(4)).isEqualTo(NOT_FOUND);

        assertThat(adsRepeating.nextInsertionPosition(0)).isEqualTo(2);
        assertThat(adsRepeating.nextInsertionPosition(1)).isEqualTo(2);
        assertThat(adsRepeating.nextInsertionPosition(2)).isEqualTo(4);
        assertThat(adsRepeating.nextInsertionPosition(3)).isEqualTo(4);
        assertThat(adsRepeating.nextInsertionPosition(4)).isEqualTo(6);
        assertThat(adsRepeating.nextInsertionPosition(5)).isEqualTo(6);
        assertThat(adsRepeating.nextInsertionPosition(6)).isEqualTo(8);

        assertThat(adsAt15repeating.nextInsertionPosition(0)).isEqualTo(1);
        assertThat(adsAt15repeating.nextInsertionPosition(1)).isEqualTo(4);
        assertThat(adsAt15repeating.nextInsertionPosition(2)).isEqualTo(4);
        assertThat(adsAt15repeating.nextInsertionPosition(3)).isEqualTo(4);
        assertThat(adsAt15repeating.nextInsertionPosition(4)).isEqualTo(6);
        assertThat(adsAt15repeating.nextInsertionPosition(5)).isEqualTo(6);
        assertThat(adsAt15repeating.nextInsertionPosition(6)).isEqualTo(8);
        assertThat(adsAt15repeating.nextInsertionPosition(7)).isEqualTo(8);
        assertThat(adsAt15repeating.nextInsertionPosition(8)).isEqualTo(10);

        assertThat(adsAt1234.nextInsertionPosition(0)).isEqualTo(1);
        assertThat(adsAt1234.nextInsertionPosition(1)).isEqualTo(NOT_FOUND);
        assertThat(adsAt1234.nextInsertionPosition(2)).isEqualTo(NOT_FOUND);
        assertThat(adsAt1234.nextInsertionPosition(3)).isEqualTo(NOT_FOUND);
        assertThat(adsAt1234.nextInsertionPosition(4)).isEqualTo(NOT_FOUND);
        assertThat(adsAt1234.nextInsertionPosition(5)).isEqualTo(NOT_FOUND);
    }

    @Test
    public void initialState_prevInsertionPosition() {
        assertThat(noAds.previousInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(noAds.previousInsertionPosition(1)).isEqualTo(NOT_FOUND);

        assertThat(adAt0.previousInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(adAt0.previousInsertionPosition(1)).isEqualTo(0);
        assertThat(adAt0.previousInsertionPosition(2)).isEqualTo(0);

        assertThat(adAt1.previousInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(adAt1.previousInsertionPosition(1)).isEqualTo(NOT_FOUND);
        assertThat(adAt1.previousInsertionPosition(2)).isEqualTo(1);
        assertThat(adAt1.previousInsertionPosition(3)).isEqualTo(1);

        assertThat(adsAt01.previousInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(adsAt01.previousInsertionPosition(1)).isEqualTo(0);
        assertThat(adsAt01.previousInsertionPosition(2)).isEqualTo(0);
        assertThat(adsAt01.previousInsertionPosition(3)).isEqualTo(0);

        assertThat(adsAt14.previousInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(adsAt14.previousInsertionPosition(1)).isEqualTo(NOT_FOUND);
        assertThat(adsAt14.previousInsertionPosition(2)).isEqualTo(1);
        assertThat(adsAt14.previousInsertionPosition(3)).isEqualTo(1);
        assertThat(adsAt14.previousInsertionPosition(4)).isEqualTo(3);
        assertThat(adsAt14.previousInsertionPosition(5)).isEqualTo(3);

        assertThat(adsRepeating.previousInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(adsRepeating.previousInsertionPosition(1)).isEqualTo(NOT_FOUND);
        assertThat(adsRepeating.previousInsertionPosition(2)).isEqualTo(NOT_FOUND);
        assertThat(adsRepeating.previousInsertionPosition(3)).isEqualTo(2);
        assertThat(adsRepeating.previousInsertionPosition(4)).isEqualTo(2);
        assertThat(adsRepeating.previousInsertionPosition(5)).isEqualTo(4);
        assertThat(adsRepeating.previousInsertionPosition(6)).isEqualTo(4);
        assertThat(adsRepeating.previousInsertionPosition(7)).isEqualTo(6);

        assertThat(adsAt15repeating.previousInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(adsAt15repeating.previousInsertionPosition(1)).isEqualTo(NOT_FOUND);
        assertThat(adsAt15repeating.previousInsertionPosition(2)).isEqualTo(1);
        assertThat(adsAt15repeating.previousInsertionPosition(3)).isEqualTo(1);
        assertThat(adsAt15repeating.previousInsertionPosition(4)).isEqualTo(1);
        assertThat(adsAt15repeating.previousInsertionPosition(5)).isEqualTo(4);
        assertThat(adsAt15repeating.previousInsertionPosition(6)).isEqualTo(4);
        assertThat(adsAt15repeating.previousInsertionPosition(7)).isEqualTo(6);
        assertThat(adsAt15repeating.previousInsertionPosition(8)).isEqualTo(6);
        assertThat(adsAt15repeating.previousInsertionPosition(9)).isEqualTo(8);

        assertThat(adsAt1234.previousInsertionPosition(0)).isEqualTo(NOT_FOUND);
        assertThat(adsAt1234.previousInsertionPosition(1)).isEqualTo(NOT_FOUND);
        assertThat(adsAt1234.previousInsertionPosition(2)).isEqualTo(1);
        assertThat(adsAt1234.previousInsertionPosition(3)).isEqualTo(1);
    }

    @Test
    public void initialState_isPlacedAd() {
        assertThat(adsAt15repeating.isPlacedAd(0)).isFalse();
        assertThat(adsAt15repeating.isPlacedAd(1)).isFalse();
        assertThat(adsAt15repeating.isPlacedAd(10)).isFalse();
        assertThat(adsAt15repeating.isPlacedAd(1000)).isFalse();

        assertThat(noAds.isPlacedAd(0)).isFalse();
        assertThat(noAds.isPlacedAd(1000)).isFalse();
    }

    @Test
    public void initialState_getOriginalPosition() {
        assertThat(adsAt15repeating.getOriginalPosition(0)).isEqualTo(0);
        assertThat(adsAt15repeating.getOriginalPosition(1)).isEqualTo(1);
        assertThat(adsAt15repeating.getOriginalPosition(10)).isEqualTo(10);
        assertThat(adsAt15repeating.getOriginalPosition(1000)).isEqualTo(1000);

        assertThat(noAds.getOriginalPosition(0)).isEqualTo(0);
        assertThat(noAds.getOriginalPosition(1000)).isEqualTo(1000);
    }

    @Test
    public void initialState_getOriginalCount() {
        assertThat(adsAt15repeating.getOriginalCount(0)).isEqualTo(0);
        assertThat(adsAt15repeating.getOriginalCount(1)).isEqualTo(1);
        assertThat(adsAt15repeating.getOriginalCount(10)).isEqualTo(10);
        assertThat(adsAt15repeating.getOriginalCount(1000)).isEqualTo(1000);

        assertThat(noAds.getOriginalCount(0)).isEqualTo(0);
        assertThat(noAds.getOriginalCount(1000)).isEqualTo(1000);
    }

    @Test
    public void initialState_getAdjustedPosition() {
        assertThat(adsAt15repeating.getAdjustedPosition(0)).isEqualTo(0);
        assertThat(adsAt15repeating.getAdjustedPosition(1)).isEqualTo(1);
        assertThat(adsAt15repeating.getAdjustedPosition(10)).isEqualTo(10);
        assertThat(adsAt15repeating.getAdjustedPosition(1000)).isEqualTo(1000);

        assertThat(noAds.getAdjustedPosition(0)).isEqualTo(0);
        assertThat(noAds.getAdjustedPosition(1000)).isEqualTo(1000);
    }

    @Test
    public void initialState_getAdjustedCount() {
        assertThat(adsAt15repeating.getAdjustedCount(0)).isEqualTo(0);
        assertThat(adsAt15repeating.getAdjustedCount(1)).isEqualTo(1);
        assertThat(adsAt15repeating.getAdjustedCount(10)).isEqualTo(10);
        assertThat(adsAt15repeating.getAdjustedCount(1000)).isEqualTo(1000);

        assertThat(noAds.getAdjustedCount(0)).isEqualTo(0);
        assertThat(noAds.getAdjustedCount(1000)).isEqualTo(1000);
    }

    @Test
    public void placeAds_inOrder_shouldUpdatePositions() {
        checkInsertionPositions(10, adsAt15repeating, 1, 4, 6, 8, 10);
        checkPlacedPositions(20, adsAt15repeating);

        adsAt15repeating.placeAd(1, mMockNativeAd);
        checkInsertionPositions(10, adsAt15repeating, 5, 7, 9);
        checkPlacedPositions(20, adsAt15repeating, 1);

        adsAt15repeating.placeAd(5, mMockNativeAd);
        checkInsertionPositions(10, adsAt15repeating, 8, 10);
        checkPlacedPositions(20, adsAt15repeating, 1, 5);

        adsAt15repeating.placeAd(8, mMockNativeAd);
        checkInsertionPositions(10, adsAt15repeating);
        checkPlacedPositions(20, adsAt15repeating, 1, 5, 8);
    }

    @Test
    public void placeAds_outOfOrder_shouldUpdatePositions() {
        checkInsertionPositions(10, adsAt15repeating, 1, 4, 6, 8, 10);
        checkPlacedPositions(20, adsAt15repeating);

        adsAt15repeating.placeAd(6, mMockNativeAd);
        checkInsertionPositions(10, adsAt15repeating, 1, 4, 9);
        checkPlacedPositions(20, adsAt15repeating, 6);

        adsAt15repeating.placeAd(4, mMockNativeAd);
        checkInsertionPositions(10, adsAt15repeating, 1, 10);
        checkPlacedPositions(20, adsAt15repeating, 4, 7);

        adsAt15repeating.placeAd(1, mMockNativeAd);
        checkInsertionPositions(10, adsAt15repeating);
        checkPlacedPositions(20, adsAt15repeating, 1, 5, 8);
    }

    @Test
    public void placedAds_getOriginalPositionAndCount() {
        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);

        assertThat(adsAt15repeating.getOriginalPosition(0)).isEqualTo(0);
        assertThat(adsAt15repeating.getOriginalPosition(1)).isEqualTo(NOT_FOUND);
        assertThat(adsAt15repeating.getOriginalPosition(2)).isEqualTo(1);
        assertThat(adsAt15repeating.getOriginalPosition(3)).isEqualTo(2);
        assertThat(adsAt15repeating.getOriginalPosition(4)).isEqualTo(3);
        assertThat(adsAt15repeating.getOriginalPosition(5)).isEqualTo(NOT_FOUND);
        assertThat(adsAt15repeating.getOriginalPosition(6)).isEqualTo(4);
        assertThat(adsAt15repeating.getOriginalPosition(7)).isEqualTo(5);
        assertThat(adsAt15repeating.getOriginalPosition(8)).isEqualTo(NOT_FOUND);
        assertThat(adsAt15repeating.getOriginalPosition(9)).isEqualTo(6);
        assertThat(adsAt15repeating.getOriginalPosition(10)).isEqualTo(7);

        assertThat(adsAt15repeating.getOriginalCount(0)).isEqualTo(0);
        assertThat(adsAt15repeating.getOriginalCount(10)).isEqualTo(7);
        assertThat(adsAt15repeating.getOriginalCount(20)).isEqualTo(17);
    }

    @Test
    public void placedAds_getAdjustedPositionAndCount() {
        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);

        assertThat(adsAt15repeating.getAdjustedPosition(0)).isEqualTo(0);
        // Ad here
        assertThat(adsAt15repeating.getAdjustedPosition(1)).isEqualTo(2);
        assertThat(adsAt15repeating.getAdjustedPosition(2)).isEqualTo(3);
        assertThat(adsAt15repeating.getAdjustedPosition(3)).isEqualTo(4);
        // Ad here
        assertThat(adsAt15repeating.getAdjustedPosition(4)).isEqualTo(6);
        assertThat(adsAt15repeating.getAdjustedPosition(5)).isEqualTo(7);
        // Ad here
        assertThat(adsAt15repeating.getAdjustedPosition(6)).isEqualTo(9);
        assertThat(adsAt15repeating.getAdjustedPosition(7)).isEqualTo(10);
        assertThat(adsAt15repeating.getAdjustedPosition(8)).isEqualTo(11);
        assertThat(adsAt15repeating.getAdjustedPosition(9)).isEqualTo(12);
        assertThat(adsAt15repeating.getAdjustedPosition(10)).isEqualTo(13);

        assertThat(adsAt15repeating.getAdjustedCount(0)).isEqualTo(0);
        assertThat(adsAt15repeating.getAdjustedCount(10)).isEqualTo(13);
        assertThat(adsAt15repeating.getAdjustedCount(20)).isEqualTo(23);
    }

    @Test
    public void placeAdsClumped_shouldPlaceAdsInOrder() {
        int nextPosition = adsAt1234.nextInsertionPosition(0);
        adsAt1234.placeAd(nextPosition, mMockNativeAd);

        nextPosition = adsAt1234.nextInsertionPosition(nextPosition);
        adsAt1234.placeAd(nextPosition, mMockNativeAd2);

        nextPosition = adsAt1234.nextInsertionPosition(nextPosition);
        adsAt1234.placeAd(nextPosition, mMockNativeAd3);

        nextPosition = adsAt1234.nextInsertionPosition(nextPosition);
        adsAt1234.placeAd(nextPosition, mMockNativeAd4);

        nextPosition = adsAt1234.nextInsertionPosition(nextPosition);
        assertThat(nextPosition).isEqualTo(NOT_FOUND);

        assertThat(adsAt1234.isPlacedAd(0)).isFalse();
        assertThat(adsAt1234.getPlacedAd(1)).isEqualTo(mMockNativeAd);
        assertThat(adsAt1234.getPlacedAd(2)).isEqualTo(mMockNativeAd2);
        assertThat(adsAt1234.getPlacedAd(3)).isEqualTo(mMockNativeAd3);
        assertThat(adsAt1234.getPlacedAd(4)).isEqualTo(mMockNativeAd4);
        assertThat(adsAt1234.isPlacedAd(5)).isFalse();
    }

    @Test
    public void placeAdsClumped_thenRemoveContentBeforeClumpedAds_shouldShiftAds() {
        adsAt1234.placeAd(1, mMockNativeAd);
        adsAt1234.placeAd(2, mMockNativeAd2);
        adsAt1234.placeAd(3, mMockNativeAd3);
        adsAt1234.placeAd(4, mMockNativeAd4);

        adsAt1234.removeItem(0);
        assertThat(adsAt1234.getPlacedAd(0)).isEqualTo(mMockNativeAd);
        assertThat(adsAt1234.getPlacedAd(1)).isEqualTo(mMockNativeAd2);
        assertThat(adsAt1234.getPlacedAd(2)).isEqualTo(mMockNativeAd3);
        assertThat(adsAt1234.getPlacedAd(3)).isEqualTo(mMockNativeAd4);
        assertThat(adsAt1234.isPlacedAd(4)).isFalse();

        adsAt1234.removeItem(0);
        assertThat(adsAt1234.getPlacedAd(0)).isEqualTo(mMockNativeAd);
        assertThat(adsAt1234.getPlacedAd(1)).isEqualTo(mMockNativeAd2);
        assertThat(adsAt1234.getPlacedAd(2)).isEqualTo(mMockNativeAd3);
        assertThat(adsAt1234.getPlacedAd(3)).isEqualTo(mMockNativeAd4);
        assertThat(adsAt1234.isPlacedAd(4)).isFalse();
    }

    @Test
    public void placeAdsClumped_thenRemoveContentAfterClumpedAds_shouldNotShiftAds() {
        adsAt1234.placeAd(1, mMockNativeAd);
        adsAt1234.placeAd(2, mMockNativeAd2);
        adsAt1234.placeAd(3, mMockNativeAd3);
        adsAt1234.placeAd(4, mMockNativeAd4);

        adsAt1234.removeItem(1);
        assertThat(adsAt1234.isPlacedAd(0)).isFalse();
        assertThat(adsAt1234.getPlacedAd(1)).isEqualTo(mMockNativeAd);
        assertThat(adsAt1234.getPlacedAd(2)).isEqualTo(mMockNativeAd2);
        assertThat(adsAt1234.getPlacedAd(3)).isEqualTo(mMockNativeAd3);
        assertThat(adsAt1234.getPlacedAd(4)).isEqualTo(mMockNativeAd4);
        assertThat(adsAt1234.isPlacedAd(5)).isFalse();

        adsAt1234.removeItem(2);
        assertThat(adsAt1234.isPlacedAd(0)).isFalse();
        assertThat(adsAt1234.getPlacedAd(1)).isEqualTo(mMockNativeAd);
        assertThat(adsAt1234.getPlacedAd(2)).isEqualTo(mMockNativeAd2);
        assertThat(adsAt1234.getPlacedAd(3)).isEqualTo(mMockNativeAd3);
        assertThat(adsAt1234.getPlacedAd(4)).isEqualTo(mMockNativeAd4);
        assertThat(adsAt1234.isPlacedAd(5)).isFalse();
    }

    @Test
    public void placeAdsClumped_thenInsertContentBeforeClumpedAds_shouldShiftAds() {
        adsAt1234.placeAd(1, mMockNativeAd);
        adsAt1234.placeAd(2, mMockNativeAd2);
        adsAt1234.placeAd(3, mMockNativeAd3);
        adsAt1234.placeAd(4, mMockNativeAd4);

        adsAt1234.insertItem(1);
        assertThat(adsAt1234.isPlacedAd(0)).isFalse();
        assertThat(adsAt1234.isPlacedAd(1)).isFalse();
        assertThat(adsAt1234.getPlacedAd(2)).isEqualTo(mMockNativeAd);
        assertThat(adsAt1234.getPlacedAd(3)).isEqualTo(mMockNativeAd2);
        assertThat(adsAt1234.getPlacedAd(4)).isEqualTo(mMockNativeAd3);
        assertThat(adsAt1234.getPlacedAd(5)).isEqualTo(mMockNativeAd4);
        assertThat(adsAt1234.isPlacedAd(6)).isFalse();

        adsAt1234.insertItem(0);
        assertThat(adsAt1234.isPlacedAd(0)).isFalse();
        assertThat(adsAt1234.isPlacedAd(1)).isFalse();
        assertThat(adsAt1234.isPlacedAd(2)).isFalse();
        assertThat(adsAt1234.getPlacedAd(3)).isEqualTo(mMockNativeAd);
        assertThat(adsAt1234.getPlacedAd(4)).isEqualTo(mMockNativeAd2);
        assertThat(adsAt1234.getPlacedAd(5)).isEqualTo(mMockNativeAd3);
        assertThat(adsAt1234.getPlacedAd(6)).isEqualTo(mMockNativeAd4);
        assertThat(adsAt1234.isPlacedAd(7)).isFalse();
    }

    @Test
    public void placeAdsClumped_thenInsertContentAfterClumpedAds_shouldNotShiftAds() {
        adsAt1234.placeAd(1, mMockNativeAd);
        adsAt1234.placeAd(2, mMockNativeAd2);
        adsAt1234.placeAd(3, mMockNativeAd3);
        adsAt1234.placeAd(4, mMockNativeAd4);

        adsAt1234.insertItem(2);
        assertThat(adsAt1234.isPlacedAd(0)).isFalse();
        assertThat(adsAt1234.getPlacedAd(1)).isEqualTo(mMockNativeAd);
        assertThat(adsAt1234.getPlacedAd(2)).isEqualTo(mMockNativeAd2);
        assertThat(adsAt1234.getPlacedAd(3)).isEqualTo(mMockNativeAd3);
        assertThat(adsAt1234.getPlacedAd(4)).isEqualTo(mMockNativeAd4);
        assertThat(adsAt1234.isPlacedAd(5)).isFalse();

        adsAt1234.removeItem(3);
        assertThat(adsAt1234.isPlacedAd(0)).isFalse();
        assertThat(adsAt1234.getPlacedAd(1)).isEqualTo(mMockNativeAd);
        assertThat(adsAt1234.getPlacedAd(2)).isEqualTo(mMockNativeAd2);
        assertThat(adsAt1234.getPlacedAd(3)).isEqualTo(mMockNativeAd3);
        assertThat(adsAt1234.getPlacedAd(4)).isEqualTo(mMockNativeAd4);
        assertThat(adsAt1234.isPlacedAd(5)).isFalse();
    }

    @Test
    public void placedAds_thenClearEmptyRange_doesNothing() {
        checkPlacedPositions(15, adsAt15repeating);
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);

        adsAt15repeating.clearAdsInRange(0, 0);
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);

        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);
        checkPlacedPositions(15, adsAt15repeating, 1, 5, 8);
        checkInsertionPositions(15, adsAt15repeating, 11, 13, 15);

        adsAt15repeating.clearAdsInRange(4, 4);
        checkPlacedPositions(15, adsAt15repeating, 1, 5, 8);
        checkInsertionPositions(15, adsAt15repeating, 11, 13, 15);
    }

    @Test
    public void placedAds_thenClearAll_shouldResetInsertionPositions() {
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);

        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);
        checkPlacedPositions(15, adsAt15repeating, 1, 5, 8);
        checkInsertionPositions(15, adsAt15repeating, 11, 13, 15);

        adsAt15repeating.clearAdsInRange(1, 10);
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);
    }

    @Test
    public void placedAds_thenClearRange_inOrder_shouldResetInsertionPositions() {
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);

        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);
        checkPlacedPositions(15, adsAt15repeating, 1, 5, 8);
        checkInsertionPositions(15, adsAt15repeating, 11, 13, 15);

        adsAt15repeating.clearAdsInRange(1, 2);
        checkPlacedPositions(15, adsAt15repeating, 4, 7);
        checkInsertionPositions(15, adsAt15repeating, 1, 10, 12, 14);

        adsAt15repeating.clearAdsInRange(4, 5);
        checkPlacedPositions(15, adsAt15repeating, 6);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 9, 11, 13, 15);

        adsAt15repeating.clearAdsInRange(6, 7);
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);
    }

    @Test
    public void placedAds_thenClearRange_descending_shouldResetInsertionPositions() {
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);

        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);
        checkPlacedPositions(15, adsAt15repeating, 1, 5, 8);
        checkInsertionPositions(15, adsAt15repeating, 11, 13, 15);

        adsAt15repeating.clearAdsInRange(8, 9);
        checkPlacedPositions(15, adsAt15repeating, 1, 5);
        checkInsertionPositions(15, adsAt15repeating, 8, 10, 12, 14);

        adsAt15repeating.clearAdsInRange(5, 6);
        checkPlacedPositions(15, adsAt15repeating, 1);
        checkInsertionPositions(15, adsAt15repeating, 5, 7, 9, 11, 13, 15);

        adsAt15repeating.clearAdsInRange(1, 2);
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);
    }

    @Test
    public void placedAds_thenClearRange_multiple_shouldResetInsertionPositions() {
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);

        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);
        checkPlacedPositions(15, adsAt15repeating, 1, 5, 8);
        checkInsertionPositions(15, adsAt15repeating, 11, 13, 15);

        adsAt15repeating.clearAdsInRange(1, 6);
        checkPlacedPositions(15, adsAt15repeating, 6);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 9, 11, 13, 15);

        adsAt15repeating.clearAdsInRange(5, 10);
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);
    }

    @Test
    public void placedAds_thenClearRange_descending_multiple_shouldResetInsertionPositions() {
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);

        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);
        checkPlacedPositions(15, adsAt15repeating, 1, 5, 8);
        checkInsertionPositions(15, adsAt15repeating, 11, 13, 15);

        adsAt15repeating.clearAdsInRange(5, 9);
        checkPlacedPositions(15, adsAt15repeating, 1);
        checkInsertionPositions(15, adsAt15repeating, 5, 7, 9, 11, 13, 15);

        adsAt15repeating.clearAdsInRange(1, 5);
        checkPlacedPositions(15, adsAt15repeating);
        checkInsertionPositions(15, adsAt15repeating, 1, 4, 6, 8, 10, 12, 14);
    }

    @Test
    public void placeAds_thenClear_shouldCallDestroy() {
        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd2);
        adsAt15repeating.placeAd(8, mMockNativeAd3);

        adsAt15repeating.clearAdsInRange(5, 10);
        verify(mMockNativeAd, never()).destroy();
        verify(mMockNativeAd2).destroy();
        verify(mMockNativeAd3).destroy();
    }

    @Test
    public void insertItems_afterPlacing() {
        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);

        adsAt15repeating.insertItem(1);
        adsAt15repeating.insertItem(4);
        adsAt15repeating.insertItem(12);

        checkPlacedPositions(20, adsAt15repeating, 2, 7, 10);
    }

    @Test
    public void insertItems_beforePlacing() {
        checkInsertionPositions(7, adsAt15repeating, 1, 4, 6);

        adsAt15repeating.insertItem(4);
        adsAt15repeating.insertItem(7);

        checkInsertionPositions(9, adsAt15repeating, 1, 5, 8);
    }

    @Test
    public void removeThenInsertItem_atZero_shouldBeAtZero() {
        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);

        adsAt15repeating.removeItem(0);
        checkPlacedPositions(20, adsAt15repeating, 0, 4, 7);
        checkInsertionPositions(16, adsAt15repeating, 10, 12, 14, 16);

        adsAt15repeating.insertItem(0);
        checkPlacedPositions(20, adsAt15repeating, 1, 5, 8);
        checkInsertionPositions(16, adsAt15repeating, 11, 13, 15);
    }

    @Test
    public void placeThenInsertThenPlace() {
        adsAt15repeating.placeAd(4, mMockNativeAd);
        adsAt15repeating.insertItem(4);
        adsAt15repeating.placeAd(1, mMockNativeAd);

        checkPlacedPositions(20, adsAt15repeating, 1, 6);
    }

    @Test
    public void removeItems_afterPlacing() {
        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);

        checkPlacedPositions(20, adsAt15repeating, 1, 5, 8);

        adsAt15repeating.removeItem(3);
        adsAt15repeating.removeItem(5);

        checkPlacedPositions(20, adsAt15repeating, 1, 4, 7);

        // Check the adjusted positions.
        assertThat(adsAt15repeating.getAdjustedPosition(0)).isEqualTo(0);
        assertThat(adsAt15repeating.getAdjustedPosition(1)).isEqualTo(2);
        assertThat(adsAt15repeating.getAdjustedPosition(2)).isEqualTo(3);
        assertThat(adsAt15repeating.getAdjustedPosition(3)).isEqualTo(5);
        assertThat(adsAt15repeating.getAdjustedPosition(4)).isEqualTo(6);
        assertThat(adsAt15repeating.getAdjustedPosition(5)).isEqualTo(8);
    }

    @Test
    public void removeItemsBetweenAds_thenInsert_shouldClumpAds() {
        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);

        adsAt15repeating.removeItem(4);
        adsAt15repeating.removeItem(4);

        checkPlacedPositions(20, adsAt15repeating, 1, 5, 6);

        adsAt15repeating.insertItem(5);

        checkPlacedPositions(20, adsAt15repeating, 1, 5, 6);

        adsAt15repeating.insertItem(4);

        checkPlacedPositions(20, adsAt15repeating, 1, 6, 7);
    }

    @Test
    public void removeItems_beforePlacing() {
        checkInsertionPositions(7, adsAt15repeating, 1, 4, 6);

        adsAt15repeating.removeItem(4);

        // Check insertion positions.
        checkInsertionPositions(7, adsAt15repeating, 1, 4, 5, 7);
    }

    @Test
    public void removeItem_withClumpedAdsBeforeIt_shouldCorrectlyRemoveItem() throws Exception {
        adsRepeating.placeAd(2, mMockNativeAd);
        adsRepeating.placeAd(5, mMockNativeAd);
        adsRepeating.placeAd(8, mMockNativeAd);

        checkPlacedPositions(20, adsRepeating, 2, 5, 8);
        assertThat(adsRepeating.getAdjustedCount(7)).isEqualTo(10);

        // Removing from the head will cause ads to pile up
        adsRepeating.removeItem(0);
        adsRepeating.removeItem(0);
        adsRepeating.removeItem(0);
        adsRepeating.removeItem(0);
        adsRepeating.removeItem(0);
        adsRepeating.removeItem(0);

        checkPlacedPositions(20, adsRepeating, 0, 1, 2);

        adsRepeating.removeItem(0);

        checkPlacedPositions(20, adsRepeating, 0, 1, 2);
    }

    @Test
    public void removeItems_afterClumpedAds_shouldStayClumped() {
        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);

        adsAt15repeating.removeItem(4);
        adsAt15repeating.removeItem(4);

        checkPlacedPositions(20, adsAt15repeating, 1, 5, 6);

        // Shouldn't move any ads.
        adsAt15repeating.removeItem(4);

        checkPlacedPositions(20, adsAt15repeating, 1, 5, 6);

        adsAt15repeating.removeItem(3);

        checkPlacedPositions(20, adsAt15repeating, 1, 4, 5);
    }

    @Test
    public void moveItems_afterPlacing() {
        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd);
        adsAt15repeating.placeAd(8, mMockNativeAd);

        assertThat(adsAt15repeating.getAdjustedPosition(4)).isEqualTo(6);
        assertThat(adsAt15repeating.getAdjustedPosition(5)).isEqualTo(7);

        adsAt15repeating.moveItem(4, 2);

        checkPlacedPositions(20, adsAt15repeating, 1, 6, 8);

        assertThat(adsAt15repeating.getAdjustedPosition(4)).isEqualTo(5);
        assertThat(adsAt15repeating.getAdjustedPosition(5)).isEqualTo(7);
    }

    @Test
    public void clearAll_shouldCallDestroyOnAdData_shouldResetPositions() {
        adsAt15repeating.placeAd(1, mMockNativeAd);
        adsAt15repeating.placeAd(5, mMockNativeAd2);
        adsAt15repeating.placeAd(8, mMockNativeAd3);

        adsAt15repeating.clearAds();

        verify(mMockNativeAd).destroy();
        verify(mMockNativeAd2).destroy();
        verify(mMockNativeAd3).destroy();

        // Should reset to original positions
        checkInsertionPositions(10, adsAt15repeating, 1, 4, 6, 8, 10);
        checkPlacedPositions(20, adsAt15repeating);
    }

    void checkInsertionPositions(int maxValue, PlacementData placementData, Integer... positions) {
        List<Integer> expected = Arrays.asList(positions);
        List<Integer> actual = new ArrayList<Integer>();
        for (int i = 0; i <= maxValue; i++) {
            if (placementData.shouldPlaceAd(i)) {
                actual.add(i);
            }
        }

        assertThat(actual).isEqualTo(expected);
    }

    void checkPlacedPositions(int maxValue, PlacementData placementData, Integer... positions) {
        List<Integer> expected = Arrays.asList(positions);
        List<Integer> actual = new ArrayList<Integer>();
        for (int i = 0; i < maxValue; i++) {
            if (placementData.isPlacedAd(i)) {
                actual.add(i);
                assertThat(placementData.getPlacedAd(i)).isEqualTo(mMockNativeAd);
            } else {
                assertThat(placementData.getPlacedAd(i)).isNull();
            }
        }

        assertThat(actual).isEqualTo(expected);

        // Also check getPlacedAdPositions
        List<Integer> actualFromAdPositions = new ArrayList<Integer>();
        for (Integer position : placementData.getPlacedAdPositions()) {
            actualFromAdPositions.add(position);
        }
        assertThat(actualFromAdPositions).isEqualTo(expected);
    }
}
