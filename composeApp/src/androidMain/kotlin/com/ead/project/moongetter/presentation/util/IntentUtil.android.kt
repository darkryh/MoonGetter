package com.ead.project.moongetter.presentation.util

import android.content.Intent
import androidx.core.net.toUri
import com.ead.project.moongetter.MoonGetterApp

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object IntentUtil {
    actual fun goIntentTo(url: String) {
        val context = MoonGetterApp.instance
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}