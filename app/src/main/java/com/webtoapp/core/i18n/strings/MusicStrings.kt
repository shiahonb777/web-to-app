package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object MusicStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== Online Music ====================
    val onlineMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "在线音乐"
        AppLanguage.ENGLISH -> "Online Music"
        AppLanguage.ARABIC -> "موسيقى عبر الإنترنت"
    }

    val searchSongName: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索歌曲名称"
        AppLanguage.ENGLISH -> "Search song name"
        AppLanguage.ARABIC -> "البحث عن اسم الأغنية"
    }

    val paid: String get() = when (lang) {
        AppLanguage.CHINESE -> "付费"
        AppLanguage.ENGLISH -> "Paid"
        AppLanguage.ARABIC -> "مدفوع"
    }

    val musicChannel: String get() = when (lang) {
        AppLanguage.CHINESE -> "音乐渠道"
        AppLanguage.ENGLISH -> "Music Channel"
        AppLanguage.ARABIC -> "قناة الموسيقى"
    }

    val testConnection: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试连接"
        AppLanguage.ENGLISH -> "Test Connection"
        AppLanguage.ARABIC -> "اختبار الاتصال"
    }

    val testAllChannels: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试全部"
        AppLanguage.ENGLISH -> "Test All"
        AppLanguage.ARABIC -> "اختبار الكل"
    }

    val channelAvailable: String get() = when (lang) {
        AppLanguage.CHINESE -> "可用"
        AppLanguage.ENGLISH -> "Available"
        AppLanguage.ARABIC -> "متاح"
    }

    val channelUnavailable: String get() = when (lang) {
        AppLanguage.CHINESE -> "不可用"
        AppLanguage.ENGLISH -> "Unavailable"
        AppLanguage.ARABIC -> "غير متاح"
    }

    val recommendedLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "推荐"
        AppLanguage.ENGLISH -> "REC"
        AppLanguage.ARABIC -> "موصى به"
    }

    val channelTesting: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试中..."
        AppLanguage.ENGLISH -> "Testing..."
        AppLanguage.ARABIC -> "جاري الاختبار..."
    }

    val channelUntested: String get() = when (lang) {
        AppLanguage.CHINESE -> "未测试"
        AppLanguage.ENGLISH -> "Not tested"
        AppLanguage.ARABIC -> "لم يتم الاختبار"
    }

    val searchOnlineMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索在线音乐"
        AppLanguage.ENGLISH -> "Search online music"
        AppLanguage.ARABIC -> "البحث عن الموسيقى عبر الإنترنت"
    }

    val noMusicResults: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有搜索结果"
        AppLanguage.ENGLISH -> "No results found"
        AppLanguage.ARABIC -> "لا توجد نتائج"
    }

    val previewListen: String get() = when (lang) {
        AppLanguage.CHINESE -> "试听"
        AppLanguage.ENGLISH -> "Preview"
        AppLanguage.ARABIC -> "معاينة"
    }

    val downloadToBgm: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载为BGM"
        AppLanguage.ENGLISH -> "Download as BGM"
        AppLanguage.ARABIC -> "تنزيل كـ BGM"
    }

    val downloadSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载成功"
        AppLanguage.ENGLISH -> "Download successful"
        AppLanguage.ARABIC -> "تم التنزيل بنجاح"
    }

    val previewNote: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览 (30秒)"
        AppLanguage.ENGLISH -> "Preview (30s)"
        AppLanguage.ARABIC -> "معاينة (30 ثانية)"
    }

    val loadingPlayUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取播放链接..."
        AppLanguage.ENGLISH -> "Loading play URL..."
        AppLanguage.ARABIC -> "جاري تحميل رابط التشغيل..."
    }

    val searchFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索失败"
        AppLanguage.ENGLISH -> "Search failed"
        AppLanguage.ARABIC -> "فشل البحث"
    }

    val selectChannelFirst: String get() = when (lang) {
        AppLanguage.CHINESE -> "请先选择一个音乐渠道"
        AppLanguage.ENGLISH -> "Please select a music channel first"
        AppLanguage.ARABIC -> "يرجى اختيار قناة موسيقية أولاً"
    }

    val results: String get() = when (lang) {
        AppLanguage.CHINESE -> "个结果"
        AppLanguage.ENGLISH -> "results"
        AppLanguage.ARABIC -> "نتائج"
    }
    // ==================== BGM Picker ====================
    val selectBgm: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择背景音乐"
        AppLanguage.ENGLISH -> "Select Background Music"
        AppLanguage.ARABIC -> "اختيار موسيقى الخلفية"
    }

    val selectedMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选音乐"
        AppLanguage.ENGLISH -> "Selected Music"
        AppLanguage.ARABIC -> "الموسيقى المحددة"
    }

    val availableMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "可用音乐"
        AppLanguage.ENGLISH -> "Available Music"
        AppLanguage.ARABIC -> "الموسيقى المتاحة"
    }

    val uploadMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "上传音乐"
        AppLanguage.ENGLISH -> "Upload Music"
        AppLanguage.ARABIC -> "رفع موسيقى"
    }

    val clickArrowToReorder: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击箭头调整顺序"
        AppLanguage.ENGLISH -> "Click arrows to reorder"
        AppLanguage.ARABIC -> "انقر على الأسهم لإعادة الترتيب"
    }

    val noMusicAvailable: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无音乐"
        AppLanguage.ENGLISH -> "No music available"
        AppLanguage.ARABIC -> "لا توجد موسيقى متاحة"
    }

    val clickToUploadMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击上方按钮上传音乐"
        AppLanguage.ENGLISH -> "Click button above to upload music"
        AppLanguage.ARABIC -> "انقر على الزر أعلاه لرفع الموسيقى"
    }

    val noMusicWithTag: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有此标签的音乐"
        AppLanguage.ENGLISH -> "No music with this tag"
        AppLanguage.ARABIC -> "لا توجد موسيقى بهذه العلامة"
    }

    val playMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放模式"
        AppLanguage.ENGLISH -> "Play Mode"
        AppLanguage.ARABIC -> "وضع التشغيل"
    }

    val loopMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "循环"
        AppLanguage.ENGLISH -> "Loop"
        AppLanguage.ARABIC -> "تكرار"
    }

    val sequentialMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "顺序"
        AppLanguage.ENGLISH -> "Sequential"
        AppLanguage.ARABIC -> "تسلسلي"
    }

    val shuffleMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "随机"
        AppLanguage.ENGLISH -> "Shuffle"
        AppLanguage.ARABIC -> "عشوائي"
    }

    val volume: String get() = when (lang) {
        AppLanguage.CHINESE -> "音量"
        AppLanguage.ENGLISH -> "Volume"
        AppLanguage.ARABIC -> "مستوى الصوت"
    }

    val showLyrics: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示歌词"
        AppLanguage.ENGLISH -> "Show Lyrics"
        AppLanguage.ARABIC -> "عرض كلمات الأغنية"
    }

    val lyricsTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "字幕主题"
        AppLanguage.ENGLISH -> "Lyrics Theme"
        AppLanguage.ARABIC -> "سمة كلمات الأغنية"
    }

    val allTag: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.ARABIC -> "الكل"
    }

    val lyricsSaved: String get() = when (lang) {
        AppLanguage.CHINESE -> "[OK] 歌词已保存"
        AppLanguage.ENGLISH -> "[OK] Lyrics saved"
        AppLanguage.ARABIC -> "[OK] تم حفظ كلمات الأغنية"
    }
    // ==================== BGM Extras ====================
    val previewLyrics: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览歌词"
        AppLanguage.ENGLISH -> "Preview Lyrics"
        AppLanguage.ARABIC -> "معاينة كلمات الأغنية"
    }

    val hasLyrics: String get() = when (lang) {
        AppLanguage.CHINESE -> "已有歌词"
        AppLanguage.ENGLISH -> "Has Lyrics"
        AppLanguage.ARABIC -> "يحتوي على كلمات"
    }

    val editTags: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑标签"
        AppLanguage.ENGLISH -> "Edit Tags"
        AppLanguage.ARABIC -> "تعديل العلامات"
    }

    val stop: String get() = when (lang) {
        AppLanguage.CHINESE -> "停止"
        AppLanguage.ENGLISH -> "Stop"
        AppLanguage.ARABIC -> "إيقاف"
    }

    val moveUp: String get() = when (lang) {
        AppLanguage.CHINESE -> "上移"
        AppLanguage.ENGLISH -> "Move Up"
        AppLanguage.ARABIC -> "نقل لأعلى"
    }

    val moveDown: String get() = when (lang) {
        AppLanguage.CHINESE -> "下移"
        AppLanguage.ENGLISH -> "Move Down"
        AppLanguage.ARABIC -> "نقل لأسفل"
    }

    val presetMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "预置音乐"
        AppLanguage.ENGLISH -> "Preset Music"
        AppLanguage.ARABIC -> "موسيقى مسبقة"
    }

    val userUploaded: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户上传"
        AppLanguage.ENGLISH -> "User Uploaded"
        AppLanguage.ARABIC -> "رفع المستخدم"
    }

    val uploadMusicTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "上传音乐"
        AppLanguage.ENGLISH -> "Upload Music"
        AppLanguage.ARABIC -> "رفع موسيقى"
    }

    val musicName: String get() = when (lang) {
        AppLanguage.CHINESE -> "音乐名称"
        AppLanguage.ENGLISH -> "Music Name"
        AppLanguage.ARABIC -> "اسم الموسيقى"
    }

    val selectMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择音乐"
        AppLanguage.ENGLISH -> "Select Music"
        AppLanguage.ARABIC -> "اختيار موسيقى"
    }

    val selectCoverOptional: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择封面(可选)"
        AppLanguage.ENGLISH -> "Select Cover (Optional)"
        AppLanguage.ARABIC -> "اختيار غلاف (اختياري)"
    }

    val coverTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "提示: 封面图片用于在选择界面展示"
        AppLanguage.ENGLISH -> "Tip: Cover image is displayed in the selection interface"
        AppLanguage.ARABIC -> "تلميح: يتم عرض صورة الغلاف في واجهة الاختيار"
    }

    val upload: String get() = when (lang) {
        AppLanguage.CHINESE -> "上传"
        AppLanguage.ENGLISH -> "Upload"
        AppLanguage.ARABIC -> "رفع"
    }

    val editTagsTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑标签"
        AppLanguage.ENGLISH -> "Edit Tags"
        AppLanguage.ARABIC -> "تعديل العلامات"
    }

    val selectTagsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择适合的标签(可多选)"
        AppLanguage.ENGLISH -> "Select suitable tags (multiple selection)"
        AppLanguage.ARABIC -> "اختر العلامات المناسبة (اختيار متعدد)"
    }

    val selectLyricsTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择字幕主题"
        AppLanguage.ENGLISH -> "Select Lyrics Theme"
        AppLanguage.ARABIC -> "اختيار سمة كلمات الأغنية"
    }

    val selectLyricsThemeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择歌词显示的视觉风格"
        AppLanguage.ENGLISH -> "Select visual style for lyrics display"
        AppLanguage.ARABIC -> "اختر النمط المرئي لعرض كلمات الأغنية"
    }

    val sampleLyricsText: String get() = when (lang) {
        AppLanguage.CHINESE -> "示例歌词文本"
        AppLanguage.ENGLISH -> "Sample Lyrics Text"
        AppLanguage.ARABIC -> "نص كلمات نموذجي"
    }

    val lyricsPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "歌词预览"
        AppLanguage.ENGLISH -> "Lyrics Preview"
        AppLanguage.ARABIC -> "معاينة كلمات الأغنية"
    }

    val lyricsUpdated: String get() = when (lang) {
        AppLanguage.CHINESE -> "[OK] 歌词已更新"
        AppLanguage.ENGLISH -> "[OK] Lyrics updated"
        AppLanguage.ARABIC -> "[OK] تم تحديث كلمات الأغنية"
    }

    val backward10s: String get() = when (lang) {
        AppLanguage.CHINESE -> "后退10秒"
        AppLanguage.ENGLISH -> "Rewind 10s"
        AppLanguage.ARABIC -> "ترجيع 10 ثوانٍ"
    }

    val forward10s: String get() = when (lang) {
        AppLanguage.CHINESE -> "前进10秒"
        AppLanguage.ENGLISH -> "Forward 10s"
        AppLanguage.ARABIC -> "تقديم 10 ثوانٍ"
    }
    // ==================== BGM Tags ====================
    val bgmTagPureMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "纯音乐"
        AppLanguage.ENGLISH -> "Instrumental"
        AppLanguage.ARABIC -> "موسيقى آلية"
    }

    val bgmTagPop: String get() = when (lang) {
        AppLanguage.CHINESE -> "流行"
        AppLanguage.ENGLISH -> "Pop"
        AppLanguage.ARABIC -> "بوب"
    }

    val bgmTagRock: String get() = when (lang) {
        AppLanguage.CHINESE -> "摇滚"
        AppLanguage.ENGLISH -> "Rock"
        AppLanguage.ARABIC -> "روك"
    }

    val bgmTagClassical: String get() = when (lang) {
        AppLanguage.CHINESE -> "古典"
        AppLanguage.ENGLISH -> "Classical"
        AppLanguage.ARABIC -> "كلاسيكي"
    }

    val bgmTagJazz: String get() = when (lang) {
        AppLanguage.CHINESE -> "爵士"
        AppLanguage.ENGLISH -> "Jazz"
        AppLanguage.ARABIC -> "جاز"
    }

    val bgmTagElectronic: String get() = when (lang) {
        AppLanguage.CHINESE -> "电子"
        AppLanguage.ENGLISH -> "Electronic"
        AppLanguage.ARABIC -> "إلكتروني"
    }

    val bgmTagFolk: String get() = when (lang) {
        AppLanguage.CHINESE -> "民谣"
        AppLanguage.ENGLISH -> "Folk"
        AppLanguage.ARABIC -> "شعبي"
    }

    val bgmTagChineseStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "古风"
        AppLanguage.ENGLISH -> "Chinese Style"
        AppLanguage.ARABIC -> "صيني تقليدي"
    }

    val bgmTagAnime: String get() = when (lang) {
        AppLanguage.CHINESE -> "动漫"
        AppLanguage.ENGLISH -> "Anime"
        AppLanguage.ARABIC -> "أنيمي"
    }

    val bgmTagGame: String get() = when (lang) {
        AppLanguage.CHINESE -> "游戏"
        AppLanguage.ENGLISH -> "Game"
        AppLanguage.ARABIC -> "ألعاب"
    }

    val bgmTagMovie: String get() = when (lang) {
        AppLanguage.CHINESE -> "影视"
        AppLanguage.ENGLISH -> "Movie"
        AppLanguage.ARABIC -> "أفلام"
    }

    val bgmTagHealing: String get() = when (lang) {
        AppLanguage.CHINESE -> "治愈"
        AppLanguage.ENGLISH -> "Healing"
        AppLanguage.ARABIC -> "شفاء"
    }

    val bgmTagExciting: String get() = when (lang) {
        AppLanguage.CHINESE -> "激昂"
        AppLanguage.ENGLISH -> "Exciting"
        AppLanguage.ARABIC -> "مثير"
    }

    val bgmTagSad: String get() = when (lang) {
        AppLanguage.CHINESE -> "伤感"
        AppLanguage.ENGLISH -> "Sad"
        AppLanguage.ARABIC -> "حزين"
    }

    val bgmTagRomantic: String get() = when (lang) {
        AppLanguage.CHINESE -> "浪漫"
        AppLanguage.ENGLISH -> "Romantic"
        AppLanguage.ARABIC -> "رومانسي"
    }

    val bgmTagRelaxing: String get() = when (lang) {
        AppLanguage.CHINESE -> "轻松"
        AppLanguage.ENGLISH -> "Relaxing"
        AppLanguage.ARABIC -> "مريح"
    }

    val bgmTagWorkout: String get() = when (lang) {
        AppLanguage.CHINESE -> "运动"
        AppLanguage.ENGLISH -> "Workout"
        AppLanguage.ARABIC -> "تمرين"
    }

    val bgmTagSleep: String get() = when (lang) {
        AppLanguage.CHINESE -> "助眠"
        AppLanguage.ENGLISH -> "Sleep"
        AppLanguage.ARABIC -> "نوم"
    }

    val bgmTagStudy: String get() = when (lang) {
        AppLanguage.CHINESE -> "学习"
        AppLanguage.ENGLISH -> "Study"
        AppLanguage.ARABIC -> "دراسة"
    }

    val bgmTagOther: String get() = when (lang) {
        AppLanguage.CHINESE -> "其他"
        AppLanguage.ENGLISH -> "Other"
        AppLanguage.ARABIC -> "أخرى"
    }
    // ==================== Online Music Search ( ) ====================
    val playbackFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放失败"
        AppLanguage.ENGLISH -> "Playback failed"
        AppLanguage.ARABIC -> "فشل التشغيل"
    }

    val playbackFailedWithCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放失败 (错误码: %d)"
        AppLanguage.ENGLISH -> "Playback failed (error: %d)"
        AppLanguage.ARABIC -> "فشل التشغيل (خطأ: %d)"
    }

    val loadingTimeout: String get() = when (lang) {
        AppLanguage.CHINESE -> "加载超时，请重试"
        AppLanguage.ENGLISH -> "Loading timeout, please retry"
        AppLanguage.ARABIC -> "انتهت المهلة، يرجى المحاولة مرة أخرى"
    }

    val musicChannelLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "音乐渠道"
        AppLanguage.ENGLISH -> "Music channel"
        AppLanguage.ARABIC -> "قناة الموسيقى"
    }

    val gettingMusicDetails: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在获取音乐详情..."
        AppLanguage.ENGLISH -> "Getting music details..."
        AppLanguage.ARABIC -> "جاري الحصول على تفاصيل الموسيقى..."
    }

    val getPlayUrlFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取播放链接失败"
        AppLanguage.ENGLISH -> "Failed to get play URL"
        AppLanguage.ARABIC -> "فشل الحصول على رابط التشغيل"
    }

    val getPlayUrlSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放链接获取成功"
        AppLanguage.ENGLISH -> "Play URL obtained"
        AppLanguage.ARABIC -> "تم الحصول على رابط التشغيل"
    }

    val startDownloadMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始下载音乐文件..."
        AppLanguage.ENGLISH -> "Downloading music file..."
        AppLanguage.ARABIC -> "جاري تنزيل ملف الموسيقى..."
    }

    val musicDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "音乐文件下载中..."
        AppLanguage.ENGLISH -> "Downloading music..."
        AppLanguage.ARABIC -> "جاري تنزيل الموسيقى..."
    }

    val downloadingCoverImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在下载封面图片..."
        AppLanguage.ENGLISH -> "Downloading cover image..."
        AppLanguage.ARABIC -> "جاري تنزيل صورة الغلاف..."
    }

    val coverDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "封面下载中..."
        AppLanguage.ENGLISH -> "Cover downloading..."
        AppLanguage.ARABIC -> "جاري تنزيل الغلاف..."
    }

    val finishing: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在完成..."
        AppLanguage.ENGLISH -> "Finishing..."
        AppLanguage.ARABIC -> "جاري الانتهاء..."
    }

    val downloadCompleteSaved: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载完成！文件已保存"
        AppLanguage.ENGLISH -> "Download complete! File saved"
        AppLanguage.ARABIC -> "اكتمل التنزيل! تم حفظ الملف"
    }

    val coverImageSaved: String get() = when (lang) {
        AppLanguage.CHINESE -> "封面图片已保存"
        AppLanguage.ENGLISH -> "Cover image saved"
        AppLanguage.ARABIC -> "تم حفظ صورة الغلاف"
    }

    val downloadError: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载出错"
        AppLanguage.ENGLISH -> "Download error"
        AppLanguage.ARABIC -> "خطأ في التنزيل"
    }

    val downloadLog: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载日志"
        AppLanguage.ENGLISH -> "Download Log"
        AppLanguage.ARABIC -> "سجل التنزيل"
    }

    val searchingText: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在搜索..."
        AppLanguage.ENGLISH -> "Searching..."
        AppLanguage.ARABIC -> "جاري البحث..."
    }

    val clearText: String get() = when (lang) {
        AppLanguage.CHINESE -> "清除"
        AppLanguage.ENGLISH -> "Clear"
        AppLanguage.ARABIC -> "مسح"
    }

    val collapseText: String get() = when (lang) {
        AppLanguage.CHINESE -> "收起"
        AppLanguage.ENGLISH -> "Collapse"
        AppLanguage.ARABIC -> "طي"
    }
}
