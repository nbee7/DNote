package id.project.df.dnote.core.ui.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MarkdownVisualTransformation(
    private val rules: List<MarkdownRule>,
    private val cursorPosition: Int = -1
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val builder = AnnotatedString.Builder()
        
        val hiddenIndices = BooleanArray(originalText.length) { false }
        
        rules.forEach { rule ->
            rule.pattern.findAll(originalText).forEach { matchResult ->
                val range = matchResult.range
                val isFocused = cursorPosition in range.first..range.last + 1
                
                if (!isFocused) {
                    val (startDelimLen, endDelimLen) = rule.getDelimiterLengths(matchResult)
                    
                    for (i in 0 until startDelimLen) {
                        if (range.first + i < originalText.length) {
                            hiddenIndices[range.first + i] = true
                        }
                    }
                    
                    for (i in 0 until endDelimLen) {
                        if (range.last - i >= 0) {
                            hiddenIndices[range.last - i] = true
                        }
                    }
                }
            }
        }


        val originalToTransformed = IntArray(originalText.length + 1)
        val transformedToOriginal = mutableListOf<Int>()
        
        var transformedIndex = 0
        for (i in originalText.indices) {
            originalToTransformed[i] = transformedIndex
            if (!hiddenIndices[i]) {
                builder.append(originalText[i])
                transformedToOriginal.add(i)
                transformedIndex++
            }
        }
        originalToTransformed[originalText.length] = transformedIndex
        transformedToOriginal.add(originalText.length)

        val transformedString = builder.toAnnotatedString()

        val styleBuilder = AnnotatedString.Builder(transformedString)

        rules.forEach { rule ->
            rule.pattern.findAll(originalText).forEach { matchResult ->
                val range = matchResult.range

                val start = originalToTransformed[range.first]
                val end = originalToTransformed[range.last + 1]
                
                if (end > start) {
                     val style = rule.getStyle(matchResult)
                     styleBuilder.addStyle(style, start, end)
                }
            }
        }

        return TransformedText(
            styleBuilder.toAnnotatedString(),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return originalToTransformed[offset.coerceIn(0, originalText.length)]
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return transformedToOriginal[offset.coerceIn(0, transformedToOriginal.lastIndex)]
                }
            }
        )
    }
}
