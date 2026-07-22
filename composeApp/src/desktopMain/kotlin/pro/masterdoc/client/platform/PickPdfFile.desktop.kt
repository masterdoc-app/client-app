package pro.masterdoc.client.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun pickPdfFile(): PickedPdf? =
    withContext(Dispatchers.IO) {
        val dialog = FileDialog(null as Frame?, "Выберите PDF руководства", FileDialog.LOAD)
        dialog.setFilenameFilter { _, name -> name.lowercase().endsWith(".pdf") }
        dialog.isMultipleMode = false
        dialog.isVisible = true
        val name = dialog.file ?: return@withContext null
        val dir = dialog.directory ?: return@withContext null
        if (!name.lowercase().endsWith(".pdf")) return@withContext null
        val file = File(dir, name)
        if (!file.isFile) return@withContext null
        PickedPdf(filename = file.name, bytes = file.readBytes())
    }
