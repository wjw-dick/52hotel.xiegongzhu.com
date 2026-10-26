#!/usr/bin/env bash
# 演示环境 API 冒烟测试（会创建订单/房间/会员等测试数据，请勿在生产库执行）
# 用法：BASE=http://127.0.0.1:19090/api bash scripts/test_api.sh
set -u
BASE="${BASE:-http://127.0.0.1:19090/api}"
pass=0; fail=0
j() { python3 -c "import sys,json;d=json.load(sys.stdin);print(eval(sys.argv[1]))" "$1"; }
call() { # method path token data expect_code
  local method=$1 path=$2 token=$3 data=${4:-} expected=${6:-${5:-0}}
  local args=(-s --max-time 20 -X "$method" "$BASE$path" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-d "$data")
  local out; out=$(curl "${args[@]}")
  local code; code=$(printf '%s' "$out" | python3 -c 'import sys,json;
try: print(json.load(sys.stdin).get("code"))
except Exception: print("BAD")' 2>/dev/null)
  if [ "$code" = "$expected" ]; then
    pass=$((pass+1)); echo "PASS [$method $path] code=$code"
  else
    fail=$((fail+1)); echo "FAIL [$method $path] expected=$expected code=$code body=$(printf '%s' "$out" | head -c 300)"
  fi
  printf '%s' "$out" > /tmp/api_last.json
}
login() { curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "$1"; }
PLAT=$(login '{"username":"admin","password":"admin123"}')
A=$(login '{"username":"admin","password":"admin123","tenantCode":"YQ-HZ"}')
B=$(login '{"username":"admin","password":"admin123","tenantCode":"XH-SH"}')
PT=$(printf '%s' "$PLAT" | j 'd["data"]["token"]')
AT=$(printf '%s' "$A" | j 'd["data"]["token"]')
BT=$(printf '%s' "$B" | j 'd["data"]["token"]')
echo "tokens loaded"

call GET '/health' '' ''
call GET '/public/YQ-HZ/home' '' ''
call GET '/public/YQ-HZ/stores/1' '' ''
call GET '/public/YQ-HZ/room-types/1' '' ''
call GET '/public/YQ-HZ/availability?roomTypeId=4&checkIn=2026-11-10&checkOut=2026-11-12' '' ''
call GET '/public/YQ-HZ/menus' '' ''
call GET '/public/YQ-HZ/i18n?lang=zh-CN' '' ''

# Public order + duplicate guard + query
PUB_ID="pub-$(date +%s%N)"
call POST '/public/YQ-HZ/orders' '' "{\"roomTypeId\":4,\"checkIn\":\"2026-11-10\",\"checkOut\":\"2026-11-12\",\"roomCount\":1,\"guestName\":\"公网测试客\",\"guestPhone\":\"13800009999\",\"idNo\":\"330106199001011234\",\"source\":\"ONLINE\",\"channel\":\"H5\",\"payType\":\"ARRIVAL\"}" 0
PUB_NO=$(j 'd["data"]["orderNo"]' < /tmp/api_last.json)
PUB_OID=$(j 'd["data"]["id"]' < /tmp/api_last.json)
echo "public order=$PUB_NO id=$PUB_OID"
curl -s -X POST "$BASE/public/YQ-HZ/orders" -H 'Content-Type: application/json' -H "X-Request-Id: $PUB_ID" -d "{\"roomTypeId\":4,\"checkIn\":\"2026-11-14\",\"checkOut\":\"2026-11-15\",\"guestName\":\"防重测试\",\"guestPhone\":\"13800009998\",\"idNo\":\"330106199001011234\"}" >/dev/null
out=$(curl -s -X POST "$BASE/public/YQ-HZ/orders" -H 'Content-Type: application/json' -H "X-Request-Id: $PUB_ID" -d "{\"roomTypeId\":4,\"checkIn\":\"2026-11-16\",\"checkOut\":\"2026-11-17\",\"guestName\":\"防重测试2\",\"guestPhone\":\"13800009997\",\"idNo\":\"330106199001011234\"}")
code=$(printf '%s' "$out" | j 'd["code"]')
if [ "$code" = 429 ]; then pass=$((pass+1)); echo "PASS duplicate request id -> 429"; else fail=$((fail+1)); echo "FAIL duplicate request id -> $code"; fi
call GET "/public/YQ-HZ/orders/query?orderNo=$PUB_NO&phone=13800009999" '' ''
call GET "/public/YQ-HZ/orders/query?orderNo=$PUB_NO&phone=13800000000" '' '' 403

# Tenant dashboards / masters
call GET '/admin/dashboard' "$AT" ''
call GET '/admin/stores' "$AT" ''
call GET '/admin/room-types' "$AT" ''
call GET '/admin/rooms' "$AT" ''
call GET '/admin/rates?roomTypeId=1&month=2026-11' "$AT" ''
call GET '/admin/rates/logs' "$AT" ''
call POST '/admin/rates/batch' "$AT" '{"roomTypeId":1,"startDate":"2026-12-01","endDate":"2026-12-03","priceType":"BAR","price":777}' ''
call GET '/admin/rates?roomTypeId=1&month=2026-12' "$AT" ''
call POST '/admin/rooms' "$AT" '{"storeId":1,"roomTypeId":1,"floor":9,"roomNo":"T999","status":"VACANT","remark":"自动化测试临时房"}' ''
TMP_ROOM=$(j 'd["data"]["id"]' < /tmp/api_last.json)
call PUT "/admin/rooms/$TMP_ROOM" "$AT" '{"storeId":1,"roomTypeId":1,"floor":9,"roomNo":"T999","status":"VACANT","remark":"自动化测试更新"}' ''
call POST "/admin/rooms/$TMP_ROOM/status" "$AT" '{"status":"DIRTY","remark":"测试置脏"}' ''
call GET "/admin/rooms/$TMP_ROOM/status-logs" "$AT" ''
call DELETE "/admin/rooms/$TMP_ROOM" "$AT" ''
# CSV export checked separately

# Order lifecycle
call POST '/admin/orders' "$AT" '{"storeId":2,"roomTypeId":4,"checkIn":"2026-11-20","checkOut":"2026-11-22","guestName":"流程测试客","guestPhone":"13800008888","idNo":"330106199001011234","source":"WALK_IN","payType":"ARRIVAL","remark":"自动化流程"}' ''
OID=$(j 'd["data"]["id"]' < /tmp/api_last.json)
ONO=$(j 'd["data"]["orderNo"]' < /tmp/api_last.json)
echo "admin order=$ONO id=$OID"
call GET "/admin/orders/$OID" "$AT" ''
call GET '/admin/orders?page=1&size=5&keyword=%E6%B5%81%E7%A8%8B%E6%B5%8B%E8%AF%95%E5%AE%A2' "$AT" ''
call GET '/admin/orders/availability?roomTypeId=4&checkIn=2026-11-20&checkOut=2026-11-22' "$AT" ''
call POST "/admin/orders/$OID/checkin" "$AT" '{"guestName":"流程测试客","idNo":"330106199001011234","deposit":300,"depositPayType":"WECHAT","minorInfo":""}' ''
call GET '/admin/checkins?status=IN_HOUSE' "$AT" ''
call POST "/admin/orders/$OID/deposit" "$AT" '{"amount":100,"type":"COLLECT","payType":"CASH"}' ''
call POST "/admin/orders/$OID/checkout" "$AT" '{"depositDeduct":100,"discount":20,"otherFee":50,"payType":"CASH"}' ''
call GET '/admin/finance/deposits' "$AT" ''
call GET '/admin/finance/settlements' "$AT" ''
call GET '/admin/finance/overview' "$AT" ''
call GET '/admin/hk/overview' "$AT" ''
call GET '/admin/hk/tasks' "$AT" ''
call GET '/admin/police-reports' "$AT" ''

# Cancel a second order and release
call POST '/admin/orders' "$AT" '{"storeId":1,"roomTypeId":2,"checkIn":"2026-11-25","checkOut":"2026-11-26","guestName":"取消测试","guestPhone":"13800007777","idNo":"330106199001011234","source":"WALK_IN"}' ''
COID=$(j 'd["data"]["id"]' < /tmp/api_last.json)
call POST "/admin/orders/$COID/cancel" "$AT" '{"reason":"自动化取消测试"}' ''

# Housekeeping task
call POST '/admin/hk/tasks' "$AT" '{"roomId":1,"type":"CHECKOUT","assignee":"house","note":"自动化清扫"}' ''
HID=$(j 'd["data"]["id"]' < /tmp/api_last.json)
call POST "/admin/hk/tasks/$HID/status" "$AT" '{"status":"DONE","markClean":true}' ''
call POST '/admin/hk/repairs' "$AT" '{"roomId":2,"title":"测试报修"}' ''
call GET '/admin/hk/repairs' "$AT" ''
call POST '/admin/hk/linen' "$AT" '{"storeId":1,"type":"SEND","item":"床单","quantity":10}' ''
call POST '/admin/hk/complaints' "$AT" '{"storeId":1,"guest":"测试客","content":"测试投诉"}' ''

# Finance AR + shift
call POST '/admin/finance/ar' "$AT" '{"company":"测试协议公司","contact":"张三","phone":"13800006666","creditLimit":5000}' ''
ARID=$(j 'd["data"]["id"]' < /tmp/api_last.json)
call GET '/admin/finance/ar' "$AT" ''
call POST '/admin/finance/ar' "$AT" '{"company":"测试协议公司2","contact":"李四","phone":"13800005555","creditLimit":3000}' ''
call GET '/admin/finance/shifts' "$AT" ''
call POST '/admin/finance/shifts/open' "$AT" '{"storeId":1,"cashAmount":1000}' '' 400
call GET '/admin/finance/shifts' "$AT" ''

# Members
call GET '/admin/members' "$AT" ''
call GET '/admin/members/levels' "$AT" ''
MEM_PHONE="139$(printf '%08d' $((RANDOM%100000000)))"
call POST '/admin/members' "$AT" "{\"name\":\"自动会员\",\"phone\":\"$MEM_PHONE\",\"gender\":\"U\",\"tags\":\"测试\"}" ''
MID=$(j 'd["data"]["id"]' < /tmp/api_last.json)
call GET "/admin/members/$MID/detail" "$AT" ''
call POST "/admin/members/$MID/points" "$AT" '{"changeValue":50,"reason":"自动化测试积分"}' ''
call POST "/admin/members/$MID/coupons" "$AT" '{"name":"测试券","amount":50,"expireDate":"2026-12-31"}' ''
call POST '/admin/members/levels/refresh' "$AT" ''

# Reports / config / users / audit
call GET '/admin/reports/summary?startDate=2026-10-01&endDate=2026-10-31' "$AT" ''
# CSV export checked separately
call GET '/admin/params' "$AT" ''
call GET '/admin/tenant-config' "$AT" ''
call PUT '/admin/tenant-config' "$AT" '{"tenant_name":"云栖民宿（杭州）","slogan":"山水之间，安心住下","service_phone":"0571-88886666"}' ''
call GET '/admin/languages' "$AT" ''
call GET '/admin/i18n' "$AT" ''
call GET '/admin/menus' "$AT" ''
call GET '/admin/users' "$AT" ''
call GET '/admin/roles' "$AT" ''
call GET '/admin/audits' "$AT" ''
call GET '/admin/wechat' "$AT" ''
call GET '/admin/notify-templates' "$AT" ''
call GET '/admin/notifications' "$AT" ''
call GET '/admin/storage' "$AT" ''
call GET '/admin/dicts' "$AT" ''
call GET '/admin/enhance/channels' "$AT" ''
call GET '/admin/enhance/dining' "$AT" ''

# Member portal
WL=$(curl -s -X POST "$BASE/auth/wechat-login" -H 'Content-Type: application/json' -d '{"tenantCode":"YQ-HZ","openId":"auto-test-member-001","nickname":"自动会员","phone":"13900002222"}')
MT=$(printf '%s' "$WL" | j 'd["data"]["token"]')
call GET '/member/profile' "$MT" ''
call GET '/member/orders' "$MT" ''
call GET '/member/points' "$MT" ''
call GET '/member/coupons' "$MT" ''
call GET '/member/notifications' "$MT" ''
call PUT '/member/profile' "$MT" '{"name":"自动会员改","phone":"13900002222","gender":"F"}' ''
call POST '/member/password' "$MT" '{"code":"002222","newPassword":"newpass123"}' ''

# Isolation
call GET '/admin/stores' "$BT" ''
call GET '/admin/orders/6' "$AT" '' 403
call GET '/admin/orders/6' "$PT" '' 400
# platform drill needs header; test separately
DRILL=$(curl -s "$BASE/admin/orders?page=1&size=20" -H "Authorization: Bearer $PT" -H 'X-Tenant-Id: 2')
DCODE=$(printf '%s' "$DRILL" | j 'd["code"]')
DTOTAL=$(printf '%s' "$DRILL" | j 'd["data"]["total"]')
if [ "$DCODE" = 0 ] && [ "$DTOTAL" -ge 5 ]; then pass=$((pass+1)); echo "PASS platform drill tenant 2 orders total=$DTOTAL"; else fail=$((fail+1)); echo "FAIL platform drill code=$DCODE total=$DTOTAL"; fi
call GET '/platform/dashboard' "$PT" ''
call GET '/platform/tenants' "$PT" ''
call GET '/platform/tenants/options' "$PT" ''
call GET '/platform/dicts' "$PT" ''
call GET '/platform/audits' "$PT" ''

echo "RESULT pass=$pass fail=$fail"
