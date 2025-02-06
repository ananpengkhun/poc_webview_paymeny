import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

class WebViewPage extends StatefulWidget {
  const WebViewPage({super.key,required this.controller, required this.channelName, required this.url, required this.onMessageReceived});
  final WebViewController controller;
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

    widget.controller.setJavaScriptMode(JavaScriptMode.unrestricted);
    widget.controller.addJavaScriptChannel(widget.channelName, onMessageReceived: widget.onMessageReceived);

    widget.controller.loadRequest(Uri.parse(widget.url));

  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          backgroundColor: Theme.of(context).colorScheme.inversePrimary,
          title: const Text(""),
        ),
        body: WebViewWidget(

        controller: widget.controller,
      ));
  }
}
