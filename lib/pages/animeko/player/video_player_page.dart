/// 视频播放器页面
/// 使用 PiliNara 内置播放器播放视频流
import 'package:PiliPlus/plugin/pl_player/controller.dart';
import 'package:PiliPlus/plugin/pl_player/models/data_source.dart';
import 'package:PiliPlus/plugin/pl_player/view/view.dart';
import 'package:flutter/material.dart' as material;
import 'package:get/get.dart';

class AnimekoVideoPlayerPage extends StatefulWidget {
  final String url;
  final String title;

  const AnimekoVideoPage({super.key, required this.url, required this.title});

  @override
  State<AnimekoVideoPlayerPage> createState() => _AnimekoVideoPlayerPageState();
}

class _AnimekoVideoPlayerPageState extends State<AnimekoVideoPlayerPage> {
  late final PlPlayerController _controller;

  @override
  void initState() {
    super.initState();
    _controller = PlPlayerController.getInstance();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return material.Scaffold(
      appBar: material.AppBar(title: material.Text(widget.title)),
      body: PlPlayerView(
        controller: _controller,
        dataSource: NetworkSource(videoSource: widget.url, audioSource: null),
      ),
    );
  }
}
