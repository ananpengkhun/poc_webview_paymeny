import 'dart:convert';
import 'dart:developer';
import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class Liveness extends StatefulWidget {
  const Liveness({super.key});

  @override
  State<Liveness> createState() => _LivenessState();
}

class _LivenessState extends State<Liveness> {

  @override
  void initState() {
    start();
    super.initState();
  }

  _LivenessState() {
    _methodChannel.setMethodCallHandler(_handleMethodCall);
  }

  Uint8List? _imageData;
  static const MethodChannel _methodChannel = MethodChannel('com.krungsri.kma/liveness/method');
  bool isStarting = false;
  String status = "";

  Future<void> start() async {
    if (isStarting) {
      debugPrint('Called start() while starting.');
      return;
    }
    isStarting = true;

    // Start the camera and liveness
    try {
      await _methodChannel.invokeMapMethod<String, dynamic>(
          'executeAinuLiveness', {"isEn" : false}
      );
    } on PlatformException catch (error) {
      isStarting = false;
    }
  }

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'success':

        final imageByte = call.arguments['imageByte'] as List<int>;
        final imageBase64 = call.arguments['imageBase64'];
        final resultCode = call.arguments['resultCode'] as String;
        final signature = call.arguments['signature'] as String;
        final keyId = call.arguments['keyId'] as String;
        final metadata = call.arguments['metadata'] as String;
        final dataLog = call.arguments['log'] as String;

        Navigator.of(context).pop(imageBase64);

        setState(() {
          _imageData = Uint8List.fromList(imageByte);
          status = resultCode;
        });

        print("status ===> $status");
        print("imageByte ===> $imageByte");
        print("imageBase64 ===> $imageBase64");
        print("metadata ===> $metadata");
        print("signature ===> $signature");
        print("keyId ===> $keyId");
        print("log ===> $dataLog");

        Map<String, dynamic> dataJson = {
          'imageData' : imageBase64,
          'signature' : signature,
          'metadata' : metadata,
          'keyId' : keyId
        };

        log('Liveness Response ===> ${jsonEncode(dataJson)}');

        isStarting = false;
      case 'livenessFailed':
        final resultCode = call.arguments['resultCode'] as String;
        setState(() {
          _imageData = null;
          status = resultCode;
        });
        print(resultCode);
        isStarting = false;
        break;
      case 'failure':
        break;
      case 'back':
        break;
      default:
        throw PlatformException(
          code: 'Unimplemented',
          details: 'The method ${call.method} is not implemented.',
        );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container();
    // return Scaffold(
    //   body: Center(
    //     child: Column(
    //       mainAxisAlignment: MainAxisAlignment.center,
    //       children: <Widget>[
    //         Text(status),
    //         _imageData != null
    //             ? Image.memory(_imageData!)
    //             : CircularProgressIndicator(),
    //         ElevatedButton(
    //           onPressed: () {
    //             start();
    //           },
    //           child: Text('Start Liveness'),
    //         ),
    //       ],
    //     ),
    //   ),
    // );
  }
}
