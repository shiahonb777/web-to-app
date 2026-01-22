package com.webtoapp.core.i18n

import kotlin.random.Random

/**
 * 随机应用名称生成器
 * 
 * 使用前缀+后缀随机拼凑的方式生成应用名称
 * 根据当前语言生成对应语言的随机名称
 */
object RandomAppNameGenerator {
    
    // ==================== 中文 ====================
    private val chinesePrefixes = listOf(
        "小", "大", "快", "智", "云", "新", "超", "酷", "妙", "神",
        "精", "万", "美", "乐", "易", "轻", "精", "巧", "火", "飞",
        "光", "星", "月", "雷", "风", "电", "金", "银", "玉", "宝",
        "梦", "天", "地", "海", "山", "林", "泉", "雪", "雨", "露",
        "红", "蓝", "绿", "紫", "青", "白", "黑", "橙", "粉", "灰"
    )
    
    private val chineseSuffixes = listOf(
        "助手", "工具", "宝", "通", "达", "星", "盒", "管家", "精灵", "魔法",
        "帮手", "大师", "侠", "神", "客", "管", "方", "统", "乐园", "向导",
        "精灵", "天使", "小精", "小宝", "宝贝", "小伙伴", "管家", "助理", "笔记", "记录",
        "空间", "世界", "宇宙", "星球", "银河", "统计", "分析", "探索", "发现", "秘密",
        "路径", "桥梁", "通道", "入口", "窗口", "门户", "平台", "中心", "基地", "站"
    )
    
    // ==================== 英文 ====================
    private val englishPrefixes = listOf(
        "Quick", "Smart", "Easy", "Super", "Magic", "Ultra", "Pro", "Neo", "Max", "Prime",
        "Flash", "Turbo", "Swift", "Rapid", "Instant", "Fast", "Speed", "Zoom", "Rush", "Blitz",
        "Power", "Mega", "Giga", "Hyper", "Omni", "Multi", "Poly", "Meta", "Cyber", "Tech",
        "Star", "Nova", "Luna", "Solar", "Cosmic", "Galaxy", "Orbit", "Sky", "Cloud", "Air",
        "Dream", "Vision", "Mind", "Soul", "Spirit", "Heart", "Core", "Pure", "True", "Real",
        "Blue", "Red", "Green", "Gold", "Silver", "Crystal", "Diamond", "Pearl", "Ruby", "Jade",
        "Fire", "Ice", "Thunder", "Storm", "Wave", "Spark", "Flame", "Frost", "Wind", "Rain"
    )
    
    private val englishSuffixes = listOf(
        "App", "Tool", "Kit", "Box", "Hub", "Lab", "Pro", "Go", "Now", "One",
        "Helper", "Master", "Genius", "Wizard", "Expert", "Guide", "Buddy", "Pal", "Mate", "Friend",
        "Space", "World", "Zone", "Land", "Realm", "Sphere", "Field", "Arena", "Studio", "Works",
        "Link", "Connect", "Bridge", "Path", "Way", "Gate", "Door", "Portal", "Channel", "Stream",
        "Base", "Center", "Core", "Point", "Spot", "Place", "Site", "Desk", "Board", "Pad",
        "Flow", "Sync", "Track", "Pulse", "Beat", "Wave", "Loop", "Ring", "Spin", "Dash"
    )
    
    // ==================== 阿拉伯语 ====================
    private val arabicPrefixes = listOf(
        "السريع", "الذكي", "السهل", "الخارق", "السحري", "الفائق", "المتقدم", "الجديد", "الأقصى", "الأول",
        "البرق", "الصاروخ", "الخفيف", "المباشر", "الفوري", "السريع", "العاجل", "المنطلق", "المندفع", "الخاطف",
        "القوي", "الضخم", "العملاق", "المفرط", "الشامل", "المتعدد", "المتنوع", "الرقمي", "الإلكتروني", "التقني",
        "النجم", "المضيء", "القمري", "الشمسي", "الكوني", "المجري", "السماوي", "العلوي", "السحابي", "الهوائي",
        "الأزرق", "الأحمر", "الأخضر", "الذهبي", "الفضي", "البلوري", "الماسي", "اللؤلؤي", "الياقوتي", "الزمردي"
    )
    
    private val arabicSuffixes = listOf(
        "التطبيق", "الأداة", "المجموعة", "الصندوق", "المركز", "المختبر", "المحترف", "المنطلق", "الآن", "الواحد",
        "المساعد", "الخبير", "العبقري", "الساحر", "المتخصص", "المرشد", "الرفيق", "الصديق", "الزميل", "الشريك",
        "الفضاء", "العالم", "المنطقة", "الأرض", "المملكة", "الكرة", "الميدان", "الساحة", "الاستوديو", "الورشة",
        "الرابط", "الموصل", "الجسر", "الطريق", "المسار", "البوابة", "الباب", "البورتال", "القناة", "التدفق",
        "القاعدة", "النقطة", "القلب", "الموقع", "المكان", "الموضع", "الموقف", "المكتب", "اللوحة", "الوسادة"
    )
    
    /**
     * 根据当前语言生成随机应用名称
     */
    fun generate(): String {
        return when (Strings.currentLanguage.value) {
            AppLanguage.CHINESE -> generateChinese()
            AppLanguage.ENGLISH -> generateEnglish()
            AppLanguage.ARABIC -> generateArabic()
        }
    }
    
    /**
     * 根据指定语言生成随机应用名称
     */
    fun generate(language: AppLanguage): String {
        return when (language) {
            AppLanguage.CHINESE -> generateChinese()
            AppLanguage.ENGLISH -> generateEnglish()
            AppLanguage.ARABIC -> generateArabic()
        }
    }
    
    private fun generateChinese(): String {
        val prefix = chinesePrefixes[Random.nextInt(chinesePrefixes.size)]
        val suffix = chineseSuffixes[Random.nextInt(chineseSuffixes.size)]
        return prefix + suffix
    }
    
    private fun generateEnglish(): String {
        val prefix = englishPrefixes[Random.nextInt(englishPrefixes.size)]
        val suffix = englishSuffixes[Random.nextInt(englishSuffixes.size)]
        return prefix + suffix
    }
    
    private fun generateArabic(): String {
        val prefix = arabicPrefixes[Random.nextInt(arabicPrefixes.size)]
        val suffix = arabicSuffixes[Random.nextInt(arabicSuffixes.size)]
        return "$prefix $suffix"
    }
}
