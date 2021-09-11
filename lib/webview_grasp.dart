
import 'dart:async';

import 'package:flutter/services.dart';

class WebviewGrasp {
  static const MethodChannel _channel = MethodChannel('webview_grasp');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
