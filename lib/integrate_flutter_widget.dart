import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

class IntegrateFlutterWidget extends StatefulWidget {
  const IntegrateFlutterWidget({super.key});

  @override
  State<IntegrateFlutterWidget> createState() => _IntegrateFlutterWidgetState();
}

class _IntegrateFlutterWidgetState extends State<IntegrateFlutterWidget> {

  var controller = WebViewController();
  @override
  void initState() {
    controller.setJavaScriptMode(JavaScriptMode.unrestricted);

    controller.loadRequest(Uri.parse("https://en.wikipedia.org/wiki/Main_Page"));
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Column(children: [
        Container(
          color: Colors.green,
          child: Text("Flutter Component"),
          padding: EdgeInsets.all(20),),
        Expanded(
          child: WebViewWidget(
            controller: controller,
          ),
        ),
        Container(
          color: Colors.grey,
            child: Text("Flutter Component"),
          padding: EdgeInsets.all(20),)
      ],),
    );
  }
}
