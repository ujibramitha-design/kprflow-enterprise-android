package com.kprflow.enterprise.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.layout.*
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.*
import com.kprflow.enterprise.data.model.DocumentItem
import com.kprflow.enterprise.data.model.MergeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Advanced Document Merger with error handling, batch processing, and quality assurance
 */
class AdvancedDocumentMerger(private val context: Context) {
    
    private val maxFileSize = 100 * 1024 * 1024 // 100MB
    private val maxRetries = 3
    private val supportedFormats = listOf("pdf", "jpg", "jpeg", "png")
    
    /**
     * Merge documents with advanced error handling and batch processing
     */
    suspend fun mergeDocumentsWithQualityAssurance(
        documents: List<DocumentItem>,
        outputPath: String,
        options: MergeOptions = MergeOptions()
    ): Result<MergeResult> = withContext(Dispatchers.IO) {
        
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            // Pre-validation
            val validationResult = validateDocuments(documents)
            if (validationResult.isFailure) {
                return@withContext Result.failure(validationResult.exceptionOrNull()!!)
            }
            
            warnings.addAll(validationResult.getOrNull() ?: emptyList())
            
            // Create output directory if needed
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            // Process documents in batches
            val processingTime = measureTimeMillis {
                val batchResult = processBatchDocuments(documents, outputFile, options)
                errors.addAll(batchResult.errors)
                warnings.addAll(batchResult.warnings)
                
                if (batchResult.isFailure) {
                    return@withContext Result.failure(batchResult.exceptionOrNull()!!)
                }
            }
            
            // Post-processing validation
            val postValidationResult = validateOutputFile(outputFile)
            if (postValidationResult.isFailure) {
                errors.add("Output validation failed: ${postValidationResult.exceptionOrNull()?.message}")
            }
            
            val result = MergeResult(
                success = errors.isEmpty(),
                outputPath = outputPath,
                processingTimeMs = processingTime,
                documentsProcessed = documents.size,
                errors = errors,
                warnings = warnings,
                fileSizeBytes = outputFile.length(),
                qualityScore = calculateQualityScore(errors, warnings, documents.size)
            )
            
            if (errors.isEmpty()) {
                Result.success(result)
            } else {
                Result.failure(Exception("Merge completed with errors: ${errors.joinToString("; ")}"))
            }
            
        } catch (e: Exception) {
            Result.failure(Exception("Document merge failed: ${e.message}", e))
        }
    }
    
    /**
     * Validate input documents
     */
    private suspend fun validateDocuments(documents: List<DocumentItem>): Result<List<String>> {
        val warnings = mutableListOf<String>()
        
        // Check document count
        if (documents.isEmpty()) {
            return Result.failure(Exception("No documents to merge"))
        }
        
        // Check file sizes
        val totalSize = documents.sumOf { it.fileSizeBytes }
        if (totalSize > maxFileSize) {
            return Result.failure(Exception("Total file size exceeds 100MB limit"))
        }
        
        // Check file formats
        documents.forEach { doc ->
            val extension = File(doc.filePath).extension.lowercase()
            if (!supportedFormats.contains(extension)) {
                return Result.failure(Exception("Unsupported file format: $extension"))
            }
            
            // Check individual file size
            if (doc.fileSizeBytes > 20 * 1024 * 1024) { // 20MB per file
                warnings.add("Large file detected: ${doc.fileName} (${doc.fileSizeBytes / 1024 / 1024}MB)")
            }
        }
        
        // Check for duplicate names
        val duplicateNames = documents.groupBy { it.fileName }
            .filter { it.value.size > 1 }
            .keys
        
        if (duplicateNames.isNotEmpty()) {
            warnings.add("Duplicate file names detected: ${duplicateNames.joinToString(", ")}")
        }
        
        return Result.success(warnings)
    }
    
    /**
     * Process documents in batches for better performance
     */
    private suspend fun processBatchDocuments(
        documents: List<DocumentItem>,
        outputFile: File,
        options: MergeOptions
    ): Result<BatchResult> = withContext(Dispatchers.IO) {
        
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val batchSize = 5 // Process 5 documents at a time
        
        try {
            val pdfDoc = PdfDocument(Writer(FileOutputStream(outputFile)))
            val document = Document(pdfDoc)
            
            // Add title page if enabled
            if (options.includeTitlePage) {
                addTitlePage(document, documents, options)
            }
            
            // Process documents in batches
            documents.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                val batchResult = processDocumentBatch(batch, document, options)
                errors.addAll(batchResult.errors)
                warnings.addAll(batchResult.warnings)
                
                // Add page break between batches (except last)
                if (batchIndex < documents.size / batchSize) {
                    document.add(AreaBreak())
                }
            }
            
            // Add table of contents if enabled
            if (options.includeTableOfContents) {
                addTableOfContents(document, documents, options)
            }
            
            // Add quality assurance page
            if (options.includeQualityPage) {
                addQualityAssurancePage(document, documents, errors, warnings)
            }
            
            document.close()
            
            Result.success(BatchResult(errors, warnings))
            
        } catch (e: Exception) {
            Result.failure(Exception("Batch processing failed: ${e.message}", e))
        }
    }
    
    /**
     * Process a single batch of documents
     */
    private suspend fun processDocumentBatch(
        batch: List<DocumentItem>,
        document: Document,
        options: MergeOptions
    ): BatchResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        batch.forEach { doc ->
            try {
                when (File(doc.filePath).extension.lowercase()) {
                    "pdf" -> processPDFDocument(doc, document, options)
                    "jpg", "jpeg", "png" -> processImageDocument(doc, document, options)
                    else -> errors.add("Unsupported format: ${doc.fileName}")
                }
            } catch (e: Exception) {
                errors.add("Failed to process ${doc.fileName}: ${e.message}")
            }
        }
        
        return BatchResult(errors, warnings)
    }
    
    /**
     * Process PDF document with error handling
     */
    private suspend fun processPDFDocument(
        doc: DocumentItem,
        document: Document,
        options: MergeOptions
    ) {
        val file = File(doc.filePath)
        
        // Check if file is corrupted
        if (!isValidPDF(file)) {
            throw Exception("PDF file is corrupted: ${doc.fileName}")
        }
        
        val pdfReader = PdfReader(FileInputStream(file))
        val sourcePdf = PdfDocument(pdfReader)
        
        // Copy pages with retry logic
        repeat(sourcePdf.numberOfPages) { pageIndex ->
            var retries = 0
            var success = false
            
            while (retries < maxRetries && !success) {
                try {
                    val sourcePage = sourcePdf.getPage(pageIndex + 1)
                    val newPage = document.pdfDocument.addNewPage(sourcePage.pageSize)
                    
                    val canvas = PdfCanvas(newPage)
                    sourcePage.copyAsFormXObject(document.pdfDocument).apply {
                        canvas.addXObject(this)
                    }
                    canvas.release()
                    
                    success = true
                } catch (e: Exception) {
                    retries++
                    if (retries >= maxRetries) {
                        throw Exception("Failed to copy page ${pageIndex + 1} after $maxRetries attempts")
                    }
                    kotlinx.coroutines.delay(100) // Wait before retry
                }
            }
        }
        
        sourcePdf.close()
        pdfReader.close()
    }
    
    /**
     * Process image document with error handling
     */
    private suspend fun processImageDocument(
        doc: DocumentItem,
        document: Document,
        options: MergeOptions
    ) {
        val file = File(doc.filePath)
        
        // Check if image is valid
        if (!isValidImage(file)) {
            throw Exception("Image file is corrupted: ${doc.fileName}")
        }
        
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw Exception("Failed to decode image: ${doc.fileName}")
        
        // Add image to document with proper scaling
        val image = Image(com.itextpdf.io.image.ImageDataFactory.create(bitmap, null))
            .setWidth(UnitValue.createPercentValue(100f))
            .setAutoScaleWidth(true)
        
        // Add caption if enabled
        if (options.includeImageCaptions) {
            document.add(Paragraph(doc.fileName)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5f))
        }
        
        document.add(image)
        
        // Clean up bitmap
        bitmap.recycle()
    }
    
    /**
     * Add title page
     */
    private fun addTitlePage(
        document: Document,
        documents: List<DocumentItem>,
        options: MergeOptions
    ) {
        document.add(Paragraph("Document Merge Report")
            .setFontSize(24)
            .setTextAlignment(TextAlignment.CENTER)
            .setBold()
            .setMarginBottom(20f))
        
        document.add(Paragraph("Generated on ${Date()}")
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(30f))
        
        // Summary table
        val table = Table(2).useAllAvailableWidth()
        table.addHeaderCell(Paragraph("Total Documents").setBold())
        table.addHeaderCell(Paragraph(documents.size.toString()))
        table.addHeaderCell(Paragraph("Total Size").setBold())
        table.addHeaderCell(Paragraph("${documents.sumOf { it.fileSizeBytes } / 1024 / 1024}MB"))
        
        document.add(table.setMarginBottom(30f))
        document.add(AreaBreak())
    }
    
    /**
     * Add table of contents
     */
    private fun addTableOfContents(
        document: Document,
        documents: List<DocumentItem>,
        options: MergeOptions
    ) {
        document.add(Paragraph("Table of Contents")
            .setFontSize(18)
            .setBold()
            .setMarginTop(30f)
            .setMarginBottom(15f))
        
        documents.forEachIndexed { index, doc ->
            document.add(Paragraph("${index + 1}. ${doc.fileName}")
                .setFontSize(12)
                .setMarginLeft(20f))
        }
        
        document.add(AreaBreak())
    }
    
    /**
     * Add quality assurance page
     */
    private fun addQualityAssurancePage(
        document: Document,
        documents: List<DocumentItem>,
        errors: List<String>,
        warnings: List<String>
    ) {
        document.add(Paragraph("Quality Assurance Report")
            .setFontSize(18)
            .setBold()
            .setMarginTop(30f)
            .setMarginBottom(15f))
        
        // Processing summary
        document.add(Paragraph("Processing Summary")
            .setFontSize(14)
            .setBold()
            .setMarginBottom(10f))
        
        val summaryTable = Table(2).useAllAvailableWidth()
        summaryTable.addCell(Paragraph("Documents Processed").setBold())
        summaryTable.addCell(Paragraph(documents.size.toString()))
        summaryTable.addCell(Paragraph("Errors").setBold())
        summaryTable.addCell(Paragraph(errors.size.toString()))
        summaryTable.addCell(Paragraph("Warnings").setBold())
        summaryTable.addCell(Paragraph(warnings.size.toString()))
        
        document.add(summaryTable.setMarginBottom(20f))
        
        // Errors section
        if (errors.isNotEmpty()) {
            document.add(Paragraph("Errors")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10f))
            
            errors.forEach { error ->
                document.add(Paragraph("• $error")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginLeft(20f))
            }
        }
        
        // Warnings section
        if (warnings.isNotEmpty()) {
            document.add(Paragraph("Warnings")
                .setFontSize(14)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(10f))
            
            warnings.forEach { warning ->
                document.add(Paragraph("• $warning")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginLeft(20f))
            }
        }
    }
    
    /**
     * Validate PDF file
     */
    private fun isValidPDF(file: File): Boolean {
        return try {
            val pdfReader = PdfReader(FileInputStream(file))
            val pdfDoc = PdfDocument(pdfReader)
            val isValid = pdfDoc.numberOfPages > 0
            pdfDoc.close()
            pdfReader.close()
            isValid
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validate image file
     */
    private fun isValidImage(file: File): Boolean {
        return try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validate output file
     */
    private fun validateOutputFile(file: File): Result<Boolean> {
        return try {
            if (!file.exists()) {
                return Result.failure(Exception("Output file does not exist"))
            }
            
            if (file.length() == 0L) {
                return Result.failure(Exception("Output file is empty"))
            }
            
            // Quick PDF validation
            val pdfReader = PdfReader(FileInputStream(file))
            val pdfDoc = PdfDocument(pdfReader)
            val isValid = pdfDoc.numberOfPages > 0
            pdfDoc.close()
            pdfReader.close()
            
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate quality score
     */
    private fun calculateQualityScore(
        errors: List<String>,
        warnings: List<String>,
        documentCount: Int
    ): Double {
        val baseScore = 100.0
        val errorPenalty = errors.size * 10.0
        val warningPenalty = warnings.size * 2.0
        val documentBonus = documentCount * 0.5
        
        return maxOf(0.0, baseScore - errorPenalty - warningPenalty + documentBonus)
    }
}

/**
 * Merge options
 */
data class MergeOptions(
    val includeTitlePage: Boolean = true,
    val includeTableOfContents: Boolean = true,
    val includeQualityPage: Boolean = true,
    val includeImageCaptions: Boolean = true,
    val optimizeForSize: Boolean = true
)

/**
 * Batch processing result
 */
data class BatchResult(
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Document item model
 */
data class DocumentItem(
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val documentType: String,
    val lastModified: Long
)
