package pro.masterdoc.client.portal

import kotlinx.browser.window

actual fun currentHostname(): String = window.location.hostname
