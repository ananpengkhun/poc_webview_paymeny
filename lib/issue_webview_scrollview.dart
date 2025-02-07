import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

class IssueWebviewScrollView extends StatefulWidget {
  const IssueWebviewScrollView({super.key});

  @override
  State<IssueWebviewScrollView> createState() => _IssueWebviewScrollViewState();
}

class _IssueWebviewScrollViewState extends State<IssueWebviewScrollView> {
  late double _webViewHeight;
  var controller = WebViewController();
  @override
  void initState() {
    controller.setJavaScriptMode(JavaScriptMode.unrestricted);

    controller.loadRequest(Uri.parse("https://flutter.dev/"));
    super.initState();
  }

  @override
  void didChangeDependencies() {
    _webViewHeight = MediaQuery.of(context).size.height;
    super.didChangeDependencies();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: SingleChildScrollView(
        child: Column(children: [
          const Text('Some scrollable header'),
          SizedBox(
            height: 10000,
            child: WebViewWidget(
                controller: controller,
              ),
          ),
        ],),
      ),
    );
  }
}
