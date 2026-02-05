package id.project.df.dnote.core.ui.markdown

import androidx.compose.ui.text.SpanStyle

interface MarkdownRule {
    val pattern: Regex
    
    /**
     * The symbol used to wrap/insert content (e.g., "**", "# ").
     * Used by the MarkdownFormatter for writing operations.
     */
    val insertionSymbol: String

    /**
     * Returns the style to be applied for a given match.
     * @param matchResult The result of the regex match, allowing for dynamic styling based on content.
     */
    fun getStyle(matchResult: MatchResult): SpanStyle

    /**
     * Returns the length of delimiters to hide when not focused.
     * @return Pair(startDelimiterLength, endDelimiterLength)
     */
    fun getDelimiterLengths(matchResult: MatchResult): Pair<Int, Int> = 0 to 0
}
