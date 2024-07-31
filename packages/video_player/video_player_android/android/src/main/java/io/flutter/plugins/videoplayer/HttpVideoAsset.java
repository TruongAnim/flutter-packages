// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import java.util.Map;

final class HttpVideoAsset extends VideoAsset {
  private static final String DEFAULT_USER_AGENT = "ExoPlayer";
  private static final String HEADER_USER_AGENT = "User-Agent";

  @NonNull private final StreamingFormat streamingFormat;
  @NonNull private final Map<String, String> httpHeaders;

  HttpVideoAsset(
      @Nullable String assetUrl,
      @NonNull StreamingFormat streamingFormat,
      @NonNull Map<String, String> httpHeaders) {
    super(assetUrl);
    this.streamingFormat = streamingFormat;
    this.httpHeaders = httpHeaders;
  }

  @OptIn(markerClass = UnstableApi.class)
  @NonNull
  @Override
  MediaItem getMediaItem() {
    MediaItem.Builder builder = new MediaItem.Builder().setUri(assetUrl);
    builder.setCustomCacheKey(assetUrl);
    String mimeType = null;
    switch (streamingFormat) {
      case SMOOTH:
        mimeType = MimeTypes.APPLICATION_SS;
        break;
      case DYNAMIC_ADAPTIVE:
        mimeType = MimeTypes.APPLICATION_MPD;
        break;
      case HTTP_LIVE:
        mimeType = MimeTypes.APPLICATION_M3U8;
        break;
    }
    mimeType = MimeTypes.APPLICATION_M3U8;
    if (mimeType != null) {
      builder.setMimeType(mimeType);
    }
    return builder.build();
  }

  @OptIn(markerClass = UnstableApi.class)
  @Override
  MediaSource.Factory getMediaSourceFactory(Context context) {
    DataSource.Factory factory = getCacheDataSourceFactory(context);
    return new HlsMediaSource.Factory(factory);
  }

  DataSource.Factory getCacheDataSourceFactory(Context context){
    return new CacheDataSourceFactory(context,
            100 * 1024 * 1024,
            100 * 1024 * 1024,
            getMediaSourceFactory(context, new DefaultHttpDataSource.Factory()));
  }

  /**
   * Returns a configured media source factory, starting at the provided factory.
   *
   * <p>This method is provided for ease of testing without making real HTTP calls.
   *
   * @param context application context.
   * @param initialFactory initial factory, to be configured.
   * @return configured factory, or {@code null} if not needed for this asset type.
   */
  @VisibleForTesting
  DataSource.Factory getMediaSourceFactory(
      Context context, DefaultHttpDataSource.Factory initialFactory) {
    String userAgent = DEFAULT_USER_AGENT;
    if (!httpHeaders.isEmpty() && httpHeaders.containsKey(HEADER_USER_AGENT)) {
      userAgent = httpHeaders.get(HEADER_USER_AGENT);
    }
    unstableUpdateDataSourceFactory(initialFactory, httpHeaders, userAgent);
    return new DefaultDataSource.Factory(context, initialFactory);
  }

  // TODO: Migrate to stable API, see https://github.com/flutter/flutter/issues/147039.
  @OptIn(markerClass = UnstableApi.class)
  private static void unstableUpdateDataSourceFactory(
      @NonNull DefaultHttpDataSource.Factory factory,
      @NonNull Map<String, String> httpHeaders,
      @Nullable String userAgent) {
    factory.setUserAgent(userAgent).setAllowCrossProtocolRedirects(true);
    if (!httpHeaders.isEmpty()) {
      factory.setDefaultRequestProperties(httpHeaders);
    }
  }
}
