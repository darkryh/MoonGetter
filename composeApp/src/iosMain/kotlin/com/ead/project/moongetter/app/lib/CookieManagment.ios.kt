package com.ead.project.moongetter.app.lib

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.cookie.managment.IosCookieManagement

actual val cookieManagement: MoonCookie.Management = IosCookieManagement()