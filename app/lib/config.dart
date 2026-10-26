class AppConfig {
  // Android 模拟器访问宿主机请使用 http://10.0.2.2:8080/api
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://127.0.0.1:8080/api',
  );
  static const String tenantCode = String.fromEnvironment(
    'TENANT_CODE',
    defaultValue: 'YQ-HZ',
  );
  static const String appName = '云栖民宿';
}
