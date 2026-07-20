package pro.masterdoc.client.technolog

import kotlinx.browser.window

actual fun currentHostname(): String = window.location.hostname
