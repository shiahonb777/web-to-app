package com.webtoapp.core.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RandomAppNameGeneratorTest {

    @Test
    fun `generate language overload returns non blank names`() {
        val zh = RandomAppNameGenerator.generate(AppLanguage.CHINESE)
        val en = RandomAppNameGenerator.generate(AppLanguage.ENGLISH)
        val ar = RandomAppNameGenerator.generate(AppLanguage.ARABIC)

        assertThat(zh).isNotEmpty()
        assertThat(en).isNotEmpty()
        assertThat(ar).isNotEmpty()
        assertThat(ar).contains(" ")
    }

    @Test
    fun `generate uses current language from Strings state`() {
        val original = Strings.currentLanguage.value
        try {
            Strings.setLanguage(AppLanguage.ENGLISH)
            val english = RandomAppNameGenerator.generate()

            Strings.setLanguage(AppLanguage.CHINESE)
            val chinese = RandomAppNameGenerator.generate()

            assertThat(english).isNotEqualTo(chinese)
        } finally {
            Strings.setLanguage(original)
        }
    }
}
