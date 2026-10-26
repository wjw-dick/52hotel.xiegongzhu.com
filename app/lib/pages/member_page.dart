import 'package:flutter/material.dart';

import '../api_client.dart';
import '../config.dart';

class MemberPage extends StatefulWidget {
  const MemberPage({super.key});

  @override
  State<MemberPage> createState() => _MemberPageState();
}

class _MemberPageState extends State<MemberPage> {
  final openId = TextEditingController(text: 'demo-app-001');
  final nickname = TextEditingController(text: '山居客人');
  final phone = TextEditingController(text: '13800000001');
  final name = TextEditingController();
  final profilePhone = TextEditingController();

  Map<String, dynamic>? profile;
  bool loading = false;
  bool logging = false;
  String? error;
  int tab = 0;

  List<Map<String, dynamic>> orders = [];
  List<Map<String, dynamic>> points = [];
  List<Map<String, dynamic>> coupons = [];
  List<Map<String, dynamic>> notifications = [];
  bool tabLoading = false;

  bool get loggedIn => ApiClient.instance.isLoggedIn;

  @override
  void initState() {
    super.initState();
    if (loggedIn) loadProfile();
  }

  @override
  void dispose() {
    openId.dispose();
    nickname.dispose();
    phone.dispose();
    name.dispose();
    profilePhone.dispose();
    super.dispose();
  }

