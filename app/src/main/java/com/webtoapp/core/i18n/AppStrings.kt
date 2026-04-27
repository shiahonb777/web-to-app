package com.webtoapp.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.webtoapp.core.i18n.generated.BaseGeneratedAppStrings
import com.webtoapp.core.i18n.generated.GeneratedAppStrings

interface AppStrings : GeneratedAppStrings {
    fun depProjectCount(count: Int): String
    fun screenAwakeTimedStatusDesc(minutes: Int): String
    fun screenAwakeTimeoutValue(minutes: Int): String
    fun tooManyAttemptsWithCountdown(remaining: String): String
}

object AppStringsProvider {
    private val persistedLanguageState = mutableStateOf(AppLanguage.CHINESE)
    private val runtimeLanguageState = mutableStateOf<AppLanguage?>(null)
    private val fixedLanguageStrings = AppLanguage.entries.associateWith(::FixedLanguageAppStrings)

    fun initialize(initialLanguage: AppLanguage = AppLanguage.CHINESE) {
        persistedLanguageState.value = initialLanguage
    }

    fun syncLanguage(language: AppLanguage) {
        persistedLanguageState.value = language
    }

    fun setRuntimeLanguage(language: AppLanguage?) {
        runtimeLanguageState.value = language
    }

    fun clearRuntimeLanguage() {
        runtimeLanguageState.value = null
    }

    val currentLanguage: AppLanguage
        get() = runtimeLanguageState.value ?: persistedLanguageState.value

    fun current(): AppStrings = CurrentAppStrings

    fun forLanguage(language: AppLanguage): AppStrings = fixedLanguageStrings.getValue(language)
}

@Composable
fun rememberAppStrings(): AppStrings {
    val language = AppStringsProvider.currentLanguage
    return remember(language) { AppStringsProvider.current() }
}

private abstract class BaseAppStringsDelegate : BaseGeneratedAppStrings(), AppStrings {
    private fun formatCurrent(pattern: String, vararg args: Any): String =
        String.format(currentLanguage.locale, pattern, *args)

    override fun depProjectCount(count: Int): String = if (count == 1) {
        formatCurrent(depProjectCountSingular, count)
    } else {
        formatCurrent(depProjectCountPlural, count)
    }

    override fun screenAwakeTimedStatusDesc(minutes: Int): String =
        formatCurrent(screenAwakeTimedStatusDescFormat, minutes)

    override fun screenAwakeTimeoutValue(minutes: Int): String = when {
        minutes >= 60 -> {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            if (remainingMinutes > 0) {
                formatCurrent(screenAwakeTimeoutHoursMinutesFormat, hours, remainingMinutes)
            } else {
                formatCurrent(screenAwakeTimeoutHoursOnlyFormat, hours)
            }
        }
        else -> formatCurrent(screenAwakeTimeoutMinutesFormat, minutes)
    }

    override fun tooManyAttemptsWithCountdown(remaining: String): String =
        formatCurrent(tooManyAttemptsWithCountdownFormat, remaining)
}

private object CurrentAppStrings : BaseAppStringsDelegate() {
    override val currentLanguage: AppLanguage
        get() = AppStringsProvider.currentLanguage
}

private class FixedLanguageAppStrings(
    override val currentLanguage: AppLanguage
) : BaseAppStringsDelegate()
