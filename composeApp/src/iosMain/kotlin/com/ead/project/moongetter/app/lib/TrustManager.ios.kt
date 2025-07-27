package com.ead.project.moongetter.app.lib

import com.ead.lib.moongetter.client.trust.manager.MoonTrust

actual val trustManager: MoonTrust.Manager = MoonTrust.Manager.newEmptyFactory()