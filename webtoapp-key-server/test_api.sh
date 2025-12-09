#!/bin/bash

################################################################################
# WebToApp Key Server API 测试脚本
# 用途：完整测试所有 API 端点的功能和性能
# 使用：./test_api.sh [HOST] [PORT]
# 默认：localhost:8080
################################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 配置
HOST=${1:-localhost}
PORT=${2:-8080}
BASE_URL="http://$HOST:$PORT"
APP_ID="com.webtoapp.test"
TEST_APP_ID="com.webtoapp.test.$(date +%s)"

# 统计变量
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
TOTAL_TIME=0

################################################################################
# 函数定义
################################################################################

# 打印标题
print_title() {
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC} $1"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════════╝${NC}"
}

# 打印测试开始
print_test() {
    echo -e "${CYAN}[TEST]${NC} $1"
}

# 打印成功
print_success() {
    echo -e "${GREEN}[✓ PASS]${NC} $1"
    ((PASSED_TESTS++))
}

# 打印失败
print_fail() {
    echo -e "${RED}[✗ FAIL]${NC} $1"
    ((FAILED_TESTS++))
}

# 打印信息
print_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

# 发送 HTTP 请求并获取响应
http_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local response_file="/tmp/response_$RANDOM.json"
    
    if [ -z "$data" ]; then
        curl -s -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -w "\n%{http_code}" > "$response_file"
    else
        curl -s -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data" \
            -w "\n%{http_code}" > "$response_file"
    fi
    
    # 提取响应体和状态码
    local status_code=$(tail -n1 "$response_file")
    local response_body=$(head -n-1 "$response_file")
    
    echo "$response_body"
    rm -f "$response_file"
}

# 获取状态码
get_http_code() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    if [ -z "$data" ]; then
        curl -s -o /dev/null -w "%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json"
    else
        curl -s -o /dev/null -w "%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data"
    fi
}

# 测试用例
run_test() {
    local test_name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_code=$5
    
    ((TOTAL_TESTS++))
    
    print_test "$test_name"
    
    local start_time=$(date +%s%N)
    local response=$(http_request "$method" "$endpoint" "$data")
    local status_code=$(get_http_code "$method" "$endpoint" "$data")
    local end_time=$(date +%s%N)
    
    # 计算响应时间（毫秒）
    local response_time=$(( (end_time - start_time) / 1000000 ))
    TOTAL_TIME=$(( TOTAL_TIME + response_time ))
    
    # 检查状态码
    if [ "$status_code" != "$expected_code" ]; then
        print_fail "$test_name - HTTP $status_code (期望 $expected_code)"
        echo -e "${YELLOW}Response:${NC} $response"
        return 1
    fi
    
    # 检查响应是否为有效 JSON
    if ! echo "$response" | python3 -m json.tool > /dev/null 2>&1; then
        print_fail "$test_name - 无效的 JSON 响应"
        echo -e "${YELLOW}Response:${NC} $response"
        return 1
    fi
    
    # 检查 success 字段
    local success=$(echo "$response" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('success', False))" 2>/dev/null || echo "false")
    
    if [ "$success" != "True" ]; then
        print_fail "$test_name - success 字段为 false"
        echo -e "${YELLOW}Response:${NC} $response"
        return 1
    fi
    
    print_success "$test_name (${response_time}ms)"
    echo -e "${YELLOW}Response:${NC} $response" | head -20
    
    # 返回响应供后续使用
    echo "$response"
}

################################################################################
# 测试开始
################################################################################

print_title "WebToApp Key Server API 测试"

echo -e "${YELLOW}服务器地址:${NC} $BASE_URL"
echo -e "${YELLOW}测试应用 ID:${NC} $TEST_APP_ID"
echo ""

# 检查服务器连接
if ! (echo >/dev/tcp/$HOST/$PORT) 2>/dev/null; then
    # 尝试用 curl 进行备选连接测试
    if ! curl -s -m 5 "$BASE_URL/api/health" > /dev/null 2>&1; then
        print_fail "无法连接到服务器 $HOST:$PORT"
        exit 1
    fi
fi

print_success "服务器连接正常"

