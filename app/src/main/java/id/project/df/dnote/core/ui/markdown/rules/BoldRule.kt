package id.project.df.dnote.core.ui.markdown.rules

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import id.project.df.dnote.core.ui.markdown.MarkdownRule

class BoldRule : MarkdownRule {
    override val pattern: Regex = Regex("(\\*\\*|__)(.*?)\\1")
    override val insertionSymbol: String = "**"

    override fun getStyle(matchResult: MatchResult): SpanStyle {
        return SpanStyle(fontWeight = FontWeight.Bold)
    }

    override fun getDelimiterLengths(matchResult: MatchResult): Pair<Int, Int> {
        return 2 to 2
    }
}
