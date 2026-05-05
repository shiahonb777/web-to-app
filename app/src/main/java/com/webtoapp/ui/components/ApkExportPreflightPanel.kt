package com.webtoapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.webtoapp.core.apkbuilder.ApkExportPreflightReport
import com.webtoapp.core.apkbuilder.ApkExportPreflightSeverity
import com.webtoapp.ui.design.WtaRowTone
import com.webtoapp.ui.design.WtaSectionDivider
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone

@Composable
fun ApkExportPreflightPanel(report: ApkExportPreflightReport) {
    val tone = when {
        report.hasErrors -> WtaStatusTone.Error
        report.warnings.isNotEmpty() -> WtaStatusTone.Warning
        else -> WtaStatusTone.Success
    }
    val message = when {
        report.hasErrors -> "发现 ${report.errors.size} 个阻塞问题，修复后才能开始构建。"
        report.warnings.isNotEmpty() -> "发现 ${report.warnings.size} 个非阻塞提醒，可以继续构建。"
        else -> "导出前检查通过。"
    }

    WtaStatusBanner(
        title = "导出前检查",
        message = message,
        tone = tone
    )

    if (report.issues.isNotEmpty()) {
        val visibleIssues = report.issues.take(6)
        WtaSettingCard {
            visibleIssues.forEachIndexed { index, issue ->
                WtaSettingRow(
                    title = issue.title,
                    subtitle = issue.message,
                    icon = if (issue.severity == ApkExportPreflightSeverity.Error) {
                        Icons.Outlined.ErrorOutline
                    } else {
                        Icons.Outlined.WarningAmber
                    },
                    tone = if (issue.severity == ApkExportPreflightSeverity.Error) {
                        WtaRowTone.Danger
                    } else {
                        WtaRowTone.Normal
                    }
                ) {
                    Text(
                        text = if (issue.severity == ApkExportPreflightSeverity.Error) "阻塞" else "提醒",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (issue.severity == ApkExportPreflightSeverity.Error) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
                if (index != visibleIssues.lastIndex) {
                    WtaSectionDivider()
                }
            }
        }
    }
}
