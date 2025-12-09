#!/usr/bin/env python3
"""
WebToApp Key Server API 完整测试报告脚本（无外部依赖）
"""

import json, time, sys, subprocess
from datetime import datetime
from typing import Dict, Any, Tuple

class APITester:
    def __init__(self, host="localhost", port=8080):
        self.base_url = f"http://{host}:{port}"
        self.tests = []
        self.test_app_id = f"com.webtoapp.test.{int(time.time())}"
        self.codes = []
    
    def curl(self, method, endpoint, data=None) -> Tuple[Dict, float, int]:
        try:
            cmd = ['curl', '-s', '-X', method, f"{self.base_url}{endpoint}",
                   '-H', 'Content-Type: application/json', '-w', '\n%{http_code}']
            if data:
                cmd.extend(['-d', json.dumps(data)])
            
            start = time.time()
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
            elapsed = (time.time() - start) * 1000
            
            lines = result.stdout.strip().split('\n')
            code = int(lines[-1]) if lines[-1].isdigit() else 0
            resp_text = '\n'.join(lines[:-1]) if len(lines) > 1 else lines[0]
            
            try:
                resp = json.loads(resp_text)
            except:
                resp = {'success': False, 'error': 'Invalid JSON'}
            
            return resp, elapsed, code
        except Exception as e:
            return {'success': False, 'error': str(e)}, 0, 0
    
    def test(self, name, method, endpoint, data=None):
        print(f"  {name}...", end=" ", flush=True)
        resp, elapsed, code = self.curl(method, endpoint, data)
        success = resp.get('success', False)
        
        self.tests.append({
            'name': name, 'method': method, 'endpoint': endpoint,
            'success': success, 'time': elapsed, 'code': code, 'resp': resp
        })
        
        print("✓" if success else "✗")
        return success
    
    def run(self) -> bool:
        print(f"\n{'='*70}")
        print(" WebToApp Key Server API 完整测试")
        print('='*70)
        print(f"\n服务器: {self.base_url}")
        print(f"App ID: {self.test_app_id}\n")
        
        # 检查服务器
        print("检查服务器连接...", end=" ", flush=True)
        resp, _, code = self.curl("GET", "/api/health")
        if not resp.get('success') or code != 200:
            print("✗\n❌ 无法连接到服务器\n")
            return False
        print("✓\n")
        
        # 运行测试
        print("运行测试套件:\n")
        self.test("健康检查", "GET", "/api/health")
        
        # 生成激活码
        gen_data = {
            "app_id": self.test_app_id,
            "count": 5,
            "expires_in_days": 30,
            "max_uses": 10,
            "device_limit": 5
        }
        resp, _, _ = self.curl("POST", "/api/activation/generate", gen_data)
        success = self.test("生成 5 个激活码", "POST", "/api/activation/generate", gen_data)
        if resp.get('codes'):
            self.codes = [c['code'] for c in resp['codes']]
        
        # 验证激活码
        if self.codes:
            verify_data = {
                "code": self.codes[0],
                "app_id": self.test_app_id,
                "device_id": "test_device_001",
                "device_info": {"device_name": "Test", "model": "Test", "os_version": "13", "app_version": "1.0"},
                "timestamp": int(time.time() * 1000)
            }
            self.test("验证激活码", "POST", "/api/activation/verify", verify_data)
        
        # 列表查询
        self.test("查询列表", "GET", f"/api/activation/list?app_id={self.test_app_id}&page=1&limit=10")
        
        # 筛选
        self.test("筛选 active", "GET", f"/api/activation/list?app_id={self.test_app_id}&status=active")
        
        # 撤销
        if len(self.codes) > 1:
            self.test("撤销激活码", "DELETE", f"/api/activation/{self.test_app_id}/{self.codes[1]}")
        
        # 多设备
        if self.codes:
            verify_data2 = {
                "code": self.codes[0],
                "app_id": self.test_app_id,
                "device_id": "test_device_002",
                "device_info": {"device_name": "Test2", "model": "Test2", "os_version": "14", "app_version": "1.1"},
                "timestamp": int(time.time() * 1000)
            }
            self.test("第二台设备验证", "POST", "/api/activation/verify", verify_data2)
        
        # 生成报告
        self._generate_report()
        
        passed = sum(1 for t in self.tests if t['success'])
        return passed == len(self.tests)
    
    def _generate_report(self):
        passed = sum(1 for t in self.tests if t['success'])
        failed = len(self.tests) - passed
        total_time = sum(t['time'] for t in self.tests)
        avg_time = total_time / len(self.tests) if self.tests else 0
        
        report = [
            f"# WebToApp Key Server API 测试报告\n\n",
            f"**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n",
            f"**服务器**: {self.base_url}\n",
            f"**应用ID**: {self.test_app_id}\n\n",
            
            f"## 📊 统计\n\n",
            f"| 项目 | 数值 |\n|------|------|\n",
            f"| 总测试 | {len(self.tests)} |\n",
            f"| 通过 | {passed} |\n",
            f"| 失败 | {failed} |\n",
            f"| 通过率 | {(passed/len(self.tests)*100 if self.tests else 0):.1f}% |\n",
            f"| 平均响应 | {avg_time:.2f}ms |\n\n",
            
            f"## 📋 详细结果\n\n",
        ]
        
        for i, t in enumerate(self.tests, 1):
            status = "✅" if t['success'] else "❌"
            report.append(f"{i}. {status} {t['name']}\n")
            report.append(f"   - {t['method']} `{t['endpoint']}`\n")
            report.append(f"   - 响应: {t['time']:.2f}ms, HTTP {t['code']}\n\n")
        
        report.append(f"## ⚡ 性能\n\n")
        times = [(t['name'], t['time']) for t in self.tests]
        if times:
            times.sort(key=lambda x: x[1])
            report.append(f"| 项目 | 数值 |\n|------|------|\n")
            report.append(f"| 最快 | {times[0][0]} ({times[0][1]:.2f}ms) |\n")
            report.append(f"| 最慢 | {times[-1][0]} ({times[-1][1]:.2f}ms) |\n")
            report.append(f"| 平均 | {avg_time:.2f}ms |\n\n")
            
            if avg_time < 10:
                rating = "🟢 **优秀** (< 10ms)"
            elif avg_time < 50:
                rating = "🟢 **很好** (< 50ms)"
            elif avg_time < 100:
                rating = "🟢 **良好** (< 100ms)"
            else:
                rating = "🟡 **可接受** (> 100ms)"
            report.append(f"**评级**: {rating}\n\n")
        
        if self.codes:
            report.append(f"## 🔐 生成的激活码\n\n")
            for code in self.codes:
                report.append(f"- `{code}`\n")
            report.append("\n")
        
        report.append(f"## ✅ 总结\n\n")
        if failed == 0:
            report.append("所有测试通过！系统运行正常。\n")
        else:
            report.append(f"有 {failed} 个测试失败，请查看详细结果。\n")
        
        # 保存报告
        filename = f"TEST_REPORT_{int(time.time())}.md"
        with open(filename, 'w', encoding='utf-8') as f:
            f.write("".join(report))
        
        print(f"\n{'='*70}")
        print("测试完成")
        print('='*70)
        print("\n".join(report))
        print(f"\n📄 报告已保存到: {filename}\n")

if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='localhost')
    parser.add_argument('--port', type=int, default=8080)
    args = parser.parse_args()
    
    tester = APITester(args.host, args.port)
    success = tester.run()
    sys.exit(0 if success else 1)
