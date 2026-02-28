package com.kprflow.enterprise.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.kprflow.enterprise.data.model.KprApplication
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    fun exportKprToExcel(context: Context, data: List<KprApplication>, fileName: String = "Laporan_KPR"): Uri? {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Data KPR")
            
            // Create styles
            val headerStyle = createHeaderStyle(workbook)
            val currencyStyle = createCurrencyStyle(workbook)
            
            // Create header row
            val headerRow = sheet.createRow(0)
            val headers = arrayOf("No", "Nama Konsumen", "Blok/Unit", "Status KPR", "Harga Unit", "Tanggal Pengajuan")
            
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }
            
            // Fill data rows
            data.forEachIndexed { index, kpr ->
                val row = sheet.createRow(index + 1)
                
                row.createCell(0).setCellValue((index + 1).toString())
                row.createCell(1).setCellValue(kpr.customerName ?: "-")
                row.createCell(2).setCellValue(kpr.unitBlock ?: "-")
                row.createCell(3).setCellValue(kpr.status ?: "-")
                
                val priceCell = row.createCell(4)
                priceCell.setCellValue(kpr.unitPrice?.toDouble() ?: 0.0)
                priceCell.cellStyle = currencyStyle
                
                row.createCell(5).setCellValue(dateFormat.format(kpr.createdAt ?: Date()))
            }
            
            // Auto-size columns
            for (i in headers.indices) {
                sheet.autoSizeColumn(i)
            }
            
            // Save file
            val fileNameWithExt = "$fileName.xlsx"
            val file = createFile(context, fileNameWithExt)
            
            FileOutputStream(file).use { out ->
                workbook.write(out)
            }
            workbook.close()
            
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
    
    fun exportKprToPdf(context: Context, data: List<KprApplication>, fileName: String = "Laporan_KPR"): Uri? {
        return try {
            // For PDF export, we'll create a simple text-based report
            // In production, consider using a proper PDF library like iText or PdfDocument
            val fileNameWithExt = "$fileName.txt"
            val file = createFile(context, fileNameWithExt)
            
            val content = generatePdfReport(data)
            
            file.writeText(content)
            
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
    
    fun openFile(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
    
    fun shareFile(context: Context, uri: Uri, fileName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when {
                fileName.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                fileName.endsWith(".pdf") -> "application/pdf"
                fileName.endsWith(".txt") -> "text/plain"
                else -> "application/octet-stream"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan KPRFlow")
            putExtra(Intent.EXTRA_TEXT, "Berikut lampiran laporan KPRFlow")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan"))
    }
    
    private fun createHeaderStyle(workbook: XSSFWorkbook): CellStyle {
        val font = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 12
        }
        
        return workbook.createCellStyle().apply {
            setFont(font)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.index
            fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
        }
    }
    
    private fun createCurrencyStyle(workbook: XSSFWorkbook): CellStyle {
        return workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.RIGHT
            dataFormat = workbook.createDataFormat().format("_(* #,##0_);_(* \\(#,##0\\);_(* \"-\"_);_(@_)")
        }
    }
    
    private fun createFile(context: Context, fileName: String): File {
        val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "KPRFlow")
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KPRFlow")
        }
        
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        
        return File(downloadsDir, fileName)
    }
    
    private fun generatePdfReport(data: List<KprApplication>): String {
        val report = StringBuilder()
        report.appendLine("LAPORAN KPRFLOW ENTERPRISE")
        report.appendLine("=" .repeat(50))
        report.appendLine("Tanggal Cetak: ${dateFormat.format(Date())}")
        report.appendLine("Total Data: ${data.size}")
        report.appendLine()
        
        report.appendLine("-".repeat(100))
        report.appendLine(String.format("%-5s %-25s %-15s %-20s %-20s %-15s", "No", "Nama", "Blok/Unit", "Status", "Harga", "Tanggal"))
        report.appendLine("-".repeat(100))
        
        data.forEachIndexed { index, kpr ->
            report.appendLine(
                String.format(
                    "%-5d %-25s %-15s %-20s %-20s %-15s",
                    index + 1,
                    kpr.customerName?.take(24) ?: "-",
                    kpr.unitBlock?.take(14) ?: "-",
                    kpr.status?.take(19) ?: "-",
                    currencyFormat.format(kpr.unitPrice ?: 0),
                    dateFormat.format(kpr.createdAt ?: Date())
                )
            )
        }
        
        report.appendLine("-".repeat(100))
        report.appendLine()
        report.appendLine("CONFIDENTIAL - KPRFlow Enterprise")
        
        return report.toString()
    }
}
