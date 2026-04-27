# 文档手册

> **本文件是项目所有文档的元规范。** 每次创建新文档或修改已有文档前，必须阅读并遵守本手册的规定。

---

## 一、文档体系

```
docs/                                              ← 源文件目录（Git 跟踪）
├── _doc-manual.md              # 文档手册（本文件）—— 创建/修改文档前必读
├── _doc-maintenance.md         # 文档维护指南—— 文档审计与纠偏流程
├── project-overview.md         # 项目概览
├── architecture-guide.md       # 架构设计文档—— 分层设计、双轨模式、核心子系统
├── build-and-release-guide.md  # 构建与发布指南—— 构建配置、签名、Shell 模板、发布流程
├── data-model-guide.md         # 数据模型文档—— WebApp 模型、嵌套配置、枚举、序列化
├── extension-module-guide.md   # 扩展模块开发指南—— 模块开发、URL 匹配、配置项、注入机制
├── shell-mode-guide.md         # Shell 模式开发指南—— Shell 架构、配置加载、生命周期、Manifest
├── security-features-guide.md  # 安全功能文档—— 加密、加固、隐私隔离、护盾、激活码
├── unit-test-guide.md          # 单元测试指南—— 测试清单、覆盖矩阵、编写规范
├── feedback-guide.md           # 用户反馈规范—— 反馈格式、截图规范、优秀示例
└── i18n-localization-guide.md  # 国际化指南

app/src/main/assets/docs/                          ← 客户端渲染目录（必须手动同步）
├── README.md                  # 中文版为 README_CN.md
├── _doc-manual.md
├── _doc-maintenance.md
├── project-overview.md
├── ...（与 docs/ 目录一一对应）
└── feedback-guide.md
```

> ⚠️ **关键**：`docs/` 是源文件，`app/src/main/assets/docs/` 是客户端渲染目录。两者必须保持同步，详见第五节。

### 文件命名规范

| 规则 | 说明 | 示例 |
|------|------|------|
| 全小写 + 连字符 | 文件名只使用小写字母和连字符 | `i18n-localization-guide.md` |
| 前缀 `_` | 系统级元文档以下划线开头，排序靠前 | `_doc-manual.md` |
| 语义化命名 | 文件名应能直接反映内容主题 | `project-overview.md` 而非 `doc1.md` |

---

## 二、创建新文档

### 2.1 创建前检查

- [ ] 是否已有同类文档？避免重复创建主题重叠的文档
- [ ] 文档主题是否明确？一个文档只解决一个主题
- [ ] 文件名是否符合命名规范？
- [ ] 是否需要更新 `project-overview.md` 的文档索引？
- [ ] 是否需要同步到 `app/src/main/assets/docs/`？（详见第五节）

### 2.2 文档模板

每个文档**必须**包含以下结构：

```markdown
# 文档标题

> **一句话摘要**，说明本文档的目的和适用场景。

---

## 一、[主题章节]

### 子章节

内容...

---

## 二、[主题章节]

内容...

---

## 修订记录

| 日期 | 修改内容 | 修改者 |
|------|---------|--------|
| YYYY-MM-DD | 初始创建 | xxx |
```

### 2.3 必须遵守的规则

1. **摘要行**：文件开头必须有一行 `>` 引用格式的摘要，说明文档目的
2. **章节编号**：使用中文数字编号（一、二、三...），子章节用 `###`
3. **代码引用**：引用项目文件时使用反引号 + 相对路径，如 `` `app/src/main/java/.../Strings.kt` ``
4. **代码块**：标注语言类型，如 ` ```kotlin `
5. **表格**：使用 Markdown 表格，必须有表头
6. **不写死具体行号**：代码行号会随迭代变化，引用时用函数名/类名/属性名定位，不要写行号
7. **不写死待办列表**：不要在文档中维护"待处理文件列表"之类的静态清单，它们会迅速过时；改为提供**发现遗漏的方法**（如 grep 命令 + 判断规则）
8. **不写死数值**：代码行数、文件数量等会变化，如需提及用"约"或"大致"，或提供获取当前值的命令
9. **语言**：文档正文使用中文，代码示例和技术术语保持英文原文
10. **修订记录**：每次实质性修改文档内容时，在末尾修订记录表中追加一行

---

## 三、修改已有文档

### 3.1 修改前检查

- [ ] 修改是否与文档主题一致？不要在 A 主题文档中塞入 B 主题内容
- [ ] 是否需要同步更新其他文档？（如 `project-overview.md` 的索引）
- [ ] 修改后摘要行是否仍然准确？
- [ ] 是否需要追加修订记录？
- [ ] 是否需要同步到 `app/src/main/assets/docs/`？（详见第五节）

### 3.2 修改原则

1. **增删对等**：新增一个章节时，评估是否应拆分为独立文档
2. **不堆积过时内容**：删除已失效的内容，不要用删除线保留（~~像这样~~），直接删
3. **方法优于清单**：优先提供"如何发现/验证"的方法论，而非静态列举
4. **示例要通用**：示例代码应反映通用模式，不要硬编码特定业务数据

---

## 四、禁止事项

| 禁止 | 原因 |
|------|------|
| 在文档中维护"待处理文件列表" | 项目持续迭代，静态列表迅速过时，应提供扫描方法 |
| 写死代码行号 | 行号随迭代变化，用函数名/属性名定位 |
| 创建重复主题文档 | 同一主题只应有一个文档，内容更新应修改现有文档 |
| 在文档中写临时笔记/草稿 | 文档是正式交付物，草稿应放在个人笔记中 |
| 文档间互相复制大段内容 | 保持单一信息源，引用其他文档即可 |

---

## 五、文档同步到客户端

> ⚠️ **这是最容易遗漏的步骤！** 每次新增或修改文档后，必须执行以下同步操作，否则用户在 App 内看到的文档将是旧版本。

### 5.1 为什么需要同步

项目文档存在两份：

| 位置 | 用途 | 说明 |
|------|------|------|
| `docs/` | 源文件目录 | Git 跟踪，开发者直接编辑 |
| `app/src/main/assets/docs/` | 客户端渲染目录 | App 内文档查看器从此加载 |

App 的 `DocsScreen` 组件从 `assets/docs/` 读取 Markdown 并渲染为 Compose UI。**它不会自动读取 `docs/` 目录**，因此每次修改源文件后必须手动同步。

### 5.2 同步操作清单

每次新增或修改文档时，必须完成以下**全部步骤**：

#### 步骤 1：复制文件到 assets

```bash
# 新增文档
cp docs/<filename>.md app/src/main/assets/docs/<filename>.md

