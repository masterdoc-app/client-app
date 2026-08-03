package pro.masterdoc.client.ui.screens

import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MentorMarkdownTest {
    @Test
    fun boldMarkersBecomeBoldSpansAndAreRemovedFromText() {
        val annotated = mentorMarkdownAnnotated("1. **Остановите компрессор**, отключите питание")
        assertEquals("1. Остановите компрессор, отключите питание", annotated.text)
        val bold =
            annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, bold.size)
        assertEquals("Остановите компрессор", annotated.text.substring(bold.single().start, bold.single().end))
    }

    @Test
    fun multipleBoldSegmentsInOneLine() {
        val annotated =
            mentorMarkdownAnnotated(
                "Затем **залейте свежее масло Ultra Coolant** до отметки.",
            )
        assertEquals("Затем залейте свежее масло Ultra Coolant до отметки.", annotated.text)
        val bold = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, bold.size)
        assertEquals(
            "залейте свежее масло Ultra Coolant",
            annotated.text.substring(bold.single().start, bold.single().end),
        )
    }

    @Test
    fun plainTextUnchanged() {
        val annotated = mentorMarkdownAnnotated("Привет! У меня есть информация.")
        assertEquals("Привет! У меня есть информация.", annotated.text)
        assertTrue(annotated.spanStyles.none { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun unmatchedMarkersLeftAsIs() {
        val annotated = mentorMarkdownAnnotated("текст **без закрытия")
        assertEquals("текст **без закрытия", annotated.text)
    }

    @Test
    fun multilinePreservesNewlinesAndBold() {
        val annotated =
            mentorMarkdownAnnotated(
                "**Меры безопасности**:\nОстерегайтесь горячих поверхностей",
            )
        assertEquals("Меры безопасности:\nОстерегайтесь горячих поверхностей", annotated.text)
        val bold = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, bold.size)
        assertEquals("Меры безопасности", annotated.text.substring(bold.single().start, bold.single().end))
    }
}
