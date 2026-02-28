package com.kprflow.enterprise.domain.usecase

import com.kprflow.enterprise.domain.model.*
import com.kprflow.enterprise.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CoroutineDispatcher
import java.math.BigDecimal
import java.util.*

/**
 * Advanced Use Cases for complete Phase 4 implementation
 * Phase 4: Domain Layer (100% Complete)
 */

// =====================================================
// AUTH USE CASES
// =====================================================

class SignInUseCase(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(email: String, password: String): Result<UserProfile> {
        return authRepository.signIn(email, password)
    }
}

class SignUpUseCase(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        fullName: String,
        phone: String?
    ): Result<UserProfile> {
        return authRepository.signUp(email, password, fullName, phone)
    }
}

class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.signOut()
    }
}

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): UserProfile? {
        return authRepository.getCurrentUser()
    }
}

class UpdateProfileUseCase(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(profile: UserProfile): Result<UserProfile> {
        return authRepository.updateProfile(profile)
    }
}

class RefreshTokenUseCase(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): Result<String> {
        return authRepository.refreshToken()
    }
}

class ChangePasswordUseCase(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit> {
        return authRepository.changePassword(currentPassword, newPassword)
    }
}

class ResetPasswordUseCase(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return authRepository.resetPassword(email)
    }
}

// =====================================================
// KPR DOSSIER USE CASES
// =====================================================

class GetDossiersUseCase(
    private val kprRepository: KprRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        customerId: String? = null,
        status: String? = null,
        forceRefresh: Boolean = false
    ): Result<List<KprDossier>> {
        return kprRepository.getDossiers(customerId, status, forceRefresh)
    }
}

class GetDossierByIdUseCase(
    private val kprRepository: KprRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(id: String): Result<KprDossier?> {
        return kprRepository.getDossierById(id)
    }
}

class CreateDossierUseCase(
    private val kprRepository: KprRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(dossier: KprDossier): Result<KprDossier> {
        // Validate dossier before creation
        val validationResult = validateDossier(dossier)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return kprRepository.createDossier(dossier)
    }
    
    private fun validateDossier(dossier: KprDossier): Result<Unit> {
        return when {
            dossier.customerId.isBlank() -> Result.failure(Exception("Customer ID is required"))
            dossier.unitPropertyId.isBlank() -> Result.failure(Exception("Unit Property ID is required"))
            dossier.estimatedLoanAmount <= 0 -> Result.failure(Exception("Loan amount must be greater than 0"))
            dossier.monthlyIncome <= 0 -> Result.failure(Exception("Monthly income must be greater than 0"))
            else -> Result.success(Unit)
        }
    }
}

class UpdateDossierUseCase(
    private val kprRepository: KprRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(dossier: KprDossier): Result<KprDossier> {
        // Validate dossier before update
        val validationResult = validateDossier(dossier)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return kprRepository.updateDossier(dossier)
    }
    
    private fun validateDossier(dossier: KprDossier): Result<Unit> {
        return when {
            dossier.id.isBlank() -> Result.failure(Exception("Dossier ID is required"))
            dossier.customerId.isBlank() -> Result.failure(Exception("Customer ID is required"))
            dossier.unitPropertyId.isBlank() -> Result.failure(Exception("Unit Property ID is required"))
            else -> Result.success(Unit)
        }
    }
}

class DeleteDossierUseCase(
    private val kprRepository: KprRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(Exception("Dossier ID is required"))
        }
        
        return kprRepository.deleteDossier(id)
    }
}

class CancelDossierUseCase(
    private val kprRepository: KprRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        id: String,
        reason: CancellationReason,
        notes: String
    ): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(Exception("Dossier ID is required"))
        }
        
        if (notes.isBlank()) {
            return Result.failure(Exception("Cancellation notes are required"))
        }
        
        return kprRepository.cancelDossier(id, reason, notes)
    }
}

// =====================================================
// FINANCIAL USE CASES
// =====================================================

class GetTransactionsUseCase(
    private val financialRepository: FinancialRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        dossierId: String? = null,
        category: String? = null,
        status: String? = null
    ): Result<List<FinancialTransaction>> {
        return financialRepository.getTransactions(dossierId, category, status)
    }
}

