// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static androidx.media3.common.Player.REPEAT_MODE_ALL;
import static androidx.media3.common.Player.REPEAT_MODE_OFF;

import android.content.Context;
import android.util.Log;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;

import io.flutter.view.TextureRegistry;

final class VideoPlayer {
  private ExoPlayer exoPlayer;
  private Surface surface;
  private final TextureRegistry.SurfaceTextureEntry textureEntry;
  private final VideoPlayerCallbacks videoPlayerEvents;
  private final VideoPlayerOptions options;

  /**
   * Creates a video player.
   *
   * @param context application context.
   * @param events event callbacks.
   * @param textureEntry texture to render to.
   * @param asset asset to play.
   * @param options options for playback.
   * @return a video player instance.
   */
  @OptIn(markerClass = UnstableApi.class)
  @NonNull
  static VideoPlayer create(
      Context context,
      VideoPlayerCallbacks events,
      TextureRegistry.SurfaceTextureEntry textureEntry,
      VideoAsset asset,
      VideoPlayerOptions options) {
    HlsMediaSource.Factory mediaSourceFactory = (HlsMediaSource.Factory) asset.getMediaSourceFactory(context);
    mediaSourceFactory.createMediaSource(asset.getMediaItem());
    DefaultLoadControl.Builder loadBuilder = new DefaultLoadControl.Builder();
    loadBuilder.setBufferDurationsMs(
            2500,
            5000,
            2500,
            2000
    );
    DefaultLoadControl loadControl = loadBuilder.build();
    ExoPlayer.Builder builder =
        new ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).setLoadControl(loadControl);
    return new VideoPlayer(builder, events, textureEntry, asset.getMediaItem(), options);
  }

  @VisibleForTesting
  VideoPlayer(
      ExoPlayer.Builder builder,
      VideoPlayerCallbacks events,
      TextureRegistry.SurfaceTextureEntry textureEntry,
      MediaItem mediaItem,
      VideoPlayerOptions options) {
    this.videoPlayerEvents = events;
    this.textureEntry = textureEntry;
    this.options = options;
    
    ExoPlayer exoPlayer = builder.build();
    exoPlayer.setMediaItem(mediaItem);
    exoPlayer.prepare();

    setUpVideoPlayer(exoPlayer);
  }

  private void setUpVideoPlayer(ExoPlayer exoPlayer) {
    this.exoPlayer = exoPlayer;

    surface = new Surface(textureEntry.surfaceTexture());
    exoPlayer.setVideoSurface(surface);
    setAudioAttributes(exoPlayer, options.mixWithOthers);
    exoPlayer.addListener(new ExoPlayerEventListener(exoPlayer, videoPlayerEvents));
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
    exoPlayer.setPlayWhenReady(true);
  }

  void pause() {
    exoPlayer.setPlayWhenReady(false);
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
    textureEntry.release();
    if (surface != null) {
      surface.release();
    }
    if (exoPlayer != null) {
      exoPlayer.release();
    }
  }
}
