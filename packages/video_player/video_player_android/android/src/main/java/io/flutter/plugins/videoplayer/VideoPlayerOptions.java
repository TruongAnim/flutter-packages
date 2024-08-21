// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import java.util.Map;

class VideoPlayerOptions {
    public boolean mixWithOthers;
    public String cacheKey;
    public boolean useCache;
    public int preCacheSize;
    public int maxCacheSize;
    public int maxCacheFileSize;
    public int minBufferMs;
    public int maxBufferMs;
    public int bufferForPlaybackMs;
    public int bufferForPlaybackAfterRebufferMs;

    public static int parseIntWithFallback(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean parseBooleanWithFallback(String value, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setHlsCacheConfig(Map<String, String> config) {
        String key = config.get("cacheKey");
        cacheKey = (key != null) ? key : "default_key";
        preCacheSize = parseIntWithFallback(config.get("preCacheSize"), 10 * 1024 * 1024);
        maxCacheSize = parseIntWithFallback(config.get("maxCacheSize"), 1024 * 1024 * 1024);
        maxCacheFileSize = parseIntWithFallback(config.get("maxCacheFileSize"), 10 * 1024 * 1024);
        useCache = parseBooleanWithFallback(config.get("useCache"), false);
    }

    public void setHlsBufferConfig(Map<String, String> config) {
        minBufferMs = parseIntWithFallback(config.get("minBufferMs"), 5000);
        maxBufferMs = parseIntWithFallback(config.get("maxBufferMs"), 10000);
        bufferForPlaybackMs = parseIntWithFallback(config.get("bufferForPlaybackMs"), 3000);
        bufferForPlaybackAfterRebufferMs = parseIntWithFallback(config.get("bufferForPlaybackAfterRebufferMs"), 3000);
    }
}