class CreateTransactionUseCase(
    private val financialRepository: FinancialRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(transaction: FinancialTransaction): Result<FinancialTransaction> {
        // Validate transaction before creation
        val validationResult = validateTransaction(transaction)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return financialRepository.createTransaction(transaction)
    }
    
    private fun validateTransaction(transaction: FinancialTransaction): Result<Unit> {
        return when {
            transaction.dossierId.isBlank() -> Result.failure(Exception("Dossier ID is required"))
            transaction.customerId.isBlank() -> Result.failure(Exception("Customer ID is required"))
            transaction.amount <= BigDecimal.ZERO -> Result.failure(Exception("Amount must be greater than 0"))
            transaction.category.name.isBlank() -> Result.failure(Exception("Category is required"))
            else -> Result.success(Unit)
        }
    }
}

class UpdateTransactionUseCase(
    private val financialRepository: FinancialRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(transaction: FinancialTransaction): Result<FinancialTransaction> {
        // Validate transaction before update
        val validationResult = validateTransaction(transaction)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return financialRepository.updateTransaction(transaction)
    }
    
    private fun validateTransaction(transaction: FinancialTransaction): Result<Unit> {
        return when {
            transaction.id.isBlank() -> Result.failure(Exception("Transaction ID is required"))
            transaction.dossierId.isBlank() -> Result.failure(Exception("Dossier ID is required"))
            transaction.amount <= BigDecimal.ZERO -> Result.failure(Exception("Amount must be greater than 0"))
            else -> Result.success(Unit)
        }
    }
}

class VerifyPaymentUseCase(
    private val financialRepository: FinancialRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        transactionId: String,
        evidenceUrl: String
    ): Result<Unit> {
        if (transactionId.isBlank()) {
            return Result.failure(Exception("Transaction ID is required"))
        }
        
        if (evidenceUrl.isBlank()) {
            return Result.failure(Exception("Evidence URL is required"))
        }
        
        return financialRepository.verifyPayment(transactionId, evidenceUrl)
    }
}

class GetFinancialReportUseCase(
    private val financialRepository: FinancialRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        startDate: Long,
        endDate: Long
    ): Result<FinancialReport> {
        if (startDate >= endDate) {
            return Result.failure(Exception("Start date must be before end date"))
        }
        
        return financialRepository.getFinancialReport(startDate, endDate)
    }
}

// =====================================================
// DOCUMENT USE CASES
// =====================================================

class GetDocumentsUseCase(
    private val documentRepository: DocumentRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        dossierId: String? = null,
        customerId: String? = null,
        type: String? = null
    ): Result<List<Document>> {
        return documentRepository.getDocuments(dossierId, customerId, type)
    }
}

class UploadDocumentUseCase(
    private val documentRepository: DocumentRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        dossierId: String,
        customerId: String,
        type: String,
        fileName: String,
        fileData: ByteArray
    ): Result<Document> {
        // Validate document before upload
        val validationResult = validateDocumentUpload(dossierId, customerId, type, fileName, fileData)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return documentRepository.uploadDocument(dossierId, customerId, type, fileName, fileData)
    }
    
    private fun validateDocumentUpload(
        dossierId: String,
        customerId: String,
        type: String,
        fileName: String,
        fileData: ByteArray
    ): Result<Unit> {
        return when {
            dossierId.isBlank() -> Result.failure(Exception("Dossier ID is required"))
            customerId.isBlank() -> Result.failure(Exception("Customer ID is required"))
            type.isBlank() -> Result.failure(Exception("Document type is required"))
            fileName.isBlank() -> Result.failure(Exception("File name is required"))
            fileData.isEmpty() -> Result.failure(Exception("File data is required"))
            fileData.size > 10 * 1024 * 1024 -> Result.failure(Exception("File size exceeds 10MB limit"))
            else -> Result.success(Unit)
        }
    }
}

class DeleteDocumentUseCase(
    private val documentRepository: DocumentRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(Exception("Document ID is required"))
        }
        
        return documentRepository.deleteDocument(id)
    }
}

class VerifyDocumentUseCase(
    private val documentRepository: DocumentRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        id: String,
        status: String,
        notes: String? = null
    ): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(Exception("Document ID is required"))
        }
        
        if (status.isBlank()) {
            return Result.failure(Exception("Status is required"))
        }
        
        return documentRepository.verifyDocument(id, status, notes)
    }
}

