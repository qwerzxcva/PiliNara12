/// Bangumi 登录页面
import 'package:PiliPlus/utils/storage_pref.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class BangumiLoginPage extends StatefulWidget {
  const BangumiLoginPage({super.key});
  @override
  State<BangumiLoginPage> createState() => _BangumiLoginPageState();
}

class _BangumiLoginPageState extends State<BangumiLoginPage> {
  final _usernameController = TextEditingController();
  bool _isLoading = false;

  Future<void> _login() async {
    final username = _usernameController.text.trim();
    if (username.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请输入 Bangumi 用户名')),
      );
      return;
    }

    setState(() => _isLoading = true);
    await Future.delayed(const Duration(seconds: 1));
    Pref.bangumiUsername = username;
    setState(() => _isLoading = false);
    Get.back(result: true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('登录 Bangumi')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            const Text(
              '登录 Bangumi 以同步追番进度\n用户名将用于获取收藏列表',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            TextField(
              controller: _usernameController,
              decoration: const InputDecoration(
                labelText: 'Bangumi 用户名',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _isLoading ? null : _login,
              child: _isLoading
                  ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('登录'),
            ),
          ],
        ),
      ),
    );
  }
}