  Future<void> loadProfile() async {
    if (!loggedIn) return;
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final data = await ApiClient.instance.get('/member/profile');
      if (!mounted) return;
      final map = Map<String, dynamic>.from(data as Map);
      final member = Map<String, dynamic>.from(map['member'] as Map? ?? {});
      name.text = member['name']?.toString() ?? '';
      profilePhone.text = member['phone']?.toString() ?? '';
      setState(() {
        profile = map;
        loading = false;
      });
      loadTab(0);
    } on ApiException catch (e) {
      if (e.code == 401) await ApiClient.instance.saveToken(null);
      if (!mounted) return;
      setState(() {
        loading = false;
        error = e.message;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        loading = false;
        error = e.toString();
      });
    }
  }

  Future<void> login() async {
    if (openId.text.trim().isEmpty) {
      _warn('请填写演示 OpenId');
      return;
    }
    setState(() {
      logging = true;
      error = null;
    });
    try {
      final data = await ApiClient.instance.post('/auth/wechat-login', body: {
        'tenantCode': AppConfig.tenantCode,
        'openId': openId.text.trim(),
        'unionId': openId.text.trim(),
        'nickname': nickname.text.trim().isEmpty ? '微信用户' : nickname.text.trim(),
        'phone': phone.text.trim().isEmpty ? null : phone.text.trim(),
      });
      final token = data['token']?.toString();
      if (token == null || token.isEmpty) throw ApiException(-1, '登录失败');
      await ApiClient.instance.saveToken(token);
      if (!mounted) return;
      setState(() => logging = false);
      await loadProfile();
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        logging = false;
        error = e.message;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        logging = false;
        error = e.toString();
      });
    }
  }

  Future<void> logout() async {
    await ApiClient.instance.saveToken(null);
    if (!mounted) return;
    setState(() {
      profile = null;
      orders = [];
      points = [];
      coupons = [];
      notifications = [];
      error = null;
      tab = 0;
    });
  }

  Future<void> loadTab(int index) async {
    setState(() {
      tab = index;
      tabLoading = true;
    });
    try {
      final path = switch (index) {
        0 => '/member/orders',
        1 => '/member/points',
        2 => '/member/coupons',
        _ => '/member/notifications',
      };
      final data = await ApiClient.instance.get(path);
      final rows = (data as List? ?? []).map((e) => Map<String, dynamic>.from(e as Map)).toList();
      if (!mounted) return;
      setState(() {
        if (index == 0) orders = rows;
        if (index == 1) points = rows;
        if (index == 2) coupons = rows;
        if (index == 3) notifications = rows;
        tabLoading = false;
      });
    } on ApiException catch (e) {
      if (e.code == 401) await ApiClient.instance.saveToken(null);
      if (!mounted) return;
      setState(() => tabLoading = false);
      if (e.code == 401) {
        setState(() => profile = null);
        _warn('登录已失效，请重新登录');
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => tabLoading = false);
      _warn(e.toString());
    }
  }

  Future<void> saveProfile() async {
    try {
      await ApiClient.instance.put('/member/profile', body: {
        'name': name.text.trim(),
        'phone': profilePhone.text.trim(),
      });
      if (!mounted) return;
      _warn('资料已保存');
      await loadProfile();
    } catch (e) {
      if (!mounted) return;
      _warn(e.toString());
    }
  }

  void _warn(String text) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('会员中心'),
        actions: [
          if (loggedIn)
            TextButton(
              onPressed: logout,
              child: const Text('退出', style: TextStyle(color: Colors.white)),
            ),
        ],
      ),
      body: profile == null ? _loginView() : _profileView(),
    );
  }

  Widget _loginView() {
    return ListView(
      padding: const EdgeInsets.all(18),
      children: [
        Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFF1C352D), Color(0xFF3C5B4C)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(8),
          ),
          child: const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('YUNQI MEMBER', style: TextStyle(color: Color(0xFFCDBB95), letterSpacing: 3, fontSize: 11)),
              SizedBox(height: 12),
              Text('会员中心', style: TextStyle(color: Colors.white, fontSize: 26, fontWeight: FontWeight.w600)),
              SizedBox(height: 8),
              Text('查看订单、积分、优惠券与门店消息', style: TextStyle(color: Color(0xFFCDD9D1), fontSize: 13)),
            ],
          ),
        ),
        const SizedBox(height: 18),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('微信登录（演示）', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
                const SizedBox(height: 6),
                const Text(
                  '演示环境不跳转微信授权，直接使用 OpenId 模拟登录；正式环境由小程序/公众号静默授权。',
                  style: TextStyle(color: Color(0xFF75817A), fontSize: 12, height: 1.7),
                ),
                const SizedBox(height: 16),
                TextField(controller: openId, decoration: const InputDecoration(labelText: 'OpenId')),
                const SizedBox(height: 12),
                TextField(controller: nickname, decoration: const InputDecoration(labelText: '昵称')),
                const SizedBox(height: 12),
                TextField(
                  controller: phone,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(labelText: '手机号（选填）'),
                ),
                if (error != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 12),
                    child: Text(error!, style: const TextStyle(color: Colors.red, fontSize: 13)),
                  ),
                const SizedBox(height: 18),
                FilledButton(
                  onPressed: logging ? null : login,
                  child: Text(logging ? '登录中…' : '微信一键登录'),
                ),
                const SizedBox(height: 10),
                const Text(
                  '登录即代表同意会员服务协议与隐私政策，演示数据仅用于功能体验。',
                  style: TextStyle(color: Color(0xFF75817A), fontSize: 11, height: 1.7),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _profileView() {
    if (loading) return const Center(child: CircularProgressIndicator());
    final member = Map<String, dynamic>.from(profile?['member'] as Map? ?? {});
    final level = Map<String, dynamic>.from(profile?['level'] as Map? ?? {});
    return RefreshIndicator(
      onRefresh: loadProfile,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF1C352D), Color(0xFF3C5B4C)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    CircleAvatar(
                      radius: 26,
                      backgroundColor: const Color(0xFFCDBB95),
                      child: Text(
                        (member['name']?.toString().isNotEmpty == true ? member['name'].toString().substring(0, 1) : 'U'),
                        style: const TextStyle(color: Color(0xFF24483C), fontSize: 20, fontWeight: FontWeight.w600),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            member['name']?.toString() ?? '微信用户',
                            style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w600),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            '${member['phone'] ?? '未绑定手机号'} · ${level['name'] ?? '普通会员'}',
                            style: const TextStyle(color: Color(0xFFCDD9D1), fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                Row(
                  children: [
                    _stat('${member['points'] ?? 0}', '可用积分'),
                    _stat('¥${money(member['balance'] as num?)}', '储值余额'),
                    _stat('${profile?['couponCount'] ?? 0}', '优惠券'),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('会员权益', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  Text(
                    level['benefits']?.toString() ?? '积分累积，会员专享房价',
                    style: const TextStyle(color: Color(0xFF75817A), fontSize: 13, height: 1.7),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),
          Card(
            child: Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
                  child: Row(
                    children: [
                      _tabButton('订单', 0),
                      _tabButton('积分', 1),
                      _tabButton('优惠券', 2),
                      _tabButton('消息', 3),
                    ],
                  ),
                ),
                const Divider(height: 1),
                if (tabLoading)
                  const Padding(padding: EdgeInsets.all(30), child: CircularProgressIndicator())
                else
                  Padding(
                    padding: const EdgeInsets.all(14),
                    child: _tabContent(),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('个人资料', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 12),
                  TextField(controller: name, decoration: const InputDecoration(labelText: '昵称')),
                  const SizedBox(height: 12),
                  TextField(
                    controller: profilePhone,
                    keyboardType: TextInputType.phone,
                    decoration: const InputDecoration(labelText: '手机号'),
                  ),
                  const SizedBox(height: 14),
                  Align(
                    alignment: Alignment.centerRight,
                    child: FilledButton(
                      onPressed: saveProfile,
                      style: FilledButton.styleFrom(minimumSize: const Size(0, 42), padding: const EdgeInsets.symmetric(horizontal: 22)),
                      child: const Text('保存资料'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _stat(String value, String label) {
    return Expanded(
      child: Column(
        children: [
          Text(value, style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text(label, style: const TextStyle(color: Color(0xFFCDD9D1), fontSize: 11)),
        ],
      ),
    );
  }

  Widget _tabButton(String label, int index) {
    final active = tab == index;
    return Expanded(
      child: TextButton(
        onPressed: tabLoading ? null : () => loadTab(index),
        style: TextButton.styleFrom(
          foregroundColor: active ? const Color(0xFF24483C) : const Color(0xFF75817A),
          shape: const RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(4))),
        ),
        child: Text(label, style: TextStyle(fontWeight: active ? FontWeight.w600 : FontWeight.normal)),
      ),
    );
  }

  Widget _tabContent() {
    if (tab == 0) {
      if (orders.isEmpty) return const _EmptyText('还没有订单，回到首页预订一间吧');
      return Column(children: orders.map((o) => _orderRow(o)).toList());
    }
    if (tab == 1) {
      if (points.isEmpty) return const _EmptyText('暂无积分记录');
      return Column(
        children: points.map((p) {
          final value = p['changeValue'] as num? ?? 0;
          return ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Icon(value >= 0 ? Icons.add_circle_outline : Icons.remove_circle_outline,
                color: value >= 0 ? const Color(0xFF24483C) : Colors.redAccent),
            title: Text('${value >= 0 ? '+' : ''}$value 积分'),
            subtitle: Text(p['reason']?.toString() ?? ''),
          );
        }).toList(),
      );
    }
    if (tab == 2) {
      if (coupons.isEmpty) return const _EmptyText('暂无优惠券');
      return Column(
        children: coupons.map((c) {
          return Card(
            margin: const EdgeInsets.only(bottom: 10),
            color: const Color(0xFFF3F7F4),
            child: ListTile(
              title: Text(c['name']?.toString() ?? '优惠券'),
              subtitle: Text('有效期至 ${c['expireDate'] ?? '长期'}'),
              trailing: Text(
                '¥${money(c['amount'] as num?)}',
                style: const TextStyle(color: Color(0xFFB08A4F), fontSize: 18, fontWeight: FontWeight.w600),
              ),
            ),
          );
        }).toList(),
      );
    }
    if (notifications.isEmpty) return const _EmptyText('暂无消息');
    return Column(
      children: notifications.map((n) {
        return ListTile(
          contentPadding: EdgeInsets.zero,
          title: Text(n['title']?.toString() ?? ''),
          subtitle: Text(n['content']?.toString() ?? ''),
        );
      }).toList(),
    );
  }

  Widget _orderRow(Map<String, dynamic> o) {
    final status = o['status']?.toString() ?? '';
    final canCancel = status == 'PENDING' || status == 'CONFIRMED';
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(o['orderNo']?.toString() ?? '', style: const TextStyle(fontWeight: FontWeight.w600)),
              ),
              Text(statusText(status), style: const TextStyle(color: Color(0xFFB08A4F), fontSize: 12)),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            '${o['checkIn'] ?? ''} → ${o['checkOut'] ?? ''} · ${o['nights'] ?? 0} 晚',
            style: const TextStyle(color: Color(0xFF75817A), fontSize: 12),
          ),
          Row(
            children: [
              Text('¥${money(o['amount'] as num?)}', style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
              const Spacer(),
              if (canCancel)
                TextButton(
                  onPressed: () async {
                    try {
                      await ApiClient.instance.post('/member/orders/${o['id']}/cancel', body: {'reason': '会员自助取消'});
                      if (!mounted) return;
                      _warn('订单已取消');
                      await loadTab(0);
                    } catch (e) {
                      if (!mounted) return;
                      _warn(e.toString());
                    }
                  },
                  child: const Text('取消订单'),
                ),
            ],
          ),
          const Divider(height: 4),
        ],
      ),
    );
  }
}

class _EmptyText extends StatelessWidget {
  final String text;
  const _EmptyText(this.text);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 24),
      child: Text(text, textAlign: TextAlign.center, style: const TextStyle(color: Color(0xFF75817A), fontSize: 13)),
    );
  }
}
