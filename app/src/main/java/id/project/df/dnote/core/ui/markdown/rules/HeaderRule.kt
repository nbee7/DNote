package id.project.df.dnote.core.ui.markdown.rules

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.project.df.dnote.core.ui.markdown.MarkdownRule

class HeaderRule : MarkdownRule {
    override val pattern: Regex = Regex("^(#{1,6})\\s+(.*)$", RegexOption.MULTILINE)

    override fun getStyle(matchResult: MatchResult): SpanStyle {
        val hashes = matchResult.groupValues[1]
        val level = hashes.length
        
        val fontSize = when (level) {
            1 -> 24.sp // H1
            2 -> 20.sp // H2
            3 -> 18.sp // H3
            else -> 16.sp // H4-H6
        }

        return SpanStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
    }

    override fun getDelimiterLengths(matchResult: MatchResult): Pair<Int, Int> {
        val totalMatch = matchResult.value
        val content = matchResult.groupValues[2]
        val prefixLength = totalMatch.length - content.length
        return prefixLength to 0
    }
}
