#!/usr/bin/env python3
"""
WebToApp Key Server API 测试脚本 (Python 版本)
用途: 全面测试所有 API 端点的功能、性能和边界条件
使用: python3 test_api.py [--host localhost] [--port 8080] [--json]
"""

import requests
import json
import time
import sys
import argparse
from datetime import datetime, timedelta
from typing import Dict, List, Tuple, Any
from dataclasses import dataclass
from enum import Enum

# 颜色定义
class Color:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    END = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'

class TestResult(Enum):
    PASS = 'PASS'
    FAIL = 'FAIL'
    SKIP = 'SKIP'

@dataclass
class TestMetrics:
    name: str
    method: str
    endpoint: str
    status_code: int
    response_time: float  # ms
    result: TestResult
    message: str = ""
    
@dataclass
class TestStats:
    total: int = 0
    passed: int = 0
    failed: int = 0
    skipped: int = 0
    total_time: float = 0.0  # ms
    
    def add_test(self, metric: TestMetrics):
        self.total += 1
        self.total_time += metric.response_time
        if metric.result == TestResult.PASS:
            self.passed += 1
        elif metric.result == TestResult.FAIL:
            self.failed += 1
        else:
            self.skipped += 1

class APITester:
    def __init__(self, host: str = "localhost", port: int = 8080):
        self.host = host
        self.port = port
        self.base_url = f"http://{host}:{port}"
        self.session = requests.Session()
        self.stats = TestStats()
        self.metrics: List[TestMetrics] = []
        self.generated_codes: List[Dict[str, Any]] = []
        self.test_app_id = f"com.webtoapp.test.{int(time.time())}"
        
    def print_header(self, text: str):
        """打印标题"""
        print(f"\n{Color.BLUE}╔{'═' * 70}╗{Color.END}")
        print(f"{Color.BLUE}║{Color.END} {text:<68} {Color.BLUE}║{Color.END}")
        print(f"{Color.BLUE}╚{'═' * 70}╝{Color.END}\n")
        
    def print_test(self, text: str):
        """打印测试"""
        print(f"{Color.CYAN}[TEST]{Color.END} {text}")
        
    def print_success(self, text: str):
        """打印成功"""
        print(f"{Color.GREEN}[✓ PASS]{Color.END} {text}")
        
    def print_fail(self, text: str):
        """打印失败"""
        print(f"{Color.RED}[✗ FAIL]{Color.END} {text}")
        
    def print_info(self, text: str):
        """打印信息"""
        print(f"{Color.YELLOW}[INFO]{Color.END} {text}")
        
    def check_server_health(self) -> bool:
        """检查服务器是否可用"""
        try:
            response = self.session.get(f"{self.base_url}/api/health", timeout=5)
            return response.status_code == 200
        except Exception as e:
            print(f"{Color.RED}[ERROR]{Color.END} 无法连接到服务器: {e}")
            return False
            
    def run_test(self, name: str, method: str, endpoint: str, 
                 data: Dict = None, expected_code: int = 200) -> Tuple[Dict, TestMetrics]:
        """运行单个测试"""
        self.print_test(name)
        
        try:
            start_time = time.time()
            
            if method.upper() == "GET":
                response = self.session.get(f"{self.base_url}{endpoint}", timeout=10)
            elif method.upper() == "POST":
                response = self.session.post(
                    f"{self.base_url}{endpoint}",
                    json=data,
                    timeout=10
                )
            elif method.upper() == "DELETE":
                response = self.session.delete(f"{self.base_url}{endpoint}", timeout=10)
            else:
                raise ValueError(f"Unsupported method: {method}")
                
            response_time = (time.time() - start_time) * 1000  # Convert to ms
            
            # 检查状态码
            if response.status_code != expected_code:
                metric = TestMetrics(
                    name=name,
                    method=method,
                    endpoint=endpoint,
                    status_code=response.status_code,
                    response_time=response_time,
                    result=TestResult.FAIL,
                    message=f"HTTP {response.status_code} (expected {expected_code})"
                )
                self.print_fail(f"{name} - {metric.message} ({response_time:.2f}ms)")
                self.metrics.append(metric)
                self.stats.add_test(metric)
                return {}, metric
            
            # 解析 JSON
            try:
                json_data = response.json()
            except json.JSONDecodeError:
                metric = TestMetrics(
                    name=name,
                    method=method,
                    endpoint=endpoint,
                    status_code=response.status_code,
                    response_time=response_time,
                    result=TestResult.FAIL,
                    message="Invalid JSON response"
                )
                self.print_fail(f"{name} - Invalid JSON")
                self.metrics.append(metric)
                self.stats.add_test(metric)
                return {}, metric
            
            # 检查 success 字段
            success = json_data.get('success', False)
            if not success:
                metric = TestMetrics(
                    name=name,
                    method=method,
                    endpoint=endpoint,
                    status_code=response.status_code,
                    response_time=response_time,
                    result=TestResult.FAIL,
                    message=f"success=false, message={json_data.get('message', '')}"
                )
                self.print_fail(f"{name} - {metric.message}")
                self.metrics.append(metric)
                self.stats.add_test(metric)
                return json_data, metric
            
            metric = TestMetrics(
                name=name,
                method=method,
                endpoint=endpoint,
                status_code=response.status_code,
                response_time=response_time,
                result=TestResult.PASS
            )
            self.print_success(f"{name} ({response_time:.2f}ms)")
            
            # 打印部分响应信息
            response_str = json.dumps(json_data, indent=2)
            if len(response_str) > 300:
                print(f"{Color.YELLOW}Response (首 300 字符):{Color.END}")
                print(response_str[:300] + "...")
            else:
                print(f"{Color.YELLOW}Response:{Color.END}")
                print(response_str)
            
            self.metrics.append(metric)
            self.stats.add_test(metric)
            return json_data, metric
            
        except requests.RequestException as e:
            metric = TestMetrics(
                name=name,
                method=method,
                endpoint=endpoint,
                status_code=0,
                response_time=0,
                result=TestResult.FAIL,
                message=str(e)
            )
            self.print_fail(f"{name} - {str(e)}")
            self.metrics.append(metric)
            self.stats.add_test(metric)
            return {}, metric
    
    def test_health_check(self):
        """测试健康检查"""
        self.print_header("1️⃣  健康检查测试")
        
        response, _ = self.run_test(
            "健康检查",
            "GET",
            "/api/health",
            expected_code=200
        )
        
        if response.get('message'):
            self.print_info(f"服务消息: {response['message']}")
            if 'healthy' in response['message'].lower():
                self.print_success("服务状态健康")
    
    def test_generate_codes(self):
        """测试生成激活码"""
        self.print_header("2️⃣  生成激活码测试")
        
        # 2.1 生成 5 个激活码
        gen_data = {
            "app_id": self.test_app_id,
            "count": 5,
            "expires_in_days": 30,
            "max_uses": 10,
            "device_limit": 3,
            "notes": "Test activation codes"
        }
        
        response, _ = self.run_test(
            "生成 5 个激活码",
            "POST",
            "/api/activation/generate",
            gen_data,
            200
        )
        
        generated_count = response.get('generated', 0)
        codes = response.get('codes', [])
        self.generated_codes = codes
        
        self.print_info(f"实际生成数量: {generated_count} 个")
        
        if generated_count == 5:
            self.print_success("激活码数量正确")
        else:
            self.print_fail(f"激活码数量错误 (期望 5, 实际 {generated_count})")
        
        # 2.2 生成单次使用激活码
        single_gen_data = {
            "app_id": self.test_app_id,
            "count": 1,
            "max_uses": 1,
            "expires_in_days": 7
        }
        
        response, _ = self.run_test(
            "生成单次使用激活码",
            "POST",
            "/api/activation/generate",
            single_gen_data,
            200
        )
        
        if response.get('codes'):
            code = response['codes'][0]['code']
            self.generated_codes.append(code)
            self.print_info(f"单次使用激活码: {code}")
        
        # 2.3 测试不同的过期时间
        days_gen_data = {
            "app_id": self.test_app_id,
            "count": 1,
            "expires_in_days": 365,
            "max_uses": 100
        }
        
        response, _ = self.run_test(
            "生成 365 天有效期激活码",
            "POST",
            "/api/activation/generate",
            days_gen_data,
            200
        )
    
    def test_verify_codes(self):
        """测试验证激活码"""
        self.print_header("3️⃣  验证激活码测试")
        
        if not self.generated_codes:
            self.print_fail("没有可用的激活码进行验证测试")
            return
        
        code = self.generated_codes[0]['code']
        
        # 3.1 正常验证激活码
        verify_data = {
            "code": code,
            "app_id": self.test_app_id,
            "device_id": f"test_device_{int(time.time())}",
            "device_info": {
                "device_name": "OPPO A57",
                "model": "OPPO A57",
                "os_version": "13",
                "app_version": "1.0.6"
            },
            "timestamp": int(time.time() * 1000)
        }
        
        response, _ = self.run_test(
            "验证激活码",
            "POST",
            "/api/activation/verify",
            verify_data,
            200
        )
        
        data = response.get('data', {})
        signature = response.get('signature', '')
        
        self.print_info(f"剩余使用次数: {data.get('remaining_uses', 0)}")
        self.print_info(f"已激活设备数: {data.get('devices_used', 0)}")
        self.print_info(f"签名 (前 20 字符): {signature[:20]}...")
        
        # 检查签名格式
        if signature and len(signature) == 64:
            self.print_success("签名格式正确 (64 个十六进制字符)")
        else:
            self.print_fail("签名格式错误")
        
        # 3.2 验证无效激活码
        invalid_verify = {
            "code": "INVALID-CODE-0000-0000",
            "app_id": self.test_app_id,
            "device_id": "test_device_invalid",
            "device_info": {},
            "timestamp": int(time.time() * 1000)
        }
        
        response, metric = self.run_test(
            "验证无效激活码 (应该失败)",
            "POST",
            "/api/activation/verify",
            invalid_verify,
            200
        )
        
        if not response.get('success', False):
            self.print_success("无效激活码验证失败（正确行为）")
        else:
            self.print_fail("无效激活码验证应该失败")
        
        # 3.3 多设备验证
        device_2_verify = {
            "code": code,
            "app_id": self.test_app_id,
            "device_id": f"test_device_2_{int(time.time())}",
            "device_info": {
                "device_name": "Test Device 2",
                "model": "Test Model",
                "os_version": "14",
                "app_version": "1.1.0"
            },
            "timestamp": int(time.time() * 1000)
        }
        
        response, _ = self.run_test(
            "验证第二台设备",
            "POST",
            "/api/activation/verify",
            device_2_verify,
            200
        )
        
        devices_used = response.get('data', {}).get('devices_used', 0)
        self.print_info(f"已激活设备数: {devices_used}")
    
    def test_list_and_filter(self):
        """测试列表查询和筛选"""
        self.print_header("4️⃣  列表查询和筛选测试")
        
        # 4.1 查询所有激活码
        response, _ = self.run_test(
            "查询所有激活码",
            "GET",
            f"/api/activation/list?app_id={self.test_app_id}&page=1&limit=10",
            expected_code=200
        )
        
        total = response.get('total', 0)
        self.print_info(f"总激活码数: {total}")
        
        # 4.2 按状态筛选 - active
        response, _ = self.run_test(
            "筛选 active 状态激活码",
            "GET",
            f"/api/activation/list?app_id={self.test_app_id}&status=active&limit=100",
            expected_code=200
        )
        
        active_count = response.get('total', 0)
        self.print_info(f"Active 状态激活码数: {active_count}")
        
        # 4.3 分页测试
        response, _ = self.run_test(
            "分页查询 (page=1, limit=2)",
            "GET",
            f"/api/activation/list?app_id={self.test_app_id}&page=1&limit=2",
            expected_code=200
        )
        
        items_count = len(response.get('items', []))
        limit = response.get('limit', 0)
        
        if items_count <= limit:
            self.print_success(f"分页限制正确 (返回 {items_count} 项)")
        else:
            self.print_fail(f"分页限制错误")
        
        # 4.4 排序和搜索
        response, _ = self.run_test(
            "查询并验证数据结构",
            "GET",
            f"/api/activation/list?app_id={self.test_app_id}&limit=1",
            expected_code=200
        )
        
        if response.get('items'):
            item = response['items'][0]
            required_fields = ['id', 'code', 'status', 'created_at', 'expires_at']
            missing = [f for f in required_fields if f not in item]
            
            if not missing:
                self.print_success("数据结构完整")
            else:
                self.print_fail(f"缺少字段: {missing}")
    
    def test_revoke_codes(self):
        """测试撤销激活码"""
        self.print_header("5️⃣  撤销激活码测试")
        
        if not self.generated_codes or len(self.generated_codes) < 2:
            self.print_fail("没有足够的激活码进行撤销测试")
            return
        
        code_to_revoke = self.generated_codes[1]['code']
        
        # 5.1 撤销激活码
        response, _ = self.run_test(
            "撤销激活码",
            "DELETE",
            f"/api/activation/{self.test_app_id}/{code_to_revoke}",
            expected_code=200
        )
        
        # 5.2 验证撤销状态
        response, _ = self.run_test(
            "查询撤销状态的激活码",
            "GET",
            f"/api/activation/list?app_id={self.test_app_id}&status=revoked",
            expected_code=200
        )
        
        revoked_count = response.get('total', 0)
        if revoked_count > 0:
            self.print_success(f"激活码撤销成功 - 已撤销 {revoked_count} 个")
            
            # 找到撤销的码
            items = response.get('items', [])
            revoked_item = next((item for item in items if item['code'] == code_to_revoke), None)
            if revoked_item:
                self.print_info(f"撤销的激活码状态: {revoked_item.get('status')}")
        else:
            self.print_fail("激活码撤销失败")
        
        # 5.3 验证撤销后无法再使用
        if revoked_count > 0:
            revoked_verify = {
                "code": code_to_revoke,
                "app_id": self.test_app_id,
                "device_id": "test_device_revoked",
                "device_info": {},
                "timestamp": int(time.time() * 1000)
            }
            
            response, _ = self.run_test(
                "验证撤销的激活码 (应该失败)",
                "POST",
                "/api/activation/verify",
                revoked_verify,
                200
            )
            
            if not response.get('success', False):
                self.print_success("撤销的激活码无法再使用（正确行为）")
            else:
                self.print_fail("撤销的激活码不应该可以使用")
    
    def test_device_records(self):
        """测试设备记录管理"""
        self.print_header("6️⃣  设备记录管理测试")
        
        if not self.generated_codes:
            self.print_fail("没有可用的激活码")
            return
        
        # 6.1 验证设备被记录
        response, _ = self.run_test(
            "查询设备激活记录",
            "GET",
            f"/api/activation/list?app_id={self.test_app_id}&limit=100",
            expected_code=200
        )
        
        items = response.get('items', [])
        if items and items[0].get('id'):
            self.print_success("设备激活信息被正确记录")
            
            first_item = items[0]
            self.print_info(f"第一个激活 ID: {first_item.get('id')}")
            self.print_info(f"创建时间: {first_item.get('created_at')}")
            self.print_info(f"过期时间: {first_item.get('expires_at')}")
        else:
            self.print_fail("未能获取设备激活信息")
        
        # 6.2 验证多设备支持
        code = self.generated_codes[0]['code']
        
        for i in range(2):
            device_verify = {
                "code": code,
                "app_id": self.test_app_id,
                "device_id": f"multi_device_{i}_{int(time.time())}",
                "device_info": {
                    "device_name": f"Device {i+1}",
                    "model": f"Model {i+1}",
                    "os_version": "14",
                    "app_version": "1.0.0"
                },
                "timestamp": int(time.time() * 1000)
            }
            
            response, _ = self.run_test(
                f"验证第 {i+1} 台设备",
                "POST",
                "/api/activation/verify",
                device_verify,
                200
            )
            
            if response.get('success'):
                devices_used = response.get('data', {}).get('devices_used', 0)
                self.print_info(f"已激活设备数: {devices_used}")
    
    def print_performance_stats(self):
        """打印性能统计"""
        self.print_header("⚡ 性能测试")
        
        if self.stats.total == 0:
            return
        
        avg_response = self.stats.total_time / self.stats.total
        
        self.print_info(f"总耗时: {self.stats.total_time:.2f}ms")
        self.print_info(f"平均每个请求: {avg_response:.2f}ms")
        self.print_info(f"总请求数: {self.stats.total}")
        
        # 找出最快和最慢的请求
        if self.metrics:
            fastest = min(self.metrics, key=lambda x: x.response_time)
            slowest = max(self.metrics, key=lambda x: x.response_time)
            
            self.print_info(f"最快: {fastest.name} ({fastest.response_time:.2f}ms)")
            self.print_info(f"最慢: {slowest.name} ({slowest.response_time:.2f}ms)")
        
        # 性能评级
        if avg_response < 10:
            self.print_success("性能优秀 (< 10ms)")
        elif avg_response < 50:
            self.print_success("性能很好 (< 50ms)")
        elif avg_response < 100:
            self.print_success("性能良好 (< 100ms)")
        else:
            self.print_info(f"性能可接受 ({avg_response:.2f}ms)")
    
    def print_summary(self):
        """打印测试总结"""
        self.print_header("📊 测试总结")
        
        print()
        print(f"{Color.CYAN}测试统计:{Color.END}")
        print(f"  总测试数:    {Color.BLUE}{self.stats.total}{Color.END}")
        print(f"  通过:        {Color.GREEN}{self.stats.passed}{Color.END}")
        print(f"  失败:        {Color.RED}{self.stats.failed}{Color.END}")
        print(f"  跳过:        {Color.YELLOW}{self.stats.skipped}{Color.END}")
        
        if self.stats.total > 0:
            pass_rate = (self.stats.passed / self.stats.total) * 100
            print(f"  通过率:      {Color.BLUE}{pass_rate:.1f}%{Color.END}")
        
        print(f"  总耗时:      {Color.CYAN}{self.stats.total_time:.2f}ms{Color.END}")
        print()
        
        if self.stats.failed == 0:
            print(f"{Color.GREEN}✅ 所有测试通过！{Color.END}")
            return 0
        else:
            print(f"{Color.RED}❌ 有 {self.stats.failed} 个测试失败{Color.END}")
            return 1
    
    def run_all_tests(self):
        """运行所有测试"""
        print(f"\n{Color.BLUE}{Color.BOLD}WebToApp Key Server API 完整测试{Color.END}")
        print(f"{Color.YELLOW}服务器地址: {self.base_url}{Color.END}")
        print(f"{Color.YELLOW}测试应用 ID: {self.test_app_id}{Color.END}")
        
        # 检查服务器
        print(f"\n{Color.CYAN}正在检查服务器连接...{Color.END}")
        if not self.check_server_health():
            print(f"{Color.RED}❌ 无法连接到服务器{Color.END}")
            return 1
        print(f"{Color.GREEN}✅ 服务器连接正常{Color.END}")
        
        # 运行所有测试
        self.test_health_check()
        self.test_generate_codes()
        self.test_verify_codes()
        self.test_list_and_filter()
        self.test_revoke_codes()
        self.test_device_records()
        
        # 打印统计
        self.print_performance_stats()
        
        # 打印总结
        return self.print_summary()

def main():
    parser = argparse.ArgumentParser(
        description='WebToApp Key Server API 测试脚本'
    )
    parser.add_argument('--host', default='localhost', help='服务器主机名')
    parser.add_argument('--port', type=int, default=8080, help='服务器端口')
    parser.add_argument('--json', action='store_true', help='输出 JSON 格式')
    
    args = parser.parse_args()
    
    tester = APITester(host=args.host, port=args.port)
    exit_code = tester.run_all_tests()
    
    sys.exit(exit_code)

if __name__ == '__main__':
    main()
