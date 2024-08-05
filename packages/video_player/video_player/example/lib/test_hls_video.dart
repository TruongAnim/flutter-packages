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

    controller = VideoPlayerController.networkUrl(Uri.parse(widget.videoUrl));
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
    'https://ccdn.dramahub.me/videos/538457732c5566285b9524cdfbb337b2/index.m3u8',
    'https://ccdn.dramahub.me/videos/033918075234465ef575018b76b2fd31/index.m3u8',
    'https://ccdn.dramahub.me/videos/bf708fc4edd230b7e8f912508b30ec51/index.m3u8',
    'https://ccdn.dramahub.me/videos/4d97870e3a3c4fd1a099dafb12e08504/index.m3u8',
    'https://ccdn.dramahub.me/videos/09b782d7e6a9229ab026ce8ca7b503be/index.m3u8',
  ];

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
