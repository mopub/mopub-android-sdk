// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util.test.support;

import android.support.annotation.NonNull;

import com.mopub.common.util.Reflection;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

import static org.robolectric.shadow.api.Shadow.directlyOn;


@Implements(Reflection.class)
public class ShadowReflection {
    private static boolean sNextClassNotFound;

    public static void reset() {
        sNextClassNotFound = false;
    }

    @Implementation
    public static boolean classFound(@NonNull final String className) {
        if (sNextClassNotFound) {
            sNextClassNotFound = false;
            return false;
        }

        return directlyOn(Reflection.class, "classFound",
                new ReflectionHelpers.ClassParameter<>(String.class, className));
    }

    public static void setNextClassNotFound(final boolean nextNotFound) {
        sNextClassNotFound = nextNotFound;
    }
}
