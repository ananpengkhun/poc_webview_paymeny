import 'package:flutter/material.dart';
import 'package:poc_webview_payment/integrate_flutter_widget.dart';
import 'package:poc_webview_payment/liveness.dart';
import 'package:poc_webview_payment/scanner.dart';
import 'package:poc_webview_payment/webview.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:webview_flutter_android/webview_flutter_android.dart';

import 'issue_webview_scrollview.dart';

class MenuList extends StatefulWidget {
  const MenuList({super.key});

  @override
  State<MenuList> createState() => _MenuListState();
}

class _MenuListState extends State<MenuList> {

  var baseUrl = "https://ananpengkhun.github.io/test_post_message/";

  var scanQrcontroller = WebViewController();
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text("รายการ"),
      ),
      body: SizedBox.expand(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            TextButton(onPressed: (){
              Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => WebViewPage(
                        controller: scanQrcontroller,
                        url: baseUrl,
                        channelName: "WebBridge",
                        onMessageReceived: (message){
                          _onRedirect(message.message);
                        },
                      )));
            }, child: Text("Scan Qr")),

            TextButton(onPressed: (){
              Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => WebViewPage(
                        controller: scanQrcontroller,
                        url: baseUrl,
                        channelName: "WebBridge",
                        onMessageReceived: (message){
                          _onRedirect(message.message);
                        },
                      )));
            }, child: Text("Liveness")),

            TextButton(onPressed: (){
              var controller = WebViewController();
              controller.loadRequest(Uri.parse("https://en.wikipedia.org/wiki/Main_Page"));
              Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => WebViewWidget.fromPlatformCreationParams(
                        params: AndroidWebViewWidgetCreationParams(
                          controller: controller.platform,
                          displayWithHybridComposition: true,
                        ),
                      )));
            }, child: Text("Navigation Stack")),

            TextButton(onPressed: (){
              var controller = WebViewController();
              controller.loadRequest(Uri.parse("https://en.wikipedia.org/wiki/Main_Page"));
              Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => WebViewWidget.fromPlatformCreationParams(
                        params: AndroidWebViewWidgetCreationParams(
                          controller: controller.platform,
                          displayWithHybridComposition: true,
                        ),
                      )));
            }, child: Text("Scroll Issue")),

            TextButton(onPressed: (){
              var controller = WebViewController();
              controller.loadRequest(Uri.parse("https://en.wikipedia.org/wiki/Main_Page"));
              Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => IntegrateFlutterWidget()));
            }, child: Text("Integrate with flutter widget")),

            TextButton(onPressed: (){
              var controller = WebViewController();
              controller.loadRequest(Uri.parse("https://en.wikipedia.org/wiki/Main_Page"));
              Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => IssueWebviewScrollView()));
            }, child: Text("Issue Webview Height")),
          ],
        ),
      ),
    );
  }

  _onRedirect(String name)async{
    if (name == "scanner") {
      var result = await Navigator.of(context).push(
        MaterialPageRoute(
          builder: (context) {
            return ScannerPage();
          },
        ),
      );

      if(result != ""){
        showProgress(context);
        await Future.delayed(Duration(seconds: 2));
        await hideProgress();

        scanQrcontroller.loadRequest(Uri.parse("$baseUrl?isFace=false&data=Update your info..."));
      }

    }else if(name == "facescan"){

      var result = Navigator.of(context).push(
        MaterialPageRoute(
          builder: (context) {
            return Liveness();
          },
        ),
      );

      if(result != ""){
        showProgress(context);
        await Future.delayed(Duration(seconds: 2));
        await hideProgress();
        result.then((value) {
          print("result :: $value");
          scanQrcontroller.loadRequest(Uri.parse("$baseUrl?isFace=true&data=Update your info..."));
        });
      }
    }
  }



}


showProgress(BuildContext context) async {
  await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (mContext) {
        return WillPopScope(
          onWillPop: () async => false,
          child: Center(
            key: progressDialog,
            child: const SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(
                strokeWidth: 3,
                color: Colors.green,
              ),
            ),
          ),
        );
      });
}

hideProgress() async {
  await Future.delayed(const Duration(milliseconds: 300)).then((value) {
    Navigator.pop(progressDialog.currentContext!);
  });
}

final GlobalKey progressDialog = GlobalKey<NavigatorState>();