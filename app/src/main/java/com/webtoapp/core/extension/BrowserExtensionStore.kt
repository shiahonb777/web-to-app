package com.webtoapp.core.extension

object BrowserExtensionStore {

    data class StoreEntry(
        val storeId: String,
        val name: String,
        val author: String,
        val description: String,
        val homepage: String,
        val iconUrl: String
    )

    private fun iconFor(storeId: String): String =
        "https://lh3.googleusercontent.com/chrome-extension/$storeId"

    val catalog: List<StoreEntry> = listOf(
        StoreEntry(
            storeId = "eimadpbcbfnmbkopoojfekhnkhdbieeh",
            name = "Dark Reader",
            author = "Alexander Shutau",
            description = "Dark mode for every website; adjustable brightness, contrast and color.",
            homepage = "https://darkreader.org",
            iconUrl = iconFor("eimadpbcbfnmbkopoojfekhnkhdbieeh")
        ),
        StoreEntry(
            storeId = "ddkjiahejlhfcafbddmgiahcphecmpfh",
            name = "uBlock Origin Lite",
            author = "Raymond Hill",
            description = "Permission-less MV3 content blocker for ads and trackers.",
            homepage = "https://github.com/uBlockOrigin/uBOL-home",
            iconUrl = iconFor("ddkjiahejlhfcafbddmgiahcphecmpfh")
        ),
        StoreEntry(
            storeId = "bgnkhhnnamicmpeenaelnjfhikgbkllg",
            name = "AdGuard AdBlocker",
            author = "AdGuard",
            description = "Blocks ads and trackers across websites, including video ads.",
            homepage = "https://adguard.com",
            iconUrl = iconFor("bgnkhhnnamicmpeenaelnjfhikgbkllg")
        ),
        StoreEntry(
            storeId = "cfhdojbkjhnklbpkdaibdccddilifddb",
            name = "Adblock Plus",
            author = "eyeo GmbH",
            description = "Popular ad blocker; blocks ads, pop-ups and trackers.",
            homepage = "https://adblockplus.org",
            iconUrl = iconFor("cfhdojbkjhnklbpkdaibdccddilifddb")
        ),
        StoreEntry(
            storeId = "gighmmpiobklfepjocnamgkkbiglidom",
            name = "AdBlock",
            author = "BetaFish",
            description = "Block ads and pop-ups on websites and video.",
            homepage = "https://getadblock.com",
            iconUrl = iconFor("gighmmpiobklfepjocnamgkkbiglidom")
        ),
        StoreEntry(
            storeId = "pkehgijcmpdhfbdbbnkijodmdjhbjlgp",
            name = "Privacy Badger",
            author = "EFF Technologists",
            description = "Automatically learns to block invisible trackers.",
            homepage = "https://privacybadger.org",
            iconUrl = iconFor("pkehgijcmpdhfbdbbnkijodmdjhbjlgp")
        ),
        StoreEntry(
            storeId = "pncfbmialoiaghdehhbnbhkkgmjanfhe",
            name = "uBlacklist",
            author = "iorate",
            description = "Block specific sites from appearing in search results.",
            homepage = "https://github.com/iorate/uBlacklist",
            iconUrl = iconFor("pncfbmialoiaghdehhbnbhkkgmjanfhe")
        ),
        StoreEntry(
            storeId = "gebbhagfogifgggkldgodflihgfeippi",
            name = "Return YouTube Dislike",
            author = "Dmitrii Selivanov",
            description = "Brings back the YouTube dislike count.",
            homepage = "https://returnyoutubedislike.com",
            iconUrl = iconFor("gebbhagfogifgggkldgodflihgfeippi")
        ),
        StoreEntry(
            storeId = "mnjggcdmjocbbbhaepdhchncahnbgone",
            name = "SponsorBlock",
            author = "Ajay Ramachandran",
            description = "Skip sponsor segments, intros and more in YouTube videos.",
            homepage = "https://sponsor.ajay.app",
            iconUrl = iconFor("mnjggcdmjocbbbhaepdhchncahnbgone")
        ),
        StoreEntry(
            storeId = "ponfpcnoihfmfllpaingbgckeeldkhle",
            name = "Enhancer for YouTube",
            author = "Maxime RF",
            description = "Customize YouTube: speed, volume, themes, ad blocking and more.",
            homepage = "https://www.mrfdev.com/enhancer-for-youtube",
            iconUrl = iconFor("ponfpcnoihfmfllpaingbgckeeldkhle")
        ),
        StoreEntry(
            storeId = "nffaoalbilbmmfgbnbgppjihopabppdk",
            name = "Video Speed Controller",
            author = "igrigorik",
            description = "Speed up, slow down and control HTML5 video playback.",
            homepage = "https://github.com/igrigorik/videospeed",
            iconUrl = iconFor("nffaoalbilbmmfgbnbgppjihopabppdk")
        ),
        StoreEntry(
            storeId = "aapbdbdomjkkjkaonfhkkikfgjllcleb",
            name = "Google Translate",
            author = "Google",
            description = "Translate selected text and whole web pages.",
            homepage = "https://translate.google.com",
            iconUrl = iconFor("aapbdbdomjkkjkaonfhkkikfgjllcleb")
        ),
        StoreEntry(
            storeId = "oldceeleldhonbafppcapldpdifcinji",
            name = "LanguageTool",
            author = "LanguageTooler GmbH",
            description = "Grammar and spelling checker for text fields on the web.",
            homepage = "https://languagetool.org",
            iconUrl = iconFor("oldceeleldhonbafppcapldpdifcinji")
        ),
        StoreEntry(
            storeId = "dgmanlpmmkibanfdgjocnabmcaclkmod",
            name = "Just Read",
            author = "ZackDeRose",
            description = "Clean, distraction-free reader view for articles.",
            homepage = "https://github.com/ZachSaucier/Just-Read",
            iconUrl = iconFor("dgmanlpmmkibanfdgjocnabmcaclkmod")
        ),
        StoreEntry(
            storeId = "bcjindcccaagfpapjjmafapmmgkkhgoa",
            name = "JSON Formatter",
            author = "Callum Locke",
            description = "Makes JSON responses readable with syntax highlighting.",
            homepage = "https://github.com/callumlocke/json-formatter",
            iconUrl = iconFor("bcjindcccaagfpapjjmafapmmgkkhgoa")
        ),
        StoreEntry(
            storeId = "gppongmhjkpfnbhagpmjfkannfbllamg",
            name = "Wappalyzer",
            author = "Wappalyzer",
            description = "Identifies the technologies used on websites.",
            homepage = "https://www.wappalyzer.com",
            iconUrl = iconFor("gppongmhjkpfnbhagpmjfkannfbllamg")
        ),
        StoreEntry(
            storeId = "bhlhnicpbhignbdhedgjhgdocnmhomnp",
            name = "ColorZilla",
            author = "ColorZilla",
            description = "Eyedropper, color picker and gradient generator for the web.",
            homepage = "https://www.colorzilla.com",
            iconUrl = iconFor("bhlhnicpbhignbdhedgjhgdocnmhomnp")
        ),
        StoreEntry(
            storeId = "clngdbkpkpeebahjckkjfobafhncgmne",
            name = "Stylus",
            author = "Stylus Team",
            description = "Restyle the web with user CSS styles.",
            homepage = "https://github.com/openstyles/stylus",
            iconUrl = iconFor("clngdbkpkpeebahjckkjfobafhncgmne")
        ),
        StoreEntry(
            storeId = "dhdgffkkebhmkfjojejmpbldmpobfkfo",
            name = "Tampermonkey",
            author = "Jan Biniok",
            description = "The popular userscript manager.",
            homepage = "https://www.tampermonkey.net",
            iconUrl = iconFor("dhdgffkkebhmkfjojejmpbldmpobfkfo")
        ),
        StoreEntry(
            storeId = "kbfnbcaeplbcioakkpcpgfkobkghlhen",
            name = "Grammarly",
            author = "Grammarly",
            description = "Writing assistant: grammar, spelling and clarity suggestions.",
            homepage = "https://www.grammarly.com",
            iconUrl = iconFor("kbfnbcaeplbcioakkpcpgfkobkghlhen")
        )
    )

    private val STORE_URL_ID_REGEX = Regex("/detail/(?:[^/]+/)?([a-p]{32})")
    private val RAW_ID_REGEX = Regex("^[a-p]{32}$")

    fun extractStoreId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        RAW_ID_REGEX.find(trimmed)?.let { return trimmed }
        STORE_URL_ID_REGEX.find(trimmed)?.let { return it.groupValues[1] }
        return trimmed.takeIf { it.length == 32 && it.all { ch -> ch in 'a'..'p' } }
    }
}
