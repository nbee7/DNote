package id.project.df.dnote.core.ui.markdown.rules

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import id.project.df.dnote.core.ui.markdown.MarkdownRule

class ItalicRule : MarkdownRule {
    override val pattern: Regex = Regex("(?<!\\*)\\*(?![\\s\\*])((?:(?!\\*).)+)(?<!\\s)\\*(?!\\*)|(?<!_)(?<!\\w)_(?![\\s_])((?:(?!_).)+)(?<!\\s)_(?!\\w)(?!_)")

    override fun getStyle(matchResult: MatchResult): SpanStyle {
        return SpanStyle(fontStyle = FontStyle.Italic)
    }

    override fun getDelimiterLengths(matchResult: MatchResult): Pair<Int, Int> {
        return 1 to 1
    }
}
