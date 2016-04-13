package com.mopub.mobileads;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.GestureUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static com.mopub.mobileads.AdAlertGestureListener.ZigZagState.FAILED;
import static com.mopub.mobileads.AdAlertGestureListener.ZigZagState.FINISHED;
import static com.mopub.mobileads.AdAlertGestureListener.ZigZagState.GOING_LEFT;
import static com.mopub.mobileads.AdAlertGestureListener.ZigZagState.GOING_RIGHT;
import static com.mopub.mobileads.AdAlertGestureListener.ZigZagState.UNSET;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.stub;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdAlertGestureListenerTest {
    @Mock
    private View mockView;
    @Mock
    private AdReport mockAdReport;
    private AdAlertGestureListener subject;
    private float threshold;
    private static final float INITIAL_X = 20;
    private static final float INITIAL_Y = 50;
    private float savedX;
    private float savedY;
    private MotionEvent actionDown;
    @Mock
    private Context mockContext;

    @Before
    public void setup() {
        stub(mockView.getContext()).toReturn(mockContext);

        subject = new AdAlertGestureListener(mockView, mockAdReport);

        savedX = INITIAL_X;
        savedY = INITIAL_Y;
        actionDown = GestureUtils.createActionDown(INITIAL_X, INITIAL_Y);

        threshold = subject.getMinimumDipsInZigZag();
    }

    @Test
    public void constructor_shouldSetThresholdToOneThirdOfViewsWidth() throws Exception {
        stub(mockView.getWidth()).toReturn(150);
        subject = new AdAlertGestureListener(mockView, mockAdReport);
        assertThat(subject.getMinimumDipsInZigZag()).isEqualTo(50);
    }

    @Test
    public void constructor_whenViewWidthIsWiderThanThreeTimesMaxThreshold_shouldSetThresholdTo100() throws Exception {
        stub(mockView.getWidth()).toReturn(500);
        subject = new AdAlertGestureListener(mockView, mockAdReport);
        assertThat(subject.getMinimumDipsInZigZag()).isEqualTo(100);
    }

    @Test
    public void constructor_whenViewWidthIs0_shouldSetThresholdTo100() throws Exception {
        stub(mockView.getWidth()).toReturn(0);
        subject = new AdAlertGestureListener(mockView, mockAdReport);
        assertThat(subject.getMinimumDipsInZigZag()).isEqualTo(100);
    }

    @Test
    public void shouldDefaultToNoZigZagsCompletedAnd100DipsInZigZag() throws Exception {
        assertThat(subject.getNumberOfZigzags()).isEqualTo(0);
        assertThat(subject.getMinimumDipsInZigZag()).isEqualTo(100);
        assertZigZagState(UNSET);
    }

    @Test
    public void onScroll_withInitialRightMovement_shouldSetStateToGoingRight() throws Exception {
        simulateScroll(INITIAL_X);
        assertZigZagState(UNSET);

        simulateScroll(INITIAL_X + 1);
        assertZigZagState(GOING_RIGHT);
    }

    @Test
    public void onScroll_withInitialLeftThenRightMovement_whenRightMovementPassesInitialX_shouldSetStateToGoingRight() throws Exception {
        simulateScroll(INITIAL_X);
        assertZigZagState(UNSET);

        simulateScroll(INITIAL_X - 10);
        assertZigZagState(UNSET);

        simulateScroll(INITIAL_X - 5);
        assertZigZagState(UNSET);

        simulateScroll(INITIAL_X + 1);
        assertZigZagState(GOING_RIGHT);
    }

    @Test
    public void onScroll_withInitialLeftThenRightMovement_BeforeZigCompletes_MovesOutOfUpperYBounds_shouldSetStateToFailed() throws Exception {
        simulateScroll(INITIAL_X);
        assertZigZagState(UNSET);

        simulateScroll(INITIAL_X + 10);
        assertZigZagState(GOING_RIGHT);

        simulateScroll(savedX, INITIAL_Y + 49);
        assertZigZagState(GOING_RIGHT);
        assertThat(subject.getNumberOfZigzags()).isEqualTo(0);

        simulateScroll(savedX, INITIAL_Y + 102);
        assertZigZagState(FAILED);
    }

    @Test
    public void onScroll_withInitialLeftThenRightMovement_BeforeZigCompletes_MovesOutOfLowerYBounds_shouldSetStateToFailed() throws Exception {
        simulateScroll(INITIAL_X);
        assertZigZagState(UNSET);

        simulateScroll(INITIAL_X + 10);
        assertZigZagState(GOING_RIGHT);

        simulateScroll(savedX, INITIAL_Y - 49);
        assertZigZagState(GOING_RIGHT);
        assertThat(subject.getNumberOfZigzags()).isEqualTo(0);

        simulateScroll(savedX, INITIAL_Y - 101);
        assertZigZagState(FAILED);
    }

    @Test
    public void onScroll_withZigZagZigZagZigZagZig_BeforeNextZagCompletes_MovesOutOfUpperYBounds_shouldSetStateToFailed() throws Exception {
        performZigZag();
        performZigZag();
        performZigZag();
        performZig();

        simulateScroll(savedX, INITIAL_Y + 49);
        assertZigZagState(GOING_RIGHT);
        assertThat(subject.getNumberOfZigzags()).isEqualTo(3);

        simulateScroll(savedX, INITIAL_Y + 101);
        assertZigZagState(FAILED);
    }

    @Test
    public void onScroll_withZigZagZigZagZigZagZig_BeforeNextZagCompletes_MovesOutOfLowerYBounds_shouldSetStateToFailed() throws Exception {
        performZigZag();
        performZigZag();
        performZigZag();
        performZig();

        simulateScroll(savedX - 1, INITIAL_Y - 49);
        assertZigZagState(GOING_LEFT);
        assertThat(subject.getNumberOfZigzags()).isEqualTo(3);

        simulateScroll(savedX, INITIAL_Y - 101);
        assertZigZagState(FAILED);
    }

    @Test
    public void onScroll_withStateFailed_withAnyMotion_shouldStayFailed() throws Exception {
        simulateScroll(savedX, INITIAL_Y + 49);
        simulateScroll(savedX, INITIAL_Y + 101);
        assertZigZagState(FAILED);

        performZigZag();
        assertZigZagState(FAILED);

        performZig();
        assertZigZagState(FAILED);

        performZag();
        assertZigZagState(FAILED);
    }

    @Test
    public void onScroll_whenGoingRightPastThreshold_thenGoingLeft_shouldSetStateToGoingLeft() throws Exception {
        simulateScroll(INITIAL_X);
        simulateScroll(INITIAL_X + threshold);
        simulateScroll(INITIAL_X + threshold + 2);
        assertZigZagState(GOING_RIGHT);

        simulateScroll(INITIAL_X + threshold - 1);
        assertZigZagState(GOING_LEFT);
    }

    @Test
    public void onScroll_withCompleteZigZag_shouldSetStateToGoingLeftAndNumberOfZigZagsIs1() throws Exception {
        performZig();
        assertZigZagState(GOING_RIGHT);
        assertThat(subject.getNumberOfZigzags()).isEqualTo(0);

        performZag();
        assertZigZagState(GOING_LEFT);
        assertThat(subject.getNumberOfZigzags()).isEqualTo(1);
    }

    @Test
    public void onScroll_withZig_butBeforeZagIsComplete_shouldKeepStateGoingLeftButNotIncrementZigZags() throws Exception {
        performZig();
        assertZigZagState(GOING_RIGHT);

        simulateScroll(INITIAL_X + threshold - 5);
        assertZigZagState(GOING_LEFT);

        // we turn back prematurely
        simulateScroll(INITIAL_X + threshold);
        assertZigZagState(GOING_LEFT);
        assertThat(subject.getNumberOfZigzags()).isEqualTo(0);
    }

    @Test
    public void onScroll_withZigZagZig_butTurningLeftBeforeSecondZigIsComplete_shouldKeepStateGoingRightButNotReachRightThreshold() throws Exception {
        performZigZag();
        assertZigZagState(GOING_LEFT);

        simulateScroll(INITIAL_X + 10);
        assertZigZagState(GOING_RIGHT);
        simulateScroll(INITIAL_X);
        assertZigZagState(GOING_RIGHT);
    }

    @Test
    public void checkReportAd_withZigZagThreeTimes_shouldNotAlertFlagAndSetStateToUnset() throws Exception {
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(1);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(2);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(3);

        subject.finishGestureDetection();

        assertThat(subject.getAdAlertReporter()).isNull();
        assertZigZagState(UNSET);
    }

    @Test
    public void onScroll_withFourZigZags_shouldSetStateToFinished() throws Exception {
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(1);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(2);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(3);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(4);

        assertZigZagState(FINISHED);
    }

    @Test
    public void checkReportAd_withZigZagFourTimes_shouldAlertFlagAndSetStateToUnset() throws Exception {
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(1);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(2);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(3);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(4);

        subject.finishGestureDetection();

        assertThat(subject.getAdAlertReporter()).isNotNull();
        assertZigZagState(UNSET);
    }

    @Test
    public void checkReportAd_withZigZagTenTimes_shouldAlertFlag_andNotIncrementZigZagCount_andSetStateToFinished() throws Exception {
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(1);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(2);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(3);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(4);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(4);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(4);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(4);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(4);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(4);
        performZigZag();
        assertThat(subject.getNumberOfZigzags()).isEqualTo(4);

        subject.finishGestureDetection();

        assertThat(subject.getAdAlertReporter()).isNotNull();
        assertZigZagState(UNSET);
    }

    @Test
    public void reset_shouldResetStateAndZigZagCount() throws Exception {
        performZigZag();
        performZig();

        subject.reset();

        assertThat(subject.getNumberOfZigzags()).isEqualTo(0);
        assertZigZagState(UNSET);
    }

    private void simulateScroll(float endX) {
        simulateScroll(endX, savedY);
    }

    private void simulateScroll(float endX, float endY) {
        final float stepSizeX = Math.signum(endX - savedX) * 0.5f;
        final float stepSizeY = Math.signum(endY - savedY) * 0.5f;
        float x = savedX;
        float y = savedY;

        if (areEqual(savedX, endX)) {
            subject.onScroll(actionDown, GestureUtils.createActionMove(x, y), 0, 0);
        } else if (savedX < endX) {
            for (; x < endX; x += stepSizeX) {
                subject.onScroll(actionDown, GestureUtils.createActionMove(x, y), 0, 0);
            }
        } else if (savedX > endX) {
            for (; x > endX; x += stepSizeX) {
                subject.onScroll(actionDown, GestureUtils.createActionMove(x, y), 0, 0);
            }
        }

        if (areEqual(savedY, endY)) {
            subject.onScroll(actionDown, GestureUtils.createActionMove(x, y), 0, 0);
        } else if (savedY < endY) {
            for (; y < endY; y += stepSizeY) {
                subject.onScroll(actionDown, GestureUtils.createActionMove(x, y), 0, 0);
            }
        } else if (savedY > endY) {
            for (; y > endY; y += stepSizeY) {
                subject.onScroll(actionDown, GestureUtils.createActionMove(x, y), 0, 0);
            }
        }

        savedX = endX;
        savedY = endY;
    }

    private void performZig() {
        simulateScroll(savedX + threshold + 1);
    }

    private void performZag() {
        simulateScroll(savedX - threshold - 1);
    }

    private void performZigZag() {
        performZig();
        performZag();
    }

    private boolean areEqual(float a, float b) {
        return (Math.abs(a - b) < 0.01f);
    }

    private void assertZigZagState(AdAlertGestureListener.ZigZagState state) {
        assertThat(subject.getCurrentZigZagState()).isEqualTo(state);
    }
}
