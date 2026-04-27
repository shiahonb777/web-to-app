package com.webtoapp.ui.viewmodel.community

data class FailureReportParams(
    val title: String,
    val stage: String,
    val summary: String,
    val serviceMessage: String? = null,
    val throwable: Throwable? = null,
    val extraContext: String? = null
)
