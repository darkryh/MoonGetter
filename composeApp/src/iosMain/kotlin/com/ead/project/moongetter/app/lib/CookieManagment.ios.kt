package com.ead.project.moongetter.app.lib

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie

actual val cookieManagement: MoonCookie.Management = MoonCookie.Management.newEmptyFactory()