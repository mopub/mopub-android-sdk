package com.mopub.common.util.test.support;

import com.mopub.common.util.Reflection;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

import static org.robolectric.internal.Shadow.directlyOn;

@Implements(Reflection.class)
public class ShadowReflection {
    private static boolean sNextClassNotFound;

    public static void reset() {
        sNextClassNotFound = false;
    }

    @Implementation
    public static boolean classFound(final String className) throws Exception {
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