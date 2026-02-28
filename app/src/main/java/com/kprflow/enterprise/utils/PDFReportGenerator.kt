package com.kprflow.enterprise.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PDFReportGenerator {
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    fun generateExecutiveSummaryReport(
        context: Context,
        data: ExecutiveReportData,
        fileName: String = "Executive_Summary_Report"
    ): Uri? {
        return try {
            val file = File(context.getExternalFilesDir(null), "$fileName.pdf")
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Add title
            val title = Paragraph("EXECUTIVE SUMMARY REPORT")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18f)
                .setBold()
            document.add(title)
            
            // Add date
            val date = Paragraph("Generated: ${dateFormat.format(Date())}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
            document.add(date)
            
            // Add spacing
            document.add(Paragraph("\n"))
            
            // Add Key Metrics section
            val metricsTitle = Paragraph("KEY METRICS")
                .setFontSize(14f)
                .setBold()
            document.add(metricsTitle)
            
            // Create metrics table
            val metricsTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
                .useAllAvailableWidth()
            
            metricsTable.addCell(Paragraph("Total Portfolio"))
            metricsTable.addCell(Paragraph(currencyFormat.format(data.totalPortfolio)))
            
            metricsTable.addCell(Paragraph("Active Applications"))
            metricsTable.addCell(Paragraph(data.activeApplications.toString()))
            
            metricsTable.addCell(Paragraph("Success Rate"))
            metricsTable.addCell(Paragraph("${data.successRate}%"))
            
            metricsTable.addCell(Paragraph("Avg Processing Time"))
            metricsTable.addCell(Paragraph("${data.avgProcessingTime} days"))
            
            document.add(metricsTable)
            
            // Add Performance Highlights
            document.add(Paragraph("\n"))
            val highlightsTitle = Paragraph("PERFORMANCE HIGHLIGHTS")
                .setFontSize(14f)
                .setBold()
            document.add(highlightsTitle)
            
            data.highlights.forEach { highlight ->
                document.add(Paragraph("• $highlight"))
            }
            
            // Add Financial Summary
            document.add(Paragraph("\n"))
            val financialTitle = Paragraph("FINANCIAL SUMMARY")
                .setFontSize(14f)
                .setBold()
            document.add(financialTitle)
            
            val financialTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
                .useAllAvailableWidth()
            
            financialTable.addCell(Paragraph("Quarterly Revenue"))
            financialTable.addCell(Paragraph(currencyFormat.format(data.quarterlyRevenue)))
            
            financialTable.addCell(Paragraph("Quarterly Profit"))
            financialTable.addCell(Paragraph(currencyFormat.format(data.quarterlyProfit)))
            
            financialTable.addCell(Paragraph("Profit Margin"))
            financialTable.addCell(Paragraph("${data.profitMargin}%"))
            
            document.add(financialTable)
            
            document.close()
            
            // Return URI for sharing
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun generateFinancialReport(
        context: Context,
        data: FinancialReportData,
        fileName: String = "Financial_Report"
    ): Uri? {
        return try {
            val file = File(context.getExternalFilesDir(null), "$fileName.pdf")
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Add title
            val title = Paragraph("FINANCIAL REPORT")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18f)
                .setBold()
            document.add(title)
            
            // Add date
            val date = Paragraph("Generated: ${dateFormat.format(Date())}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
            document.add(date)
            
            // Add spacing
            document.add(Paragraph("\n"))
            
            // Add Revenue Analysis
            val revenueTitle = Paragraph("REVENUE ANALYSIS")
                .setFontSize(14f)
                .setBold()
            document.add(revenueTitle)
            
            val revenueTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
                .useAllAvailableWidth()
            
            revenueTable.addCell(Paragraph("Total Revenue"))
            revenueTable.addCell(Paragraph(currencyFormat.format(data.totalRevenue)))
            
            revenueTable.addCell(Paragraph("Growth Rate"))
            revenueTable.addCell(Paragraph("${data.growthRate}%"))
            
            revenueTable.addCell(Paragraph("Revenue Sources"))
            revenueTable.addCell(Paragraph(data.revenueSources.joinToString(", ")))
            
            document.add(revenueTable)
            
            // Add Cost Analysis
            document.add(Paragraph("\n"))
            val costTitle = Paragraph("COST ANALYSIS")
                .setFontSize(14f)
                .setBold()
            document.add(costTitle)
            
            val costTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
                .useAllAvailableWidth()
            
            costTable.addCell(Paragraph("Total Costs"))
            costTable.addCell(Paragraph(currencyFormat.format(data.totalCosts)))
            
            costTable.addCell(Paragraph("Operating Costs"))
            costTable.addCell(Paragraph(currencyFormat.format(data.operatingCosts)))
            
            costTable.addCell(Paragraph("Cost Efficiency"))
            costTable.addCell(Paragraph("${data.costEfficiency}%"))
            
            document.add(costTable)
            
            // Add Cash Flow
            document.add(Paragraph("\n"))
            val cashFlowTitle = Paragraph("CASH FLOW ANALYSIS")
                .setFontSize(14f)
                .setBold()
            document.add(cashFlowTitle)
            
            val cashFlowTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
                .useAllAvailableWidth()
            
            cashFlowTable.addCell(Paragraph("Total Cash Flow"))
            cashFlowTable.addCell(Paragraph(currencyFormat.format(data.totalCashFlow)))
            
            cashFlowTable.addCell(Paragraph("Net Cash Flow"))
            cashFlowTable.addCell(Paragraph(currencyFormat.format(data.netCashFlow)))
            
            cashFlowTable.addCell(Paragraph("Cash Flow Margin"))
            cashFlowTable.addCell(Paragraph("${data.cashFlowMargin}%"))
            
            document.add(cashFlowTable)
            
            document.close()
            
            // Return URI for sharing
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun generateCustomReport(
        context: Context,
        data: CustomReportData,
        fileName: String = "Custom_Report"
    ): Uri? {
        return try {
            val file = File(context.getExternalFilesDir(null), "$fileName.pdf")
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Add title
            val title = Paragraph(data.title)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18f)
                .setBold()
            document.add(title)
            
            // Add date
            val date = Paragraph("Generated: ${dateFormat.format(Date())}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
            document.add(date)
            
            // Add spacing
            document.add(Paragraph("\n"))
            
            // Add description
            if (data.description.isNotEmpty()) {
                val desc = Paragraph(data.description)
                    .setFontSize(12f)
                document.add(desc)
                document.add(Paragraph("\n"))
            }
            
            // Add sections
            data.sections.forEach { section ->
                val sectionTitle = Paragraph(section.title)
                    .setFontSize(14f)
                    .setBold()
                document.add(sectionTitle)
                
                if (section.data.isNotEmpty()) {
                    val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
                        .useAllAvailableWidth()
                    
                    section.data.forEach { (key, value) ->
                        table.addCell(Paragraph(key))
                        table.addCell(Paragraph(value))
                    }
                    
                    document.add(table)
                }
                
                document.add(Paragraph("\n"))
            }
            
            document.close()
            
            // Return URI for sharing
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun shareReport(context: Context, uri: Uri, title: String = "Report") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Please find attached $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share $title"))
    }
    
    fun openReport(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        context.startActivity(intent)
    }
}

// Data classes for reports
data class ExecutiveReportData(
    val totalPortfolio: Double,
    val activeApplications: Int,
    val successRate: Int,
    val avgProcessingTime: Int,
    val highlights: List<String>,
    val quarterlyRevenue: Double,
    val quarterlyProfit: Double,
    val profitMargin: Int
)

data class FinancialReportData(
    val totalRevenue: Double,
    val growthRate: Int,
    val revenueSources: List<String>,
    val totalCosts: Double,
    val operatingCosts: Double,
    val costEfficiency: Int,
    val totalCashFlow: Double,
    val netCashFlow: Double,
    val cashFlowMargin: Int
)

data class CustomReportData(
    val title: String,
    val description: String,
    val sections: List<ReportSection>
)

data class ReportSection(
    val title: String,
    val data: Map<String, String>
)
