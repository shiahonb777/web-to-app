# APK 模板说明

此目录用于存放预编译的 WebView Shell APK 模板。

## 模板要求

模板 APK (`webview_shell.apk`) 需要满足以下条件：

1. 包含 `assets/app_config.json` 配置文件入口
2. 启动时读取配置并加载对应 URL
3. 支持以下配置项：
   - appName: 应用名称
   - targetUrl: 目标网址
   - activationEnabled: 是否启用激活码
   - adBlockEnabled: 是否启用广告拦截
   - 等等...

## 动态构建模式

如果没有预编译模板，系统将使用当前应用作为基础进行动态构建。
这种模式下，配置将注入到 assets/app_config.json 中。

## 注意事项

- 模板 APK 需要使用 debug 签名或不签名
- 构建时会自动替换图标和重新签名
- 包名修改需要额外的 ARSC 处理（当前版本暂不支持）
