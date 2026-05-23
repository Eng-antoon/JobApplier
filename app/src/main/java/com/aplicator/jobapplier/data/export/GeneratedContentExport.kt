package com.aplicator.jobapplier.data.export

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
) {
    Pdf("pdf", "application/pdf"),
    Docx("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
}

object GeneratedContentExport {
    private const val DEFAULT_FILE_NAME = "Generated_Answer"
    private val unsupportedFileCharacters = Regex("""[\\/:*?"<>|]+""")
    private val repeatedSeparator = Regex("""[_\s]+""")

    fun suggestedFileName(
        contentTypeLabel: String,
        companyName: String,
        roleTitle: String,
    ): String {
        return sanitizeBaseName(
            listOf(contentTypeLabel, companyName, roleTitle)
                .filter { it.isNotBlank() }
                .joinToString("_"),
        )
    }

    fun buildFileName(userInput: String, format: ExportFormat): String {
        val withoutKnownExtension = userInput
            .trim()
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
            .removeSuffix(".docx")
            .removeSuffix(".DOCX")
        return "${sanitizeBaseName(withoutKnownExtension)}.${format.extension}"
    }

    fun createBytes(content: String, format: ExportFormat): ByteArray {
        return when (format) {
            ExportFormat.Pdf -> createPdfBytes(content)
            ExportFormat.Docx -> createDocxBytes(content)
        }
    }

    fun createPdfBytes(content: String): ByteArray {
        val lines = content.normalizedParagraphs().flatMapIndexed { index, paragraph ->
            buildList {
                if (index > 0) add("")
                addAll(wrapText(paragraph, maxCharacters = 86))
            }
        }
        val contentStream = buildString {
            appendLine("BT")
            appendLine("/F1 11 Tf")
            appendLine("54 738 Td")
            appendLine("16 TL")
            lines.forEach { line ->
                append("(")
                append(line.escapePdfText())
                appendLine(") Tj")
                appendLine("T*")
            }
            appendLine("ET")
        }
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            "<< /Length ${contentStream.toByteArray(Charsets.ISO_8859_1).size} >>\nstream\n$contentStream\nendstream",
        )
        return buildPdf(objects)
    }

    fun createDocxBytes(content: String): ByteArray {
        return XWPFDocument().use { document ->
            content.normalizedParagraphs().forEach { paragraphText ->
                val paragraph = document.createParagraph()
                paragraph.alignment = ParagraphAlignment.LEFT
                paragraph.spacingAfter = 180
                val run = paragraph.createRun()
                run.fontFamily = "Arial"
                run.fontSize = 11
                run.setText(paragraphText)
            }
            ByteArrayOutputStream().use { output ->
                document.write(output)
                output.toByteArray()
            }
        }
    }

    fun saveToDownloads(
        context: Context,
        userFileName: String,
        content: String,
        format: ExportFormat,
    ): Result<String> = runCatching {
        val fileName = buildFileName(userFileName, format)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create Downloads file")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(createBytes(content, format))
            } ?: error("Could not open Downloads file")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            fileName
        } catch (exception: Exception) {
            resolver.delete(uri, null, null)
            throw exception
        }
    }

    private fun sanitizeBaseName(value: String): String {
        val sanitized = value
            .replace(unsupportedFileCharacters, "_")
            .replace(repeatedSeparator, "_")
            .trim('_', '.', ' ')
        return sanitized.ifBlank { DEFAULT_FILE_NAME }
    }

    private fun String.normalizedParagraphs(): List<String> {
        return trim()
            .split(Regex("""\n\s*\n"""))
            .map { it.replace(Regex("""\s*\n\s*"""), " ").trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("") }
    }

    private fun wrapText(text: String, maxCharacters: Int): List<String> {
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.isEmpty()) return listOf("")

        val lines = mutableListOf<String>()
        var current = words.first()
        words.drop(1).forEach { word ->
            val candidate = "$current $word"
            if (candidate.length <= maxCharacters) {
                current = candidate
            } else {
                lines += current
                current = word
            }
        }
        lines += current
        return lines
    }

    private fun String.escapePdfText(): String {
        return replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
    }

    private fun buildPdf(objects: List<String>): ByteArray {
        val output = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf(0)
        objects.forEachIndexed { index, body ->
            offsets += output.toString().toByteArray(Charsets.ISO_8859_1).size
            output.append("${index + 1} 0 obj\n")
            output.append(body)
            output.append("\nendobj\n")
        }
        val xrefOffset = output.toString().toByteArray(Charsets.ISO_8859_1).size
        output.append("xref\n")
        output.append("0 ${objects.size + 1}\n")
        output.append("0000000000 65535 f \n")
        offsets.drop(1).forEach { offset ->
            output.append(offset.toString().padStart(10, '0'))
            output.append(" 00000 n \n")
        }
        output.append("trailer\n")
        output.append("<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        output.append("startxref\n")
        output.append("$xrefOffset\n")
        output.append("%%EOF")
        return output.toString().toByteArray(Charsets.ISO_8859_1)
    }
}
