package com.kprflow.enterprise.legal.generator

import com.kprflow.enterprise.domain.model.*
import com.kprflow.enterprise.legal.model.*
import com.kprflow.enterprise.legal.template.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * PPJB Document Generator - Template-based Document Generation
 * Phase 16: Legal & Documentation Automation
 */
class PPJBDocumentGenerator(
    private val kprRepository: KprRepository,
    private val documentRepository: DocumentRepository,
    private val templateEngine: PPJBTemplateEngine
) {
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val dateTimeFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
    
    /**
     * Generate PPJB document
     */
    suspend fun generatePPJBDocument(
        process: PPJBDeveloperProcess
    ): Result<Document> = withContext(Dispatchers.IO) {
        
        try {
            // Get dossier details
            val dossier = kprRepository.getDossierById(process.dossierId)
                ?: return Result.failure(Exception("Dossier not found"))
            
            // Get customer details
            val customer = getCustomerDetails(process.customerId)
                ?: return Result.failure(Exception("Customer not found"))
            
            // Get unit details
            val unit = getUnitDetails(process.unitPropertyId)
                ?: return Result.failure(Exception("Unit not found"))
            
            // Generate document content based on type
            val content = when (process.ppjbType) {
                PPJBType.KPR -> generateKPRPPJBContent(dossier, customer, unit, process)
                PPJBType.CASH_KERAS -> generateCashKerasPPJBContent(dossier, customer, unit, process)
            }
            
            // Create document
            val document = Document(
                id = UUID.randomUUID().toString(),
                dossierId = process.dossierId,
                customerId = process.customerId,
                documentType = DocumentType.PPJB_DEVELOPER,
                documentName = "PPJB_${process.ppjbType.name}_${dossier.applicationNumber}",
                fileName = "PPJB_${process.ppjbType.name}_${dossier.applicationNumber}.pdf",
                fileUrl = content.fileUrl,
                fileSize = content.fileSize,
                mimeType = "application/pdf",
                status = DocumentStatus.GENERATED,
                verificationStatus = VerificationStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Save document
            documentRepository.createDocument(
                dossierId = process.dossierId,
                customerId = process.customerId,
                type = "PPJB_DEVELOPER",
                fileName = document.fileName,
                fileData = content.fileData
            )
            
            Result.success(document)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate invitation document
     */
    suspend fun generateInvitation(
        process: PPJBDeveloperProcess,
        invitationDate: Date
    ): Result<Document> = withContext(Dispatchers.IO) {
        
        try {
            // Get dossier details
            val dossier = kprRepository.getDossierById(process.dossierId)
                ?: return Result.failure(Exception("Dossier not found"))
            
            // Get customer details
            val customer = getCustomerDetails(process.customerId)
                ?: return Result.failure(Exception("Customer not found"))
            
            // Get unit details
            val unit = getUnitDetails(process.unitPropertyId)
                ?: return Result.failure(Exception("Unit not found"))
            
            // Generate invitation content
            val content = generateInvitationContent(dossier, customer, unit, process, invitationDate)
            
            // Create document
            val document = Document(
                id = UUID.randomUUID().toString(),
                dossierId = process.dossierId,
                customerId = process.customerId,
                documentType = DocumentType.UNDANGAN_PPJB,
                documentName = "Undangan_PPJB_${dossier.applicationNumber}",
                fileName = "Undangan_PPJB_${dossier.applicationNumber}.pdf",
                fileUrl = content.fileUrl,
                fileSize = content.fileSize,
                mimeType = "application/pdf",
                status = DocumentStatus.GENERATED,
                verificationStatus = VerificationStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Save document
            documentRepository.createDocument(
                dossierId = process.dossierId,
                customerId = process.customerId,
                type = "UNDANGAN_PPJB",
                fileName = document.fileName,
                fileData = content.fileData
            )
            
            Result.success(document)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate KPR PPJB content
     */
    private suspend fun generateKPRPPJBContent(
        dossier: KprDossier,
        customer: UserProfile,
        unit: UnitProperty,
        process: PPJBDeveloperProcess
    ): PPJBDocumentContent {
        
        val templateData = PPJBTemplateData(
            documentType = "PPJB KPR",
            documentNumber = generateDocumentNumber(),
            documentDate = dateFormat.format(Date()),
            developerName = "PT. KPRFlow Enterprise",
            developerAddress = "Jl. Developer No. 123, Jakarta",
            customerName = customer.fullName,
            customerNIK = customer.nik ?: "",
            customerAddress = customer.address ?: "",
            projectName = unit.projectName,
            unitBlock = unit.block,
            unitNumber = unit.unitNumber,
            unitType = unit.unitType,
            buildingSize = unit.buildingSize.toString(),
            landSize = unit.landSize.toString(),
            unitPrice = unit.price,
            loanAmount = dossier.loanAmount,
            loanTerm = dossier.loanTermMonths,
            interestRate = dossier.interestRate,
            monthlyIncome = dossier.monthlyIncome,
            ppjbDate = dateFormat.format(process.scheduledDate),
            expiryDate = dateFormat.format(process.expiryDate),
            applicationNumber = dossier.applicationNumber
        )
        
        val content = templateEngine.renderPPJBTemplate(templateData)
        
        return PPJBDocumentContent(
            fileData = content.toByteArray(),
            fileSize = content.length.toLong(),
            fileUrl = "generated://ppjb_${process.id}.pdf"
        )
    }
    
    /**
     * Generate Cash Keras PPJB content
     */
    private suspend fun generateCashKerasPPJBContent(
        dossier: KprDossier,
        customer: UserProfile,
        unit: UnitProperty,
        process: PPJBDeveloperProcess
    ): PPJBDocumentContent {
        
        val templateData = PPJBTemplateData(
            documentType = "PPJB CASH KERAS",
            documentNumber = generateDocumentNumber(),
            documentDate = dateFormat.format(Date()),
            developerName = "PT. KPRFlow Enterprise",
            developerAddress = "Jl. Developer No. 123, Jakarta",
            customerName = customer.fullName,
            customerNIK = customer.nik ?: "",
            customerAddress = customer.address ?: "",
            projectName = unit.projectName,
            unitBlock = unit.block,
            unitNumber = unit.unitNumber,
            unitType = unit.unitType,
            buildingSize = unit.buildingSize.toString(),
            landSize = unit.landSize.toString(),
            unitPrice = unit.price,
            loanAmount = 0.0,
            loanTerm = 0,
            interestRate = 0.0,
            monthlyIncome = dossier.monthlyIncome,
            ppjbDate = dateFormat.format(process.scheduledDate),
            expiryDate = dateFormat.format(process.expiryDate),
            applicationNumber = dossier.applicationNumber
        )
        
        val content = templateEngine.renderPPJBTemplate(templateData)
        
        return PPJBDocumentContent(
            fileData = content.toByteArray(),
            fileSize = content.length.toLong(),
            fileUrl = "generated://ppjb_${process.id}.pdf"
        )
    }
    
    /**
     * Generate invitation content
     */
    private suspend fun generateInvitationContent(
        dossier: KprDossier,
        customer: UserProfile,
        unit: UnitProperty,
        process: PPJBDeveloperProcess,
        invitationDate: Date
    ): PPJBDocumentContent {
        
        val templateData = InvitationTemplateData(
            customerName = customer.fullName,
            customerAddress = customer.address ?: "",
            invitationDate = dateFormat.format(invitationDate),
            invitationTime = "10:00 WIB",
            venue = "Kantor Marketing PT. KPRFlow Enterprise",
            venueAddress = "Jl. Developer No. 123, Jakarta",
            applicationNumber = dossier.applicationNumber,
            unitBlock = unit.block,
            unitNumber = unit.unitNumber,
            ppjbType = process.ppjbType.name,
            contactPerson = "Marketing Team",
            contactPhone = "0812-3456-7890",
            contactEmail = "marketing@kprflow.com",
            documentDate = dateFormat.format(Date())
        )
        
        val content = templateEngine.renderInvitationTemplate(templateData)
        
        return PPJBDocumentContent(
            fileData = content.toByteArray(),
            fileSize = content.length.toLong(),
            fileUrl = "generated://invitation_${process.id}.pdf"
        )
    }
    
    // Private helper methods
    private suspend fun getCustomerDetails(customerId: String): UserProfile? {
        // Implementation depends on your user repository
        return null // Dummy implementation
    }
    
    private suspend fun getUnitDetails(unitId: String): UnitProperty? {
        // Implementation depends on your unit repository
        return null // Dummy implementation
    }
    
    private fun generateDocumentNumber(): String {
        return "PPJB-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
}

/**
 * PPJB Document Content
 */
data class PPJBDocumentContent(
    val fileData: ByteArray,
    val fileSize: Long,
    val fileUrl: String
)

/**
 * PPJB Template Data
 */
data class PPJBTemplateData(
    val documentType: String,
    val documentNumber: String,
    val documentDate: String,
    val developerName: String,
    val developerAddress: String,
    val customerName: String,
    val customerNIK: String,
    val customerAddress: String,
    val projectName: String,
    val unitBlock: String,
    val unitNumber: String,
    val unitType: String,
    val buildingSize: String,
    val landSize: String,
    val unitPrice: Double,
    val loanAmount: Double,
    val loanTerm: Int,
    val interestRate: Double,
    val monthlyIncome: Double,
    val ppjbDate: String,
    val expiryDate: String,
    val applicationNumber: String
)

/**
 * Invitation Template Data
 */
data class InvitationTemplateData(
    val customerName: String,
    val customerAddress: String,
    val invitationDate: String,
    val invitationTime: String,
    val venue: String,
    val venueAddress: String,
    val applicationNumber: String,
    val unitBlock: String,
    val unitNumber: String,
    val ppjbType: String,
    val contactPerson: String,
    val contactPhone: String,
    val contactEmail: String,
    val documentDate: String
)