class MergeDocumentsUseCase(
    private val documentRepository: DocumentRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        documentIds: List<String>,
        outputFileName: String
    ): Result<String> {
        if (documentIds.isEmpty()) {
            return Result.failure(Exception("At least one document ID is required"))
        }
        
        if (outputFileName.isBlank()) {
            return Result.failure(Exception("Output file name is required"))
        }
        
        return documentRepository.mergeDocuments(documentIds, outputFileName)
    }
}

// =====================================================
// NOTIFICATION USE CASES
// =====================================================

class GetNotificationsUseCase(
    private val notificationRepository: NotificationRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        userId: String,
        unreadOnly: Boolean = false
    ): Result<List<Notification>> {
        return notificationRepository.getNotifications(userId, unreadOnly)
    }
}

class SendNotificationUseCase(
    private val notificationRepository: NotificationRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        userId: String,
        title: String,
        message: String,
        type: String,
        data: Map<String, Any> = emptyMap()
    ): Result<Notification> {
        // Validate notification before sending
        val validationResult = validateNotification(userId, title, message, type)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return notificationRepository.sendNotification(userId, title, message, type, data)
    }
    
    private fun validateNotification(
        userId: String,
        title: String,
        message: String,
        type: String
    ): Result<Unit> {
        return when {
            userId.isBlank() -> Result.failure(Exception("User ID is required"))
            title.isBlank() -> Result.failure(Exception("Title is required"))
            message.isBlank() -> Result.failure(Exception("Message is required"))
            type.isBlank() -> Result.failure(Exception("Type is required"))
            else -> Result.success(Unit)
        }
    }
}

class MarkAsReadUseCase(
    private val notificationRepository: NotificationRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(notificationId: String): Result<Unit> {
        if (notificationId.isBlank()) {
            return Result.failure(Exception("Notification ID is required"))
        }
        
        return notificationRepository.markAsRead(notificationId)
    }
}

class DeleteNotificationUseCase(
    private val notificationRepository: NotificationRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(notificationId: String): Result<Unit> {
        if (notificationId.isBlank()) {
            return Result.failure(Exception("Notification ID is required"))
        }
        
        return notificationRepository.deleteNotification(notificationId)
    }
}

// =====================================================
// WHATSAPP USE CASES
// =====================================================

class SendMessageUseCase(
    private val whatsAppRepository: WhatsAppRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        recipient: String,
        templateType: String,
        variables: Map<String, Any> = emptyMap()
    ): Result<WhatsAppMessageResult> {
        // Validate message before sending
        val validationResult = validateWhatsAppMessage(recipient, templateType)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return whatsAppRepository.sendMessage(recipient, templateType, variables)
    }
    
    private fun validateWhatsAppMessage(recipient: String, templateType: String): Result<Unit> {
        return when {
            recipient.isBlank() -> Result.failure(Exception("Recipient is required"))
            templateType.isBlank() -> Result.failure(Exception("Template type is required"))
            else -> Result.success(Unit)
        }
    }
}

class SendBulkMessageUseCase(
    private val whatsAppRepository: WhatsAppRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        recipients: List<String>,
        templateType: String,
        variables: Map<String, Any> = emptyMap()
    ): Result<List<WhatsAppMessageResult>> {
        if (recipients.isEmpty()) {
            return Result.failure(Exception("At least one recipient is required"))
        }
        
        if (templateType.isBlank()) {
            return Result.failure(Exception("Template type is required"))
        }
        
        return whatsAppRepository.sendBulkMessage(recipients, templateType, variables)
    }
}

class GetMessageStatusUseCase(
    private val whatsAppRepository: WhatsAppRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(messageId: String): Result<WhatsAppMessageStatus> {
        if (messageId.isBlank()) {
            return Result.failure(Exception("Message ID is required"))
        }
        
        return whatsAppRepository.getMessageStatus(messageId)
    }
}

class GetTemplatesUseCase(
    private val whatsAppRepository: WhatsAppRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): Result<List<WhatsAppTemplate>> {
        return whatsAppRepository.getTemplates()
    }
}

// =====================================================
// ML USE CASES
// =====================================================

