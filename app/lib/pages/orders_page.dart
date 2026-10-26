import 'package:flutter/material.dart';

import '../api_client.dart';

class OrdersPage extends StatefulWidget {
  const OrdersPage({super.key});

  @override
  State<OrdersPage> createState() => _OrdersPageState();
}

class _OrdersPageState extends State<OrdersPage> {
  List<Map<String, dynamic>> orders = [];
  bool loading = true;
  String? error;
  bool needLogin = false;
  int? cancellingId;

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    if (!ApiClient.instance.isLoggedIn) {
      setState(() {
        loading = false;
        needLogin = true;
        error = null;
      });
      return;
    }
    setState(() {
      loading = true;
      error = null;
      needLogin = false;
    });
    try {
      final data = await ApiClient.instance.get('/member/orders');
      if (!mounted) return;
      setState(() {
        orders = (data as List? ?? [])
            .map((e) => Map<String, dynamic>.from(e as Map))
            .toList();
        loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        loading = false;
        error = e.message;
        needLogin = e.code == 401;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        loading = false;
        error = e.toString();
      });
    }
  }

  Future<void> cancel(Map<String, dynamic> order) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('取消订单'),
        content: Text('确定取消订单 ${order['orderNo']} 吗？取消后房间将重新释放。'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('再想想')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('确认取消')),
        ],
      ),
    );
    if (confirmed != true) return;
    setState(() => cancellingId = order['id'] as int?);
    try {
      await ApiClient.instance.post(
        '/member/orders/${order['id']}/cancel',
        body: {'reason': '会员自助取消'},
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('订单已取消')));
      await load();
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      if (mounted) setState(() => cancellingId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('我的订单')),
      body: RefreshIndicator(
        onRefresh: load,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 28),
          children: [
            if (loading)
              const Padding(
                padding: EdgeInsets.only(top: 100),
                child: Center(child: CircularProgressIndicator()),
              ),
            if (!loading && needLogin) _loginHint(),
            if (!loading && !needLogin && error != null) _errorBox(),
            if (!loading && !needLogin && error == null && orders.isEmpty) _emptyBox(),
            if (!loading && !needLogin && error == null)
              ...orders.map((order) => _orderCard(order)),
            if (!loading && !needLogin && orders.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Text(
                  '如需修改入住信息，请联系门店前台。',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.grey.shade600, fontSize: 12),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _loginHint() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          children: [
            const Icon(Icons.lock_outline, size: 52, color: Color(0xFF24483C)),
            const SizedBox(height: 14),
            const Text('登录后查看订单', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
            const SizedBox(height: 8),
            const Text(
              '请前往「我的」使用微信演示登录，登录后可查询订单、积分与优惠券。',
              textAlign: TextAlign.center,
              style: TextStyle(color: Color(0xFF75817A), height: 1.7, fontSize: 13),
            ),
            const SizedBox(height: 18),
            OutlinedButton(onPressed: load, child: const Text('已登录，刷新')),
          ],
        ),
      ),
    );
  }

  Widget _errorBox() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            Text(error!, textAlign: TextAlign.center, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 12),
            OutlinedButton(onPressed: load, child: const Text('重新加载')),
          ],
        ),
      ),
    );
  }

  Widget _emptyBox() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(30),
        child: Column(
          children: [
            const Icon(Icons.receipt_long_outlined, size: 52, color: Color(0x6624483C)),
            const SizedBox(height: 12),
            const Text('还没有订单', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w600)),
            const SizedBox(height: 6),
            const Text('回到首页挑选一间山居，开启下一段旅程。',
                style: TextStyle(color: Color(0xFF75817A), fontSize: 13)),
            const SizedBox(height: 16),
            OutlinedButton(onPressed: load, child: const Text('刷新')),
          ],
        ),
      ),
    );
  }

  Widget _orderCard(Map<String, dynamic> order) {
    final status = order['status']?.toString() ?? '';
    final canCancel = status == 'PENDING' || status == 'CONFIRMED';
    final isCancelling = cancellingId == order['id'];
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    order['orderNo']?.toString() ?? '',
                    style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                  ),
                ),
                _statusTag(status),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              '${order['checkIn'] ?? ''} → ${order['checkOut'] ?? ''} · ${order['nights'] ?? 0} 晚',
              style: const TextStyle(color: Color(0xFF75817A), fontSize: 13),
            ),
            const SizedBox(height: 4),
            Text(
              '入住人 ${order['guestName'] ?? ''}  ${order['guestPhone'] ?? ''}',
              style: const TextStyle(color: Color(0xFF75817A), fontSize: 13),
            ),
            if (order['roomNo'] != null)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text('房间 ${order['roomNo']}', style: const TextStyle(color: Color(0xFF75817A), fontSize: 13)),
              ),
            const Divider(height: 24),
            Row(
              children: [
                Text(
                  '¥${money(order['amount'] as num?)}',
                  style: const TextStyle(color: Color(0xFF24483C), fontSize: 20, fontWeight: FontWeight.w600),
                ),
                const Spacer(),
                if (canCancel)
                  OutlinedButton(
                    onPressed: isCancelling ? null : () => cancel(order),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 14),
                      minimumSize: const Size(0, 36),
                    ),
                    child: Text(isCancelling ? '取消中…' : '取消订单'),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _statusTag(String status) {
    final colors = <String, Color>{
      'PENDING': const Color(0xFFB08A4F),
      'CONFIRMED': const Color(0xFF24483C),
      'CHECKED_IN': const Color(0xFF1F6F4A),
      'CHECKED_OUT': const Color(0xFF75817A),
      'CANCELLED': const Color(0xFF9A9A9A),
      'NO_SHOW': const Color(0xFF9A9A9A),
    };
    final color = colors[status] ?? const Color(0xFF75817A);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(statusText(status), style: TextStyle(color: color, fontSize: 12)),
    );
  }
}
