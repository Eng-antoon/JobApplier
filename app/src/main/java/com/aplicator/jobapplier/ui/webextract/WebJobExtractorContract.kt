package com.aplicator.jobapplier.ui.webextract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

data class WebExtractResult(
    val title: String?,
    val company: String?,
    val description: String?,
)

class WebJobExtractorContract : ActivityResultContract<String, WebExtractResult?>() {

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, WebJobExtractorActivity::class.java).apply {
            putExtra(WebJobExtractorActivity.EXTRA_URL, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): WebExtractResult? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return WebExtractResult(
            title = intent.getStringExtra(WebJobExtractorActivity.RESULT_TITLE),
            company = intent.getStringExtra(WebJobExtractorActivity.RESULT_COMPANY),
            description = intent.getStringExtra(WebJobExtractorActivity.RESULT_DESCRIPTION),
        )
    }
}
