package com.webtoapp.core.errorpage

/**
 * 网络错误诊断库。
 *
 * 目的：把 WebView 抛出的晦涩错误（EADDRNOTAVAIL / ECONNREFUSED / ERR_NAME_NOT_RESOLVED …）
 * 映射成终端用户能看懂的"原因 + 建议动作"，在错误页上直接展示。
 *
 * 设计原则：
 *  - 只匹配高价值、有操作建议的错误；匹配不到时返回 null，交还给调用方决定 fallback 行为。
 *  - 纯函数，无状态、无 IO、不访问 Context，便于复用 / 单测。
 *  - 三语文案内联维护，和 [ErrorPageManager] 的 I18nStrings 保持同样的风格，避免跨 Strings 文件的强耦合。
 */
object NetworkErrorDiagnostics {

    /**
     * 一条诊断结果。所有字段对 UI 都是可展示文本，已经过语言解析，不含占位符。
     */
    data class Diagnostic(
        /** 短标题，用作错误页 H1 / 卡片标题。 */
        val title: String,
        /** 一句话原因解释，给普通用户看。 */
        val cause: String,
        /** 1~4 条可执行建议。先验方案在前。 */
        val suggestions: List<String>,
        /** 建议后续行为：重试是否有意义。 */
        val retryable: Boolean,
        /** 严重程度：info / warning / error。决定 UI 高亮颜色。 */
        val severity: Severity,
        /** 内部命中的诊断键，便于日志与埋点。 */
        val key: String,
    )

    enum class Severity { INFO, WARNING, ERROR }

