package com.webtoapp.util

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin











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
