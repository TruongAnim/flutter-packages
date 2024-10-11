// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static androidx.media3.common.Player.REPEAT_MODE_ALL;
import static androidx.media3.common.Player.REPEAT_MODE_OFF;

import android.content.Context;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.OptIn;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;

import io.flutter.view.TextureRegistry;

final class VideoPlayer implements TextureRegistry.SurfaceProducer.Callback {
  @NonNull private final ExoPlayerProvider exoPlayerProvider;
  @NonNull private final MediaItem mediaItem;
  @NonNull private final TextureRegistry.SurfaceProducer surfaceProducer;
  @NonNull private final VideoPlayerCallbacks videoPlayerEvents;
  @NonNull private final VideoPlayerOptions options;
  @NonNull private ExoPlayer exoPlayer;
  @Nullable private ExoPlayerState savedStateDuring;

  /**
   * Creates a video player.
   *
   * @param context application context.
   * @param events event callbacks.
   * @param surfaceProducer produces a texture to render to.
   * @param asset asset to play.
   * @param options options for playback.
   * @return a video player instance.
   */
  @NonNull
  static VideoPlayer create(
      @NonNull Context context,
      @NonNull VideoPlayerCallbacks events,
      @NonNull TextureRegistry.SurfaceProducer surfaceProducer,
      @NonNull VideoAsset asset,
      @NonNull VideoPlayerOptions options) {
    return new VideoPlayer(
        () -> {
          ExoPlayer.Builder builder;
          if (options.useCache) {
              HlsMediaSource.Factory mediaSourceFactory = (HlsMediaSource.Factory) asset.getMediaSourceFactory(context);
              mediaSourceFactory.createMediaSource(asset.getMediaItem());
              builder = new ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory);
          } else {
              builder = new ExoPlayer.Builder(context).setMediaSourceFactory(asset.getMediaSourceFactory(context));
          }
          DefaultLoadControl.Builder loadBuilder = new DefaultLoadControl.Builder();
          loadBuilder.setBufferDurationsMs(
                  options.minBufferMs,
                  options.maxBufferMs,
                  options.bufferForPlaybackMs,
                  options.bufferForPlaybackAfterRebufferMs
          );
          DefaultLoadControl loadControl = loadBuilder.build();
          builder.setLoadControl(loadControl);
          return builder.build();
        },
        events,
        surfaceProducer,
        asset.getMediaItem(),
        options);
  }
  /**
   * Precache a video player.
   *
   * @param context application context.
   * @param asset   asset to play.
   * @param options options for playback.
   */
  @OptIn(markerClass = UnstableApi.class)
  static boolean preCache(
          Context context,
          VideoAsset asset,
          VideoPlayerOptions options) {
      if (options.useCache) {
          HlsMediaSource.Factory mediaSourceFactory = (HlsMediaSource.Factory) asset.getMediaSourceFactory(context);
          mediaSourceFactory.createMediaSource(asset.getMediaItem());
          ExoPlayer.Builder builder = new ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory);
          DefaultLoadControl.Builder loadBuilder = new DefaultLoadControl.Builder();
          loadBuilder.setBufferDurationsMs(
                  options.minBufferMs,
                  options.maxBufferMs,
                  options.bufferForPlaybackMs,
                  options.bufferForPlaybackAfterRebufferMs
          );
          DefaultLoadControl loadControl = loadBuilder.build();
          builder.setLoadControl(loadControl);
          ExoPlayer exoPlayer = builder.build();
          exoPlayer.setMediaItem(asset.getMediaItem());
          exoPlayer.prepare();
          // Pause immediately to prevent playback
          exoPlayer.pause();
          // Add a listener to monitor the caching progress
          exoPlayer.addListener(new Player.Listener() {
              @Override
              public void onPlaybackStateChanged(int playbackState) {
                  if (playbackState == Player.STATE_READY) {
                      // The player has buffered all available segments.
                      // You can check buffering progress here or monitor cache usage.
                      // If the content is fully buffered, you can stop the player.
                      exoPlayer.release(); // Release the player when done
                  }
              }
          });
          return true;
      }
      return false;
  }

  /** A closure-compatible signature since {@link java.util.function.Supplier} is API level 24. */
  interface ExoPlayerProvider {
    /**
     * Returns a new {@link ExoPlayer}.
     *
     * @return new instance.
     */
    ExoPlayer get();
  }

  @VisibleForTesting
  VideoPlayer(
      @NonNull ExoPlayerProvider exoPlayerProvider,
      @NonNull VideoPlayerCallbacks events,
      @NonNull TextureRegistry.SurfaceProducer surfaceProducer,
      @NonNull MediaItem mediaItem,
      @NonNull VideoPlayerOptions options) {
    this.exoPlayerProvider = exoPlayerProvider;
    this.videoPlayerEvents = events;
    this.surfaceProducer = surfaceProducer;
    this.mediaItem = mediaItem;
    this.options = options;
    this.exoPlayer = createVideoPlayer();
    surfaceProducer.setCallback(this);
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY)
  // TODO(matanlurey): https://github.com/flutter/flutter/issues/155131.
  @SuppressWarnings({"deprecation", "removal"})
  public void onSurfaceCreated() {
    if (savedStateDuring != null) {
      exoPlayer = createVideoPlayer();
      savedStateDuring.restore(exoPlayer);
      savedStateDuring = null;
    }
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY)
  public void onSurfaceDestroyed() {
    // Intentionally do not call pause/stop here, because the surface has already been released
    // at this point (see https://github.com/flutter/flutter/issues/156451).
    savedStateDuring = ExoPlayerState.save(exoPlayer);
    exoPlayer.release();
  }

  private ExoPlayer createVideoPlayer() {
    ExoPlayer exoPlayer = exoPlayerProvider.get();
    exoPlayer.setMediaItem(mediaItem);
    exoPlayer.prepare();

    exoPlayer.setVideoSurface(surfaceProducer.getSurface());

    boolean wasInitialized = savedStateDuring != null;
    exoPlayer.addListener(new ExoPlayerEventListener(exoPlayer, videoPlayerEvents, wasInitialized));
    setAudioAttributes(exoPlayer, options.mixWithOthers);

    return exoPlayer;
  }

  void sendBufferingUpdate() {
    videoPlayerEvents.onBufferingUpdate(exoPlayer.getBufferedPosition());
  }

  private static void setAudioAttributes(ExoPlayer exoPlayer, boolean isMixMode) {
    exoPlayer.setAudioAttributes(
        new AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
        !isMixMode);
  }

  void play() {
    exoPlayer.play();
  }

  void pause() {
    exoPlayer.pause();
  }

  void setLooping(boolean value) {
    exoPlayer.setRepeatMode(value ? REPEAT_MODE_ALL : REPEAT_MODE_OFF);
  }

  void setVolume(double value) {
    float bracketedValue = (float) Math.max(0.0, Math.min(1.0, value));
    exoPlayer.setVolume(bracketedValue);
  }

  void setPlaybackSpeed(double value) {
    // We do not need to consider pitch and skipSilence for now as we do not handle them and
    // therefore never diverge from the default values.
    final PlaybackParameters playbackParameters = new PlaybackParameters(((float) value));

    exoPlayer.setPlaybackParameters(playbackParameters);
  }

  void seekTo(int location) {
    exoPlayer.seekTo(location);
  }

  long getPosition() {
    return exoPlayer.getCurrentPosition();
  }

  void dispose() {
    exoPlayer.release();
    surfaceProducer.release();

    // TODO(matanlurey): Remove when embedder no longer calls-back once released.
    // https://github.com/flutter/flutter/issues/156434.
    surfaceProducer.setCallback(null);
  }
}
