package com.mopub.mobileads.factories;

import android.content.Context;

import com.mopub.mobileads.AdConfiguration;
import com.mopub.mobileads.MraidView;
import com.mopub.mobileads.MraidView.NativeCloseButtonStyle;

public class MraidViewFactory {
    protected static MraidViewFactory instance = new MraidViewFactory();

    @Deprecated // for testing
    public static void setInstance(MraidViewFactory factory) {
        instance = factory;
    }

    public static MraidView create(Context context, AdConfiguration adConfiguration) {
        return instance.internalCreate(context, adConfiguration);
    }

    public static MraidView create(
            Context context,
            AdConfiguration adConfiguration,
            MraidView.ExpansionStyle expansionStyle,
            NativeCloseButtonStyle buttonStyle,
            MraidView.PlacementType placementType) {
        return instance.internalCreate(context, adConfiguration, expansionStyle, buttonStyle, placementType);
    }

    protected MraidView internalCreate(Context context, AdConfiguration adConfiguration) {
        return new MraidView(context, adConfiguration);
    }

    protected MraidView internalCreate(
            Context context,
            AdConfiguration adConfiguration,
            MraidView.ExpansionStyle expansionStyle,
            NativeCloseButtonStyle buttonStyle,
            MraidView.PlacementType placementType) {
        return new MraidView(context, adConfiguration, expansionStyle, buttonStyle, placementType);
    }
}
