@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.ead.project.moongetter.presentation.util

expect object IntentUtil {
    fun goIntentTo(url: String)
}