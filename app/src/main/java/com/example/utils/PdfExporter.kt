package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import com.example.network.GroqClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfExporter {
    fun exportAnalysisToPdf(context: Context, result: GroqClient.AnalysisResult, bookTitle: String) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }
        val headingPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 18f
            isFakeBoldText = true
        }
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 14f
        }

        val pageWidth = 595 // Standard A4 width at 72 PPI
        val pageHeight = 842
        var yPosition = 50
        val margin = 50
        val contentWidth = pageWidth - 2 * margin

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawText(text: String, currentPaint: TextPaint): Int {
            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, currentPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()

            if (yPosition + staticLayout.height > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.pages.size + 1).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = margin
            }

            canvas.save()
            canvas.translate(margin.toFloat(), yPosition.toFloat())
            staticLayout.draw(canvas)
            canvas.restore()

            yPosition += staticLayout.height + 20
            return yPosition
        }

        drawText("Study Session Summary: $bookTitle", titlePaint)
        
        drawText("Summary", headingPaint)
        drawText(result.summary, textPaint)

        if (result.keyPoints.isNotEmpty()) {
            drawText("Key Insights", headingPaint)
            val pointsStr = result.keyPoints.joinToString("\n") { "• $it" }
            drawText(pointsStr, textPaint)
        }

        if (result.connections.isNotEmpty()) {
            drawText("Broad Connections", headingPaint)
            val connsStr = result.connections.joinToString("\n") { "• $it" }
            drawText(connsStr, textPaint)
        }

        if (result.questions.isNotEmpty()) {
            drawText("Discussion Questions", headingPaint)
            val qStr = result.questions.mapIndexed { i, q -> "${i+1}. $q" }.joinToString("\n")
            drawText(qStr, textPaint)
        }

        pdfDocument.finishPage(page)

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        
        val sanitizedTitle = bookTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val file = File(dir, "Summary_${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
