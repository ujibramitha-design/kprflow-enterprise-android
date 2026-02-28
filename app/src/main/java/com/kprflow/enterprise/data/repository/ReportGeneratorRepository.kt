package com.kprflow.enterprise.data.repository

import com.kprflow.enterprise.data.model.KprStatus
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportGeneratorRepository @Inject constructor(
    private val storage: Storage,
    private val postgrest: Postgrest
) {
    
    private val finalReportsBucket = "final_reports"
    
    suspend fun generateBASTReport(
        dossierId: String,
        handoverDate: String,
        handoverTime: String,
        handoverLocation: String,
        witnessName: String,
        witnessPosition: String,
        customerSignature: Boolean = false,
        developerSignature: Boolean = false
    ): Result<ReportGenerationResult> {
        return try {
            // Step 1: Get dossier and unit information
            val dossierData = getDossierWithUnit(dossierId)
                .getOrNull() ?: return Result.failure(Exception("Dossier not found"))
            
            // Step 2: Generate BAST PDF content
            val bastContent = generateBASTContent(
                dossier = dossierData.dossier,
                unit = dossierData.unit,
                customer = dossierData.customer,
                handoverDate = handoverDate,
                handoverTime = handoverTime,
                handoverLocation = handoverLocation,
                witnessName = witnessName,
                witnessPosition = witnessPosition
            )
            
            // Step 3: Create PDF file
            val bastFile = createPDFFile("BAST_${dossierId}_${System.currentTimeMillis()}.pdf", bastContent)
            
            // Step 4: Upload to storage
            val fileName = "BAST_${dossierId}_${System.currentTimeMillis()}.pdf"
            val filePath = "dossiers/$dossierId/$fileName"
            
            storage[finalReportsBucket].upload(filePath, bastFile.readBytes())
            val publicUrl = storage[finalReportsBucket].publicUrl(filePath)
            
            // Step 5: Save report record
            val reportRecord = FinalReportRecord(
                id = UUID.randomUUID().toString(),
                dossierId = dossierId,
                reportType = ReportType.BAST,
                fileName = fileName,
                filePath = filePath,
                publicUrl = publicUrl,
                handoverDate = handoverDate,
                handoverTime = handoverTime,
                handoverLocation = handoverLocation,
                witnessName = witnessName,
                witnessPosition = witnessPosition,
                customerSigned = customerSignature,
                developerSigned = developerSignature,
                generatedAt = java.time.Instant.now().toString()
            )
            
            saveReportRecord(reportRecord)
            
            // Step 6: Update dossier status
            updateDossierStatus(dossierId, KprStatus.BAST_COMPLETED)
            
            Result.success(
                ReportGenerationResult(
                    success = true,
                    reportId = reportRecord.id,
                    publicUrl = publicUrl,
                    reportType = ReportType.BAST
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateHandoverCertificate(
        dossierId: String,
        handoverDate: String,
        propertyCondition: String,
        includedItems: List<String>,
        excludedItems: List<String> = emptyList(),
        specialNotes: String? = null
    ): Result<ReportGenerationResult> {
        return try {
            // Step 1: Get dossier and unit information
            val dossierData = getDossierWithUnit(dossierId)
                .getOrNull() ?: return Result.failure(Exception("Dossier not found"))
            
            // Step 2: Generate Handover Certificate content
            val certificateContent = generateHandoverCertificateContent(
                dossier = dossierData.dossier,
                unit = dossierData.unit,
                customer = dossierData.customer,
                handoverDate = handoverDate,
                propertyCondition = propertyCondition,
                includedItems = includedItems,
                excludedItems = excludedItems,
                specialNotes = specialNotes
            )
            
            // Step 3: Create PDF file
            val certificateFile = createPDFFile("Handover_${dossierId}_${System.currentTimeMillis()}.pdf", certificateContent)
            
            // Step 4: Upload to storage
            val fileName = "Handover_${dossierId}_${System.currentTimeMillis()}.pdf"
            val filePath = "dossiers/$dossierId/$fileName"
            
            storage[finalReportsBucket].upload(filePath, certificateFile.readBytes())
            val publicUrl = storage[finalReportsBucket].publicUrl(filePath)
            
            // Step 5: Save report record
            val reportRecord = FinalReportRecord(
                id = UUID.randomUUID().toString(),
                dossierId = dossierId,
                reportType = ReportType.HANDOVER_CERTIFICATE,
                fileName = fileName,
                filePath = filePath,
                publicUrl = publicUrl,
                handoverDate = handoverDate,
                propertyCondition = propertyCondition,
                includedItems = includedItems,
                excludedItems = excludedItems,
                specialNotes = specialNotes,
                generatedAt = java.time.Instant.now().toString()
            )
            
            saveReportRecord(reportRecord)
            
            Result.success(
                ReportGenerationResult(
                    success = true,
                    reportId = reportRecord.id,
                    publicUrl = publicUrl,
                    reportType = ReportType.HANDOVER_CERTIFICATE
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFinalReports(dossierId: String): Result<List<FinalReportRecord>> {
        return try {
            val reports = postgrest.from("final_reports")
                .select()
                .filter { eq("dossier_id", dossierId) }
                .order("generated_at", ascending = false)
                .data
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getReportDownloadUrl(reportId: String): Result<String> {
        return try {
            val report = postgrest.from("final_reports")
                .select("public_url")
                .filter { eq("id", reportId) }
                .maybeSingle()
                .data ?: return Result.failure(Exception("Report not found"))
            
            Result.success(report.public_url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteReport(reportId: String): Result<Unit> {
        return try {
            // Get report record
            val report = postgrest.from("final_reports")
                .select()
                .filter { eq("id", reportId) }
                .maybeSingle()
                .data ?: return Result.failure(Exception("Report not found"))
            
            // Delete from storage
            storage[finalReportsBucket].delete(report.file_path)
            
            // Delete from database
            postgrest.from("final_reports")
                .delete()
                .filter { eq("id", reportId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateReportSignatures(
        reportId: String,
        customerSigned: Boolean,
        developerSigned: Boolean
    ): Result<FinalReportRecord> {
        return try {
            val updateData = mapOf(
                "customer_signed" to customerSigned,
                "developer_signed" to developerSigned,
                "updated_at" to java.time.Instant.now().toString()
            )
            
            val updatedReport = postgrest.from("final_reports")
                .update(updateData)
                .filter { eq("id", reportId) }
                .maybeSingle()
                .data
            
            updatedReport?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to update report"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeReportGeneration(dossierId: String): Flow<ReportGenerationStatus> = flow {
        try {
            val reports = getFinalReports(dossierId).getOrNull().orEmpty()
            val hasBAST = reports.any { it.reportType == ReportType.BAST }
            val hasCertificate = reports.any { it.reportType == ReportType.HANDOVER_CERTIFICATE }
            
            if (hasBAST && hasCertificate) {
                emit(ReportGenerationStatus.Complete)
            } else if (hasBAST || hasCertificate) {
                emit(ReportGenerationStatus.Partial)
            } else {
                emit(ReportGenerationStatus.NotStarted)
            }
        } catch (e: Exception) {
            emit(ReportGenerationStatus.Error(e.message ?: "Unknown error"))
        }
    }
    
    private suspend fun getDossierWithUnit(dossierId: String): Result<DossierWithUnitData> {
        return try {
            // TODO: Implement actual database query
            // For now, return mock data
            val mockData = DossierWithUnitData(
                dossier = mockDossier(),
                unit = mockUnit(),
                customer = mockCustomer()
            )
            Result.success(mockData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateBASTContent(
        dossier: com.kprflow.enterprise.data.model.KprDossier,
        unit: com.kprflow.enterprise.data.model.UnitProperty,
        customer: com.kprflow.enterprise.data.model.UserProfile,
        handoverDate: String,
        handoverTime: String,
        handoverLocation: String,
        witnessName: String,
        witnessPosition: String
    ): String {
        return """
            BERITA ACARA SERAH TERIMA (BAST)
            
            Nomor: BAST/${dossier.id}/${System.currentTimeMillis()}
            Tanggal: $handoverDate
            Waktu: $handoverTime
            Tempat: $handoverLocation
            
            PIHAK PERTAMA (PENGEMBANG)
            Nama: PT. KPRFlow Developer
            Alamat: Alamat Developer
            
            PIHAK KEDUA (PEMBELI)
            Nama: ${customer.name}
            Alamat: Alamat Pembeli
            No. KTP: ${customer.nik}
            
            OBJEK SERAH TERIMA
            Tipe Properti: ${unit.type}
            Blok: ${unit.block}
            Unit: ${unit.unitNumber}
            Harga: Rp ${String.format("%,.0f", unit.price)}
            
            KONDISI PROPERTI
            - Properti dalam kondisi baik dan siap ditempati
            - Semua fasilitas berfungsi dengan baik
            - Tidak ada kerusakan struktural
            
            BARANG YANG DISERAHKAN
            - Sertifikat kepemilikan
            - Kunci unit dan akses area
            - Manual buku panduan
            - Garansi peralatan
            
            PERNYATAAN
            Pihak Pertama menyatakan bahwa properti telah selesai dibangun sesuai spesifikasi
            dan siap untuk diserahkan kepada Pihak Kedua.
            
            Pihak Kedua menyatakan bahwa properti telah diterima dalam kondisi baik
            dan tidak ada keluhan mengenai kondisi properti.
            
            SAKSI-SAKSI
            Nama: $witnessName
            Jabatan: $witnessPosition
            
            Tanda Tangan:
            
            Pihak Pertama           Pihak Kedua           Saksi
            ___________           ___________           ___________
            
            Dibuat pada: $handoverDate
        """.trimIndent()
    }
    
    private fun generateHandoverCertificateContent(
        dossier: com.kprflow.enterprise.data.model.KprDossier,
        unit: com.kprflow.enterprise.data.model.UnitProperty,
        customer: com.kprflow.enterprise.data.model.UserProfile,
        handoverDate: String,
        propertyCondition: String,
        includedItems: List<String>,
        excludedItems: List<String>,
        specialNotes: String?
    ): String {
        return """
            SERTIFIKAT SERAH TERIMA PROPERTI
            
            Nomor Sertifikat: HC/${dossier.id}/${System.currentTimeMillis()}
            Tanggal Terbit: $handoverDate
            
            DATA PEMBELI
            Nama: ${customer.name}
            No. KTP: ${customer.nik}
            No. Telepon: ${customer.phoneNumber}
            
            DATA PROPERTI
            Tipe: ${unit.type}
            Blok: ${unit.block}
            Unit: ${unit.unitNumber}
            Luas: [Luas Bangunan] m²
            Harga: Rp ${String.format("%,.0f", unit.price)}
            
            KONDISI PROPERTI SAAT SERAH TERIMA
            $propertyCondition
            
            ITEM YANG DISEDIKAN:
            ${includedItems.joinToString("\n- ", "- ", "")}
            
            ${if (excludedItems.isNotEmpty()) """
            ITEM YANG TIDAK DISEDIKAN:
            ${excludedItems.joinToString("\n- ", "- ", "")}
            """ else ""}
            
            ${specialNotes?.let { """
            CATATAN KHUSUS:
            $it
            """ } ?: ""}
            
            PERNYATAAN
            Sertifikat ini menyatakan bahwa properti telah diserahkan kepada pembeli
            dalam kondisi baik dan lengkap sesuai perjanjian.
            
            Tanggal: $handoverDate
            
            Tanda Tangan Pengembang:
            ___________
            
            Nama Jelas:
            PT. KPRFlow Developer
        """.trimIndent()
    }
    
    private fun createPDFFile(fileName: String, content: String): File {
        // TODO: Implement actual PDF generation using a library like PDFBox or similar
        // For now, create a text file as placeholder
        
        val tempFile = File.createTempFile(fileName, ".txt")
        tempFile.writeText(content)
        return tempFile
    }
    
    private suspend fun saveReportRecord(record: FinalReportRecord): Result<Unit> {
        return try {
            postgrest.from("final_reports")
                .insert(record)
                .maybeSingle()
                .data
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun updateDossierStatus(dossierId: String, newStatus: KprStatus): Result<Unit> {
        return try {
            postgrest.from("kpr_dossiers")
                .update(
                    mapOf(
                        "status" to newStatus.name,
                        "updated_at" to java.time.Instant.now().toString()
                    )
                )
                .filter { eq("id", dossierId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Mock data for demonstration
    private fun mockDossier() = com.kprflow.enterprise.data.model.KprDossier(
        id = "mock-dossier-id",
        userId = "mock-user-id",
        unitId = "mock-unit-id",
        status = KprStatus.BAST_READY,
        bookingDate = java.time.LocalDate.now(),
        kprAmount = java.math.BigDecimal("500000000"),
        dpAmount = java.math.BigDecimal("100000000"),
        bankName = "Mock Bank",
        notes = "Mock notes",
        createdAt = java.time.Instant.now().toString(),
        updatedAt = java.time.Instant.now().toString()
    )
    
    private fun mockUnit() = com.kprflow.enterprise.data.model.UnitProperty(
        id = "mock-unit-id",
        block = "A",
        unitNumber = "001",
        type = "Type 36/72",
        price = java.math.BigDecimal("750000000"),
        status = com.kprflow.enterprise.data.model.UnitStatus.SOLD,
        description = "Mock unit description",
        createdAt = java.time.Instant.now().toString(),
        updatedAt = java.time.Instant.now().toString()
    )
    
    private fun mockCustomer() = com.kprflow.enterprise.data.model.UserProfile(
        id = "mock-user-id",
        name = "Mock Customer",
        email = "customer@example.com",
        nik = "1234567890123456",
        phoneNumber = "08123456789",
        maritalStatus = "Menikah",
        role = com.kprflow.enterprise.data.model.UserRole.CUSTOMER,
        createdAt = java.time.Instant.now().toString(),
        updatedAt = java.time.Instant.now().toString(),
        isActive = true
    )
}

enum class ReportType {
    BAST,
    HANDOVER_CERTIFICATE
}

sealed class ReportGenerationStatus {
    object NotStarted : ReportGenerationStatus()
    object Partial : ReportGenerationStatus()
    object Complete : ReportGenerationStatus()
    data class Error(val message: String) : ReportGenerationStatus()
}

data class ReportGenerationResult(
    val success: Boolean,
    val reportId: String,
    val publicUrl: String,
    val reportType: ReportType
)

data class FinalReportRecord(
    val id: String,
    val dossierId: String,
    val reportType: ReportType,
    val fileName: String,
    val filePath: String,
    val publicUrl: String,
    val handoverDate: String,
    val handoverTime: String? = null,
    val handoverLocation: String? = null,
    val witnessName: String? = null,
    val witnessPosition: String? = null,
    val propertyCondition: String? = null,
    val includedItems: List<String>? = null,
    val excludedItems: List<String>? = null,
    val specialNotes: String? = null,
    val customerSigned: Boolean = false,
    val developerSigned: Boolean = false,
    val generatedAt: String
)

data class DossierWithUnitData(
    val dossier: com.kprflow.enterprise.data.model.KprDossier,
    val unit: com.kprflow.enterprise.data.model.UnitProperty,
    val customer: com.kprflow.enterprise.data.model.UserProfile
)
