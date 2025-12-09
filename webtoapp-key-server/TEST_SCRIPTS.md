# 🧪 WebToApp Key Server API 测试脚本套件

完整的 API 测试解决方案，包含多种脚本选项满足不同需求。

## 📦 可用脚本概览

### 1. **test_api_simple.sh** ⭐ 推荐用于快速验证
```bash
./test_api_simple.sh [HOST] [PORT]
```
**特点：**
- ✅ 简洁清晰，快速执行
- ✅ 覆盖所有核心功能
- ✅ 零依赖，仅需 curl 和 bash
- ✅ 适合 CI/CD 快速验证

**示例：**
```bash
./test_api_simple.sh localhost 8080
./test_api_simple.sh example.com 9080
```

**输出：**
```
╔════════════════════════════════════════════════════════════════╗
║ WebToApp Key Server API 测试
╚════════════════════════════════════════════════════════════════╝

=== 1️⃣  健康检查 ===
[✓ PASS] 健康检查

=== 2️⃣  生成激活码 ===
[✓ PASS] 生成 3 个激活码

... (更多结果)

通过: 6
失败: 0

✅ 所有测试通过！
```

---

### 2. **test_api.sh** 详细的 Bash 脚本
```bash
./test_api.sh [HOST] [PORT]
```
**特点：**
- ✅ 详细的测试输出
- ✅ 完整的测试覆盖（7+ 个测试）
- ✅ 性能测试和统计
- ✅ 彩色高亮输出

**包含的测试：**
- 健康检查
- 批量生成激活码（5 个）
- 单次使用激活码
- 长期有效激活码（365 天）
- 激活码验证和签名生成
- 列表查询和分页
- 按状态筛选
- 撤销激活码
- 设备记录管理
- 多设备支持

---

### 3. **test_api.py** Python 版本（结构化）
```bash
python3 test_api.py --host localhost --port 8080
```
**特点：**
- ✅ 更好的代码结构
- ✅ 容易扩展和维护
- ✅ 详细的测试报告
- ✅ 支持命令行参数

**可用参数：**
```bash
python3 test_api.py --help
  --host       服务器主机名 (默认: localhost)
  --port       服务器端口 (默认: 8080)
  --json       输出 JSON 格式（预留）
```

---

### 4. **test_api_report.py** ⭐ 推荐用于生成报告
```bash
python3 test_api_report.py --host localhost --port 8080
```
**特点：**
- ✅ 自动生成 Markdown 报告
- ✅ 详细的性能分析
- ✅ 清晰的统计数据
- ✅ 易于分享和存档

**生成的报告包含：**
- 📊 测试统计（总数、通过率等）
- 📋 详细测试结果
- ⚡ 性能分析（最快、最慢、平均）
- 🔐 生成的激活码列表
- ✅ 总结和建议

**报告文件：**
```
TEST_REPORT_1765241525.md
```

---

## 🚀 快速开始

### 第 1 步：启动 Key Server
```bash
cd webtoapp-key-server
mkdir -p data
./bin/keyserver &
sleep 2
```

### 第 2 步：选择测试脚本

#### 快速验证（1 分钟）
```bash
bash test_api_simple.sh
```

#### 详细测试（2 分钟）
```bash
bash test_api.sh
```

#### 生成报告（2 分钟）
```bash
python3 test_api_report.py
```

### 第 3 步：查看结果
```bash
# 如果生成了报告
cat TEST_REPORT_*.md

# 停止 Key Server
pkill keyserver
```

---

## 🧪 测试覆盖范围

所有脚本都包含以下测试：

| 功能 | 简单版 | 完整版 | Python | 报告版 |
|------|--------|--------|--------|--------|
| 健康检查 | ✅ | ✅ | ✅ | ✅ |
| 生成激活码 | ✅ | ✅ | ✅ | ✅ |
| 验证激活码 | ✅ | ✅ | ✅ | ✅ |
| 列表查询 | ✅ | ✅ | ✅ | ✅ |
| 筛选和分页 | ✅ | ✅ | ✅ | ✅ |
| 撤销激活码 | ✅ | ✅ | ✅ | ✅ |
| 多设备支持 | ✅ | ✅ | ✅ | ✅ |
| 性能测试 | ✅ | ✅ | ✅ | ✅ |
| Markdown 报告 | ❌ | ❌ | ❌ | ✅ |

