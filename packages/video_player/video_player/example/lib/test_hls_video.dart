import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

class VideoPlayerWidget extends StatefulWidget {
  final String videoUrl;
  final int index;

  VideoPlayerWidget({super.key, required this.videoUrl, required this.index});

  @override
  _VideoPlayerWidgetState createState() => _VideoPlayerWidgetState();
}

class _VideoPlayerWidgetState extends State<VideoPlayerWidget> {
  late VideoPlayerController controller;

  @override
  void initState() {
    super.initState();
    print('init ${widget.index}');

    controller = VideoPlayerController.networkUrl(Uri.parse(widget.videoUrl),
        videoPlayerOptions: VideoPlayerOptions(
            webOptions: const VideoPlayerWebOptions(),
            hlsCacheConfig: HlsCacheConfig(
                useCache: true,
                cacheKey: widget.videoUrl,
                maxCacheSize: 1024 * 1024 * 1024),
            bufferingConfig:
                const BufferingConfig(minBufferMs: 3000, maxBufferMs: 5000)));
    int time = DateTime.now().millisecondsSinceEpoch;
    print('start ${DateTime.now()} ${widget.videoUrl}}');

    controller.initialize().then((event) async {
      print('initialize time ${DateTime.now().millisecondsSinceEpoch - time}');
      await controller.play();
      setState(() {});
    });
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Stack(children: [
      VideoPlayer(controller),
      Center(
        child: IconButton(
          onPressed: () {
            if (controller.value.isPlaying) {
              controller.pause();
            } else {
              controller.play();
            }
            setState(() {});
          },
          icon:
              Icon(controller.value.isPlaying ? Icons.pause : Icons.play_arrow),
        ),
      ),
      Positioned(
        bottom: 10,
        left: 10,
        right: 10,
        child: VideoProgressIndicator(
          controller,
          allowScrubbing: true,
        ),
      )
    ]);
  }
}

class TikTokPageView extends StatefulWidget {
  const TikTokPageView({super.key});

  @override
  State<TikTokPageView> createState() => _TikTokPageViewState();
}

class _TikTokPageViewState extends State<TikTokPageView> {
  final List<String> videoUrls = [
    'https://customer-5jdhfnsg3n4uo7jz.cloudflarestream.com/3d3bafbb8f8245189733757fb9f06b20/manifest/video.m3u8',
    'https://aka-cdn.dramahub.me/videos/3babc6605794a5ec4dbda9e18a87f598/index.m3u8',
    'https://customer-5jdhfnsg3n4uo7jz.cloudflarestream.com/a07cbb3c111848e3806eeecc0cdcad63/manifest/video.m3u8',
    'https://ccdn.dramahub.me/videos/4d97870e3a3c4fd1a099dafb12e08504/index.m3u8',
    'https://ccdn.dramahub.me/videos/09b782d7e6a9229ab026ce8ca7b503be/index.m3u8',
  ];

  @override
  void initState() {
    super.initState();
    VideoPlayerController.preCache(
        'https://aka-cdn.dramahub.me/videos/3babc6605794a5ec4dbda9e18a87f598/index.m3u8',
        videoPlayerOptions: VideoPlayerOptions(
            hlsCacheConfig: HlsCacheConfig(
                useCache: true,
                cacheKey:
                    'https://aka-cdn.dramahub.me/videos/3babc6605794a5ec4dbda9e18a87f598/index.m3u8')));
    VideoPlayerController.preCache(
        'https://customer-5jdhfnsg3n4uo7jz.cloudflarestream.com/3d3bafbb8f8245189733757fb9f06b20/manifest/video.m3u8',
        videoPlayerOptions: VideoPlayerOptions(
            hlsCacheConfig: HlsCacheConfig(
                useCache: true,
                cacheKey:
                    'https://customer-5jdhfnsg3n4uo7jz.cloudflarestream.com/3d3bafbb8f8245189733757fb9f06b20/manifest/video.m3u8')));
    VideoPlayerController.preCache(
        'https://customer-5jdhfnsg3n4uo7jz.cloudflarestream.com/a07cbb3c111848e3806eeecc0cdcad63/manifest/video.m3u8',
        videoPlayerOptions: VideoPlayerOptions(
            hlsCacheConfig: HlsCacheConfig(
                useCache: true,
                cacheKey:
                    'https://customer-5jdhfnsg3n4uo7jz.cloudflarestream.com/a07cbb3c111848e3806eeecc0cdcad63/manifest/video.m3u8')));
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          height: 600,
          child: PageView.builder(
            scrollDirection: Axis.vertical,
            itemCount: videoUrls.length,
            itemBuilder: (context, index) {
              return VideoPlayerWidget(
                  videoUrl: videoUrls[index], index: index);
            },
          ),
        ),
        GestureDetector(
            onTap: () {
              videoUrls.add(
                  'https://ccdn.dramahub.me/videos/09b782d7e6a9229ab026ce8ca7b503be/index.m3u8');
              setState(() {});
            },
            child: const Text(
              'Add more',
              style: TextStyle(fontSize: 25),
            )),
      ],
    );
  }
}

class TestHlsVideo extends StatelessWidget {
  const TestHlsVideo({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: Scaffold(
        body: TikTokPageView(),
      ),
    );
  }
}
