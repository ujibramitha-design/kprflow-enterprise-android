package com.kprflow.enterprise.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    suspend fun createPaymentSchedule(
        dossierId: String,
        totalAmount: BigDecimal,
        downPayment: BigDecimal,
        loanAmount: BigDecimal,
        interestRate: Double,
        loanTermMonths: Int,
        startDate: LocalDate
    ): Result<PaymentSchedule> {
        return try {
            // Calculate monthly payment using standard loan formula
            val monthlyInterestRate = interestRate / 100 / 12
            val monthlyPayment = if (monthlyInterestRate > 0) {
                loanAmount * (monthlyInterestRate * Math.pow(1 + monthlyInterestRate, loanTermMonths.toDouble())) /
                        (Math.pow(1 + monthlyInterestRate, loanTermMonths.toDouble()) - 1)
            } else {
                loanAmount / BigDecimal(loanTermMonths)
            }
            
            val installments = mutableListOf<PaymentInstallment>()
            var remainingBalance = loanAmount
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            
            for (month in 1..loanTermMonths) {
                val dueDate = startDate.plusMonths(month.toLong())
                val interestPayment = remainingBalance * BigDecimal(monthlyInterestRate)
                val principalPayment = monthlyPayment - interestPayment
                remainingBalance -= principalPayment
                
                val installment = PaymentInstallment(
                    id = java.util.UUID.randomUUID().toString(),
                    dossierId = dossierId,
                    installmentNumber = month,
                    dueDate = dueDate.toString(),
                    principalAmount = principalPayment,
                    interestAmount = interestPayment,
                    totalAmount = principalPayment + interestPayment,
                    remainingBalance = remainingBalance,
                    status = if (month == 1) "PENDING" else "SCHEDULED",
                    paidAmount = BigDecimal.ZERO,
                    paidAt = null,
                    createdAt = java.time.Instant.now().toString()
                )
                
                installments.add(installment)
            }
            
            val paymentSchedule = PaymentSchedule(
                id = java.util.UUID.randomUUID().toString(),
                dossierId = dossierId,
                totalAmount = totalAmount,
                downPayment = downPayment,
                loanAmount = loanAmount,
                interestRate = interestRate,
                loanTermMonths = loanTermMonths,
                monthlyPayment = monthlyPayment,
                startDate = startDate.toString(),
                installments = installments,
                status = "ACTIVE",
                createdAt = java.time.Instant.now().toString()
            )
            
            // Save to database
            val scheduleData = mapOf(
                "id" to paymentSchedule.id,
                "dossier_id" to paymentSchedule.dossierId,
                "total_amount" to paymentSchedule.totalAmount,
                "down_payment" to paymentSchedule.downPayment,
                "loan_amount" to paymentSchedule.loanAmount,
                "interest_rate" to paymentSchedule.interestRate,
                "loan_term_months" to paymentSchedule.loanTermMonths,
                "monthly_payment" to paymentSchedule.monthlyPayment,
                "start_date" to paymentSchedule.startDate,
                "status" to paymentSchedule.status,
                "created_at" to paymentSchedule.createdAt
            )
            
            val createdSchedule = postgrest.from("payment_schedules")
                .insert(scheduleData)
                .maybeSingle()
                .data
            
            // Save installments
            installments.forEach { installment ->
                val installmentData = mapOf(
                    "id" to installment.id,
                    "schedule_id" to paymentSchedule.id,
                    "dossier_id" to installment.dossierId,
                    "installment_number" to installment.installmentNumber,
                    "due_date" to installment.dueDate,
                    "principal_amount" to installment.principalAmount,
                    "interest_amount" to installment.interestAmount,
                    "total_amount" to installment.totalAmount,
                    "remaining_balance" to installment.remainingBalance,
                    "status" to installment.status,
                    "paid_amount" to installment.paidAmount,
                    "created_at" to installment.createdAt
                )
                
                postgrest.from("payment_installments")
                    .insert(installmentData)
                    .maybeSingle()
                    .data
            }
            
            Result.success(paymentSchedule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPaymentSchedule(dossierId: String): Result<PaymentSchedule?> {
        return try {
            val schedule = postgrest.from("payment_schedules")
                .select()
                .filter { eq("dossier_id", dossierId) }
                .maybeSingle()
                .data
            
            if (schedule != null) {
                // Get installments
                val installments = postgrest.from("payment_installments")
                    .select()
                    .filter { eq("schedule_id", schedule.id) }
                    .order("installment_number", ascending = true)
                    .data
                
                val completeSchedule = schedule.copy(installments = installments)
                Result.success(completeSchedule)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun makePayment(
        installmentId: String,
        paymentAmount: BigDecimal,
        paymentMethod: String,
        paymentDate: LocalDate = LocalDate.now()
    ): Result<PaymentInstallment> {
        return try {
            // Get installment details
            val installment = postgrest.from("payment_installments")
                .select()
                .filter { eq("id", installmentId) }
                .maybeSingle()
                .data ?: return Result.failure(Exception("Installment not found"))
            
            if (installment.status == "PAID") {
                return Result.failure(Exception("Installment already paid"))
            }
            
            val paidAmount = installment.paidAmount + paymentAmount
            val status = if (paidAmount >= installment.totalAmount) "PAID" else "PARTIALLY_PAID"
            
            val updateData = mapOf(
                "paid_amount" to paidAmount,
                "status" to status,
                "paid_at" to paymentDate.toString(),
                "payment_method" to paymentMethod,
                "updated_at" to java.time.Instant.now().toString()
            )
            
            val updatedInstallment = postgrest.from("payment_installments")
                .update(updateData)
                .filter { eq("id", installmentId) }
                .maybeSingle()
                .data
            
            updatedInstallment?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to process payment"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPaymentHistory(dossierId: String): Result<List<PaymentInstallment>> {
        return try {
            val schedule = getPaymentSchedule(dossierId).getOrNull()
                ?: return Result.failure(Exception("Payment schedule not found"))
            
            val paidInstallments = schedule.installments.filter { 
                it.status == "PAID" || it.status == "PARTIALLY_PAID" 
            }.sortedByDescending { it.paidAt }
            
            Result.success(paidInstallments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUpcomingPayments(dossierId: String): Result<List<PaymentInstallment>> {
        return try {
            val schedule = getPaymentSchedule(dossierId).getOrNull()
                ?: return Result.failure(Exception("Payment schedule not found"))
            
            val today = LocalDate.now()
            val upcomingInstallments = schedule.installments.filter { 
                it.status != "PAID" && 
                LocalDate.parse(it.dueDate).isAfter(today.minusDays(7)) // Next 7 days
            }.sortedBy { it.dueDate }
            
            Result.success(upcomingInstallments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getOverduePayments(dossierId: String): Result<List<PaymentInstallment>> {
        return try {
            val schedule = getPaymentSchedule(dossierId).getOrNull()
                ?: return Result.failure(Exception("Payment schedule not found"))
            
            val today = LocalDate.now()
            val overdueInstallments = schedule.installments.filter { 
                it.status != "PAID" && 
                LocalDate.parse(it.dueDate).isBefore(today)
            }.sortedBy { it.dueDate }
            
            Result.success(overdueInstallments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPaymentSummary(dossierId: String): Result<PaymentSummary> {
        return try {
            val schedule = getPaymentSchedule(dossierId).getOrNull()
                ?: return Result.failure(Exception("Payment schedule not found"))
            
            val totalPaid = schedule.installments.sumOf { it.paidAmount }
            val totalRemaining = schedule.installments.sumOf { it.totalAmount - it.paidAmount }
            val paidInstallments = schedule.installments.count { it.status == "PAID" }
            val overdueInstallments = schedule.installments.count { 
                it.status != "PAID" && LocalDate.parse(it.dueDate).isBefore(LocalDate.now())
            }
            
            val nextPayment = schedule.installments
                .filter { it.status != "PAID" }
                .minByOrNull { it.dueDate }
            
            val summary = PaymentSummary(
                scheduleId = schedule.id,
                totalAmount = schedule.totalAmount,
                downPayment = schedule.downPayment,
                loanAmount = schedule.loanAmount,
                totalPaid = totalPaid,
                totalRemaining = totalRemaining,
                paidInstallments = paidInstallments,
                totalInstallments = schedule.installments.size,
                overdueInstallments = overdueInstallments,
                nextPayment = nextPayment,
                completionPercentage = if (schedule.totalAmount > BigDecimal.ZERO) {
                    (totalPaid / schedule.totalAmount) * BigDecimal(100)
                } else BigDecimal.ZERO
            )
            
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generatePaymentReport(
        dossierId: String,
        reportType: PaymentReportType
    ): Result<PaymentReport> {
        return try {
            val schedule = getPaymentSchedule(dossierId).getOrNull()
                ?: return Result.failure(Exception("Payment schedule not found"))
            
            val summary = getPaymentSummary(dossierId).getOrNull()
                ?: return Result.failure(Exception("Failed to generate payment summary"))
            
            val report = PaymentReport(
                id = java.util.UUID.randomUUID().toString(),
                dossierId = dossierId,
                reportType = reportType,
                schedule = schedule,
                summary = summary,
                generatedAt = java.time.Instant.now().toString()
            )
            
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observePaymentUpdates(dossierId: String): Flow<PaymentUpdate> = flow {
        try {
            // TODO: Implement real-time updates via Supabase Realtime
            emit(PaymentUpdate.PaymentReceived)
        } catch (e: Exception) {
            emit(PaymentUpdate.Error(e.message ?: "Unknown error"))
        }
    }
}

// Data classes
data class PaymentSchedule(
    val id: String,
    val dossierId: String,
    val totalAmount: BigDecimal,
    val downPayment: BigDecimal,
    val loanAmount: BigDecimal,
    val interestRate: Double,
    val loanTermMonths: Int,
    val monthlyPayment: BigDecimal,
    val startDate: String,
    val installments: List<PaymentInstallment>,
    val status: String,
    val createdAt: String
)

data class PaymentInstallment(
    val id: String,
    val scheduleId: String,
    val dossierId: String,
    val installmentNumber: Int,
    val dueDate: String,
    val principalAmount: BigDecimal,
    val interestAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val remainingBalance: BigDecimal,
    val status: String, // PENDING, PARTIALLY_PAID, PAID, OVERDUE
    val paidAmount: BigDecimal,
    val paidAt: String? = null,
    val paymentMethod: String? = null,
    val createdAt: String
)

data class PaymentSummary(
    val scheduleId: String,
    val totalAmount: BigDecimal,
    val downPayment: BigDecimal,
    val loanAmount: BigDecimal,
    val totalPaid: BigDecimal,
    val totalRemaining: BigDecimal,
    val paidInstallments: Int,
    val totalInstallments: Int,
    val overdueInstallments: Int,
    val nextPayment: PaymentInstallment?,
    val completionPercentage: BigDecimal
)

data class PaymentReport(
    val id: String,
    val dossierId: String,
    val reportType: PaymentReportType,
    val schedule: PaymentSchedule,
    val summary: PaymentSummary,
    val generatedAt: String
)

enum class PaymentReportType {
    MONTHLY,
    QUARTERLY,
    ANNUAL,
    COMPLETE
}

sealed class PaymentUpdate {
    object PaymentReceived : PaymentUpdate()
    object PaymentOverdue : PaymentUpdate()
    object ScheduleUpdated : PaymentUpdate()
    data class Error(val message: String) : PaymentUpdate()
}
