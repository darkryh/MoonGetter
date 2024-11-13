@file:Suppress("UNUSED")

package com.ead.lib.moongetter.core

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE )
annotation class Pending

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE )
annotation class Unstable(val reason : String)

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This function is experimental some ExperimentalServer are not compatible with it"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class ExperimentalFeature

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This server is experimental his basic functionality is not guaranteed"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class ExperimentalServer