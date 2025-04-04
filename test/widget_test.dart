// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:poc_webview_payment/main.dart';

void main() {
  test('cancel one request', () async {
    final dio = Dio()..options.baseUrl = 'https://httpbun.com';

    List<CancelToken> cancelTokens = [CancelToken(), CancelToken()];
    await dio.get('/drip?delay=0&duration=2&numbytes=10', cancelToken: cancelTokens.first); // done
    // dio.get('/drip?delay=0&duration=2&numbytes=10');  // no await
    // cancelTokens.first.cancel();
  });

  testWidgets('Counter increments smoke test', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const MyApp());

    // Verify that our counter starts at 0.
    expect(find.text('0'), findsOneWidget);
    expect(find.text('1'), findsNothing);

    // Tap the '+' icon and trigger a frame.
    await tester.tap(find.byIcon(Icons.add));
    await tester.pump();

    // Verify that our counter has incremented.
    expect(find.text('0'), findsNothing);
    expect(find.text('1'), findsOneWidget);
  });
}
