package com.ead.lib.moongetter.vidguard.util

import java.util.concurrent.Executors


/**
 * A single-threaded executor service dedicated to VidGuard operations.
 *
 * This executor uses the custom ThreadFactory defined above to ensure
 * all tasks run on a thread with adequate stack size and grouping.
 */
internal val threadService = Executors.newSingleThreadExecutor(ThreadFactory)