    /**
     * 主入口。
     *
     * @param rawDescription WebResourceError.description 原文（不要预先 normalize，errno 会被丢失）。
     * @param errorCode WebView 错误码（负数 Android 常量）或 HTTP 状态码。
     * @param failedUrl 失败 URL，用来辅助判断是否访问 loopback / 私网。
     * @param language [com.webtoapp.core.i18n.AppLanguage.name] 字符串，取值 CHINESE / ENGLISH / ARABIC。
     */
    fun diagnose(
        rawDescription: String?,
        errorCode: Int,
        failedUrl: String?,
        language: String,
    ): Diagnostic? {
        val desc = (rawDescription ?: "").uppercase()
        val url = failedUrl ?: ""
        val host = runCatching {
            android.net.Uri.parse(url).host?.lowercase()
        }.getOrNull() ?: ""
        val isLoopback = host == "127.0.0.1" || host == "localhost" || host == "::1" ||
            host.startsWith("127.") || host.endsWith(".localhost")
        val isPrivate = host.startsWith("10.") ||
            host.startsWith("192.168.") ||
            (host.startsWith("172.") && host.substringBefore(".", "").let { _ ->
                val parts = host.split(".")
                parts.size >= 2 && parts[1].toIntOrNull()?.let { it in 16..31 } == true
            })
        val s = strings(language)

        // ========= 1. 地址不可绑定：EADDRNOTAVAIL（用户截图里那条） =========
        if (desc.contains("EADDRNOTAVAIL") || desc.contains("CANNOT ASSIGN REQUESTED ADDRESS")) {
            return Diagnostic(
                title = s.eaddrnotavailTitle,
                cause = s.eaddrnotavailCause,
                suggestions = listOf(
                    s.suggestionDisableProxy,
                    s.suggestionDisableDoh,
                    s.suggestionDisableVpn,
                    s.suggestionRestartNetwork,
                ),
                retryable = true,
                severity = Severity.ERROR,
                key = "EADDRNOTAVAIL",
            )
        }

        // ========= 2. 端口被占：EADDRINUSE =========
        if (desc.contains("EADDRINUSE") || desc.contains("ADDRESS ALREADY IN USE")) {
            return Diagnostic(
                title = s.eaddrinuseTitle,
                cause = s.eaddrinuseCause,
                suggestions = listOf(s.suggestionRestartApp, s.suggestionRebootDevice),
                retryable = true,
                severity = Severity.WARNING,
                key = "EADDRINUSE",
            )
        }

        // ========= 3. 连接被拒：ECONNREFUSED / ERR_CONNECTION_REFUSED =========
        val connectionRefused = desc.contains("ECONNREFUSED") ||
            desc.contains("CONNECTION REFUSED") ||
            desc.contains("CONNECTION_REFUSED") ||
            desc.contains("ERR_CONNECTION_REFUSED")
        if (connectionRefused) {
            return if (isLoopback) {
                Diagnostic(
                    title = s.localServerDownTitle,
                    cause = s.localServerDownCause,
                    suggestions = listOf(
                        s.suggestionWaitRuntime,
                        s.suggestionCheckRuntimeLog,
                        s.suggestionRestartApp,
                    ),
                    retryable = true,
                    severity = Severity.ERROR,
                    key = "LOCAL_CONN_REFUSED",
                )
            } else {
                Diagnostic(
                    title = s.econnrefusedTitle,
                    cause = s.econnrefusedCause,
                    suggestions = listOf(s.suggestionCheckSite, s.suggestionRetryLater),
                    retryable = true,
                    severity = Severity.WARNING,
                    key = "ECONNREFUSED",
                )
            }
        }

        // ========= 4. 网络不可达 / 主机不可达 =========
        if (desc.contains("ENETUNREACH") || desc.contains("NETWORK IS UNREACHABLE") ||
            desc.contains("ERR_NETWORK_CHANGED") || desc.contains("ERR_INTERNET_DISCONNECTED")
        ) {
            return Diagnostic(
                title = s.netUnreachableTitle,
                cause = s.netUnreachableCause,
                suggestions = listOf(s.suggestionCheckNetwork, s.suggestionToggleAirplane),
                retryable = true,
                severity = Severity.WARNING,
                key = "ENETUNREACH",
            )
        }
        if (desc.contains("EHOSTUNREACH") || desc.contains("NO ROUTE TO HOST")) {
            val suggestions = mutableListOf(s.suggestionCheckNetwork)
            if (isPrivate) suggestions += s.suggestionCheckSameLan
            suggestions += s.suggestionDisableVpn
            return Diagnostic(
                title = s.hostUnreachableTitle,
                cause = s.hostUnreachableCause,
                suggestions = suggestions,
                retryable = true,
                severity = Severity.WARNING,
                key = "EHOSTUNREACH",
            )
        }

        // ========= 5. 超时 =========
        if (desc.contains("ETIMEDOUT") || desc.contains("TIMED_OUT") ||
            desc.contains("ERR_TIMED_OUT") || desc.contains("ERR_CONNECTION_TIMED_OUT") ||
            errorCode == android.webkit.WebViewClient.ERROR_TIMEOUT
        ) {
            return Diagnostic(
                title = s.timeoutTitle,
                cause = s.timeoutCause,
                suggestions = listOf(s.suggestionRetryLater, s.suggestionCheckNetwork, s.suggestionDisableProxy),
                retryable = true,
                severity = Severity.WARNING,
                key = "ETIMEDOUT",
            )
        }

        // ========= 6. DNS 解析失败 =========
        if (desc.contains("ERR_NAME_NOT_RESOLVED") || desc.contains("ERR_NAME_RESOLUTION_FAILED") ||
            errorCode == android.webkit.WebViewClient.ERROR_HOST_LOOKUP
        ) {
            return Diagnostic(
                title = s.dnsTitle,
                cause = s.dnsCause,
                suggestions = listOf(s.suggestionCheckUrl, s.suggestionDisableDoh, s.suggestionCheckNetwork),
                retryable = true,
                severity = Severity.WARNING,
                key = "DNS",
            )
        }

        // ========= 7. Cleartext 被阻 =========
        if (desc.contains("CLEARTEXT") || desc.contains("ERR_CLEARTEXT_NOT_PERMITTED")) {
            return Diagnostic(
                title = s.cleartextTitle,
                cause = s.cleartextCause,
                suggestions = listOf(s.suggestionUseHttps),
                retryable = true,
                severity = Severity.WARNING,
                key = "CLEARTEXT",
            )
        }

        // ========= 8. SSL 握手失败 =========
        if (desc.contains("ERR_SSL") || desc.contains("SSL_ERROR") ||
            errorCode == android.webkit.WebViewClient.ERROR_FAILED_SSL_HANDSHAKE
        ) {
            return Diagnostic(
                title = s.sslTitle,
                cause = s.sslCause,
                suggestions = listOf(s.suggestionCheckDeviceTime, s.suggestionDisableProxy, s.suggestionDisableVpn),
                retryable = true,
                severity = Severity.ERROR,
                key = "SSL",
            )
        }

        // 其它错误：交给调用方默认处理
        return null
    }

