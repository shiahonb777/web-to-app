#!/bin/bash

################################################################################
# WebToApp Key Server API 简化测试脚本
# 用途：快速测试所有 API 端点
# 使用：./test_api_simple.sh [HOST] [PORT]
################################################################################

HOST=${1:-localhost}
PORT=${2:-8080}
BASE_URL="http://$HOST:$PORT"
APP_ID="com.webtoapp.test.$(date +%s)"

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASSED=0
FAILED=0

test_api() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    
    echo -e "${BLUE}[TEST]${NC} $name"
    
    if [ -z "$data" ]; then
        response=$(curl -s -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json")
    else
        response=$(curl -s -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    fi
    
    # 检查响应
    if echo "$response" | grep -q '"success":true'; then
        echo -e "${GREEN}[✓ PASS]${NC} $name"
        echo "$response" | head -c 200
        echo ""
        ((PASSED++))
    else
        echo -e "${RED}[✗ FAIL]${NC} $name"
        echo "$response" | head -c 200
        echo ""
        ((FAILED++))
    fi
    
    echo ""
}

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║${NC} WebToApp Key Server API 测试"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "服务器: $BASE_URL"
echo "App ID: $APP_ID"
echo ""

# 健康检查
echo -e "${YELLOW}=== 1️⃣  健康检查 ===${NC}"
test_api "健康检查" "GET" "/api/health"

# 生成激活码
echo -e "${YELLOW}=== 2️⃣  生成激活码 ===${NC}"
GEN_DATA=$(cat <<EOF
{
    "app_id": "$APP_ID",
    "count": 3,
    "expires_in_days": 30,
    "max_uses": 10,
    "device_limit": 5
}
EOF
)
test_api "生成 3 个激活码" "POST" "/api/activation/generate" "$GEN_DATA"

# 从响应中提取激活码
GEN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/activation/generate" \
    -H "Content-Type: application/json" \
    -d "$GEN_DATA")

CODE1=$(echo "$GEN_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['codes'][0]['code'] if data.get('codes') else '')" 2>/dev/null)
CODE2=$(echo "$GEN_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['codes'][1]['code'] if len(data.get('codes', [])) > 1 else '')" 2>/dev/null)

echo -e "${YELLOW}Code 1:${NC} $CODE1"
echo -e "${YELLOW}Code 2:${NC} $CODE2"
echo ""

# 验证激活码
echo -e "${YELLOW}=== 3️⃣  验证激活码 ===${NC}"
if [ -n "$CODE1" ]; then
    VERIFY_DATA=$(cat <<EOF
{
    "code": "$CODE1",
    "app_id": "$APP_ID",
    "device_id": "test_device_001",
    "device_info": {
        "device_name": "Test Device",
        "model": "Test",
        "os_version": "13",
        "app_version": "1.0"
    },
    "timestamp": $(date +%s)000
}
EOF
)
    test_api "验证激活码" "POST" "/api/activation/verify" "$VERIFY_DATA"
fi

# 列表查询
echo -e "${YELLOW}=== 4️⃣  列表查询 ===${NC}"
test_api "查询激活码列表" "GET" "/api/activation/list?app_id=$APP_ID&page=1&limit=10"

# 撤销激活码
echo -e "${YELLOW}=== 5️⃣  撤销激活码 ===${NC}"
if [ -n "$CODE2" ]; then
    test_api "撤销激活码" "DELETE" "/api/activation/$APP_ID/$CODE2"
fi

# 多设备验证
echo -e "${YELLOW}=== 6️⃣  多设备支持 ===${NC}"
if [ -n "$CODE1" ]; then
    VERIFY_DATA_2=$(cat <<EOF
{
    "code": "$CODE1",
    "app_id": "$APP_ID",
    "device_id": "test_device_002",
    "device_info": {
        "device_name": "Test Device 2",
        "model": "Test 2",
        "os_version": "14",
        "app_version": "1.1"
    },
    "timestamp": $(date +%s)000
}
EOF
)
    test_api "第二台设备验证" "POST" "/api/activation/verify" "$VERIFY_DATA_2"
fi

# 总结
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║${NC} 测试总结"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "总测试数: $((PASSED + FAILED))"
echo -e "${GREEN}通过: $PASSED${NC}"
echo -e "${RED}失败: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ 所有测试通过！${NC}"
    exit 0
else
    echo -e "${RED}❌ 有 $FAILED 个测试失败${NC}"
    exit 1
fi
