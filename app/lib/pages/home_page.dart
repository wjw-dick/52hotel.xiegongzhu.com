import 'package:flutter/material.dart';

import '../api_client.dart';
import '../models.dart';
import 'room_detail_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  HomeData? home;
  String? error;
  bool loading = true;
  DateTime checkIn = DateTime.now();
  DateTime checkOut = DateTime.now().add(const Duration(days: 2));

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final data = await ApiClient.instance.get(ApiClient.instance.publicPath('/home'));
      if (!mounted) return;
      setState(() {
        home = HomeData.fromJson(Map<String, dynamic>.from(data));
        loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        error = e.toString();
        loading = false;
      });
    }
  }

  Future<void> pickDates() async {
    final range = await showDateRangePicker(
      context: context,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
      initialDateRange: DateTimeRange(start: checkIn, end: checkOut),
    );
    if (range != null) {
      setState(() {
        checkIn = range.start;
        checkOut = range.end;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final config = home?.config ?? {};
    return Scaffold(
      body: RefreshIndicator(
        onRefresh: load,
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            Container(
              padding: const EdgeInsets.fromLTRB(22, 64, 22, 100),
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  colors: [Color(0xFF1C352D), Color(0xFF3C5B4C)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('YUNQI RETREAT', style: TextStyle(color: Color(0xFFCDBB95), letterSpacing: 4, fontSize: 11)),
                  const SizedBox(height: 18),
                  Text(
                    config['tenant_name']?.toString() ?? '云栖民宿',
                    style: const TextStyle(color: Color(0xFFF7F2E5), fontSize: 34, height: 1.3),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    config['slogan']?.toString() ?? '山水之间，安心住下',
                    style: const TextStyle(color: Color(0xFFCDD9D1), fontSize: 14, height: 1.8),
                  ),
                ],
              ),
            ),
            Transform.translate(
              offset: const Offset(0, -60),
              child: Container(
                margin: const EdgeInsets.symmetric(horizontal: 18),
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(8),
                  boxShadow: const [BoxShadow(color: Color(0x1A24483C), blurRadius: 24, offset: Offset(0, 10))],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('快速预订', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 14),
                    InkWell(
                      onTap: pickDates,
                      child: Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          border: Border.all(color: const Color(0xFFE7E4DA)),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.calendar_today_outlined, size: 18, color: Color(0xFF24483C)),
                            const SizedBox(width: 10),
                            Text('${formatDate(checkIn)}  →  ${formatDate(checkOut)}'),
                            const Spacer(),
                            const Icon(Icons.chevron_right, color: Colors.grey),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),
                    FilledButton(
                      onPressed: () {
                        Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) => RoomListPage(
                            rooms: home?.roomTypes ?? [],
                            checkIn: checkIn,
                            checkOut: checkOut,
                          ),
                        ));
                      },
                      child: const Text('查询房态并预订'),
                    ),
                  ],
                ),
              ),
            ),
            if (loading) const Center(child: Padding(padding: EdgeInsets.all(30), child: CircularProgressIndicator())),
            if (error != null)
              Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  children: [
                    Text(error!, style: const TextStyle(color: Colors.red)),
                    const SizedBox(height: 10),
                    OutlinedButton(onPressed: load, child: const Text('重新加载')),
                  ],
                ),
              ),
            if (home != null) ...[
              Container(
                margin: const EdgeInsets.fromLTRB(18, 0, 18, 18),
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(color: const Color(0xFFE7EFE9), borderRadius: BorderRadius.circular(6)),
                child: Text(home!.announcement, style: const TextStyle(color: Color(0xFF24483C), height: 1.7)),
              ),
              const Padding(
                padding: EdgeInsets.fromLTRB(20, 10, 20, 4),
                child: Text('房型甄选', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w600)),
              ),
              const Padding(
                padding: EdgeInsets.fromLTRB(20, 0, 20, 16),
                child: Text('价格为门市参考价，实际以所选日期为准', style: TextStyle(color: Color(0xFF75817A), fontSize: 12)),
              ),
              ...home!.roomTypes.map((room) => _RoomCard(
                    room: room,
                    checkIn: checkIn,
                    checkOut: checkOut,
                  )),
              const Padding(
                padding: EdgeInsets.fromLTRB(20, 18, 20, 10),
                child: Text('两处山居', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w600)),
              ),
              ...home!.stores.map((store) => Card(
                    margin: const EdgeInsets.fromLTRB(18, 0, 18, 12),
                    child: Padding(
                      padding: const EdgeInsets.all(18),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(store.name, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600)),
                          const SizedBox(height: 6),
                          Text('${store.city} · ${store.address}', style: const TextStyle(color: Color(0xFF75817A), fontSize: 12)),
                          const SizedBox(height: 8),
                          Text(store.intro, style: const TextStyle(height: 1.7, fontSize: 13)),
                          const SizedBox(height: 8),
                          Text('电话 ${store.phone}', style: const TextStyle(color: Color(0xFFB08A4F), fontSize: 12)),
                        ],
                      ),
                    ),
                  )),
              const SizedBox(height: 30),
            ],
          ],
        ),
      ),
    );
  }
}

class RoomListPage extends StatelessWidget {
  final List<RoomType> rooms;
  final DateTime checkIn;
  final DateTime checkOut;
  const RoomListPage({super.key, required this.rooms, required this.checkIn, required this.checkOut});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('房型列表')),
      body: ListView(
        padding: const EdgeInsets.all(14),
        children: rooms.map((room) => _RoomCard(room: room, checkIn: checkIn, checkOut: checkOut)).toList(),
      ),
    );
  }
}

class _RoomCard extends StatelessWidget {
  final RoomType room;
  final DateTime checkIn;
  final DateTime checkOut;
  const _RoomCard({required this.room, required this.checkIn, required this.checkOut});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.fromLTRB(18, 0, 18, 14),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => Navigator.of(context).push(MaterialPageRoute(
          builder: (_) => RoomDetailPage(roomId: room.id, checkIn: checkIn, checkOut: checkOut),
        )),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              height: 170,
              width: double.infinity,
              color: const Color(0xFFDCE7DF),
              child: room.gallery.isNotEmpty
                  ? Image.network(room.gallery.first, fit: BoxFit.cover, errorBuilder: (_, __, ___) => const Icon(Icons.landscape, size: 60))
                  : const Icon(Icons.landscape, size: 60, color: Color(0x6624483C)),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(child: Text(room.name, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600))),
                      Text('¥${money(room.basePrice)}', style: const TextStyle(color: Color(0xFF24483C), fontSize: 18, fontWeight: FontWeight.w600)),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text('${room.storeName} · ${room.area.toStringAsFixed(0)}㎡ · 可住 ${room.capacity} 人 · ${room.bedType}',
                      style: const TextStyle(color: Color(0xFF75817A), fontSize: 12)),
                  const SizedBox(height: 10),
                  Text(room.intro, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(height: 1.6, fontSize: 13)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
