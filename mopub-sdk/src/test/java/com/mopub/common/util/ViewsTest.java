package com.mopub.common.util;

import android.app.Activity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class ViewsTest {
    private Activity activity;
    private View frameLayout;
    private RelativeLayout relativeLayout;
    private View view;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();

        // Expected view hierarchy:
        // - FrameLayout
        // -- RelativeLayout
        // --- View
        frameLayout = activity.findViewById(android.R.id.content);
        relativeLayout = new RelativeLayout(activity);
        view = new View(activity);

        activity.setContentView(relativeLayout);
    }

    @Test
    public void removeFromParent_shouldRemoveViewFromParent() throws Exception {
        assertThat(relativeLayout.getChildCount()).isEqualTo(0);

        relativeLayout.addView(view);
        assertThat(relativeLayout.getChildCount()).isEqualTo(1);
        assertThat(view.getParent()).isEqualTo(relativeLayout);

        Views.removeFromParent(view);

        assertThat(relativeLayout.getChildCount()).isEqualTo(0);
        assertThat(view.getParent()).isNull();
    }

    @Test
    public void removeFromParent_withMultipleChildren_shouldRemoveCorrectChild() throws Exception {
        relativeLayout.addView(new TextView(activity));

        assertThat(relativeLayout.getChildCount()).isEqualTo(1);

        relativeLayout.addView(view);

        assertThat(relativeLayout.getChildCount()).isEqualTo(2);

        Views.removeFromParent(view);
        assertThat(relativeLayout.getChildCount()).isEqualTo(1);

        assertThat(relativeLayout.getChildAt(0)).isInstanceOf(TextView.class);
    }

    @Test
    public void removeFromParent_whenViewIsNull_shouldNotThrowException() throws Exception {
        Views.removeFromParent(null);

        // pass
    }

    @Test
    public void removeFromParent_whenViewsParentIsNull_shouldNotThrowException() throws Exception {
        assertThat(view.getParent()).isNull();

        Views.removeFromParent(view);

        // pass
    }

    @Test
    public void getTopmostView_withActivityContext_shouldReturnRootView() {
        relativeLayout.addView(view);

        View rootView = Views.getTopmostView(activity, view);

        assertThat(rootView).isEqualTo(frameLayout);
    }

    @Test
    public void getTopmostView_withNonActivityContext_shouldReturnRootView() {
        relativeLayout.addView(view);

        View rootView = Views.getTopmostView(activity.getApplicationContext(), view);

        assertThat(rootView).isEqualTo(frameLayout);
    }

    @Test
    public void getTopmostView_withNonActivityContext_withUnattachedView_shouldReturnView() {
        // don't add "view" to the view hierarchy

        View rootView = Views.getTopmostView(activity.getApplicationContext(), view);

        assertThat(rootView).isEqualTo(view);
    }

    @Test
    public void getTopmostView_withNonActivityContext_withNullView_shouldReturnNull() {
        relativeLayout.addView(view);

        View rootView = Views.getTopmostView(activity.getApplicationContext(), null);

        assertThat(rootView).isNull();
    }

    @Test
    public void getTopmostView_withNullArguments_shouldReturnNull() {
        relativeLayout.addView(view);

        View rootView = Views.getTopmostView(null, null);

        assertThat(rootView).isNull();
    }
}
