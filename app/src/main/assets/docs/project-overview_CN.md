# Web-to-App 项目概览

## 项目简介

Web-to-App 是一个 Android 应用，核心功能是将网站/网页内容打包为离线可用的 Android 应用。支持社区、云服务、模块市场等功能。

## 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **架构**：MVVM（ViewModel + StateFlow）
- **构建**：Gradle (Kotlin DSL)
- **国际化**：自定义 `Strings.kt` 集中管理（中文/英文/阿拉伯语）

## 项目结构

```
web-to-app/
├── app/                          # 主应用模块
│   └── src/main/java/com/webtoapp/
│       ├── core/
│       │   ├── auth/             # 认证（登录/注册/Token管理）
│       │   ├── cloud/            # 云服务 API 客户端 + 社区数据模型
│       │   ├── extension/        # 扩展模块系统
│       │   ├── i18n/             # 国际化（Strings.kt, AppLanguage.kt）
│       │   ├── logging/          # 日志系统
│       │   └── scraper/          # 网站抓取引擎
│       ├── ui/
│       │   ├── components/       # 通用 UI 组件
│       │   ├── navigation/       # 导航图
│       │   ├── screens/          # 页面级 Composable
│       │   ├── viewmodel/        # ViewModel 层
│       │   └── webview/          # WebView 相关组件
│       └── schemas/              # Room 数据库 Schema
├── shell/                        # Shell 模块（WebView 壳）
├── docs/                         # 开发文档
├── res/                          # 资源文件
└── png/                          # 图片素材
```

## 核心模块说明

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | `core/auth/` | 登录/注册/Token 管理/头像上传 |
| 云服务 | `core/cloud/` | 项目管理/版本发布/APK上传/激活码/公告/配置/备份/模块市场/评分分布/评论排序 |
| 国际化 | `core/i18n/` | `Strings.kt`（33000+ 行）集中管理所有 UI 字符串 |
| 扩展 | `core/extension/` | 模块系统/分享码/远程脚本 |
| DNS | `core/dns/` | 自定义 DNS/DoH 解析/绕过 ISP DNS 污染 |
| 抓取 | `core/scraper/` | 网站离线打包引擎 |
| 社区 | `ui/viewmodel/CommunityViewModel.kt` | 帖子/模块/评论/通知/关注/搜索/评分分布/评论排序 |
| 云管理 | `ui/viewmodel/CloudViewModel.kt` | 项目/版本/激活码/备份/推送/精选推荐/更新检测 |

## 代码规模

- 总代码量：30 万+ 行
- `Strings.kt`：33000+ 行（最大的单文件，含全部本地化字符串）
- ViewModel 文件：平均 1000-1800 行

## 开发文档索引

> 每份文档均有中英文两个版本。中文版文件名以 `_CN.md` 结尾，英文版为标准文件名。App 内根据语言设置自动加载对应版本。

| 文档 | 说明 |
|------|------|
| [_doc-manual_CN.md](./_doc-manual_CN.md) | 文档手册——创建/修改文档前必读的元规范 |
| [_doc-maintenance_CN.md](./_doc-maintenance_CN.md) | 文档维护指南——文档审计与纠偏流程 |
| [architecture-guide_CN.md](./architecture-guide_CN.md) | 架构设计文档——分层设计、双轨模式、核心子系统协作关系 |
| [build-and-release-guide_CN.md](./build-and-release-guide_CN.md) | 构建与发布指南——构建配置、签名、Shell 模板打包、发布流程 |
| [data-model-guide_CN.md](./data-model-guide_CN.md) | 数据模型文档——WebApp 模型、嵌套配置类、枚举、序列化机制 |
| [extension-module-guide_CN.md](./extension-module-guide_CN.md) | 扩展模块开发指南——模块开发、URL 匹配、配置项、代码注入 |
| [shell-mode-guide_CN.md](./shell-mode-guide_CN.md) | Shell 模式开发指南——Shell 架构、配置加载、生命周期、Manifest 合并 |
| [security-features-guide_CN.md](./security-features-guide_CN.md) | 安全功能文档——加密系统、加固系统、隐私隔离、护盾、激活码 |
| [i18n-localization-guide_CN.md](./i18n-localization-guide_CN.md) | 国际化改造方法论、经验教训 |
| [unit-test-guide_CN.md](./unit-test-guide_CN.md) | 单元测试指南——测试清单、覆盖矩阵、编写规范、运行方法 |
| [feedback-guide_CN.md](./feedback-guide_CN.md) | 用户反馈规范——反馈标准格式、截图规范、优秀示例 |