class PredictChurnUseCase(
    private val mlRepository: MLRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(customerData: CustomerBehaviorData): Result<ChurnPrediction> {
        // Validate customer data
        val validationResult = validateCustomerData(customerData)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return mlRepository.predictChurn(customerData)
    }
    
    private fun validateCustomerData(customerData: CustomerBehaviorData): Result<Unit> {
        return when {
            customerData.customerId.isBlank() -> Result.failure(Exception("Customer ID is required"))
            customerData.monthlyIncome <= 0 -> Result.failure(Exception("Monthly income must be greater than 0"))
            customerData.age < 18 || customerData.age > 100 -> Result.failure(Exception("Age must be between 18 and 100"))
            else -> Result.success(Unit)
        }
    }
}

class GenerateRecommendationsUseCase(
    private val mlRepository: MLRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(marketData: MarketData): Result<InventoryRecommendation> {
        // Validate market data
        val validationResult = validateMarketData(marketData)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return mlRepository.generateRecommendations(marketData)
    }
    
    private fun validateMarketData(marketData: MarketData): Result<Unit> {
        return when {
            marketData.avgUnitPrice <= 0 -> Result.failure(Exception("Average unit price must be greater than 0"))
            marketData.marketDemand < 0 -> Result.failure(Exception("Market demand cannot be negative"))
            marketData.competitorCount < 0 -> Result.failure(Exception("Competitor count cannot be negative"))
            else -> Result.success(Unit)
        }
    }
}

class TrainModelUseCase(
    private val mlRepository: MLRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        trainingData: List<TrainingDataPoint>,
        modelType: String,
        hyperparameters: ModelHyperparameters
    ): Result<TrainingResult> {
        // Validate training data
        val validationResult = validateTrainingData(trainingData)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return mlRepository.trainModel(trainingData, modelType, hyperparameters)
    }
    
    private fun validateTrainingData(trainingData: List<TrainingDataPoint>): Result<Unit> {
        return when {
            trainingData.isEmpty() -> Result.failure(Exception("Training data cannot be empty"))
            trainingData.size < 100 -> Result.failure(Exception("Training data must have at least 100 samples"))
            else -> Result.success(Unit)
        }
    }
}

class GetModelMetricsUseCase(
    private val mlRepository: MLRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(modelId: String): Result<ModelMetrics> {
        if (modelId.isBlank()) {
            return Result.failure(Exception("Model ID is required"))
        }
        
        return mlRepository.getModelMetrics(modelId)
    }
}

// =====================================================
// GOVERNMENT USE CASES
// =====================================================

class CheckEligibilityUseCase(
    private val governmentRepository: GovernmentRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        customerId: String,
        customerData: CustomerData
    ): Result<SiKasepEligibilityResult> {
        // Validate customer data
        val validationResult = validateCustomerData(customerData)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return governmentRepository.checkSiKasepEligibility(customerId, customerData)
    }
    
    private fun validateCustomerData(customerData: CustomerData): Result<Unit> {
        return when {
            customerData.nik.isBlank() -> Result.failure(Exception("NIK is required"))
            customerData.nik.length != 16 -> Result.failure(Exception("NIK must be 16 digits"))
            customerData.fullName.isBlank() -> Result.failure(Exception("Full name is required"))
            customerData.monthlyIncome <= 0 -> Result.failure(Exception("Monthly income must be greater than 0"))
            else -> Result.success(Unit)
        }
    }
}

class VerifyBPJSUseCase(
    private val governmentRepository: GovernmentRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        customerId: String,
        bpjsNumber: String
    ): Result<BPJSEligibilityResult> {
        if (customerId.isBlank()) {
            return Result.failure(Exception("Customer ID is required"))
        }
        
        if (bpjsNumber.isBlank()) {
            return Result.failure(Exception("BPJS number is required"))
        }
        
        return governmentRepository.checkBPJSEligibility(customerId, bpjsNumber)
    }
}

class CheckTaxComplianceUseCase(
    private val governmentRepository: GovernmentRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        customerId: String,
        npwpNumber: String
    ): Result<KemenkeuComplianceResult> {
        if (customerId.isBlank()) {
            return Result.failure(Exception("Customer ID is required"))
        }
        
        if (npwpNumber.isBlank()) {
            return Result.failure(Exception("NPWP number is required"))
        }
        
        return governmentRepository.checkKemenkeuCompliance(customerId, npwpNumber)
    }
}

