package com.kprflow.enterprise.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min

class BulkZipGenerator(
    private val context: Context
) {
    
    data class DocumentFile(
        val name: String,
        val url: String,
        val type: String,
        val folder: String,
        val uploadedAt: String? = null
    )
    
    data class ZipProgress(
        val currentFile: Int,
        val totalFiles: Int,
        val currentFileName: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val isComplete: Boolean = false,
        val error: String? = null
    )
    
    suspend fun generateBankSubmissionPack(
        dossierId: String,
        customerName: String,
        unitBlock: String,
        documents: List<DocumentFile>,
        onProgress: (ZipProgress) -> Unit = {}
    ): Result<Uri> = coroutineScope {
        try {
            withContext(Dispatchers.IO) {
                val progress = ZipProgress(0, documents.size, "", 0, 0)
                onProgress(progress)
                
                // Create temporary directory
                val tempDir = File(context.cacheDir, "zip_${System.currentTimeMillis()}")
                tempDir.mkdirs()
                
                try {
                    // Download all documents
                    val downloadedFiles = mutableListOf<File>()
                    var totalBytes = 0L
                    
                    for ((index, doc) in documents.withIndex()) {
                        val progress = ZipProgress(
                            currentFile = index,
                            totalFiles = documents.size,
                            currentFileName = doc.name,
                            bytesDownloaded = 0,
                            totalBytes = totalBytes
                        )
                        onProgress(progress)
                        
                        val downloadedFile = downloadDocument(doc, tempDir)
                        downloadedFiles.add(downloadedFile)
                        totalBytes += downloadedFile.length()
                    }
                    
                    // Create ZIP file
                    val zipFileName = "${customerName.replace(" ", "_")}_${unitBlock}_KPR.zip"
                    val zipFile = File(tempDir, zipFileName)
                    
                    createZipFile(downloadedFiles, zipFile, customerName, unitBlock) { current, total ->
                        val progress = ZipProgress(
                            currentFile = documents.size,
                            totalFiles = documents.size,
                            currentFileName = "Creating ZIP...",
                            bytesDownloaded = current,
                            totalBytes = total,
                            isComplete = current >= total
                        )
                        onProgress(progress)
                    }
                    
                    // Copy to external storage for sharing
                    val finalZipFile = File(context.getExternalFilesDir(null), zipFileName)
                    zipFile.copyTo(finalZipFile, overwrite = true)
                    
                    // Clean up
                    tempDir.deleteRecursively()
                    
                    Result.success(Uri.fromFile(finalZipFile))
                    
                } catch (e: Exception) {
                    tempDir.deleteRecursively()
                    throw e
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun downloadDocument(
        document: DocumentFile,
        tempDir: File
    ): File = withContext(Dispatchers.IO) {
        val fileName = sanitizeFileName(document.name)
        val file = File(tempDir, fileName)
        
        // In a real implementation, this would download from Supabase Storage
        // For now, we'll create a placeholder file
        file.writeText("Placeholder for ${document.name}\nURL: ${document.url}\nType: ${document.type}")
        
        file
    }
    
    private fun createZipFile(
        files: List<File>,
        zipFile: File,
        customerName: String,
        unitBlock: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ) {
        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // Create folder structure
                val folders = mapOf(
                    "01_Legal" to listOf("KTP", "KK", "AKTA_KELAHIRAN", "SURAT_NIKAH"),
                    "02_Income" to listOf("SLIP_GAJI", "REKENING_KORAN", "BUKU_TABUNGAN"),
                    "03_Property" to listOf("SERTIFIKAT", "IMB", "PBB"),
                    "04_Dossier" to listOf("DOSSIER_MERGED")
                )
                
                // Add summary file
                val summaryContent = createSummaryFile(customerName, unitBlock, files)
                addFileToZip(zos, "SUMMARY.txt", summaryContent.toByteArray())
                
                // Add files to appropriate folders
                var processedBytes = 0L
                val totalBytes = files.sumOf { it.length() }
                
                files.forEach { file ->
                    val folder = determineFolder(file.name, folders)
                    val entryPath = if (folder.isNotEmpty()) {
                        "$folder/${file.name}"
                    } else {
                        file.name
                    }
                    
                    addFileToZip(zos, entryPath, file.readBytes())
                    processedBytes += file.length()
                    onProgress(processedBytes, totalBytes)
                }
            }
        }
    }
    
    private fun addFileToZip(
        zipOutputStream: ZipOutputStream,
        entryPath: String,
        content: ByteArray
    ) {
        val entry = ZipEntry(entryPath)
        zipOutputStream.putNextEntry(entry)
        zipOutputStream.write(content)
        zipOutputStream.closeEntry()
    }
    
    private fun determineFolder(
        fileName: String,
        folders: Map<String, List<String>>
    ): String {
        val upperFileName = fileName.uppercase()
        
        folders.forEach { (folder, keywords) ->
            if (keywords.any { keyword -> upperFileName.contains(keyword) }) {
                return folder
            }
        }
        
        return ""
    }
    
    private fun createSummaryFile(
        customerName: String,
        unitBlock: String,
        files: List<File>
    ): String {
        val summary = StringBuilder()
        summary.appendLine("KPRFLOW ENTERPRISE - BANK SUBMISSION PACK")
        summary.appendLine("==========================================")
        summary.appendLine()
        summary.appendLine("Customer Name: $customerName")
        summary.appendLine("Unit Block: $unitBlock")
        summary.appendLine("Generated: ${Date()}")
        summary.appendLine("Total Files: ${files.size}")
        summary.appendLine()
        summary.appendLine("FILE LIST:")
        summary.appendLine("----------")
        
        files.forEach { file ->
            val sizeKB = file.length() / 1024
            summary.appendLine("${file.name} (${sizeKB}KB)")
        }
        
        summary.appendLine()
        summary.appendLine("FOLDER STRUCTURE:")
        summary.appendLine("------------------")
        summary.appendLine("01_Legal/ - Legal documents (KTP, KK, etc.)")
        summary.appendLine("02_Income/ - Income verification documents")
        summary.appendLine("03_Property/ - Property documents")
        summary.appendLine("04_Dossier/ - Merged dossier document")
        summary.appendLine()
        summary.appendLine("This ZIP package contains all required documents")
        summary.appendLine("for KPR bank submission.")
        summary.appendLine()
        summary.appendLine("Generated by KPRFlow Enterprise v2.0")
        
        return summary.toString()
    }
    
    private fun sanitizeFileName(fileName: String): String {
        // Remove or replace invalid characters
        return fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
    
    fun validateDocuments(documents: List<DocumentFile>): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Check for required documents
        val requiredTypes = setOf("KTP", "KK", "SLIP_GAJI")
        val presentTypes = documents.map { it.type.uppercase() }.toSet()
        val missingTypes = requiredTypes - presentTypes
        
        if (missingTypes.isNotEmpty()) {
            errors.add("Missing required documents: ${missingTypes.joinToString(", ")}")
        }
        
        // Check total size (limit 100MB)
        val estimatedTotalSize = documents.size * 2_000_000L // Assume 2MB per file
        if (estimatedTotalSize > 100_000_000L) {
            warnings.add("Estimated package size exceeds 100MB limit")
        }
        
        // Check for duplicate names
        val duplicateNames = documents
            .groupBy { it.name.uppercase() }
            .filter { it.value.size > 1 }
            .keys
        
        if (duplicateNames.isNotEmpty()) {
            warnings.add("Duplicate document names: ${duplicateNames.joinToString(", ")}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>
    )
}
