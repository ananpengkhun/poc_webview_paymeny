import 'dart:convert';
import 'dart:developer';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:poc_webview_payment/liveness.dart';
import 'package:poc_webview_payment/menu_list.dart';
import 'package:poc_webview_payment/scanner.dart';
import 'package:poc_webview_payment/webview.dart';
import 'package:webview_flutter/webview_flutter.dart';

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
                        builder: (context) => MenuList()));
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
}
