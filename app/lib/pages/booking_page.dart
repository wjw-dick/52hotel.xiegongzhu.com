import 'package:flutter/material.dart';

import '../api_client.dart';

class BookingPage extends StatefulWidget {
  final int? roomTypeId;
  final DateTime checkIn;
  final DateTime checkOut;
  const BookingPage({super.key, this.roomTypeId, required this.checkIn, required this.checkOut});

  @override
  State<BookingPage> createState() => _BookingPageState();
}

class _BookingPageState extends State<BookingPage> {
  final name = TextEditingController();
  final phone = TextEditingController();
  final idNo = TextEditingController();
  final company = TextEditingController();
  final remark = TextEditingController();
  late DateTime checkIn = widget.checkIn;
  late DateTime checkOut = widget.checkOut;
  bool submitting = false;
  bool success = false;
  String? orderNo;

  @override
  void dispose() {
    name.dispose();
    phone.dispose();
    idNo.dispose();
    company.dispose();
    remark.dispose();
    super.dispose();
  }

  Future<void> pickDates() async {
    final range = await showDateRangePicker(
      context: context,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
      initialDateRange: DateTimeRange(start: checkIn, end: checkOut),
    );
    if (range != null) setState(() {
      checkIn = range.start;
      checkOut = range.end;
    });
  }

  Future<void> submit() async {
    if (widget.roomTypeId == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('请从房型详情进入预订')));
      return;
    }
    if (name.text.trim().isEmpty) return _warn('请填写入住人姓名');
    if (!RegExp(r'^1\d{10}$').hasMatch(phone.text.trim())) return _warn('请填写正确手机号');
    if (idNo.text.trim().length < 15) return _warn('请填写身份证号');
    setState(() => submitting = true);
    try {
      final data = await ApiClient.instance.post(
        ApiClient.instance.publicPath('/orders'),
        headers: {'X-Request-Id': 'app-${DateTime.now().millisecondsSinceEpoch}'},
        body: {
          'roomTypeId': widget.roomTypeId,
          'checkIn': formatDate(checkIn),
          'checkOut': formatDate(checkOut),
          'roomCount': 1,
          'guestName': name.text.trim(),
          'guestPhone': phone.text.trim(),
          'idNo': idNo.text.trim(),
          'company': company.text.trim(),
          'remark': remark.text.trim(),
          'source': 'ONLINE',
          'channel': 'APP',
          'payType': 'ARRIVAL',
        },
      );
      if (!mounted) return;
      setState(() {
        success = true;
        orderNo = data['orderNo']?.toString();
        submitting = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => submitting = false);
      _warn(e.toString());
    }
  }

  void _warn(String text) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('在线预订')),
      body: success
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(28),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.check_circle_outline, size: 70, color: Color(0xFF24483C)),
                    const SizedBox(height: 16),
                    const Text('预订成功', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 10),
                    Text('订单号 $orderNo', style: const TextStyle(color: Color(0xFF24483C), fontSize: 16)),
                    const SizedBox(height: 8),
                    const Text('请保持手机畅通，入住时出示所有入住人身份证件。',
                        textAlign: TextAlign.center, style: TextStyle(color: Color(0xFF75817A), height: 1.7, fontSize: 13)),
                    const SizedBox(height: 22),
                    FilledButton(onPressed: () => Navigator.of(context).popUntil((route) => route.isFirst), child: const Text('返回首页')),
                  ],
                ),
              ),
            )
          : ListView(
              padding: const EdgeInsets.all(18),
              children: [
                const Text('预订信息', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w600)),
                const SizedBox(height: 14),
                OutlinedButton.icon(
                  onPressed: pickDates,
                  icon: const Icon(Icons.calendar_today_outlined, size: 18),
                  label: Text('${formatDate(checkIn)}  →  ${formatDate(checkOut)}'),
                ),
                const SizedBox(height: 22),
                const Text('入住人信息', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w600)),
                const SizedBox(height: 14),
                TextField(controller: name, decoration: const InputDecoration(labelText: '姓名')),
                const SizedBox(height: 12),
                TextField(controller: phone, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: '手机号')),
                const SizedBox(height: 12),
                TextField(controller: idNo, decoration: const InputDecoration(labelText: '身份证号（加密存储）')),
                const SizedBox(height: 12),
                TextField(controller: company, decoration: const InputDecoration(labelText: '协议单位（选填）')),
                const SizedBox(height: 12),
                TextField(controller: remark, maxLines: 3, decoration: const InputDecoration(labelText: '备注')),
                const SizedBox(height: 22),
                const Text('提交即表示同意入住须知与隐私政策，证件信息仅用于入住登记与合规报送。',
                    style: TextStyle(color: Color(0xFF75817A), fontSize: 11, height: 1.7)),
                const SizedBox(height: 20),
                FilledButton(
                  onPressed: submitting ? null : submit,
                  child: Text(submitting ? '提交中…' : '提交预订'),
                ),
              ],
            ),
    );
  }
}