    // ------------------------------------------------------------------
    // i18n
    // ------------------------------------------------------------------

    private data class Strings(
        val eaddrnotavailTitle: String,
        val eaddrnotavailCause: String,
        val eaddrinuseTitle: String,
        val eaddrinuseCause: String,
        val econnrefusedTitle: String,
        val econnrefusedCause: String,
        val localServerDownTitle: String,
        val localServerDownCause: String,
        val netUnreachableTitle: String,
        val netUnreachableCause: String,
        val hostUnreachableTitle: String,
        val hostUnreachableCause: String,
        val timeoutTitle: String,
        val timeoutCause: String,
        val dnsTitle: String,
        val dnsCause: String,
        val cleartextTitle: String,
        val cleartextCause: String,
        val sslTitle: String,
        val sslCause: String,
        val suggestionDisableProxy: String,
        val suggestionDisableDoh: String,
        val suggestionDisableVpn: String,
        val suggestionRestartNetwork: String,
        val suggestionRestartApp: String,
        val suggestionRebootDevice: String,
        val suggestionWaitRuntime: String,
        val suggestionCheckRuntimeLog: String,
        val suggestionCheckSite: String,
        val suggestionRetryLater: String,
        val suggestionCheckNetwork: String,
        val suggestionToggleAirplane: String,
        val suggestionCheckSameLan: String,
        val suggestionCheckUrl: String,
        val suggestionUseHttps: String,
        val suggestionCheckDeviceTime: String,
    )

    private fun strings(language: String): Strings = when (language.uppercase()) {
        "ENGLISH" -> english()
        "ARABIC" -> arabic()
        else -> chinese()
    }

