package com.ead.project.moongetter.presentation.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

object IntentUtil {
    fun goIntentTo(context: Context,url: String) = Intent().apply {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }
}