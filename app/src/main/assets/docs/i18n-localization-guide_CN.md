# 国际化（i18n）本地化指南

> 本文档记录了项目国际化改造的完整方法论、经验教训和待办事项，供后续开发者参考。

---

## 一、项目本地化架构

### 核心文件

| 文件 | 作用 |
|------|------|
| `app/src/main/java/com/webtoapp/core/i18n/Strings.kt` | 所有本地化字符串的集中定义文件 |
| `app/src/main/java/com/webtoapp/core/i18n/LanguageManager.kt` | 语言枚举定义（`AppLanguage` 枚举在此文件中） |

### 支持语言

- **中文** (`AppLanguage.CHINESE`) — 默认语言
- **英文** (`AppLanguage.ENGLISH`)
- **阿拉伯语** (`AppLanguage.ARABIC`)

### 字符串属性定义规范

```kotlin
// 静态字符串
val communityPostsLoadFailed: String get() = when (lang) {
    AppLanguage.CHINESE -> "社区帖子加载失败"
    AppLanguage.ENGLISH -> "Community posts load failed"
    AppLanguage.ARABIC -> "فشل تحميل منشورات المجتمع"
}

// 动态字符串（使用 %s / %d 占位符）
val activationCodesGenerated: String get() = when (lang) {
    AppLanguage.CHINESE -> "生成了 %d 个激活码"
    AppLanguage.ENGLISH -> "Generated %d activation codes"
    AppLanguage.ARABIC -> "تم إنشاء %d رمز تفعيل"
}
```

**调用动态字符串时使用 `.format()`：**
```kotlin
Strings.activationCodesGenerated.format(result.data.size)
```

---

## 二、标准工作流

```
1. Grep 扫描 → 2. 逐文件阅读确认 → 3. Strings.kt 添加属性 → 4. 源文件替换 → 5. 验证无遗漏
```

### Step 1: Grep 扫描定位

用正则匹配 CJK Unified Ideographs 范围内的中文字符：

```bash
# 扫描指定目录下所有 Kotlin 文件
grep -rn "[\x{4e00}-\x{9fff}]" --include="*.kt" app/src/main/java/com/webtoapp/
```

按文件分组，逐文件处理，避免混乱。

### Step 2: 逐文件阅读确认上下文

**不能只看 grep 结果就动手，必须读文件确认上下文。** 原因：

- 同一字符串可能在不同位置出现，替换时需要区分上下文使其唯一
- 需要区分 UI 字符串和纯日志/调试信息（日志中的中文通常不需要本地化）
- 需要确认字符串是否为动态拼接（需要 `.format()`）

### Step 3: Strings.kt 添加本地化属性

- 追加到文件末尾的 `}` 之前
- 命名规范：`功能+动作+状态`，如 `communityPostsLoadFailed`、`serverUploadStage`
- 动态字符串用 `%s`（字符串）、`%d`（整数）占位符
- **三种语言缺一不可**

### Step 4: 源文件替换

1. 先确认是否已有 `import com.webtoapp.core.i18n.Strings`，没有则添加
2. 用 `multi_edit` 批量替换同文件内的多个字符串
3. 替换时保持缩进和代码风格一致

### Step 5: 验证

- 再次 grep 扫描确认零残留
- 检查 Strings.kt 中引用的属性名是否都已定义（避免编译错误）

---

## 三、踩过的坑与经验

### 3.1 枚举类不能用构造参数做本地化

```kotlin
// ❌ 错误：枚举加载时只执行一次，切换语言不会更新
enum class Template(val displayName: String, val description: String) {
    FOO("中文", "描述")
}

// ✅ 正确：computed getter 每次访问时根据当前语言求值
enum class Template {
    FOO;
    val displayName: String get() = when (Strings.lang) {
        AppLanguage.CHINESE -> "中文"
        AppLanguage.ENGLISH -> "English"
        AppLanguage.ARABIC -> "العربية"
    }
}
```

### 3.2 `multi_edit` 的 old_string 必须唯一

同一文件中相同字符串出现多次时，需要扩大上下文使其唯一。

```kotlin
// "举报已提交" 出现两次（帖子举报 + 模块举报），需要包含前后行区分：
// 帖子举报上下文：
is AuthResult.Success -> _message.value = "举报已提交"
    is AuthResult.Error -> {
        AppLogger.e(TAG, "Report post failed: ...")

// 模块举报上下文：
is AuthResult.Success -> _message.value = "举报已提交"
    is AuthResult.Error -> {
        AppLogger.e(TAG, "Report module failed: ...")
```

