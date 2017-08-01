package com.mopub.mobileads.resource;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

public class DrawableConstants {

    public static final int TRANSPARENT_GRAY = 0x88000000;

    public static class ProgressBar {
        public static final int HEIGHT_DIPS = 4;
        public static final int NUGGET_WIDTH_DIPS = 4;

        public static final int BACKGROUND_COLOR = Color.WHITE;
        public static final int BACKGROUND_ALPHA = 128;
        public static final Paint.Style BACKGROUND_STYLE = Paint.Style.FILL;

        public static final int PROGRESS_COLOR = Color.parseColor("#FFCC4D");
        public static final int PROGRESS_ALPHA = 255;
        public static final Paint.Style PROGRESS_STYLE = Paint.Style.FILL;
    }

    public static class RadialCountdown {
        public static final int SIDE_LENGTH_DIPS = 45;
        public static final int TOP_MARGIN_DIPS = 16;
        public static final int RIGHT_MARGIN_DIPS = 16;
        public static final int PADDING_DIPS = 5;

        public static final int CIRCLE_STROKE_WIDTH_DIPS = 3;
        public static final float TEXT_SIZE_SP = 18f;
        public static final float START_ANGLE = -90f;

        public static final int BACKGROUND_COLOR = Color.WHITE;
        public static final int BACKGROUND_ALPHA = 128;
        public static final Paint.Style BACKGROUND_STYLE = Paint.Style.STROKE;

        public static final int PROGRESS_COLOR = Color.WHITE;
        public static final int PROGRESS_ALPHA = 255;
        public static final Paint.Style PROGRESS_STYLE = Paint.Style.STROKE;

        public static final int TEXT_COLOR = Color.WHITE;
        public static final Paint.Align TEXT_ALIGN = Paint.Align.CENTER;
    }

    public static class CtaButton {
        public static final int WIDTH_DIPS = 150;
        public static final int HEIGHT_DIPS = 38;
        public static final int MARGIN_DIPS = 16;
        public static final int CORNER_RADIUS_DIPS = 6;
        public static final int OUTLINE_STROKE_WIDTH_DIPS = 2;
        public static final float TEXT_SIZE_SP = 15f;

        public static final int BACKGROUND_COLOR = Color.BLACK;
        public static final int BACKGROUND_ALPHA = 51;
        public static final Paint.Style BACKGROUND_STYLE = Paint.Style.FILL;

        public static final int OUTLINE_COLOR = Color.WHITE;
        public static final int OUTLINE_ALPHA = 51;
        public static final Paint.Style OUTLINE_STYLE = Paint.Style.STROKE;

        public static final String DEFAULT_CTA_TEXT = "Learn More";
        public static final Typeface TEXT_TYPEFACE = Typeface.create("Helvetica", Typeface.NORMAL);
        public static final int TEXT_COLOR = Color.WHITE;
        public static final Paint.Align TEXT_ALIGN = Paint.Align.CENTER;
    }
    
    public static class CloseButton {
        public static final int WIDGET_HEIGHT_DIPS = 56;
        public static final int EDGE_PADDING = 6;
        public static final int IMAGE_PADDING_DIPS = 15;
        public static final int TEXT_RIGHT_MARGIN_DIPS = 0;
        public static final float TEXT_SIZE_SP = 20f;

        public static final int STROKE_COLOR = Color.WHITE;
        public static final float STROKE_WIDTH = 8f;
        public static final Paint.Cap STROKE_CAP = Paint.Cap.ROUND;

        public static final String DEFAULT_CLOSE_BUTTON_TEXT = "";
        public static final Typeface TEXT_TYPEFACE = Typeface.create("Helvetica", Typeface.NORMAL);
        public static final int TEXT_COLOR = Color.WHITE;
    }
    
    public static class GradientStrip {
        public static final int GRADIENT_STRIP_HEIGHT_DIPS = 72;
        public static final int START_COLOR = Color.argb(102, 0, 0, 0);
        public static final int END_COLOR = Color.argb(0, 0, 0, 0);
    }

    public static class BlurredLastVideoFrame {
        public static final int ALPHA = 100;
    }

    public static class PrivacyInfoIcon {
        public static final int LEFT_MARGIN_DIPS = 12;
        public static final int TOP_MARGIN_DIPS = 12;
    }

    public static class SocialActions {
        public static final int ADS_BY_LEFT_MARGIN_DIPS = 6;
        public static final int SOCIAL_ACTIONS_LEFT_MARGIN_DIPS = 16;
    }
}
