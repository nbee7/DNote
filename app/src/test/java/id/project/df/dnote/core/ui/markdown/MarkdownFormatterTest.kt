package id.project.df.dnote.core.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import id.project.df.dnote.core.ui.markdown.rules.BoldRule
import id.project.df.dnote.core.ui.markdown.rules.HeaderRule
import id.project.df.dnote.core.ui.markdown.rules.ItalicRule
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownFormatterTest {

    private val formatter = MarkdownFormatter()

    @Test
    fun `toggleCyclicHeading cycles correctly`() {
        val rule = HeaderRule()
        
        // Initial: No header
        var value = TextFieldValue("Text", TextRange(4))
        
        // 1st Tap: H1
        value = formatter.toggleCyclicHeading(value, rule)
        assertEquals("# Text", value.text)
        assertEquals(6, value.selection.start) // Cursor shifted by +2 (# )

        // 2nd Tap: H2
        value = formatter.toggleCyclicHeading(value, rule)
        assertEquals("## Text", value.text)
        assertEquals(7, value.selection.start) // Cursor shifted by +1 (#)

        // 3rd Tap: H3
        value = formatter.toggleCyclicHeading(value, rule)
        assertEquals("### Text", value.text)
        assertEquals(8, value.selection.start) // Cursor shifted by +1 (#)

        // 4th Tap: Back to Normal
        value = formatter.toggleCyclicHeading(value, rule)
        assertEquals("Text", value.text)
        assertEquals(4, value.selection.start) // Cursor shifted back
    }

    @Test
    fun `toggleStyle bold wraps selection`() {
        val rule = BoldRule()
        val value = TextFieldValue("Select me", TextRange(0, 6)) // "Select"
        
        val result = formatter.toggleStyle(value, rule)
        
        assertEquals("**Select** me", result.text)
        assertEquals(TextRange(0, 10), result.selection) // Selection matches wrapped content
    }

    @Test
    fun `toggleStyle bold unwraps selection`() {
        val rule = BoldRule()
        val value = TextFieldValue("**Select** me", TextRange(0, 10)) // "**Select**"
        
        val result = formatter.toggleStyle(value, rule)
        
        assertEquals("Select me", result.text)
        assertEquals(TextRange(0, 6), result.selection)
    }

    @Test
    fun `toggleStyle expands to word when collapsed`() {
        val rule = ItalicRule()
        // Cursor at "W|ord"
        val value = TextFieldValue("Hello Word", TextRange(7)) 
        
        val result = formatter.toggleStyle(value, rule)
        
        assertEquals("Hello *Word*", result.text)
    }

    @Test
    fun `toggleStyle inserts wrapper in whitespace`() {
        val rule = BoldRule()
        val value = TextFieldValue("Hello ", TextRange(6)) // End of string

        val result = formatter.toggleStyle(value, rule)

        assertEquals("Hello ****", result.text)
        assertEquals(8, result.selection.start) // Cursor in middle (6 + 2)
    }

    @Test
    fun `processInput normal char no change`() {
        val oldValue = TextFieldValue("hello", TextRange(5))
        val newValue = TextFieldValue("hello ", TextRange(6))

        val result = formatter.processInput(newValue, oldValue)

        assertEquals(newValue, result)
    }

    @Test
    fun `processInput completing bold adds space`() {
        val oldValue = TextFieldValue("**word", TextRange(6))
        val newValue = TextFieldValue("**word**", TextRange(8))

        val result = formatter.processInput(newValue, oldValue)

        assertEquals("**word** ", result.text)
        assertEquals(9, result.selection.start)
    }

    @Test
    fun `processInput completing italic adds space`() {
        val oldValue = TextFieldValue("*word", TextRange(5))
        val newValue = TextFieldValue("*word*", TextRange(6))

        val result = formatter.processInput(newValue, oldValue)

        assertEquals("*word* ", result.text)
        assertEquals(7, result.selection.start)
    }

    @Test
    fun `processInput text deletion returns unchanged`() {
        val oldValue = TextFieldValue("hello", TextRange(5))
        val newValue = TextFieldValue("hell", TextRange(4))

        val result = formatter.processInput(newValue, oldValue)

        assertEquals(newValue, result)
    }

    @Test
    fun `processInput cursor not at match end returns unchanged`() {
        val oldValue = TextFieldValue("**word**", TextRange(3))
        val newValue = TextFieldValue("**word**x", TextRange(4))

        val result = formatter.processInput(newValue, oldValue)

        assertEquals(newValue, result)
    }

    @Test
    fun `processInput empty rules returns unchanged`() {
        val oldValue = TextFieldValue("**word", TextRange(6))
        val newValue = TextFieldValue("**word**", TextRange(8))

        val result = formatter.processInput(newValue, oldValue, emptyList())

        assertEquals(newValue, result)
    }
}
