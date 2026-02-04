package id.project.df.dnote.core.ui.markdown

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.project.df.dnote.core.ui.markdown.rules.BoldRule
import id.project.df.dnote.core.ui.markdown.rules.HeaderRule
import id.project.df.dnote.core.ui.markdown.rules.ItalicRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownRulesTest {

    @Test
    fun `BoldRule matches double asterisks`() {
        val rule = BoldRule()
        val text = "This is **bold** text"
        val match = rule.pattern.find(text)

        assertTrue("Should find a match", match != null)
        assertEquals("**bold**", match?.value)
        assertEquals(FontWeight.Bold, rule.getStyle(match!!).fontWeight)
    }

    @Test
    fun `ItalicRule matches single asterisk`() {
        val rule = ItalicRule()
        val text = "This is *italic* text"
        val match = rule.pattern.find(text)

        assertTrue("Should find a match", match != null)
        assertEquals("*italic*", match?.value)
        assertEquals(FontStyle.Italic, rule.getStyle(match!!).fontStyle)
    }
    
    @Test
    fun `ItalicRule does not match bold`() {
        val rule = ItalicRule()
        val text = "This is **bold** text"
        val matches = rule.pattern.findAll(text).toList()
        
        assertTrue("Should not match bold syntax as italic", matches.isEmpty())
    }

    @Test
    fun `HeaderRule matches H1`() {
        val rule = HeaderRule()
        val text = "# Header One"
        val match = rule.pattern.find(text)

        assertTrue("Should find a match", match != null)
        assertEquals("# Header One", match?.value)
        
        val style = rule.getStyle(match!!)
        assertEquals(FontWeight.Bold, style.fontWeight)
        assertEquals(24.sp, style.fontSize)
    }

    @Test
    fun `HeaderRule matches H2`() {
        val rule = HeaderRule()
        val text = "## Header Two"
        val match = rule.pattern.find(text)

        assertTrue("Should find a match", match != null)
        
        val style = rule.getStyle(match!!)
        assertEquals(20.sp, style.fontSize)
    }

    @Test
    fun `BoldRule has correct delimiter lengths`() {
        val rule = BoldRule()
        val text = "**bold**"
        val match = rule.pattern.find(text)!!
        val (start, end) = rule.getDelimiterLengths(match)
        assertEquals(2, start)
        assertEquals(2, end)
    }

    @Test
    fun `ItalicRule has correct delimiter lengths`() {
        val rule = ItalicRule()
        val text = "*italic*"
        val match = rule.pattern.find(text)!!
        val (start, end) = rule.getDelimiterLengths(match)
        assertEquals(1, start)
        assertEquals(1, end)
    }

    @Test
    fun `HeaderRule has correct delimiter lengths`() {
        val rule = HeaderRule()
        val text = "# Header"
        val match = rule.pattern.find(text)!!
        val (start, end) = rule.getDelimiterLengths(match)
        assertEquals(2, start)
        assertEquals(0, end)
    }
}
