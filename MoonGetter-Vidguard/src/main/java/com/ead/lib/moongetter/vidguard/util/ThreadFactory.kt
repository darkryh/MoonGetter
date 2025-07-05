package com.ead.lib.moongetter.vidguard.util

import java.util.concurrent.ThreadFactory

/**
 * A custom ThreadFactory that creates dedicated worker threads for VidGuard tasks.
 *
 * This factory sets up a specific ThreadGroup and configures each thread
 * with a descriptive name and an increased stack size to handle deep recursion
 * or heavy computational loads without stack overflow.
 */
internal object ThreadFactory : ThreadFactory {
    // A named ThreadGroup to group all VidGuard-related threads together
    private val group = ThreadGroup("VidGuard")

    /**
     * Creates a new Thread belonging to the VidGuard thread group.
     *
     * @param runnable The Runnable task to be executed on the new thread.
     * @return A Thread configured with:
     *   - Group: 'VidGuard'
     *   - Name: 'VidguardWorkerThread'
     *   - Stack size: 8 MB (8 * 1024 * 1024 bytes)
     */
    override fun newThread(runnable: Runnable): Thread = Thread(
        group,
        runnable,
        "VidguardWorkerThread",
        8 * 1024 * 1024 // Custom stack size in bytes
    )
}