---

## 📊 典型输出

### 简单版输出
```
✅ 所有测试通过！

总测试数: 6
通过: 6
失败: 0
```

### 报告版输出
```
## 📊 统计

| 项目 | 数值 |
|------|------|
| 总测试 | 7 |
| 通过 | 7 |
| 失败 | 0 |
| 通过率 | 100.0% |
| 平均响应 | 20.62ms |

## ⚡ 性能

**评级**: 🟢 **很好** (< 50ms)
```

---

## 🔧 自定义参数

### 修改服务器地址
```bash
# Bash 脚本
./test_api_simple.sh 192.168.1.100 9080

# Python 脚本
python3 test_api_report.py --host 192.168.1.100 --port 9080
```

### 扩展测试
编辑脚本，添加新的测试用例：

**Bash 示例：**
```bash
test_api "自定义测试" "POST" "/api/your-endpoint" '{"param":"value"}'
```

**Python 示例：**
```python
def test_custom(self):
    self.test("自定义测试", "POST", "/api/your-endpoint", {"param": "value"})
```

---

## 📈 性能评级标准

| 平均响应时间 | 评级 | 说明 |
|-------------|------|------|
| < 10ms | 🟢 优秀 | 性能顶级 |
| 10-50ms | 🟢 很好 | 性能优异 |
| 50-100ms | 🟢 良好 | 性能满足要求 |
| > 100ms | 🟡 可接受 | 需要优化 |

---

## 🛠️ 故障排除

### 问题 1：连接被拒绝
```
error: [Errno 111] Connection refused
```

**解决方案：**
```bash
# 确保 Key Server 已启动
./bin/keyserver &

# 检查端口是否在监听
lsof -i :8080
```

### 问题 2：权限错误
```
bash: ./test_api_simple.sh: Permission denied
```

**解决方案：**
```bash
chmod +x test_api_simple.sh
chmod +x test_api.sh
```

### 问题 3：Python 模块未找到
```
ModuleNotFoundError: No module named 'requests'
```

**解决方案：**
使用 `test_api_report.py` 而不是 `test_api.py`（前者不需要外部依赖）

---

## 📚 详细文档

完整的 API 文档和使用指南请参考：
```bash
cat TEST_GUIDE.md
```

包含以下内容：
- 🔗 API 端点详情
- 📝 请求/响应示例
- 💡 自定义测试方法
- 🔧 CI/CD 集成
- 📞 故障排除

---

## 🎯 推荐使用场景

| 场景 | 推荐脚本 | 原因 |
|------|----------|------|
| CI/CD 快速验证 | `test_api_simple.sh` | 速度快，输出清晰 |
| 本地开发调试 | `test_api.sh` | 详细输出便于调试 |
| 性能基准测试 | `test_api_report.py` | 生成对比报告 |
| 生产环境验收 | `test_api_report.py` | 生成正式报告 |
| 自动化集成 | `test_api.py` | 易于集成 |

---

## 📋 快速命令参考

```bash
# 快速验证
./test_api_simple.sh

# 完整测试
./test_api.sh

# 生成报告
python3 test_api_report.py

# 指定服务器
./test_api_simple.sh example.com 8080
python3 test_api_report.py --host example.com --port 8080

# 查看报告
ls -lah TEST_REPORT_*.md
cat TEST_REPORT_*.md

# 停止服务器
pkill keyserver
```

---

## ✨ 特性总结

- ✅ **多种选择** - 4 种不同的脚本满足各种需求
- ✅ **零依赖** - Bash 脚本仅需 curl 和 bash
- ✅ **易于使用** - 简单的命令行接口
- ✅ **详细报告** - 自动生成 Markdown 格式报告
- ✅ **性能分析** - 响应时间统计和性能评级
- ✅ **易于扩展** - 清晰的代码结构便于添加新测试
- ✅ **生产就绪** - 可直接用于生产环境验收

---

## 📞 支持

如有问题或建议，请查阅 TEST_GUIDE.md 或检查 Key Server 日志。

---

*此测试脚本套件是 WebToApp Key Server 项目的重要组成部分*