    private fun chinese() = Strings(
        eaddrnotavailTitle = "无法绑定本机网络地址",
        eaddrnotavailCause = "系统拒绝绑定请求的本机地址（EADDRNOTAVAIL）。通常是代理、DoH 或 VPN 指向了一个当前设备无法使用的地址。",
        eaddrinuseTitle = "端口已被占用",
        eaddrinuseCause = "本地服务要用的端口已经被其它程序占用（EADDRINUSE）。",
        econnrefusedTitle = "连接被对方拒绝",
        econnrefusedCause = "目标服务器主动拒绝了连接。",
        localServerDownTitle = "本地服务未启动",
        localServerDownCause = "应用内置的本地服务（Node.js / PHP / Python / Go / WordPress）未监听请求的端口。",
        netUnreachableTitle = "网络不可用",
        netUnreachableCause = "当前设备没有有效的网络连接。",
        hostUnreachableTitle = "无法到达目标主机",
        hostUnreachableCause = "网络路由层面找不到到目标主机的路径。",
        timeoutTitle = "连接超时",
        timeoutCause = "目标服务器在合理时间内没有响应。",
        dnsTitle = "域名解析失败",
        dnsCause = "无法把这个域名解析成 IP 地址。",
        cleartextTitle = "不允许明文 HTTP",
        cleartextCause = "当前应用安全策略阻止了非 HTTPS 的明文请求。",
        sslTitle = "HTTPS 证书握手失败",
        sslCause = "和服务器建立安全连接时证书校验不通过。",
        suggestionDisableProxy = "关闭应用设置里的代理配置，或改回「直连」",
        suggestionDisableDoh = "把 DNS-over-HTTPS 切回「系统默认」",
        suggestionDisableVpn = "关闭设备上的 VPN / 流量防火墙类应用",
        suggestionRestartNetwork = "切换到其它网络后再试（WiFi ↔ 移动网络）",
        suggestionRestartApp = "关闭应用后重新打开",
        suggestionRebootDevice = "如果多次出现，重启设备",
        suggestionWaitRuntime = "稍等 5–10 秒等本地运行时启动完成后重试",
        suggestionCheckRuntimeLog = "在主应用里查看该子应用的运行时日志",
        suggestionCheckSite = "确认目标站点地址或联系对方",
        suggestionRetryLater = "稍后再试",
        suggestionCheckNetwork = "检查网络是否连通，确认没有进入飞行模式",
        suggestionToggleAirplane = "尝试开关一次飞行模式",
        suggestionCheckSameLan = "确认设备和目标设备在同一个局域网",
        suggestionCheckUrl = "检查地址是否拼写正确",
        suggestionUseHttps = "把 URL 协议改为 https",
        suggestionCheckDeviceTime = "确认设备日期和时间正确",
    )

    private fun english() = Strings(
        eaddrnotavailTitle = "Can't bind a local network address",
        eaddrnotavailCause = "The system refused to bind the requested local address (EADDRNOTAVAIL). Usually a proxy, DoH, or VPN is pointing at an address this device can't use.",
        eaddrinuseTitle = "Port already in use",
        eaddrinuseCause = "Another process is already using the port this service needs (EADDRINUSE).",
        econnrefusedTitle = "Connection refused",
        econnrefusedCause = "The target server actively refused the connection.",
        localServerDownTitle = "Local server isn't running",
        localServerDownCause = "The app's embedded local runtime (Node.js / PHP / Python / Go / WordPress) isn't listening on the requested port.",
        netUnreachableTitle = "Network unavailable",
        netUnreachableCause = "This device has no working network connection.",
        hostUnreachableTitle = "Host unreachable",
        hostUnreachableCause = "No route exists from this network to the target host.",
        timeoutTitle = "Connection timed out",
        timeoutCause = "The server didn't respond within a reasonable time.",
        dnsTitle = "Domain name lookup failed",
        dnsCause = "The hostname couldn't be resolved to an IP address.",
        cleartextTitle = "Cleartext HTTP is blocked",
        cleartextCause = "This app's security policy blocks non-HTTPS requests.",
        sslTitle = "HTTPS handshake failed",
        sslCause = "The server's certificate failed verification while establishing a secure connection.",
        suggestionDisableProxy = "Disable the proxy in app settings, or switch back to Direct",
        suggestionDisableDoh = "Switch DNS-over-HTTPS back to System default",
        suggestionDisableVpn = "Turn off any VPN or traffic-firewall apps",
        suggestionRestartNetwork = "Switch to a different network (Wi-Fi ↔ mobile) and try again",
        suggestionRestartApp = "Close and reopen the app",
        suggestionRebootDevice = "If this keeps happening, reboot the device",
        suggestionWaitRuntime = "Wait 5–10 seconds for the local runtime to finish starting, then retry",
        suggestionCheckRuntimeLog = "Check the runtime log for this app from the host app",
        suggestionCheckSite = "Verify the target URL or contact the site owner",
        suggestionRetryLater = "Try again later",
        suggestionCheckNetwork = "Check connectivity and make sure airplane mode is off",
        suggestionToggleAirplane = "Toggle airplane mode once",
        suggestionCheckSameLan = "Make sure your device and the target are on the same LAN",
        suggestionCheckUrl = "Check the URL for typos",
        suggestionUseHttps = "Change the URL protocol to https",
        suggestionCheckDeviceTime = "Make sure the device date and time are correct",
    )

