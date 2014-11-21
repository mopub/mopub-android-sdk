package com.mopub.mobileads;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class TaskTrackerTest {

    private TaskTracker taskTracker;

    @Before
    public void setUp() throws Exception {
        taskTracker = new TaskTracker();
    }

    @Test
    public void newTaskStarted_shouldIncrementIdsFromNegativeOne() throws Exception {
        assertThat(taskTracker.getCurrentTaskId()).isEqualTo(-1);

        taskTracker.newTaskStarted();

        assertThat(taskTracker.getCurrentTaskId()).isEqualTo(0);
    }

    @Test
    public void isMostCurrentTask_onFirstTask_whenSecondTaskIsCompleted_shouldBeFalse() throws Exception {
        taskTracker.newTaskStarted();
        taskTracker.newTaskStarted();
        taskTracker.markTaskCompleted(taskTracker.getCurrentTaskId());

        assertThat(taskTracker.isMostCurrentTask(0)).isFalse();
    }

    @Test
    public void isMostCurrentTask_onFirstTask_whenSecondTaskIsNotCompleted_shouldBeTrue() throws Exception {
        taskTracker.newTaskStarted();
        taskTracker.newTaskStarted();

        assertThat(taskTracker.isMostCurrentTask(0)).isTrue();
    }

    @Test
    public void mostCurrentTaskIsLastCompletedTaskOrLater() throws Exception {
        taskTracker.newTaskStarted();
        taskTracker.newTaskStarted();
        taskTracker.newTaskStarted();
        taskTracker.markTaskCompleted(1);

        assertThat(taskTracker.isMostCurrentTask(0)).isFalse();
        assertThat(taskTracker.isMostCurrentTask(1)).isTrue();
        assertThat(taskTracker.isMostCurrentTask(2)).isTrue();
    }

    @Test
    public void markTaskCompleted_shouldKeepTrackOfMostCurrentTaskRegardlessOfCompletionOrder() throws Exception {
        taskTracker.newTaskStarted();
        taskTracker.newTaskStarted();
        taskTracker.newTaskStarted();
        taskTracker.markTaskCompleted(1);
        taskTracker.markTaskCompleted(0);

        assertThat(taskTracker.isMostCurrentTask(0)).isFalse();
        assertThat(taskTracker.isMostCurrentTask(1)).isTrue();
    }
}
