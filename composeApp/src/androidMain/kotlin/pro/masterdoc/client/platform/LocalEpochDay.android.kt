package pro.masterdoc.client.platform

import java.time.LocalDate

actual fun localEpochDay(): Long = LocalDate.now().toEpochDay()