# 修改文档（覆盖更新）
cp docs/<filename>.md app/src/main/assets/docs/<filename>.md
```

#### 步骤 2：注册到 DocsScreen

在 `app/src/main/java/com/webtoapp/ui/screens/DocsScreen.kt` 中：

1. **`DOC_ITEMS` 列表**：添加 `DocItem` 条目（仅新增文档时需要）
   ```kotlin
   DocItem("feedback-guide.md", "feedbackGuide", "feedbackGuideDesc", Icons.Outlined.Feedback),
   ```

2. **`resolveTitle()`**：添加标题映射
   ```kotlin
   "feedbackGuide" -> Strings.docFeedbackGuide
   ```

3. **`resolveDesc()`**：添加描述映射
   ```kotlin
   "feedbackGuide" -> Strings.docFeedbackGuideDesc
   ```

#### 步骤 3：添加 i18n 字符串

在 `app/src/main/java/com/webtoapp/core/i18n/Strings.kt` 中添加标题和描述的三语字符串：

```kotlin
val docFeedbackGuide: String get() = when (lang) {
    AppLanguage.CHINESE -> "用户反馈规范"
    AppLanguage.ENGLISH -> "Feedback Guide"
    AppLanguage.ARABIC -> "دليل الملاحظات"
}
val docFeedbackGuideDesc: String get() = when (lang) {
    AppLanguage.CHINESE -> "反馈标准格式、截图规范、优秀示例"
    AppLanguage.ENGLISH -> "Feedback format, screenshot rules & great examples"
    AppLanguage.ARABIC -> "تنسيق الملاحظات، قواعد لقطات الشاشة وأمثلة رائعة"
}
```

#### 步骤 4：更新文档索引

在 `docs/project-overview.md` 的文档索引表中添加新条目（仅新增文档时需要）。

### 5.3 验证同步完整性

```bash
# 检查 docs/ 和 assets/docs/ 的文件是否一一对应
diff <(ls docs/*.md | xargs -n1 basename | sort) \
     <(ls app/src/main/assets/docs/*.md | xargs -n1 basename | sort)
```

如果 diff 有输出，说明有文件未同步。

### 5.4 常见遗漏场景

| 场景 | 容易遗漏的操作 | 后果 |
|------|---------------|------|
| 新增文档 | 忘记复制到 `assets/docs/` | App 内看不到新文档 |
| 新增文档 | 忘记在 `DocsScreen` 注册 `DocItem` | App 内文档列表不显示 |
| 新增文档 | 忘记添加 i18n 字符串 | 标题/描述显示为 key 原文 |
| 修改文档内容 | 忘记覆盖 `assets/docs/` 中的旧版本 | App 内显示旧内容 |
| 修改文档内容 | 不需要更新 `DocItem` 或 i18n | — （内容变更无需改注册） |

---

## 六、DocsScreen 技术细节

`DocsScreen` 是 App 内的 Markdown 文档查看器，基于 `commonmark-java` 解析 + Jetpack Compose 渲染。

### 核心组件

| 组件 | 文件 | 职责 |
|------|------|------|
| `DocsScreen` | `ui/screens/DocsScreen.kt` | 主入口，列表/详情切换 |
| `DocListScreen` | 同上 | 文档卡片列表 |
| `DocDetailScreen` | 同上 | 加载 assets 中的 Markdown 并渲染 |
| `MarkdownContent` | 同上 | commonmark AST → Compose UI 递归渲染 |

### 支持的 Markdown 特性

- 标题（H1-H6，H1 带强调下划线）
- 段落、粗体、斜体、行内代码
- 有序/无序列表（支持嵌套）
- 代码块（圆角卡片样式）
- 引用块（左侧强调条）
- 分隔线
- GFM 表格（表头高亮 + 斑马纹）
- 链接（下划线 + 主题色）

### 中文版支持

`DocItem` 有可选的 `chineseFileName` 字段。当 App 语言为中文时，自动加载中文版文件：

```kotlin
DocItem("README.md", "readme", "readmeDesc", Icons.Outlined.Article, chineseFileName = "README_CN.md")
```

---

## 修订记录

| 日期 | 修改内容 | 修改者 |
|------|---------|--------|
| 2026-04-19 | 初始创建 | Cascade |
| 2026-04-25 | 新增第五节（文档同步到客户端）、第六节（DocsScreen 技术细节）；更新文档体系树和检查清单 | Cascade |
