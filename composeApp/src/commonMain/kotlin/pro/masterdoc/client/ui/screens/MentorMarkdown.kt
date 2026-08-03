package pro.masterdoc.client.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Minimal mentor markdown: `**bold**` → bold spans. Unmatched markers stay literal.
 * Enough for LLM replies without pulling a full markdown renderer into Wasm.
 */
fun mentorMarkdownAnnotated(markdown: String): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < markdown.length) {
            if (markdown.startsWith("**", i)) {
                val end = markdown.indexOf("**", startIndex = i + 2)
                if (end >= 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(markdown.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            append(markdown[i])
            i++
        }
    }

@Composable
fun MentorMarkdownText(
    content: String,
    modifier: Modifier = Modifier,
) {
    val annotated = remember(content) { mentorMarkdownAnnotated(content) }
    Text(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
