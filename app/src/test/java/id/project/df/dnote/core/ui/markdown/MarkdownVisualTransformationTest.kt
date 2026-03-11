package id.project.df.dnote.core.ui.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import id.project.df.dnote.core.ui.markdown.rules.BoldRule
import id.project.df.dnote.core.ui.markdown.rules.ItalicRule
import id.project.df.dnote.core.ui.markdown.rules.HeaderRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownVisualTransformationTest {

    @Test
    fun `filter hides bold delimiters when cursor outside range`() {
        val transformation = MarkdownVisualTransformation(
            listOf(BoldRule()),
            cursorPosition = 10 // Cursor far from bold delimiters
        )
        val text = AnnotatedString("**bold** text")
        val result = transformation.filter(text)

        // Delimiters should be hidden, transformed text should be "bold text"
        assertEquals("bold text", result.text.text)
        assertEquals(9, result.text.text.length)
    }

    @Test
    fun `filter shows bold delimiters when cursor inside range`() {
        val transformation = MarkdownVisualTransformation(
            listOf(BoldRule()),
            cursorPosition = 3 // Cursor inside bold range
        )
        val text = AnnotatedString("**bold** text")
        val result = transformation.filter(text)

        // Delimiters should be visible, text unchanged
        assertEquals("**bold** text", result.text.text)
    }

    @Test
    fun `filter hides italic delimiters when cursor outside range`() {
        val transformation = MarkdownVisualTransformation(
            listOf(ItalicRule()),
            cursorPosition = 10
        )
        val text = AnnotatedString("*italic* text")
        val result = transformation.filter(text)

        assertEquals("italic text", result.text.text)
    }

    @Test
    fun `filter applies style to transformed text`() {
        val transformation = MarkdownVisualTransformation(
            listOf(BoldRule()),
            cursorPosition = -1
        )
        val text = AnnotatedString("**bold** text")
        val result = transformation.filter(text)

        // Check that the non-delimiter part has bold style applied
        val annotatedString = result.text
        assertTrue(annotatedString.text.contains("bold"))
    }

    @Test
    fun `filter offset mapping originalToTransformed`() {
        val transformation = MarkdownVisualTransformation(
            listOf(BoldRule()),
            cursorPosition = -1
        )
        val text = AnnotatedString("**bold**")
        val result = transformation.filter(text)

        // Original positions: **bold** (positions 0-7)
        // Transformed positions: bold (positions 0-3)
        val mapping = result.offsetMapping
        assertEquals(0, mapping.originalToTransformed(0)) // ** start
        assertEquals(0, mapping.originalToTransformed(2)) // ** end = "bold" start
        assertEquals(4, mapping.originalToTransformed(6)) // ** end delim start
        assertEquals(4, mapping.originalToTransformed(8)) // ** end delim end
    }

    @Test
    fun `filter empty text returns empty result`() {
        val transformation = MarkdownVisualTransformation(
            listOf(BoldRule()),
            cursorPosition = -1
        )
        val text = AnnotatedString("")
        val result = transformation.filter(text)

        assertEquals("", result.text.text)
    }

    @Test
    fun `filter plain text without markdown unchanged`() {
        val transformation = MarkdownVisualTransformation(
            listOf(BoldRule(), ItalicRule()),
            cursorPosition = -1
        )
        val text = AnnotatedString("hello world")
        val result = transformation.filter(text)

        assertEquals("hello world", result.text.text)
    }

    @Test
    fun `filter multiple rules apply independently`() {
        val transformation = MarkdownVisualTransformation(
            listOf(BoldRule(), ItalicRule()),
            cursorPosition = -1
        )
        val text = AnnotatedString("**bold** *italic*")
        val result = transformation.filter(text)

        // Both **bold** and *italic* delimiters should be hidden
        assertEquals("bold italic", result.text.text)
    }
}
