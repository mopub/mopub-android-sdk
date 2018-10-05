// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

/**
 * Methods that are accessed via reflection should be annotated with this so proguard does not
 * obfuscate them.
 */
public @interface ReflectionTarget { }
