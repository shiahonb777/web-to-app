package com.webtoapp.util

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin

/**
 * JUnit Rule that stops Koin after each test to prevent
 * [org.koin.core.error.KoinAppAlreadyStartedException] when multiple
 * Robolectric tests share the same JVM process.
 *
 * Usage:
 * ```
 * @Rule @JvmField val koinRule = KoinCleanupRule()
 * ```
 */
class KoinCleanupRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    if (GlobalContext.getOrNull() != null) {
                        stopKoin()
                    }
                }
            }
        }
    }
}
