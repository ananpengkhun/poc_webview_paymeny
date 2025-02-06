import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

class WebViewPage extends StatefulWidget {
  const WebViewPage({super.key, required this.channelName, required this.url, required this.onMessageReceived});
  final String channelName;
  final String url;
  final Function(JavaScriptMessage) onMessageReceived;

  @override
  State<WebViewPage> createState() => _WebViewPageState();
}

class _WebViewPageState extends State<WebViewPage> {
    late WebViewController controller;

  @override
  void initState() {
    super.initState();
     controller = WebViewController()
      ..loadRequest(
        Uri.parse(widget.url),
      )
    ..addJavaScriptChannel(widget.channelName, onMessageReceived: widget.onMessageReceived);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          backgroundColor: Theme.of(context).colorScheme.inversePrimary,
          title: const Text(""),
        ),
        body: WebViewWidget(
        controller: controller,
      ));
  }
}
