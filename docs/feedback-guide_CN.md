# 用户反馈规范

> **一份好的反馈 = 开发者能立即理解问题并开始工作。** 本文档定义了用户反馈的标准格式，帮助用户写出清晰、可操作的反馈，也帮助开发者快速定位和处理问题。

---

## 一、为什么需要反馈规范

模糊的反馈让开发者无从下手：

| ❌ 模糊反馈 | ✅ 清晰反馈 |
|-----------|-----------|
| "App 不好用" | "在 Android 14 上打开网页后 3 秒闪退（附截图+日志）" |
| "网络有问题" | "使用移动数据时无法加载图片，Wi-Fi 下正常（附截图）" |
| "加个功能" | "希望支持自定义 DNS，比如 Cloudflare DoH，用途是绕过 ISP DNS 污染" |

**好的反馈能让开发者：**
1. 立刻理解问题是什么
2. 知道如何复现
3. 明白用户期望的结果
4. 开始动手解决

---

## 二、反馈标准格式

每份反馈应包含以下结构：

```markdown
## 标题

[类型] 简明描述问题或需求

类型前缀：
- [Bug] — 功能异常、崩溃、行为不符合预期
- [Feature] — 新功能请求
- [Enhancement] — 现有功能改进
- [Question] — 使用疑问

## 描述

清晰说明你遇到了什么，或你希望什么。

## 复现步骤（Bug 必填）

1. ...
2. ...
3. ...

## 期望行为

你期望发生什么。

## 实际行为

实际发生了什么。

## 环境信息

- 设备型号：
- Android 版本：
- App 版本：
- 网络环境（Wi-Fi / 移动数据 / 代理）：

## 截图 / 录屏

[附上相关截图或录屏]

## 补充信息

任何你觉得可能有帮助的信息。
```

---

## 三、优秀反馈示例

以下是一份真实的 Feature Request 反馈，结构完整、表达清晰，是反馈的标杆：

> **Custom DNS Support #74** — @BlazeVertex
>
> ### Description
> 
> First of all, thanks for this amazing project it's really useful and powerful.
> 
> I would like to request a feature related to network access and restrictions.
> 
> Currently, when accessing certain websites, they may be blocked or restricted depending on the user's ISP or region. The only workaround is to use a VPN connection, which is not always ideal (battery usage, speed loss, extra setup, etc.).
> 
> ### Feature Idea
> 
> Would it be possible to add support for custom DNS configuration inside the app?
> 
> For example:
> - Allow users to set a custom DNS provider (e.g., Cloudflare DNS, etc.)
> - Option to apply DNS per app instance (per created web app)
> - Possibly support DNS-over-HTTPS (DoH) or DNS-over-TLS (DoT)
> 
> ### Benefits
> 
> - Access to blocked or restricted websites without needing a full VPN
> - Better performance compared to VPN in many cases
> - More control over privacy
> - Fits well with existing features like hosts blocking and privacy protection
> 
> ### Possible Implementation Ideas
> 
> - Integrate a lightweight DNS resolver or allow system DNS override within WebView/GeckoView
> - Add DNS settings in the app configuration screen
> - Optional toggle per project/app
> 
> ### Additional Context
> 
> The project already includes features like:
> - Hosts blocking
> - Privacy protection
> - Custom WebView/GeckoView engines and ad blocking
> 
> So adding DNS control would be a natural extension of these capabilities.

**为什么这份反馈好：**

| 要素 | 体现 |
|------|------|
| **背景说明** | 解释了当前问题（ISP/地区限制）和现有方案的不足（VPN 的缺点） |
| **具体需求** | 明确列出了 3 个要点：自定义提供商、per-app 配置、DoH/DoT 支持 |
| **价值论证** | 列出了 4 个好处，让开发者理解"为什么要做" |
| **实现建议** | 给出了思路但不强制，尊重开发者的技术判断 |
| **关联上下文** | 指出与现有功能（hosts blocking、隐私保护）的关联，帮助开发者定位 |

---

## 四、截图与录屏规范

### 4.1 什么时候需要截图

| 场景 | 是否需要截图 |
|------|------------|
| UI 显示异常（错位、遮挡、文字截断） | ✅ 必须 |
| 崩溃/闪退 | ✅ 必须（崩溃截图 + 日志） |
| 功能行为不符合预期 | ✅ 推荐（截图标注期望 vs 实际） |
| 网络相关错误 | ✅ 必须（错误提示截图） |
| 新功能请求 | ⬜ 不需要（用文字描述即可） |
| 性能问题 | ⬜ 推荐录屏展示卡顿 |

### 4.2 截图要求

- **标注关键区域**：用红框或箭头指出问题所在，不要让开发者猜"你让我看哪里"
- **完整界面**：不要只截问题区域，保留上下文（状态栏、导航栏、周围 UI）
- **多张对比**：如果有"期望 vs 实际"的对比，放两张图并标注

### 4.3 截图示例说明

```
❌ 差的截图：只截了中间一小块，不知道是什么页面、什么操作触发的
✅ 好的截图：完整页面 + 红框标注错误位置 + 标注"此处应显示 XX 但实际显示 YY"
```

### 4.4 录屏要求

- **展示完整操作流程**：从打开 App 到触发问题的每一步
- **时长控制在 30 秒内**：只展示关键操作，不要录无关内容
- **标注关键时刻**：如果录屏较长，用文字说明"在 XX 秒出现问题"

---

## 五、各类型反馈的要点

### Bug 反馈

**核心原则：让开发者能 100% 复现。**

必填项：
- **复现步骤**：逐步描述，像教一个完全不了解 App 的人操作
- **环境信息**：设备、系统版本、App 版本、网络环境
- **实际行为 vs 期望行为**：明确对比

```markdown
## 复现步骤

1. 打开 App，进入 [某个 WebApp]
2. 点击右上角菜单 → 设置
3. 开启"后台运行"
4. 按 Home 键回到桌面
5. 等待 5 分钟后重新打开 App
6. App 已被系统杀死，页面重新加载

## 期望行为
按 Home 键后 App 应在后台持续运行，重新打开时恢复之前的页面状态。

## 实际行为
App 被系统杀死，重新打开后从头加载。

## 环境信息
- 设备：Xiaomi 14
- Android 14 (HyperOS 1.0)
- App 版本：1.7.0
- 网络：Wi-Fi
```

### Feature Request 反馈

**核心原则：说清"为什么要"和"想要什么"。**

必填项：
- **背景/痛点**：你遇到了什么问题？现有方案为什么不够？
- **具体需求**：你希望 App 做什么？尽量具体
- **价值/好处**：这个功能对其他用户也有用吗？

参考 #74 示例的格式：Description → Feature Idea → Benefits → Possible Implementation Ideas → Additional Context

### Enhancement 反馈

**核心原则：说清"现在哪里不好"和"怎么改更好"。**

必填项：
- **现状描述**：当前功能的行为和不足
- **改进建议**：你期望的改进方式
- **理由**：为什么这样改更好

---

## 六、反馈提交渠道

| 渠道 | 适用场景 |
|------|---------|
| GitHub Issues | Bug、Feature Request、Enhancement — 正式跟踪 |
| 社区/群聊 | 快速提问、使用疑问、轻量讨论 |
| 邮件 | 涉及隐私信息（如账号问题） |

**正式的功能请求和 Bug 报告请提交 GitHub Issues**，这样有完整的跟踪记录，不会被遗漏。

---

## 修订记录

| 日期 | 修改内容 | 修改者 |
|------|---------|--------|
| 2026-04-25 | 初始创建 | Cascade |
