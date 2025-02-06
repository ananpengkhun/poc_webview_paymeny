import 'package:flutter/material.dart';
import 'package:poc_webview_payment/scanner.dart';
import 'package:poc_webview_payment/webview.dart';

class LandingPage extends StatefulWidget {
  const LandingPage({super.key});
  @override
  State<LandingPage> createState() => _LandingPageState();
}

class _LandingPageState extends State<LandingPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text(""),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            TextButton(
              onPressed: () {
                Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) => WebViewPage(
                              url:
                                  "https://ananpengkhun.github.io/test_post_message/",
                          channelName: "WebBridge",
                          onMessageReceived: (message){
                                _onRedirect(message.message);
                          },
                            )));
              },
              style: TextButton.styleFrom(
                  foregroundColor: Colors.black,
                  elevation: 2,
                  backgroundColor: Colors.amber),
              child: const Text(
                "เปิดบัญชี",
                style: TextStyle(fontSize: 25),
              ),
            ),
          ],
        ),
      ),
    );
  }

  _onRedirect(String name){
    if (name == "scanner") {
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (context) {
            return ScannerPage();
          },
        ),
      );
    }else if(name == "facescan"){

    }
  }
}
