/// 视频播放器页面
import 'package:PiliPlus/plugin/pl_player/controller.dart';
import 'package:PiliPlus/plugin/pl_player/view/view.dart';
import 'package:flutter/material.dart';

class AnimekoVideoPlayerPage extends StatefulWidget {
  final String url;
  final String title;

  const AnimekoVideoPlayerPage({super.key, required this.url, required this.title});

  @override
  State<AnimekoVideoPlayerPage> createState() => _AnimekoVideoPlayerPageState();
}

class _AnimekoVideoPlayerPageState extends State<AnimekoVideoPlayerPage> {
  late final PlPlayerController _controller;

  @override
  void initState() {
    super.initState();
    _controller = PlPlayerController.getInstance();
    // TODO: 设置数据源
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.title)),
      body: Center(child: Text('播放器页面（待完善）')),
    );
  }
}
