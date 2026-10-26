import 'package:flutter/material.dart';

import '../api_client.dart';
import '../models.dart';
import 'booking_page.dart';

class RoomDetailPage extends StatefulWidget {
  final int roomId;
  final DateTime checkIn;
  final DateTime checkOut;
  const RoomDetailPage({super.key, required this.roomId, required this.checkIn, required this.checkOut});

  @override
  State<RoomDetailPage> createState() => _RoomDetailPageState();
}

class _RoomDetailPageState extends State<RoomDetailPage> {
  RoomType? room;
  List<RoomCalendar> calendar = [];
  int? available;
  late DateTime checkIn = widget.checkIn;
  late DateTime checkOut = widget.checkOut;
  bool loading = true;

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    try {
      final data = await ApiClient.instance.get(ApiClient.instance.publicPath('/room-types/${widget.roomId}'));
      final map = Map<String, dynamic>.from(data);
      final roomType = RoomType.fromJson(Map<String, dynamic>.from(map['roomType'] ?? {}));
      final days = (map['calendar'] as List? ?? []).map((e) => RoomCalendar.fromJson(Map<String, dynamic>.from(e))).toList();
      if (!mounted) return;
      setState(() {
        room = roomType;
        calendar = days;
        loading = false;
      });
      checkAvailability();
    } catch (e) {
      if (!mounted) return;
      setState(() => loading = false);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
    }
  }

  Future<void> checkAvailability() async {
    try {
      final data = await ApiClient.instance.get(
        ApiClient.instance.publicPath('/availability'),
        query: {
          'roomTypeId': widget.roomId,
          'checkIn': formatDate(checkIn),
          'checkOut': formatDate(checkOut),
        },
      );
      if (!mounted) return;
      setState(() => available = (data['rooms'] as List? ?? []).length);
    } catch (_) {
      if (!mounted) return;
      setState(() => available = 0);
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
      checkAvailability();
    }
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const Scaffold(body: Center(child: CircularProgressIndicator()));
    if (room == null) return Scaffold(appBar: AppBar(title: const Text('房型详情')), body: const Center(child: Text('房型不存在')));
    final nights = checkOut.difference(checkIn).inDays;
    final total = room!.basePrice * nights;
    return Scaffold(
      appBar: AppBar(title: Text(room!.name)),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text('¥${money(total)}', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w700, color: Color(0xFF24483C))),
                  Text('$nights 晚 · 可订 ${available ?? '-'} 间', style: const TextStyle(fontSize: 11, color: Color(0xFF75817A))),
                ],
              ),
              const SizedBox(width: 18),
              Expanded(
                child: FilledButton(
                  onPressed: available == 0
                      ? null
                      : () => Navigator.of(context).push(MaterialPageRoute(
                            builder: (_) => BookingPage(roomTypeId: widget.roomId, checkIn: checkIn, checkOut: checkOut),
                          )),
                  child: const Text('立即预订'),
                ),
              ),
            ],
          ),
        ),
      ),
      body: ListView(
        children: [
          Container(
            height: 220,
            color: const Color(0xFFDCE7DF),
            child: room!.cover != null
                ? Image.network(room!.cover!, fit: BoxFit.cover, errorBuilder: (_, __, ___) => const Icon(Icons.landscape, size: 70))
                : const Icon(Icons.landscape, size: 70, color: Color(0x6624483C)),
          ),
          Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(room!.name, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                Text('${room!.storeName} · ${room!.area.toStringAsFixed(0)}㎡ · 可住 ${room!.capacity} 人 · ${room!.bedType}',
                    style: const TextStyle(color: Color(0xFF75817A), fontSize: 13)),
                const SizedBox(height: 14),
                Text(room!.intro, style: const TextStyle(height: 1.8, fontSize: 14)),
                const SizedBox(height: 14),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: room!.facilities
                      .map((f) => Chip(label: Text(f, style: const TextStyle(fontSize: 11)), visualDensity: VisualDensity.compact))
                      .toList(),
                ),
                const SizedBox(height: 22),
                const Text('未来 30 天房价', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
                const SizedBox(height: 10),
                GridView.count(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  crossAxisCount: 4,
                  childAspectRatio: 1.9,
                  mainAxisSpacing: 8,
                  crossAxisSpacing: 8,
                  children: calendar
                      .map((d) => Container(
                            decoration: BoxDecoration(
                              color: d.available ? Colors.white : const Color(0xFFF3F2EE),
                              border: Border.all(color: const Color(0xFFE7E4DA)),
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(d.date.substring(5), style: const TextStyle(fontSize: 10, color: Color(0xFF75817A))),
                                Text('¥${money(d.price)}', style: const TextStyle(fontSize: 12, color: Color(0xFF24483C), fontWeight: FontWeight.w600)),
                              ],
                            ),
                          ))
                      .toList(),
                ),
                const SizedBox(height: 22),
                const Text('选择入住日期', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
                const SizedBox(height: 10),
                OutlinedButton.icon(
                  onPressed: pickDates,
                  icon: const Icon(Icons.calendar_today_outlined, size: 18),
                  label: Text('${formatDate(checkIn)}  →  ${formatDate(checkOut)}'),
                ),
                const SizedBox(height: 40),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
