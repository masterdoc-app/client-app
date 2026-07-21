package pro.masterdoc.client

import kotlinx.browser.window

actual fun currentHostname(): String = window.location.hostname
