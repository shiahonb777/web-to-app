package com.webtoapp.core.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `fromCode resolves supported languages and defaults to chinese`() {
        assertThat(AppLanguage.fromCode("zh")).isEqualTo(AppLanguage.CHINESE)
        assertThat(AppLanguage.fromCode("en")).isEqualTo(AppLanguage.ENGLISH)
        assertThat(AppLanguage.fromCode("ar")).isEqualTo(AppLanguage.ARABIC)
        assertThat(AppLanguage.fromCode("unknown")).isEqualTo(AppLanguage.CHINESE)
    }

    @Test
    fun `language metadata includes rtl flag and locale information`() {
        assertThat(AppLanguage.ARABIC.isRtl).isTrue()
        assertThat(AppLanguage.CHINESE.isRtl).isFalse()
        assertThat(AppLanguage.ENGLISH.locale.language).isEqualTo("en")
    }
}