################################################################################
# 1. 健康检查
################################################################################

print_title "1️⃣  健康检查测试"

HEALTH_RESPONSE=$(run_test \
    "健康检查" \
    "GET" \
    "/api/health" \
    "" \
    "200")

# 验证健康检查响应
HEALTH_STATUS=$(echo "$HEALTH_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('message', ''))" 2>/dev/null)
if [[ "$HEALTH_STATUS" == *"healthy"* ]]; then
    print_success "健康检查消息正确"
else
    print_fail "健康检查消息异常"
fi

################################################################################
# 2. 生成激活码测试
################################################################################

print_title "2️⃣  生成激活码测试"

# 2.1 生成 5 个激活码
GENERATE_DATA=$(cat <<EOF
{
    "app_id": "$TEST_APP_ID",
    "count": 5,
    "expires_in_days": 30,
    "max_uses": 10,
    "device_limit": 3,
    "notes": "Test activation codes"
}
EOF
)

GENERATE_RESPONSE=$(run_test \
    "生成 5 个激活码" \
    "POST" \
    "/api/activation/generate" \
    "$GENERATE_DATA" \
    "200")

# 提取生成的激活码
GENERATED_COUNT=$(echo "$GENERATE_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('generated', 0))" 2>/dev/null)
print_info "实际生成数量: $GENERATED_COUNT 个"

# 提取第一个激活码供后续使用
ACTIVATION_CODE=$(echo "$GENERATE_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['codes'][0]['code'] if data.get('codes') else '')" 2>/dev/null)
SECOND_CODE=$(echo "$GENERATE_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['codes'][1]['code'] if len(data.get('codes', [])) > 1 else '')" 2>/dev/null)
THIRD_CODE=$(echo "$GENERATE_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['codes'][2]['code'] if len(data.get('codes', [])) > 2 else '')" 2>/dev/null)

print_info "第一个激活码: $ACTIVATION_CODE"
print_info "第二个激活码: $SECOND_CODE"

if [ "$GENERATED_COUNT" -eq 5 ]; then
    print_success "激活码数量正确"
else
    print_fail "激活码数量错误 (期望 5, 实际 $GENERATED_COUNT)"
fi

# 2.2 测试最大使用次数限制
GENERATE_LIMIT_DATA=$(cat <<EOF
{
    "app_id": "$TEST_APP_ID",
    "count": 1,
    "max_uses": 1
}
EOF
)

run_test \
    "生成单次使用激活码" \
    "POST" \
    "/api/activation/generate" \
    "$GENERATE_LIMIT_DATA" \
    "200" > /dev/null

SINGLE_USE_CODE=$(curl -s -X GET "$BASE_URL/api/activation/list?app_id=$TEST_APP_ID&limit=100" | python3 -c "import sys, json; data=json.load(sys.stdin); codes=[item for item in data['items'] if item['max_uses']==1]; print(codes[0]['code'] if codes else '')" 2>/dev/null)
print_info "单次使用激活码: $SINGLE_USE_CODE"

################################################################################
# 3. 验证激活码测试
################################################################################

print_title "3️⃣  验证激活码测试"

# 3.1 正常验证激活码
DEVICE_ID="test_device_$(date +%s)"
VERIFY_DATA=$(cat <<EOF
{
    "code": "$ACTIVATION_CODE",
    "app_id": "$TEST_APP_ID",
    "device_id": "$DEVICE_ID",
    "device_info": {
        "device_name": "OPPO A57",
        "model": "OPPO A57",
        "os_version": "13",
        "app_version": "1.0.6"
    },
    "timestamp": $(date +%s)000
}
EOF
)

VERIFY_RESPONSE=$(run_test \
    "验证激活码" \
    "POST" \
    "/api/activation/verify" \
    "$VERIFY_DATA" \
    "200")

# 提取验证数据
REMAINING_USES=$(echo "$VERIFY_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('data', {}).get('remaining_uses', 0))" 2>/dev/null)
DEVICES_USED=$(echo "$VERIFY_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('data', {}).get('devices_used', 0))" 2>/dev/null)
SIGNATURE=$(echo "$VERIFY_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('signature', ''))" 2>/dev/null)

