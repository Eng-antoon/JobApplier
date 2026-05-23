package com.aplicator.jobapplier.data.export

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedContentExportTest {
    @Test
    fun exportFileName_sanitizesUserInputAndAppendsExtension() {
        assertEquals(
            "My_Answer_Acme_Role.pdf",
            GeneratedContentExport.buildFileName("  My Answer: Acme/Role  ", ExportFormat.Pdf),
        )
        assertEquals(
            "answer.docx",
            GeneratedContentExport.buildFileName("answer.docx", ExportFormat.Docx),
        )
    }

    @Test
    fun suggestedFileName_usesContentTypeCompanyAndRole() {
        assertEquals(
            "Why_This_Company_Acme_Android_Engineer",
            GeneratedContentExport.suggestedFileName(
                contentTypeLabel = "Why This Company",
                companyName = "Acme",
                roleTitle = "Android Engineer",
            ),
        )
    }

    @Test
    fun createPdfBytes_producesReadablePdfWithAnswerText() {
        val bytes = GeneratedContentExport.createPdfBytes("Hello hiring team.\n\nI am excited to apply.")

        assertTrue(bytes.isNotEmpty())
        val pdf = bytes.toString(Charsets.ISO_8859_1)
        assertTrue(pdf.startsWith("%PDF-1.4"))
        assertTrue(pdf.contains("Hello hiring team."))
        assertTrue(pdf.contains("I am excited to apply."))
    }

    @Test
    fun createDocxBytes_producesReadableDocxWithAnswerText() {
        val bytes = GeneratedContentExport.createDocxBytes("First paragraph.\n\nSecond paragraph.")

        assertTrue(bytes.isNotEmpty())
        XWPFDocument(bytes.inputStream()).use { document ->
            val text = document.paragraphs.joinToString("\n") { it.text }
            assertTrue(text.contains("First paragraph."))
            assertTrue(text.contains("Second paragraph."))
        }
    }
}