### 3.3 动态字符串必须用 `.format()`，不能拼接

```kotlin
// ❌ 错误：丢失本地化
"生成了 ${result.data.size} 个激活码"

// ✅ 正确：Strings.kt 中定义为 "生成了 %d 个激活码"，调用时：
Strings.activationCodesGenerated.format(result.data.size)
```

### 3.4 条件分支中的字符串也要替换

```kotlin
// 原：
_message.value = if (current?.isFavorited == true) "已取消收藏" else "已收藏"

// 改：
_message.value = if (current?.isFavorited == true) Strings.unfavorited else Strings.favorited
```

### 3.5 `Strings.kt` 文件超大（1MB+），无法整体读取

- 必须用 `grep_search` 查找已有属性，避免重复定义
- 用 `read_file` + offset/limit 读取特定行范围
- 新属性统一追加到文件末尾的 `}` 之前

### 3.6 替换后立即验证

每完成一个文件就 grep 一次，确认零残留。发现遗漏立即补，不要攒到最后。

### 3.7 computed getter vs 普通属性

```kotlin
// ❌ 错误：普通属性，初始化后不再变化
val xxx: String = when(lang) { ... }

// ✅ 正确：computed getter，每次访问时根据当前语言求值
val xxx: String get() = when(lang) { ... }
```

---

## 四、如何发现遗漏的翻译

**不要依赖"待处理文件列表"**——项目持续迭代，新功能不断加入，静态列表会迅速过时。唯一可靠的方法是：**每次用 grep 扫描，用结果说话。**

### 扫描命令

```bash
# 全量扫描：找出所有包含硬编码中文的 Kotlin 文件
grep -rn "[\x{4e00}-\x{9fff}]" --include="*.kt" app/src/main/java/com/webtoapp/
```

### 判断哪些需要本地化

扫描结果中，**需要本地化**的典型模式：

| 模式 | 示例 | 说明 |
|------|------|------|
| `_message.value = "中文"` | `_message.value = "登录成功"` | Toast / Snackbar 消息 |
| `FormState.Error("中文")` | `FormState.Error("请输入邮箱")` | 表单验证错误 |
| `FormState.Success("中文")` | `FormState.Success("注册成功")` | 操作成功提示 |
| `title = "中文"` | `title = "社区帖子加载失败"` | 失败报告标题 |
| `stage = "中文"` | `stage = "加载帖子列表"` | 失败报告阶段 |
| `summary = "中文"` | `summary = "请求失败，未获取数据"` | 失败报告摘要 |
| `Text("中文")` | `Text("正在加载...")` | Compose 文本 |
| `contentDescription = "中文"` | `contentDescription = "复制"` | 无障碍描述 |

**不需要本地化**的典型模式：

| 模式 | 示例 | 说明 |
|------|------|------|
| Log tag | `AppLogger.e("TAG", ...)` | 第一个参数是日志标签，用户不可见 |
| API 参数 | `reason = "inappropriate"` | 发送给服务端的固定值 |
| 代码注释 | `// 加载更多` | 注释不需要本地化 |
| 调试字符串 | `extraContext = "page: $page"` | 纯技术上下文，用户不可见 |
| `.contains("中文")` | `result.message.contains("过期")` | 匹配服务端返回的中文错误码（需与服务端保持一致） |

### 特殊情况：`.contains("中文")`

服务端返回的错误消息中可能包含中文关键词，客户端用 `.contains()` 匹配来做逻辑判断。这类字符串**通常不应本地化**，因为它是与服务端约定的匹配规则，改了会导致逻辑失效。如需本地化，应改为匹配错误码而非中文字符串。

---

## 五、接手者工作流程

1. **全量扫描**：`grep -rn "[\x{4e00}-\x{9fff}]" --include="*.kt" app/src/main/java/com/webtoapp/`
2. **按上述规则筛选**：排除日志 tag、API 参数、注释、调试字符串等
3. **按文件 → 功能模块** 顺序逐个处理
4. **每个文件**：先加 Strings 属性 → 再替换 → 再验证
5. **替换完成后再次全量 grep** 确认零残留
6. **三种语言缺一不可**
7. **动态字符串用 `%s`/`%d` + `.format()`**
8. **用 computed getter**（`val xxx: String get() = when(lang)`）
9. **不要动非 UI 代码**（纯逻辑、数据模型等）
10. **不要删注释**
