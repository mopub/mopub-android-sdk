package com.mopub.common.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class ViewsTest {
    private Context context;
    private View subject;
    private RelativeLayout parent;

    @Before
    public void setup() {
        context = new Activity();
        subject = new View(context);
        parent = new RelativeLayout(context);
    }

    @Test
    public void removeFromParent_shouldRemoveViewFromParent() throws Exception {
        assertThat(parent.getChildCount()).isEqualTo(0);

        parent.addView(subject);
        assertThat(parent.getChildCount()).isEqualTo(1);
        assertThat(subject.getParent()).isEqualTo(parent);

        Views.removeFromParent(subject);

        assertThat(parent.getChildCount()).isEqualTo(0);
        assertThat(subject.getParent()).isNull();
    }

    @Test
    public void removeFromParent_withMultipleChildren_shouldRemoveCorrectChild() throws Exception {
        parent.addView(new TextView(context));

        assertThat(parent.getChildCount()).isEqualTo(1);

        parent.addView(subject);

        assertThat(parent.getChildCount()).isEqualTo(2);

        Views.removeFromParent(subject);
        assertThat(parent.getChildCount()).isEqualTo(1);

        assertThat(parent.getChildAt(0)).isInstanceOf(TextView.class);
    }

    @Test
    public void removeFromParent_whenViewIsNull_shouldPass() throws Exception {
        Views.removeFromParent(null);

        // pass
    }

    @Test
    public void removeFromParent_whenViewsParentIsNull_shouldPass() throws Exception {
        assertThat(subject.getParent()).isNull();

        Views.removeFromParent(subject);

        // pass
    }
}
