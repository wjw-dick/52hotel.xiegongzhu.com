import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import 'config.dart';

class ApiException implements Exception {
  final int code;
  final String message;
  ApiException(this.code, this.message);
  @override
  String toString() => message;
}

class ApiClient {
  ApiClient._();
  static final ApiClient instance = ApiClient._();

  String? _token;

  Future<void> loadToken() async {
    final prefs = await SharedPreferences.getInstance();
    _token = prefs.getString('member_token');
  }

  Future<void> saveToken(String? token) async {
    _token = token;
    final prefs = await SharedPreferences.getInstance();
    if (token == null) {
      await prefs.remove('member_token');
    } else {
      await prefs.setString('member_token', token);
    }
  }

  bool get isLoggedIn => _token != null && _token!.isNotEmpty;

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        if (_token != null && _token!.isNotEmpty) 'Authorization': 'Bearer $_token',
      };

  Future<dynamic> get(String path, {Map<String, dynamic>? query}) async {
    final uri = Uri.parse('${AppConfig.baseUrl}$path').replace(
      queryParameters: query?.map((key, value) => MapEntry(key, '$value')),
    );
    final response = await http.get(uri, headers: _headers);
    return _handle(response);
  }

  Future<dynamic> post(String path, {Map<String, dynamic>? body, Map<String, String>? headers}) async {
    final response = await http.post(
      Uri.parse('${AppConfig.baseUrl}$path'),
      headers: {..._headers, ...?headers},
      body: jsonEncode(body ?? {}),
    );
    return _handle(response);
  }

  Future<dynamic> put(String path, {Map<String, dynamic>? body}) async {
    final response = await http.put(
      Uri.parse('${AppConfig.baseUrl}$path'),
      headers: _headers,
      body: jsonEncode(body ?? {}),
    );
    return _handle(response);
  }

  dynamic _handle(http.Response response) {
    final Map<String, dynamic> payload = jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
    final code = payload['code'] as int? ?? -1;
    if (code == 0) return payload['data'];
    if (code == 401) {
      saveToken(null);
    }
    throw ApiException(code, payload['msg']?.toString() ?? '请求失败');
  }

  String publicPath(String path) => '/public/${AppConfig.tenantCode}$path';
}

String money(num? value) {
  final text = (value ?? 0).toStringAsFixed(2);
  return text.endsWith('.00') ? text.substring(0, text.length - 3) : text;
}

String statusText(String? status) {
  const map = {
    'PENDING': '待确认',
    'CONFIRMED': '已确认',
    'CHECKED_IN': '在住',
    'CHECKED_OUT': '已退房',
    'CANCELLED': '已取消',
    'NO_SHOW': '未到店',
  };
  return map[status] ?? status ?? '';
}

String formatDate(DateTime date) {
  String pad(int n) => n < 10 ? '0$n' : '$n';
  return '${date.year}-${pad(date.month)}-${pad(date.day)}';
}
