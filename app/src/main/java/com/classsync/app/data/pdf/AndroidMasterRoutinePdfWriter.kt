package com.classsync.app.data.pdf

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.classsync.app.domain.master.MasterPdfDocument
import com.classsync.app.domain.master.MasterPdfSection
import java.io.OutputStream

object AndroidMasterRoutinePdfWriter {
    private const val PortraitWidth = 595
    private const val PortraitHeight = 842
    private const val LandscapeWidth = 842
    private const val LandscapeHeight = 595
    private const val Margin = 36f
    private const val FooterHeight = 24f

    fun write(document: MasterPdfDocument, output: OutputStream) {
        val pdf = PdfDocument()
        var pageNumber = 0
        document.sections.forEach { section ->
            val width = if (section.landscape) LandscapeWidth else PortraitWidth
            val height = if (section.landscape) LandscapeHeight else PortraitHeight
            val availableHeight = height - Margin * 2 - FooterHeight - 64f
            val rowHeight = if (section.landscape) 42f else 34f
            val rowsPerPage = if (section.rows.isEmpty()) 1 else (availableHeight / rowHeight).toInt().coerceAtLeast(1)
            val chunks = if (section.rows.isEmpty()) listOf(emptyList()) else section.rows.chunked(rowsPerPage)
            chunks.forEachIndexed { chunkIndex, rows ->
                pageNumber += 1
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
                drawSection(
                    canvas = page.canvas,
                    section = section.copy(rows = rows),
                    continued = chunkIndex > 0,
                    pageNumber = pageNumber,
                    documentTitle = document.title,
                    width = width.toFloat(),
                    height = height.toFloat(),
                    rowHeight = rowHeight,
                )
                pdf.finishPage(page)
            }
        }
        pdf.writeTo(output)
        pdf.close()
    }

    private fun drawSection(
        canvas: Canvas,
        section: MasterPdfSection,
        continued: Boolean,
        pageNumber: Int,
        documentTitle: String,
        width: Float,
        height: Float,
        rowHeight: Float,
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF102A43.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1F2933.toInt(); textSize = 9f }
        val headerPaint = Paint(bodyPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8FA3B8.toInt(); style = Paint.Style.STROKE; strokeWidth = 0.8f }

        var y = Margin
        canvas.drawText(section.title + if (continued) " (continued)" else "", Margin, y + 18f, titlePaint)
        y += 28f
        if (section.subtitle.isNotBlank()) {
            canvas.drawText(section.subtitle, Margin, y + 11f, bodyPaint)
            y += 22f
        }
        section.notes.forEach { note ->
            canvas.drawText(note.take(100), Margin, y + 11f, bodyPaint)
            y += 18f
        }
        if (section.headers.isNotEmpty()) {
            val tableWidth = width - Margin * 2
            val columnWidth = tableWidth / section.headers.size
            drawRow(canvas, section.headers, Margin, y, columnWidth, rowHeight, headerPaint, linePaint)
            y += rowHeight
            section.rows.forEach { row ->
                drawRow(canvas, row, Margin, y, columnWidth, rowHeight, bodyPaint, linePaint)
                y += rowHeight
            }
        }
        val footer = "$documentTitle  •  Page $pageNumber"
        canvas.drawText(footer, Margin, height - Margin + 8f, bodyPaint)
    }

    private fun drawRow(
        canvas: Canvas,
        cells: List<String>,
        x: Float,
        y: Float,
        columnWidth: Float,
        rowHeight: Float,
        textPaint: Paint,
        linePaint: Paint,
    ) {
        cells.forEachIndexed { index, value ->
            val left = x + columnWidth * index
            canvas.drawRect(left, y, left + columnWidth, y + rowHeight, linePaint)
            val lines = value.split('\n').flatMap { wrap(it, textPaint, columnWidth - 8f) }.take(3)
            lines.forEachIndexed { lineIndex, line ->
                canvas.drawText(line, left + 4f, y + 13f + lineIndex * 11f, textPaint)
            }
        }
    }

    private fun wrap(value: String, paint: Paint, width: Float): List<String> {
        if (value.isBlank()) return listOf("")
        val result = mutableListOf<String>()
        var current = ""
        value.split(' ').forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (paint.measureText(candidate) <= width) current = candidate else {
                if (current.isNotBlank()) result += current
                current = word
            }
        }
        if (current.isNotBlank()) result += current
        return result
    }
}
