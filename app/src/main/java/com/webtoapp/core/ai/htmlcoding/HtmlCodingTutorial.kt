package com.webtoapp.core.ai.htmlcoding

/**
 * HTML编程AI - 教程内容
 * 帮助用户更好地使用AI生成HTML
 */
object HtmlCodingTutorial {

    /**
     * 教程章节
     */
    data class TutorialChapter(
        val id: String,
        val title: String,
        val icon: String,           // Material Icon 名称
        val sections: List<TutorialSection>
    )

    /**
     * 教程小节
     */
    data class TutorialSection(
        val title: String,
        val content: String,
        val codeExample: String? = null,
        val tips: List<String> = emptyList()
    )

    /**
     * 所有教程章节
     */
    val chapters = listOf(
        // ===== 第一章：基础概念 =====
        TutorialChapter(
            id = "basics",
            title = "基础概念",
            icon = "School",
            sections = listOf(
                TutorialSection(
                    title = "什么是HTML？",
                    content = """
HTML（HyperText Markup Language）是构建网页的标准标记语言。它通过标签来定义网页的结构和内容。

**核心要素：**
- **标签**：如 `<div>`、`<p>`、`<h1>` 等，用于定义元素
- **属性**：如 `class`、`id`、`style`，用于配置元素
- **嵌套**：元素可以包含其他元素，形成树状结构

**现代HTML5特性：**
- 语义化标签：`<header>`、`<nav>`、`<main>`、`<footer>`
- 多媒体支持：`<video>`、`<audio>`
- 表单增强：日期选择器、颜色选择器等
                    """.trimIndent(),
                    codeExample = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>我的网页</title>
</head>
<body>
    <header>页头</header>
    <main>主要内容</main>
    <footer>页脚</footer>
</body>
</html>
                    """.trimIndent(),
                    tips = listOf(
                        "告诉AI使用语义化标签，代码更规范",
                        "HTML5不需要关闭某些标签如<br>、<img>"
                    )
                ),
                TutorialSection(
                    title = "什么是CSS？",
                    content = """
CSS（Cascading Style Sheets）用于控制网页的视觉呈现，包括布局、颜色、字体、动画等。

**核心概念：**
- **选择器**：定位要样式化的元素
- **属性**：如 `color`、`margin`、`display`
- **盒模型**：content → padding → border → margin

**现代CSS特性：**
- **Flexbox**：一维布局神器
- **Grid**：二维网格布局
- **CSS变量**：`--primary-color: #3B82F6;`
- **动画**：`transition`、`animation`、`@keyframes`
                    """.trimIndent(),
                    codeExample = """
/* CSS变量定义 */
:root {
    --primary: #3B82F6;
    --radius: 8px;
}

/* Flexbox居中 */
.container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
}

/* 卡片样式 */
.card {
    background: white;
    border-radius: var(--radius);
    padding: 20px;
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}
                    """.trimIndent(),
                    tips = listOf(
                        "让AI使用CSS变量，方便后续调整主题",
                        "优先使用Flexbox和Grid，避免float布局"
                    )
                ),
                TutorialSection(
                    title = "什么是SVG？",
                    content = """
SVG（Scalable Vector Graphics）是一种矢量图形格式，可以无限缩放而不失真。

**为什么使用SVG：**
- **无限缩放**：不像PNG/JPG，放大不会模糊
- **文件小**：简单图形比位图小很多
- **可编程**：可以用CSS和JavaScript操控
- **可内嵌**：直接写在HTML中

**常见用途：**
- 图标（Icons）
- Logo
- 插画
- 数据可视化图表
- 动画效果

**让AI生成SVG的技巧：**
告诉AI "使用内嵌SVG图标" 或 "用SVG画一个..."
                    """.trimIndent(),
                    codeExample = """
<!-- 简单的SVG图标 -->
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
    <circle cx="12" cy="12" r="10"/>
    <path d="M12 6v6l4 2"/>
</svg>

<!-- SVG可以用CSS样式化 -->
<style>
    .icon { 
        color: #3B82F6; 
        transition: transform 0.2s;
    }
    .icon:hover { 
        transform: scale(1.1); 
    }
</style>
                    """.trimIndent(),
                    tips = listOf(
                        "告诉AI：'所有图标使用内嵌SVG，不要用图片'",
                        "SVG图标可以继承父元素的color属性",
                        "AI可以生成复杂的SVG插画和动画"
                    )
                ),
                TutorialSection(
                    title = "什么是TailwindCSS？",
                    content = """
TailwindCSS是一个实用优先（Utility-First）的CSS框架，通过组合小型工具类来构建界面。

**为什么选择Tailwind：**
- **快速开发**：无需写CSS文件
- **一致性**：内置设计系统
- **响应式**：简单的断点前缀 `md:`, `lg:`
- **状态变体**：`hover:`, `focus:`, `dark:`

**常用类名示例：**
- 间距：`p-4`(padding), `m-2`(margin), `gap-4`
- 颜色：`bg-blue-500`, `text-white`, `border-gray-200`
- 布局：`flex`, `grid`, `items-center`, `justify-between`
- 尺寸：`w-full`, `h-screen`, `max-w-md`
- 圆角：`rounded`, `rounded-lg`, `rounded-full`

**使用方法：**
通过CDN快速引入：
                    """.trimIndent(),
                    codeExample = """
<!DOCTYPE html>
<html>
<head>
    <!-- 引入TailwindCSS CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 min-h-screen flex items-center justify-center">
    <div class="bg-white p-8 rounded-xl shadow-lg max-w-md">
        <h1 class="text-2xl font-bold text-gray-800 mb-4">
            欢迎使用
        </h1>
        <p class="text-gray-600 mb-6">
            这是一个使用TailwindCSS的示例。
        </p>
        <button class="w-full bg-blue-500 hover:bg-blue-600 text-white font-medium py-2 px-4 rounded-lg transition">
            开始
        </button>
    </div>
</body>
</html>
                    """.trimIndent(),
                    tips = listOf(
                        "告诉AI '使用TailwindCSS CDN'",
                        "Tailwind生成的代码更容易理解和修改",
                        "可以组合使用，如 'hover:bg-blue-600 transition'"
                    )
                )
            )
        ),

        // ===== 第二章：设计风格 =====
        TutorialChapter(
            id = "design-styles",
            title = "设计风格",
            icon = "Palette",
            sections = listOf(
                TutorialSection(
                    title = "什么是设计风格？",
                    content = """
设计风格是视觉元素的整体组合方式，包括配色、排版、形状、质感等。一致的设计风格能让界面更专业、更有辨识度。

**风格的核心要素：**
- **配色方案**：主色、次色、背景色、强调色
- **排版**：字体、字号层级、行高、间距
- **形状语言**：圆角程度、几何vs有机
- **质感**：扁平、拟物、玻璃、渐变
- **动效**：过渡方式、缓动曲线

**如何定义风格：**
你可以用一个词来概括风格，AI会理解并模仿。比如：
- 一部电影：《阿凡达》风格
- 一本书：《哈利波特》风格
- 一个品牌：苹果风格
- 一种流派：赛博朋克风格
                    """.trimIndent(),
                    tips = listOf(
                        "直接说'这个网页要有《你的名字》那种唯美感'",
                        "AI非常擅长模仿已知的视觉风格",
                        "可以混合风格：'赛博朋克风格但配色要柔和'"
                    )
                ),
                TutorialSection(
                    title = "风格参考词的魔力",
                    content = """
AI对人类文化有广泛的理解，你只需要说出一个它知道的名字，它就能生成对应风格的设计。

**电影风格示例：**
- **《银翼杀手》风格**：霓虹灯、雨夜、赛博朋克
- **《阿凡达》风格**：生物发光、外星植物、科幻
- **《哈利波特》风格**：魔法、古典、神秘学院
- **《星球大战》风格**：太空歌剧、全息投影

**动画风格示例：**
- **吉卜力风格**：温暖、自然、手绘质感
- **新海诚风格**：极致光影、唯美青春
- **赛博朋克边缘行者**：霓虹、暴力美学

**品牌风格示例：**
- **苹果风格**：极简、精致、大量留白
- **Spotify风格**：活力渐变、音乐感
- **赛博朋克2077**：黄色警告色、故障艺术
                    """.trimIndent(),
                    tips = listOf(
                        "告诉AI：'做一个《查理九世》风格的神秘解谜页面'",
                        "可以具体到角色：'蜘蛛侠的红蓝配色风格'",
                        "结合场景：'霍格沃茨图书馆的感觉'"
                    )
                ),
                TutorialSection(
                    title = "流行设计趋势",
                    content = """
了解当前流行的设计趋势，可以让你的作品更现代。

**玻璃拟态（Glassmorphism）**
透明模糊的玻璃效果，创造层次感。
关键词：`backdrop-filter: blur()`, 半透明背景

**新拟物化（Neumorphism）**
柔和阴影创造的凸起/凹陷效果。
关键词：双层阴影（亮/暗），柔和背景色

**渐变回归**
多彩的渐变色彩重新流行。
关键词：`linear-gradient`, 渐变边框

**暗黑模式**
护眼且现代的深色界面。
关键词：深色背景，亮色文字，发光效果

**微交互**
细微的动画反馈提升体验。
关键词：hover效果，按钮反馈，加载动画
                    """.trimIndent(),
                    codeExample = """
/* 玻璃拟态效果 */
.glass {
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-radius: 16px;
}

/* 新拟物化效果 */
.neumorphic {
    background: #e0e5ec;
    box-shadow: 
        8px 8px 16px #b8bcc2,
        -8px -8px 16px #ffffff;
    border-radius: 12px;
}
                    """.trimIndent(),
                    tips = listOf(
                        "说'用玻璃拟态风格'，AI就会应用这种效果",
                        "可以组合趋势：'暗黑主题+玻璃拟态'"
                    )
                )
            )
        ),

        // ===== 第三章：交互设计 =====
        TutorialChapter(
            id = "interaction",
            title = "交互设计",
            icon = "TouchApp",
            sections = listOf(
                TutorialSection(
                    title = "什么是交互？",
                    content = """
交互（Interaction）是用户与界面之间的对话。好的交互设计让用户操作自然、反馈清晰。

**交互的基本原则：**

**1. 可见性**
用户能看到可以做什么。
- 按钮看起来可点击
- 输入框有明确边界
- 可拖拽的元素有把手

**2. 反馈**
每个操作都有响应。
- 点击有按压效果
- 加载有进度提示
- 成功/失败有提示

**3. 一致性**
相同操作有相同反馈。
- 所有按钮行为一致
- 相同类型元素样式一致

**4. 容错**
允许用户犯错和撤销。
- 危险操作有确认
- 提供撤销选项
                    """.trimIndent(),
                    tips = listOf(
                        "告诉AI：'所有按钮添加hover和active状态'",
                        "要求AI：'添加加载状态和成功提示'"
                    )
                ),
                TutorialSection(
                    title = "CSS动画与过渡",
                    content = """
动画是提升交互体验的重要手段，但要适度使用。

**过渡（Transition）**
简单的状态变化动画。
- 适用于：hover效果、展开收起
- 属性：`transition: all 0.3s ease`

**动画（Animation）**
复杂的、可循环的动画序列。
- 适用于：加载动画、引导动画
- 属性：`animation`, `@keyframes`

**性能优化**
只动画这些属性（GPU加速）：
- `transform`（移动、缩放、旋转）
- `opacity`（透明度）
- 避免动画`width`、`height`、`margin`
                    """.trimIndent(),
                    codeExample = """
/* 按钮交互效果 */
.button {
    background: #3B82F6;
    color: white;
    padding: 12px 24px;
    border-radius: 8px;
    transition: all 0.2s ease;
    transform: translateY(0);
}

.button:hover {
    background: #2563EB;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.button:active {
    transform: translateY(0);
}

/* 加载动画 */
@keyframes spin {
    to { transform: rotate(360deg); }
}

.loading {
    width: 24px;
    height: 24px;
    border: 3px solid #e5e7eb;
    border-top-color: #3B82F6;
    border-radius: 50%;
    animation: spin 1s linear infinite;
}
                    """.trimIndent(),
                    tips = listOf(
                        "说'添加流畅的动画效果'",
                        "指定'使用transform动画保证性能'",
                        "要求'添加骨架屏加载效果'"
                    )
                ),
                TutorialSection(
                    title = "响应式设计",
                    content = """
响应式设计让网页在不同设备上都有良好的显示效果。

**核心技术：**

**1. 媒体查询**
```css
@media (max-width: 768px) {
    .sidebar { display: none; }
}
```

**2. 相对单位**
- `%` - 相对父元素
- `vw/vh` - 相对视口
- `rem` - 相对根字体大小
- `em` - 相对当前字体大小

**3. 弹性布局**
- Flexbox: `flex-wrap: wrap`
- Grid: `grid-template-columns: repeat(auto-fit, minmax(300px, 1fr))`

**断点参考：**
- 手机：< 640px
- 平板：640px - 1024px
- 桌面：> 1024px
                    """.trimIndent(),
                    tips = listOf(
                        "告诉AI：'必须适配移动端'",
                        "说'使用移动优先的响应式设计'",
                        "要求'在手机上隐藏侧边栏，显示汉堡菜单'"
                    )
                )
            )
        ),

        // ===== 第四章：高效提示词 =====
        TutorialChapter(
            id = "prompting",
            title = "提示词技巧",
            icon = "AutoAwesome",
            sections = listOf(
                TutorialSection(
                    title = "如何写好提示词？",
                    content = """
好的提示词能让AI准确理解你的需求，生成更符合预期的代码。

**提示词结构：**

**1. 说明你要什么**
"做一个登录页面"

**2. 描述视觉风格**
"使用玻璃拟态风格，深色渐变背景"

**3. 指定技术要求**
"使用TailwindCSS，单文件输出"

**4. 列出功能需求**
"包含用户名、密码输入框，记住我选项，登录按钮"

**5. 补充细节**
"按钮要有hover动画，输入框有focus高亮"

**完整示例：**
"做一个登录页面，使用玻璃拟态风格，深紫色渐变背景。用TailwindCSS CDN。包含邮箱、密码输入框，记住我复选框，登录按钮，还有'忘记密码'和'注册'链接。所有交互要有动画效果。"
                    """.trimIndent(),
                    tips = listOf(
                        "越具体越好，但也不用过度详细",
                        "可以分步骤提需求，先框架后细节",
                        "如果效果不好，补充说明或重新描述"
                    )
                ),
                TutorialSection(
                    title = "常用提示词模板",
                    content = """
这里是一些经过验证的高效提示词模板：

**完整页面模板：**
"创建一个[页面类型]，风格是[风格描述]。使用[技术栈]。包含以下功能：[功能列表]。要求：[额外要求]。"

**组件模板：**
"做一个[组件名称]组件，[视觉描述]。功能：[功能描述]。交互：[交互要求]。"

**修改优化模板：**
"在当前基础上，[修改内容]。保持[保留内容]不变。"

**风格迁移模板：**
"把当前页面改成[目标风格]风格，保持功能不变，调整[调整项目]。"
                    """.trimIndent(),
                    codeExample = """
// 示例提示词

// 1. 完整页面
"创建一个个人作品集首页，赛博朋克风格。使用TailwindCSS。
包含：导航栏、英雄区域（大标题+副标题+CTA按钮）、
作品展示网格、技能标签、联系表单。要求响应式，有炫酷的动画。"

// 2. 组件
"做一个定价卡片组件，三列布局，中间推荐套餐突出显示。
每个卡片有：标题、价格、功能列表、按钮。
推荐卡片要有发光边框效果。"

// 3. 修改
"把按钮改成圆角更大的胶囊形状，颜色换成渐变色。
添加点击时的缩放动画。"

// 4. 风格迁移
"把这个页面改成吉卜力风格，保持布局不变。
背景换成天空和云朵，配色改成温暖的自然色系。"
                    """.trimIndent(),
                    tips = listOf(
                        "保存你常用的提示词模板",
                        "针对不同类型的页面使用不同模板",
                        "好的提示词可以复用和微调"
                    )
                ),
                TutorialSection(
                    title = "避免常见错误",
                    content = """
这些是新手常犯的提示词错误：

**❌ 太模糊**
"做个好看的网页"
→ AI不知道什么是"好看"

**✅ 改进**
"做一个现代简约风格的网页，配色用蓝白灰，大量留白"

---

**❌ 太笼统**
"做个商城"
→ 商城太复杂，AI无法一次完成

**✅ 改进**
"做商城的商品详情页，包含图片轮播、商品信息、规格选择、加购按钮"

---

**❌ 矛盾的要求**
"做个极简风格的页面，要有很多装饰和动画"
→ 极简和很多装饰矛盾

**✅ 改进**
"做个简洁的页面，少量精致的微交互动画"

---

**❌ 假设AI知道上下文**
"改一下那个按钮"
→ AI不知道是哪个按钮

**✅ 改进**
"把页面底部的'提交'按钮改成绿色"
                    """.trimIndent(),
                    tips = listOf(
                        "如果AI理解错了，重新明确描述",
                        "复杂需求分步骤提出",
                        "检查你的要求是否自相矛盾"
                    )
                )
            )
        ),

        // ===== 第五章：实战案例 =====
        TutorialChapter(
            id = "examples",
            title = "实战案例",
            icon = "Code",
            sections = listOf(
                TutorialSection(
                    title = "案例1：落地页",
                    content = """
**场景**：为一个产品创建营销落地页

**提示词示例：**
"创建一个SaaS产品的落地页，现代渐变风格。

结构：
1. 导航栏 - Logo、导航链接、CTA按钮
2. 英雄区 - 大标题、副标题、两个按钮（主要+次要）、产品截图
3. 特性区 - 三列图标+标题+描述的特性展示
4. 定价区 - 三个定价卡片，中间突出
5. FAQ区 - 手风琴式问答
6. 页脚 - 链接分组、社交媒体图标、版权

要求：
- 使用TailwindCSS
- 响应式设计
- 平滑滚动
- 所有图标用SVG"
                    """.trimIndent(),
                    tips = listOf(
                        "落地页适合分区块描述",
                        "指定每个区块的组成元素",
                        "CTA按钮要突出"
                    )
                ),
                TutorialSection(
                    title = "案例2：仪表盘",
                    content = """
**场景**：创建数据仪表盘界面

**提示词示例：**
"创建一个数据分析仪表盘，暗黑主题。

布局：
- 左侧：收起的侧边导航栏（图标+悬停显示文字）
- 顶部：搜索框、通知图标、用户头像
- 主区域：卡片网格布局

卡片内容（用CSS模拟数据，不要真实数据）：
1. 数字统计卡 x4 - 图标、数字、标签、趋势箭头
2. 折线图区域 - 用SVG画一个简单的折线图形状
3. 最近活动列表 - 5条记录样式
4. 环形进度图 - 用SVG画

要求：
- 配色用深灰配霓虹蓝/绿/紫
- 卡片有微妙的发光效果
- 数字用等宽字体"
                    """.trimIndent(),
                    tips = listOf(
                        "仪表盘重点是布局结构",
                        "可以用SVG形状代替真实图表",
                        "暗色主题配发光效果很酷"
                    )
                ),
                TutorialSection(
                    title = "案例3：互动游戏",
                    content = """
**场景**：创建一个简单的HTML5小游戏

**提示词示例：**
"创建一个贪吃蛇小游戏，复古像素风格。

功能：
- Canvas绘制游戏画面
- 方向键或WASD控制
- 吃食物增长、加分
- 碰到边界或自己游戏结束
- 显示当前分数和最高分
- 开始/重新开始按钮
- 移动端支持触摸方向按钮

视觉：
- 像素化的蛇和食物
- 绿色蛇身、红色食物
- 复古的网格背景
- 像素字体显示分数
- 游戏结束有闪烁效果

音效：（可选）
- 吃食物音效
- 游戏结束音效"
                    """.trimIndent(),
                    tips = listOf(
                        "游戏要明确控制方式",
                        "Canvas游戏需要游戏循环",
                        "添加移动端支持很重要"
                    )
                ),
                TutorialSection(
                    title = "案例4：交互表单",
                    content = """
**场景**：创建一个多步骤表单

**提示词示例：**
"创建一个多步骤注册表单，玻璃拟态风格，渐变背景。

步骤：
1. 账号信息 - 邮箱、密码、确认密码
2. 个人信息 - 姓名、头像上传区域、生日选择
3. 偏好设置 - 兴趣标签多选、通知开关
4. 完成 - 成功动画、确认信息摘要

功能：
- 顶部步骤指示器（显示当前步骤）
- 上一步/下一步按钮
- 表单验证（邮箱格式、密码强度、必填项）
- 错误提示显示在输入框下方
- 密码强度指示条
- 完成时显示打勾动画

交互：
- 步骤切换有滑动动画
- 输入框focus有边框高亮
- 错误时输入框抖动
- 按钮loading状态"
                    """.trimIndent(),
                    tips = listOf(
                        "多步骤表单要有进度指示",
                        "表单验证提升用户体验",
                        "完成动画给用户成就感"
                    )
                )
            )
        )
    )

    /**
     * 获取章节通过ID
     */
    fun getChapterById(id: String): TutorialChapter? = chapters.find { it.id == id }

    /**
     * 快速提示词示例（用于快速复制）
     */
    val quickPrompts: List<String> get() = listOf(
        com.webtoapp.core.i18n.Strings.quickPrompt1,
        com.webtoapp.core.i18n.Strings.quickPrompt2,
        com.webtoapp.core.i18n.Strings.quickPrompt3,
        com.webtoapp.core.i18n.Strings.quickPrompt4,
        com.webtoapp.core.i18n.Strings.quickPrompt5,
        com.webtoapp.core.i18n.Strings.quickPrompt6,
        com.webtoapp.core.i18n.Strings.quickPrompt7,
        com.webtoapp.core.i18n.Strings.quickPrompt8
    )
}
