import 'dart:convert';

class HomeData {
  final Map<String, dynamic> tenant;
  final Map<String, dynamic> config;
  final List<Store> stores;
  final List<RoomType> roomTypes;
  final String announcement;

  HomeData({required this.tenant, required this.config, required this.stores, required this.roomTypes, required this.announcement});

  factory HomeData.fromJson(Map<String, dynamic> json) {
    return HomeData(
      tenant: Map<String, dynamic>.from(json['tenant'] ?? {}),
      config: Map<String, dynamic>.from(json['config'] ?? {}),
      stores: (json['stores'] as List? ?? []).map((e) => Store.fromJson(e)).toList(),
      roomTypes: (json['roomTypes'] as List? ?? []).map((e) => RoomType.fromJson(e)).toList(),
      announcement: json['announcement']?.toString() ?? '',
    );
  }
}

class Store {
  final int id;
  final String name;
  final String city;
  final String address;
  final String phone;
  final String intro;

  Store({required this.id, required this.name, required this.city, required this.address, required this.phone, required this.intro});

  factory Store.fromJson(Map<String, dynamic> json) => Store(
        id: json['id'] ?? 0,
        name: json['name']?.toString() ?? '',
        city: json['city']?.toString() ?? '',
        address: json['address']?.toString() ?? '',
        phone: json['phone']?.toString() ?? '',
        intro: json['intro']?.toString() ?? '',
      );
}

class RoomType {
  final int id;
  final int storeId;
  final String storeName;
  final String name;
  final double basePrice;
  final int capacity;
  final double area;
  final String bedType;
  final List<String> facilities;
  final String intro;
  final String? cover;
  final List<String> images;

  RoomType({
    required this.id,
    required this.storeId,
    required this.storeName,
    required this.name,
    required this.basePrice,
    required this.capacity,
    required this.area,
    required this.bedType,
    required this.facilities,
    required this.intro,
    this.cover,
    this.images = const [],
  });

  List<String> get gallery =>
      images.isNotEmpty ? images : (cover != null && cover!.isNotEmpty ? [cover!] : []);

  factory RoomType.fromJson(Map<String, dynamic> json) => RoomType(
        id: json['id'] ?? 0,
        storeId: json['storeId'] ?? 0,
        storeName: json['storeName']?.toString() ?? '',
        name: json['name']?.toString() ?? '',
        basePrice: (json['basePrice'] as num? ?? 0).toDouble(),
        capacity: json['capacity'] ?? 0,
        area: (json['area'] as num? ?? 0).toDouble(),
        bedType: json['bedType']?.toString() ?? '',
        facilities: (json['facilities'] as List? ?? []).map((e) => '$e').toList(),
        intro: json['intro']?.toString() ?? '',
        cover: json['cover']?.toString(),
        images: parseImages(json['images']),
      );
}

/// 后端 images 既可能是 JSON 数组，也可能是被序列化后的字符串（"[\"a\",\"b\"]"）。
List<String> parseImages(dynamic raw) {
  if (raw == null) return const [];
  if (raw is List) {
    return raw.map((e) => '$e').where((e) => e.isNotEmpty).toList();
  }
  final s = raw.toString().trim();
  if (s.isEmpty || s == 'null') return const [];
  try {
    final decoded = jsonDecode(s);
    if (decoded is List) {
      return decoded.map((e) => '$e').where((e) => e.isNotEmpty).toList();
    }
    if (decoded is String) return decoded.isEmpty ? const [] : <String>[decoded];
  } catch (_) {
    if (s.startsWith('http')) return <String>[s];
    return s.split(',').map((e) => e.trim()).where((e) => e.isNotEmpty).toList();
  }
  return const [];
}

class RoomCalendar {
  final String date;
  final double price;
  final bool available;
  RoomCalendar({required this.date, required this.price, required this.available});
  factory RoomCalendar.fromJson(Map<String, dynamic> json) => RoomCalendar(
        date: json['date']?.toString() ?? '',
        price: (json['price'] as num? ?? 0).toDouble(),
        available: json['available'] == true,
      );
}

class HotelOrder {
  final int id;
  final String orderNo;
  final String guestName;
  final String guestPhone;
  final String checkIn;
  final String checkOut;
  final int nights;
  final double amount;
  final String status;
  final String roomTypeName;
  final String? roomNo;

  HotelOrder({
    required this.id,
    required this.orderNo,
    required this.guestName,
    required this.guestPhone,
    required this.checkIn,
    required this.checkOut,
    required this.nights,
    required this.amount,
    required this.status,
    required this.roomTypeName,
    this.roomNo,
  });

  factory HotelOrder.fromJson(Map<String, dynamic> json) => HotelOrder(
        id: json['id'] ?? 0,
        orderNo: json['orderNo']?.toString() ?? '',
        guestName: json['guestName']?.toString() ?? '',
        guestPhone: json['guestPhone']?.toString() ?? '',
        checkIn: json['checkIn']?.toString() ?? '',
        checkOut: json['checkOut']?.toString() ?? '',
        nights: json['nights'] ?? 0,
        amount: (json['amount'] as num? ?? 0).toDouble(),
        status: json['status']?.toString() ?? '',
        roomTypeName: json['roomTypeName']?.toString() ?? '',
        roomNo: json['roomNo']?.toString(),
      );
}
