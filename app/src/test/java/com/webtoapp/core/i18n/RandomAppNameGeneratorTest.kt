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
    fun `generate uses current language from app strings provider state`() {
        val original = AppStringsProvider.currentLanguage
        try {
            AppStringsProvider.setRuntimeLanguage(AppLanguage.ENGLISH)
            val english = RandomAppNameGenerator.generate()

            AppStringsProvider.setRuntimeLanguage(AppLanguage.CHINESE)
            val chinese = RandomAppNameGenerator.generate()

            assertThat(english).isNotEqualTo(chinese)
        } finally {
            AppStringsProvider.setRuntimeLanguage(original)
        }
    }
}
