package com.kprflow.enterprise.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object DocumentMerger {
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    
    suspend fun mergeDocuments(
        context: Context,
        dossierId: String,
        customerName: String,
        documentPaths: List<String>
    ): File? = withContext(Dispatchers.IO) {
        try {
            // Initialize PDFBox
            PDFBoxResourceLoader.init(context)
            
            // Create output file
            val outputFile = createOutputFile(context, dossierId, customerName)
            val mergedDoc = PDDocument()
            
            // Document order: KTP -> KK -> NPWP -> Slip Gaji -> Other
            val sortedPaths = sortDocumentsByType(documentPaths)
            
            var pageCount = 0
            
            for (path in sortedPaths) {
                try {
                    val file = File(path)
                    if (!file.exists()) continue
                    
                    when {
                        path.lowercase().endsWith(".pdf") -> {
                            // Process PDF
                            val pdfDoc = PDDocument.load(FileInputStream(file))
                            val renderer = PDFRenderer(pdfDoc)
                            
                            for (pageIndex in 0 until pdfDoc.numberOfPages) {
                                val page = pdfDoc.getPage(pageIndex)
                                val newPage = PDPage(PDRectangle.A4)
                                mergedDoc.addPage(newPage)
                                
                                val contentStream = PDPageContentStream(mergedDoc, newPage, PDPageContentStream.AppendMode.APPEND, true, true)
                                
                                // Convert PDF page to image and embed
                                val bitmap = renderer.renderImageWithDPI(pageIndex, 150f)
                                val tempFile = File(context.cacheDir, "temp_page_$pageCount.png")
                                tempFile.createNewFile()
                                FileOutputStream(tempFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                
                                val pdImage = PDImageXObject.createFromByteArray(
                                    mergedDoc,
                                    tempFile.readBytes(),
                                    tempFile.name
                                )
                                
                                // Calculate scaling to fit A4
                                val a4Width = PDRectangle.A4.width
                                val a4Height = PDRectangle.A4.height
                                val imageWidth = pdImage.width.toFloat()
                                val imageHeight = pdImage.height.toFloat()
                                
                                val scale = minOf(a4Width / imageWidth, a4Height / imageHeight) * 0.9f
                                val scaledWidth = imageWidth * scale
                                val scaledHeight = imageHeight * scale
                                val x = (a4Width - scaledWidth) / 2
                                val y = (a4Height - scaledHeight) / 2
                                
                                contentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight)
                                contentStream.close()
                                
                                tempFile.delete()
                                pageCount++
                            }
                            
                            pdfDoc.close()
                        }
                        
                        path.lowercase().endsWith(".jpg") || path.lowercase().endsWith(".jpeg") -> {
                            // Process JPEG
                            addImageToDocument(mergedDoc, file, customerName, pageCount++)
                        }
                        
                        path.lowercase().endsWith(".png") -> {
                            // Process PNG
                            addImageToDocument(mergedDoc, file, customerName, pageCount++)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue with next document
                }
            }
            
            // Add metadata and watermark
            addMetadataAndWatermark(mergedDoc, customerName, dossierId)
            
            // Save merged document
            mergedDoc.save(outputFile)
            mergedDoc.close()
            
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private suspend fun addImageToDocument(
        doc: PDDocument,
        imageFile: File,
        customerName: String,
        pageNumber: Int
    ) = withContext(Dispatchers.IO) {
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        
        val contentStream = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
        
        try {
            // Load and embed image
            val pdImage = PDImageXObject.createFromByteArray(doc, imageFile.readBytes(), imageFile.name)
            
            // Calculate scaling to fit A4 with margins
            val a4Width = PDRectangle.A4.width
            val a4Height = PDRectangle.A4.height
            val imageWidth = pdImage.width.toFloat()
            val imageHeight = pdImage.height.toFloat()
            
            val margin = 50f
            val maxWidth = a4Width - (2 * margin)
            val maxHeight = a4Height - (2 * margin)
            
            val scale = minOf(maxWidth / imageWidth, maxHeight / imageHeight)
            val scaledWidth = imageWidth * scale
            val scaledHeight = imageHeight * scale
            val x = (a4Width - scaledWidth) / 2
            val y = (a4Height - scaledHeight) / 2
            
            contentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight)
            
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            contentStream.close()
        }
    }
    
    private fun addMetadataAndWatermark(doc: PDDocument, customerName: String, dossierId: String) {
        try {
            // Add metadata
            val info = doc.documentInformation
            info.title = "Berkas KPR - $customerName"
            info.subject = "Dossier ID: $dossierId"
            info.creator = "KPRFlow Enterprise"
            info.producer = "KPRFlow Document Merger"
            info.creationDate = Calendar.getInstance()
            
            // Add watermark to each page
            for (pageIndex in 0 until doc.numberOfPages) {
                val page = doc.getPage(pageIndex)
                val contentStream = PDPageContentStream(
                    doc, page, 
                    PDPageContentStream.AppendMode.APPEND, 
                    true, true
                )
                
                try {
                    // Add watermark text
                    contentStream.beginText()
                    contentStream.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 48f)
                    contentStream.setNonStrokingColor(200, 200, 200) // Light gray
                    contentStream.newLineAtOffset(100f, page.mediaBox.height - 100f)
                    
                    val watermarkText = "BERKAS KPR - $customerName"
                    contentStream.showText(watermarkText)
                    contentStream.endText()
                    
                    // Add page number
                    contentStream.beginText()
                    contentStream.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12f)
                    contentStream.setNonStrokingColor(100, 100, 100)
                    contentStream.newLineAtOffset(
                        page.mediaBox.width - 100f, 
                        50f
                    )
                    contentStream.showText("Halaman ${pageIndex + 1} dari ${doc.numberOfPages}")
                    contentStream.endText()
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    contentStream.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun sortDocumentsByType(paths: List<String>): List<String> {
        val priorityOrder = mapOf(
            "ktp" to 1,
            "kk" to 2,
            "npwp" to 3,
            "slip gaji" to 4,
            "payslip" to 4,
            "paystub" to 4,
            "bank statement" to 5,
            "rekening koran" to 5,
            "surat kerja" to 6,
            "work certificate" to 6
        )
        
        return paths.sortedBy { path ->
            val fileName = path.lowercase()
            priorityOrder.entries.find { (key, _) -> fileName.contains(key) }?.value ?: 999
        }
    }
    
    private fun createOutputFile(context: Context, dossierId: String, customerName: String): File {
        val fileName = "Berkas_KPR_${customerName.replace(" ", "_")}_$dossierId.pdf"
        val downloadsDir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "KPRFlow/Merged")
        } else {
            File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "KPRFlow/Merged")
        }
        
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        
        return File(downloadsDir, fileName)
    }
    
    fun validateDocuments(documentPaths: List<String>): DocumentValidationResult {
        val validPaths = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        for (path in documentPaths) {
            val file = File(path)
            when {
                !file.exists() -> errors.add("File tidak ditemukan: $path")
                !file.canRead() -> errors.add("File tidak dapat dibaca: $path")
                file.length() > 10 * 1024 * 1024 -> errors.add("File terlalu besar (>10MB): $path")
                !isValidFileType(path) -> errors.add("Format file tidak didukung: $path")
                else -> validPaths.add(path)
            }
        }
        
        return DocumentValidationResult(
            isValid = errors.isEmpty(),
            validPaths = validPaths,
            errors = errors
        )
    }
    
    private fun isValidFileType(path: String): Boolean {
        val supportedExtensions = listOf("pdf", "jpg", "jpeg", "png")
        val extension = path.substringAfterLast('.', "").lowercase()
        return supportedExtensions.contains(extension)
    }
    
    data class DocumentValidationResult(
        val isValid: Boolean,
        val validPaths: List<String>,
        val errors: List<String>
    )
}
