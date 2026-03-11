package id.project.df.dnote.core.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import id.project.df.dnote.core.ui.markdown.rules.BoldRule
import id.project.df.dnote.core.ui.markdown.rules.HeaderRule
import id.project.df.dnote.core.ui.markdown.rules.ItalicRule

class MarkdownFormatter {

    fun toggleCyclicHeading(value: TextFieldValue, headerRule: HeaderRule): TextFieldValue {
        val text = value.text
        val selection = value.selection
        
        val cursor = selection.start
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val lineContent = text.substring(lineStart, lineEnd)

        val match = headerRule.pattern.find(lineContent)
        
        var newText = text
        var newSelection = selection

        if (match != null) {
            val hashes = match.groupValues[1]
            val content = match.groupValues[2]
            val currentLevel = hashes.length
            
            if (currentLevel < 3) {
                val newHashes = "#".repeat(currentLevel + 1)
                val prefixLen = match.value.length - content.length
                
                newText = text.replaceRange(lineStart, lineStart + prefixLen, "$newHashes ")
                
                newSelection = TextRange(selection.start + 1)
            } else {
                val prefixLen = match.value.length - content.length
                newText = text.replaceRange(lineStart, lineStart + prefixLen, "")
                
                val newCursor = (selection.start - prefixLen).coerceAtLeast(lineStart)
                newSelection = TextRange(newCursor)
            }
        } else {
            val replacement = headerRule.insertionSymbol
            newText = text.replaceRange(lineStart, lineStart, replacement)
            newSelection = TextRange(selection.start + replacement.length)
        }

        return value.copy(text = newText, selection = newSelection)
    }

    fun toggleStyle(value: TextFieldValue, rule: MarkdownRule): TextFieldValue {
        val text = value.text
        var selection = value.selection
        val delimiter = rule.insertionSymbol

        if (selection.collapsed) {
            val cursor = selection.start
            var start = cursor
            while (start > 0 && !text[start - 1].isWhitespace()) {
                start--
            }
            var end = cursor
            while (end < text.length && !text[end].isWhitespace()) {
                end++
            }
            
            if (start < end) {
                selection = TextRange(start, end)
            } else {
                val newText = text.replaceRange(cursor, cursor, "$delimiter$delimiter")
                return value.copy(text = newText, selection = TextRange(cursor + delimiter.length))
            }
        }

        val selectedText = text.substring(selection.start, selection.end)

        val isWrapped = selectedText.startsWith(delimiter) && 
                       selectedText.endsWith(delimiter) && 
                       selectedText.length >= delimiter.length * 2
        
        val newText: String
        val newSelection: TextRange

        if (isWrapped) {
            val content = selectedText.substring(delimiter.length, selectedText.length - delimiter.length)
            newText = text.replaceRange(selection.start, selection.end, content)
            newSelection = TextRange(selection.start, selection.start + content.length)
        } else {
            val content = "$delimiter$selectedText$delimiter"
            newText = text.replaceRange(selection.start, selection.end, content)
            newSelection = TextRange(selection.start, selection.start + content.length)
        }

        return value.copy(text = newText, selection = newSelection)
    }

    fun processInput(
        newValue: TextFieldValue,
        oldValue: TextFieldValue,
        rules: List<MarkdownRule> = listOf(BoldRule(), ItalicRule(), HeaderRule())
    ): TextFieldValue {
        val newText = newValue.text
        if (newText.length <= oldValue.text.length) return newValue
        val addedCharIndex = newValue.selection.start - 1
        if (addedCharIndex < 0 || addedCharIndex >= newText.length) return newValue
        return if (checkIfMatchCompleted(newText, newValue.selection.start, rules)) {
            newValue.copy(text = "$newText ", selection = TextRange(newValue.selection.start + 1))
        } else {
            newValue
        }
    }

    private fun checkIfMatchCompleted(text: String, cursorPosition: Int, rules: List<MarkdownRule>): Boolean {
        for (rule in rules) {
            for (match in rule.pattern.findAll(text)) {
                if (match.range.last + 1 == cursorPosition) {
                    val (_, endLen) = rule.getDelimiterLengths(match)
                    if (endLen > 0) return true
                }
            }
        }
        return false
    }
}
