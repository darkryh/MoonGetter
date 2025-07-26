package com.ead.project.moongetter.app.lib

import com.ead.lib.moongetter.client.cookie.java.net.JavaNetCookieManagement
import com.ead.lib.moongetter.client.cookie.managment.MoonGetterClientCookieManagement

actual val cookieManagement: MoonGetterClientCookieManagement = JavaNetCookieManagement()