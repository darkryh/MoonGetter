package com.ead.project.moongetter.app.lib

import com.ead.lib.moongetter.client.cookie.java.net.JavaNetCookieManagement
import com.ead.lib.moongetter.client.cookie.managment.MoonCookie

actual val cookieManagement: MoonCookie.Management = JavaNetCookieManagement()