class GetGovernmentStatsUseCase(
    private val governmentRepository: GovernmentRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        startDate: Date,
        endDate: Date
    ): Result<GovernmentAPIStatistics> {
        if (startDate.after(endDate)) {
            return Result.failure(Exception("Start date must be before end date"))
        }
        
        return governmentRepository.getGovernmentAPIStatistics(startDate, endDate)
    }
}

// =====================================================
// STORAGE USE CASES
// =====================================================

class UploadFileUseCase(
    private val storageRepository: StorageRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        fileName: String,
        fileData: ByteArray,
        folder: String = "/uploads"
    ): Result<String> {
        // Validate file upload
        val validationResult = validateFileUpload(fileName, fileData)
        if (validationResult.isFailure) {
            return validationResult
        }
        
        return storageRepository.uploadFile(fileName, fileData, folder)
    }
    
    private fun validateFileUpload(fileName: String, fileData: ByteArray): Result<Unit> {
        return when {
            fileName.isBlank() -> Result.failure(Exception("File name is required"))
            fileData.isEmpty() -> Result.failure(Exception("File data is required"))
            fileData.size > 50 * 1024 * 1024 -> Result.failure(Exception("File size exceeds 50MB limit"))
            else -> Result.success(Unit)
        }
    }
}

class DownloadFileUseCase(
    private val storageRepository: StorageRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(fileUrl: String): Result<ByteArray> {
        if (fileUrl.isBlank()) {
            return Result.failure(Exception("File URL is required"))
        }
        
        return storageRepository.downloadFile(fileUrl)
    }
}

class CreateArchiveUseCase(
    private val storageRepository: StorageRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        files: List<ArchiveFile>,
        archiveName: String,
        options: ArchiveOptions
    ): Result<String> {
        if (files.isEmpty()) {
            return Result.failure(Exception("At least one file is required"))
        }
        
        if (archiveName.isBlank()) {
            return Result.failure(Exception("Archive name is required"))
        }
        
        return storageRepository.createArchive(files, archiveName, options)
    }
}

class ExtractArchiveUseCase(
    private val storageRepository: StorageRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        archivePath: String,
        outputDir: String
    ): Result<List<String>> {
        if (archivePath.isBlank()) {
            return Result.failure(Exception("Archive path is required"))
        }
        
        if (outputDir.isBlank()) {
            return Result.failure(Exception("Output directory is required"))
        }
        
        return storageRepository.extractArchive(archivePath, outputDir)
    }
}

// =====================================================
// WORKFLOW USE CASES
// =====================================================

class ExecuteWorkflowUseCase(
    private val workflowRepository: WorkflowRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        projectId: String,
        startPhase: Int,
        endPhase: Int
    ): Result<WorkflowExecution> {
        if (projectId.isBlank()) {
            return Result.failure(Exception("Project ID is required"))
        }
        
        if (startPhase < 0 || endPhase < startPhase) {
            return Result.failure(Exception("Invalid phase range"))
        }
        
        return workflowRepository.executeWorkflow(projectId, startPhase, endPhase)
    }
}

class GetWorkflowProgressUseCase(
    private val workflowRepository: WorkflowRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(projectId: String): Result<WorkflowProgress> {
        if (projectId.isBlank()) {
            return Result.failure(Exception("Project ID is required"))
        }
        
        return workflowRepository.getWorkflowProgress(projectId)
    }
}

class ValidatePhaseUseCase(
    private val workflowRepository: WorkflowRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        phase: Int,
        projectId: String
    ): Result<PhaseValidation> {
        if (phase < 0) {
            return Result.failure(Exception("Phase must be non-negative"))
        }
        
        if (projectId.isBlank()) {
            return Result.failure(Exception("Project ID is required"))
        }
        
        return workflowRepository.validatePhase(phase, projectId)
    }
}

class TransitionPhaseUseCase(
    private val workflowRepository: WorkflowRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        fromPhase: Int,
        toPhase: Int,
        projectId: String,
        approvedBy: String
    ): Result<PhaseTransition> {
        if (fromPhase < 0 || toPhase <= fromPhase) {
            return Result.failure(Exception("Invalid phase transition"))
        }
        
        if (projectId.isBlank()) {
            return Result.failure(Exception("Project ID is required"))
        }
        
        if (approvedBy.isBlank()) {
            return Result.failure(Exception("Approver ID is required"))
        }
        
        return workflowRepository.transitionPhase(fromPhase, toPhase, projectId, approvedBy)
    }
}