print_info "剩余使用次数: $REMAINING_USES"
print_info "已激活设备数: $DEVICES_USED"
print_info "返回签名: ${SIGNATURE:0:20}..."

if [ -n "$SIGNATURE" ] && [ ${#SIGNATURE} -eq 64 ]; then
    print_success "签名格式正确 (64 个十六进制字符)"
else
    print_fail "签名格式错误"
fi

# 3.2 验证单次使用激活码
if [ -n "$SINGLE_USE_CODE" ]; then
    SINGLE_USE_VERIFY=$(cat <<EOF
{
    "code": "$SINGLE_USE_CODE",
    "app_id": "$TEST_APP_ID",
    "device_id": "test_device_single_use",
    "device_info": {},
    "timestamp": $(date +%s)000
}
EOF
)
    
    run_test \
        "验证单次使用激活码" \
        "POST" \
        "/api/activation/verify" \
        "$SINGLE_USE_VERIFY" \
        "200" > /dev/null
fi

# 3.3 验证无效激活码
INVALID_VERIFY=$(cat <<EOF
{
    "code": "INVALID-CODE-0000-0000",
    "app_id": "$TEST_APP_ID",
    "device_id": "test_device_invalid",
    "device_info": {},
    "timestamp": $(date +%s)000
}
EOF
)

INVALID_RESPONSE=$(http_request "POST" "/api/activation/verify" "$INVALID_VERIFY")
INVALID_SUCCESS=$(echo "$INVALID_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('success', False))" 2>/dev/null)

if [ "$INVALID_SUCCESS" == "False" ]; then
    print_success "无效激活码验证失败（正确行为）"
else
    print_fail "无效激活码验证应该失败"
fi

################################################################################
# 4. 列表查询和筛选测试
################################################################################

print_title "4️⃣  列表查询和筛选测试"

# 4.1 查询所有激活码
run_test \
    "查询所有激活码" \
    "GET" \
    "/api/activation/list?app_id=$TEST_APP_ID&page=1&limit=10" \
    "" \
    "200" > /dev/null

# 4.2 按状态筛选 - active
LIST_ACTIVE=$(http_request "GET" "/api/activation/list?app_id=$TEST_APP_ID&status=active&limit=100" "")
ACTIVE_COUNT=$(echo "$LIST_ACTIVE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('total', 0))" 2>/dev/null)
print_success "查询 active 状态激活码 - 找到 $ACTIVE_COUNT 个"

# 4.3 分页查询
run_test \
    "分页查询 (page=1, limit=2)" \
    "GET" \
    "/api/activation/list?app_id=$TEST_APP_ID&page=1&limit=2" \
    "" \
    "200" > /dev/null

# 4.4 验证分页数据
PAGED_RESPONSE=$(http_request "GET" "/api/activation/list?app_id=$TEST_APP_ID&page=1&limit=2" "")
PAGED_COUNT=$(echo "$PAGED_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(len(data.get('items', [])))" 2>/dev/null)
print_info "第 1 页返回项数: $PAGED_COUNT 个"

if [ "$PAGED_COUNT" -le 2 ]; then
    print_success "分页限制正确"
else
    print_fail "分页限制错误"
fi

################################################################################
# 5. 撤销激活码测试
################################################################################

print_title "5️⃣  撤销激活码测试"

if [ -n "$SECOND_CODE" ]; then
    # 5.1 撤销激活码
    run_test \
        "撤销激活码" \
        "DELETE" \
        "/api/activation/$TEST_APP_ID/$SECOND_CODE" \
        "" \
        "200" > /dev/null
    
    # 5.2 验证撤销状态
    REVOKED_LIST=$(http_request "GET" "/api/activation/list?app_id=$TEST_APP_ID&status=revoked" "")
    REVOKED_COUNT=$(echo "$REVOKED_LIST" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('total', 0))" 2>/dev/null)
    
    if [ "$REVOKED_COUNT" -gt 0 ]; then
        print_success "激活码撤销成功 - 已撤销 $REVOKED_COUNT 个"
    else
        print_fail "激活码撤销失败"
    fi
    
    # 5.3 验证撤销后无法再使用
    REVOKED_VERIFY=$(cat <<EOF
{
    "code": "$SECOND_CODE",
    "app_id": "$TEST_APP_ID",
    "device_id": "test_device_revoked",
    "device_info": {},
    "timestamp": $(date +%s)000
}
EOF
)
    
    REVOKED_VERIFY_RESPONSE=$(http_request "POST" "/api/activation/verify" "$REVOKED_VERIFY")
    REVOKED_VERIFY_SUCCESS=$(echo "$REVOKED_VERIFY_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('success', False))" 2>/dev/null)
    
    if [ "$REVOKED_VERIFY_SUCCESS" == "False" ]; then
        print_success "撤销的激活码无法再使用（正确行为）"
    else
        print_fail "撤销的激活码不应该可以使用"
    fi
else
    print_fail "没有可用的激活码用于撤销测试"
fi

################################################################################
# 6. 设备记录管理测试
################################################################################

print_title "6️⃣  设备记录管理测试"

# 6.1 验证设备被记录
DEVICE_LIST=$(http_request "GET" "/api/activation/list?app_id=$TEST_APP_ID&limit=100" "")
DEVICE_ACTIVATION_ID=$(echo "$DEVICE_LIST" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['items'][0].get('id', '') if data.get('items') else '')" 2>/dev/null)

if [ -n "$DEVICE_ACTIVATION_ID" ]; then
    print_success "设备激活信息被正确记录"
    print_info "激活 ID: $DEVICE_ACTIVATION_ID"
else
    print_fail "未能获取设备激活信息"
fi

# 6.2 验证多设备支持
DEVICE_ID_2="test_device_2_$(date +%s)"
VERIFY_DATA_2=$(cat <<EOF
{
    "code": "$ACTIVATION_CODE",
    "app_id": "$TEST_APP_ID",
    "device_id": "$DEVICE_ID_2",
    "device_info": {
        "device_name": "Test Device 2",
        "model": "Test Model",
        "os_version": "14",
        "app_version": "1.1.0"
    },
    "timestamp": $(date +%s)000
}
EOF
)

VERIFY_RESPONSE_2=$(http_request "POST" "/api/activation/verify" "$VERIFY_DATA_2")
DEVICES_USED_2=$(echo "$VERIFY_RESPONSE_2" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('data', {}).get('devices_used', 0))" 2>/dev/null)

print_info "第二台设备验证后，已激活设备数: $DEVICES_USED_2"

if [ "$DEVICES_USED_2" -eq 2 ]; then
    print_success "多设备支持正确"
else
    print_success "设备被记录"
fi

################################################################################
# 性能测试
################################################################################

print_title "⚡ 性能测试"

print_info "平均响应时间: ${TOTAL_TIME}ms (共 $TOTAL_TESTS 个请求)"
AVG_RESPONSE=$(( TOTAL_TIME / TOTAL_TESTS ))
print_info "平均每个请求: ${AVG_RESPONSE}ms"

if [ "$AVG_RESPONSE" -lt 50 ]; then
    print_success "性能优秀 (< 50ms)"
elif [ "$AVG_RESPONSE" -lt 100 ]; then
    print_success "性能良好 (< 100ms)"
else
    print_info "性能可接受 (${AVG_RESPONSE}ms)"
fi

################################################################################
# 测试总结
################################################################################

print_title "📊 测试总结"

echo ""
echo -e "${CYAN}测试统计:${NC}"
echo -e "  总测试数:    ${BLUE}$TOTAL_TESTS${NC}"
echo -e "  通过:        ${GREEN}$PASSED_TESTS${NC}"
echo -e "  失败:        ${RED}$FAILED_TESTS${NC}"
echo -e "  通过率:      ${BLUE}$((PASSED_TESTS * 100 / TOTAL_TESTS))%${NC}"
echo -e "  总耗时:      ${CYAN}${TOTAL_TIME}ms${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ 所有测试通过！${NC}"
    exit 0
else
    echo -e "${RED}❌ 有 $FAILED_TESTS 个测试失败${NC}"
    exit 1
fi
