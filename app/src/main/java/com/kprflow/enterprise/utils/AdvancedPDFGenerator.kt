package com.kprflow.enterprise.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.*
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.*
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.kprflow.enterprise.data.model.ExportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Advanced PDF Generator with iText 7 Core (Free for AGPL)
 * Supports bookmarks, annotations, watermarks, and advanced formatting
 */
class AdvancedPDFGenerator(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    /**
     * Generate advanced PDF with bookmarks and annotations
     */
    suspend fun generateAdvancedPDF(
        exportData: ExportData,
        outputPath: String,
        includeBookmarks: Boolean = true,
        includeWatermark: Boolean = true,
        includeAnnotations: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument(Writer(FileOutputStream(outputPath)))
            val document = Document(pdfDoc)
            
            // Setup fonts
            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
            
            // Add metadata
            pdfDoc.documentInfo = PdfDocumentInfo().apply {
                title = "Laporan KPRFlow - ${exportData.title}"
                author = "KPRFlow Enterprise"
                subject = exportData.title
                creator = "KPRFlow Enterprise"
                producer = "KPRFlow PDF Generator"
                creationDate = Calendar.getInstance()
            }
            
            // Add watermark if enabled
            if (includeWatermark) {
                addWatermark(pdfDoc, "CONFIDENTIAL - KPRFlow Enterprise")
            }
            
            // Add bookmarks and content
            if (includeBookmarks) {
                addBookmarkedContent(document, exportData, font, boldFont)
            } else {
                addBasicContent(document, exportData, font, boldFont)
            }
            
            // Add annotations if enabled
            if (includeAnnotations) {
                addAnnotations(pdfDoc, exportData)
            }
            
            document.close()
            Result.success(outputPath)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add content with bookmarks
     */
    private fun addBookmarkedContent(
        document: Document,
        exportData: ExportData,
        font: PdfFont,
        boldFont: PdfFont
    ) {
        // Executive Summary Bookmark
        val executiveBookmark = PdfOutline(document.pdfDocument.outlines, "Executive Summary")
        addExecutiveSummary(document, exportData, font, boldFont)
        
        // Detailed Data Bookmark
        val dataBookmark = PdfOutline(document.pdfDocument.outlines, "Detailed Data")
        addDetailedData(document, exportData, font, boldFont)
        
        // Analytics Bookmark
        val analyticsBookmark = PdfOutline(document.pdfDocument.outlines, "Analytics")
        addAnalyticsSection(document, exportData, font, boldFont)
        
        // Compliance Bookmark
        val complianceBookmark = PdfOutline(document.pdfDocument.outlines, "Compliance")
        addComplianceSection(document, exportData, font, boldFont)
    }
    
    /**
     * Add executive summary section
     */
    private fun addExecutiveSummary(
        document: Document,
        exportData: ExportData,
        font: PdfFont,
        boldFont: PdfFont
    ) {
        document.add(Paragraph("Executive Summary")
            .setFont(boldFont)
            .setFontSize(18)
            .setMarginBottom(20f))
        
        // Summary cards
        val summaryTable = Table(4).useAllAvailableWidth()
        summaryTable.addHeaderCell(Paragraph("Total Applications").setFont(boldFont))
        summaryTable.addHeaderCell(Paragraph("Conversion Rate").setFont(boldFont))
        summaryTable.addHeaderCell(Paragraph("Total Revenue").setFont(boldFont))
        summaryTable.addHeaderCell(Paragraph("Avg Processing Time").setFont(boldFont))
        
        summaryTable.addCell(Paragraph(exportData.totalApplications.toString()).setFont(font))
        summaryTable.addCell(Paragraph("${exportData.conversionRate}%").setFont(font))
        summaryTable.addCell(Paragraph("Rp ${formatCurrency(exportData.totalRevenue)}").setFont(font))
        summaryTable.addCell(Paragraph("${exportData.avgProcessingTime} days").setFont(font))
        
        document.add(summaryTable.setMarginBottom(20f))
    }
    
    /**
     * Add detailed data section
     */
    private fun addDetailedData(
        document: Document,
        exportData: ExportData,
        font: PdfFont,
        boldFont: PdfFont
    ) {
        document.add(Paragraph("Detailed Data")
            .setFont(boldFont)
            .setFontSize(16)
            .setMarginTop(30f)
            .setMarginBottom(15f))
        
        // Create detailed table
        val dataTable = Table(UnitValue.createPercentArray(floatArrayOf(15f, 20f, 15f, 15f, 15f, 20f)))
            .useAllAvailableWidth()
        
        // Headers
        dataTable.addHeaderCell(Paragraph("Customer Name").setFont(boldFont))
        dataTable.addHeaderCell(Paragraph("Unit").setFont(boldFont))
        dataTable.addHeaderCell(Paragraph("Status").setFont(boldFont))
        dataTable.addHeaderCell(Paragraph("Progress").setFont(boldFont))
        dataTable.addHeaderCell(Paragraph("Revenue").setFont(boldFont))
        dataTable.addHeaderCell(Paragraph("Last Updated").setFont(boldFont))
        
        // Data rows
        exportData.detailedData.forEach { item ->
            dataTable.addCell(Paragraph(item.customerName).setFont(font))
            dataTable.addCell(Paragraph(item.unitInfo).setFont(font))
            dataTable.addCell(Paragraph(item.status).setFont(font))
            dataTable.addCell(Paragraph("${item.progress}%").setFont(font))
            dataTable.addCell(Paragraph("Rp ${formatCurrency(item.revenue)}").setFont(font))
            dataTable.addCell(Paragraph(dateFormat.format(item.lastUpdated)).setFont(font))
        }
        
        document.add(dataTable)
    }
    
    /**
     * Add analytics section
     */
    private fun addAnalyticsSection(
        document: Document,
        exportData: ExportData,
        font: PdfFont,
        boldFont: PdfFont
    ) {
        document.add(Paragraph("Analytics & Insights")
            .setFont(boldFont)
            .setFontSize(16)
            .setMarginTop(30f)
            .setMarginBottom(15f))
        
        // Performance metrics
        val performanceList = List()
        performanceList.add(ListItem("Total Applications Processed: ${exportData.totalApplications}"))
        performanceList.add(ListItem("Average Processing Time: ${exportData.avgProcessingTime} days"))
        performanceList.add(ListItem("Conversion Rate: ${exportData.conversionRate}%"))
        performanceList.add(ListItem("Revenue per Application: Rp ${formatCurrency(exportData.totalRevenue / exportData.totalApplications)}"))
        
        document.add(performanceList)
        
        // Add simple chart representation
        addSimpleChart(document, exportData, font, boldFont)
    }
    
    /**
     * Add compliance section
     */
    private fun addComplianceSection(
        document: Document,
        exportData: ExportData,
        font: PdfFont,
        boldFont: PdfFont
    ) {
        document.add(Paragraph("Compliance & Audit")
            .setFont(boldFont)
            .setFontSize(16)
            .setMarginTop(30f)
            .setMarginBottom(15f))
        
        val complianceInfo = Paragraph(
            "This report was generated on ${dateFormat.format(Date())} at ${timeFormat.format(Date())} " +
            "by KPRFlow Enterprise System. All data is confidential and intended for internal use only."
        ).setFont(font).setFontSize(10)
        
        document.add(complianceInfo)
        
        // Add digital signature placeholder
        document.add(Paragraph("\n\nDigital Signature: _________________________")
            .setFont(font)
            .setFontSize(12)
            .setMarginTop(50f))
    }
    
    /**
     * Add watermark to all pages
     */
    private fun addWatermark(pdfDoc: PdfDocument, watermarkText: String) {
        val pageCount = pdfDoc.numberOfPages
        for (i in 1..pageCount) {
            val page = pdfDoc.getPage(i)
            val canvas = PdfCanvas(page.newContentStreamBefore(), page.resources, pdfDoc)
            
            canvas.setFillColor(Color.LTGRAY, 0.3f)
            canvas.beginText()
            canvas.setFontAndSize(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD), 48)
            canvas.showTextAligned(watermarkText, 297.5f, 421f, 45f, PdfCanvas.TextAlignment.CENTER, PdfCanvas.BaseLine.MIDDLE, 0.4f)
            canvas.endText()
            canvas.release()
        }
    }
    
    /**
     * Add annotations to PDF
     */
    private fun addAnnotations(pdfDoc: PdfDocument, exportData: ExportData) {
        // Add note annotations for important metrics
        val noteText = "Key metrics show ${if (exportData.conversionRate > 70) "strong" else "moderate"} performance"
        
        // Add annotation to first page
        val page = pdfDoc.getFirstPage()
        val annotation = PdfTextAnnotation(
            Rectangle(50f, 700f, 200f, 50f)
        ).setTitle(PdfString("Performance Note"))
         .setContents(PdfString(noteText))
         .setOpen(true)
        
        page.addAnnotation(annotation)
    }
    
    /**
     * Add simple chart representation
     */
    private fun addSimpleChart(
        document: Document,
        exportData: ExportData,
        font: PdfFont,
        boldFont: PdfFont
    ) {
        document.add(Paragraph("Revenue Distribution")
            .setFont(boldFont)
            .setFontSize(14)
            .setMarginTop(20f)
            .setMarginBottom(10f))
        
        // Create simple bar chart using table
        val chartTable = Table(2).useAllAvailableWidth()
        
        exportData.revenueByCategory.forEach { (category, amount) ->
            chartTable.addCell(Paragraph(category).setFont(font))
            chartTable.addCell(Paragraph("Rp ${formatCurrency(amount)}").setFont(font))
        }
        
        document.add(chartTable)
    }
    
    /**
     * Add basic content (non-bookmarked version)
     */
    private fun addBasicContent(
        document: Document,
        exportData: ExportData,
        font: PdfFont,
        boldFont: PdfFont
    ) {
        addExecutiveSummary(document, exportData, font, boldFont)
        addDetailedData(document, exportData, font, boldFont)
        addAnalyticsSection(document, exportData, font, boldFont)
        addComplianceSection(document, exportData, font, boldFont)
    }
    
    /**
     * Format currency for display
     */
    private fun formatCurrency(amount: Double): String {
        return String.format("%,.0f", amount)
    }
    
    /**
     * Generate PDF with error handling
     */
    suspend fun generatePDFWithErrorHandling(
        exportData: ExportData,
        outputPath: String,
        options: PDFOptions = PDFOptions()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate input data
            if (exportData.detailedData.isEmpty()) {
                return@withContext Result.failure(Exception("No data to export"))
            }
            
            // Check file permissions
            val outputFile = File(outputPath)
            if (outputFile.exists() && !outputFile.canWrite()) {
                return@withContext Result.failure(Exception("Cannot write to output file"))
            }
            
            // Generate PDF
            val result = generateAdvancedPDF(
                exportData = exportData,
                outputPath = outputPath,
                includeBookmarks = options.includeBookmarks,
                includeWatermark = options.includeWatermark,
                includeAnnotations = options.includeAnnotations
            )
            
            // Verify output file
            if (outputFile.exists() && outputFile.length() > 0) {
                Result.success(outputPath)
            } else {
                Result.failure(Exception("PDF generation failed - empty file"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * PDF generation options
 */
data class PDFOptions(
    val includeBookmarks: Boolean = true,
    val includeWatermark: Boolean = true,
    val includeAnnotations: Boolean = true,
    val includeCharts: Boolean = true,
    val includeCompliance: Boolean = true
)
