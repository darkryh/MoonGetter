package com.ead.project.moongetter.presentation.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object IntentUtil {
    fun goIntentTo(context: Context,url: String) = Intent().apply {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}