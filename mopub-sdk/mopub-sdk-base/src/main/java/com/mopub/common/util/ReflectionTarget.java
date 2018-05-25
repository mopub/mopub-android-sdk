package com.mopub.common.util;

/**
 * Methods that are accessed via reflection should be annotated with this so proguard does not
 * obfuscate them.
 */
public @interface ReflectionTarget { }