    private fun arabic() = Strings(
        eaddrnotavailTitle = "تعذّر ربط عنوان شبكة محلي",
        eaddrnotavailCause = "رفض النظام ربط العنوان المحلي المطلوب (EADDRNOTAVAIL). عادةً بسبب وكيل أو DoH أو VPN يشير إلى عنوان لا يمكن للجهاز استخدامه.",
        eaddrinuseTitle = "المنفذ قيد الاستخدام بالفعل",
        eaddrinuseCause = "عملية أخرى تستخدم بالفعل المنفذ الذي تحتاجه هذه الخدمة (EADDRINUSE).",
        econnrefusedTitle = "تم رفض الاتصال",
        econnrefusedCause = "رفض الخادم الهدف الاتصال.",
        localServerDownTitle = "الخدمة المحلية لا تعمل",
        localServerDownCause = "لا يستمع وقت التشغيل المحلي المضمن (Node.js / PHP / Python / Go / WordPress) للمنفذ المطلوب.",
        netUnreachableTitle = "الشبكة غير متاحة",
        netUnreachableCause = "لا يوجد اتصال شبكة صالح بهذا الجهاز.",
        hostUnreachableTitle = "تعذّر الوصول إلى المضيف",
        hostUnreachableCause = "لا يوجد مسار من هذه الشبكة إلى المضيف الهدف.",
        timeoutTitle = "انتهت مهلة الاتصال",
        timeoutCause = "لم يستجب الخادم خلال وقت معقول.",
        dnsTitle = "فشل تحليل اسم النطاق",
        dnsCause = "تعذّر ترجمة اسم المضيف إلى عنوان IP.",
        cleartextTitle = "HTTP غير مشفّر ممنوع",
        cleartextCause = "سياسة أمان هذا التطبيق تمنع الطلبات غير المشفّرة بـ HTTPS.",
        sslTitle = "فشل مصافحة HTTPS",
        sslCause = "فشل التحقق من شهادة الخادم أثناء إنشاء الاتصال الآمن.",
        suggestionDisableProxy = "عطّل الوكيل من إعدادات التطبيق أو اضبطه على الاتصال المباشر",
        suggestionDisableDoh = "أعد DNS-over-HTTPS إلى الإعداد الافتراضي للنظام",
        suggestionDisableVpn = "أوقف أي تطبيقات VPN أو جدار حماية للشبكة",
        suggestionRestartNetwork = "بدّل إلى شبكة أخرى (Wi-Fi ↔ بيانات الجوال) وأعد المحاولة",
        suggestionRestartApp = "أغلق التطبيق وأعد فتحه",
        suggestionRebootDevice = "إذا تكرّر الخطأ، أعد تشغيل الجهاز",
        suggestionWaitRuntime = "انتظر 5–10 ثوانٍ حتى يكتمل تشغيل وقت التشغيل المحلي ثم أعد المحاولة",
        suggestionCheckRuntimeLog = "اطّلع على سجل وقت تشغيل هذا التطبيق من التطبيق الرئيسي",
        suggestionCheckSite = "تحقّق من عنوان الموقع الهدف أو تواصل مع صاحبه",
        suggestionRetryLater = "أعد المحاولة لاحقًا",
        suggestionCheckNetwork = "تحقق من الاتصال وتأكد أن وضع الطيران مغلق",
        suggestionToggleAirplane = "قم بتشغيل وإيقاف وضع الطيران مرة واحدة",
        suggestionCheckSameLan = "تأكد أن جهازك والجهاز الهدف على نفس الشبكة المحلية",
        suggestionCheckUrl = "تحقق من صحة العنوان",
        suggestionUseHttps = "غيّر بروتوكول الرابط إلى https",
        suggestionCheckDeviceTime = "تأكد من صحة تاريخ الجهاز ووقته",
    )
}
