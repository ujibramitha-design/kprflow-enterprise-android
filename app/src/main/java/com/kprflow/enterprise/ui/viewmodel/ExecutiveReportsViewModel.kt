package com.kprflow.enterprise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kprflow.enterprise.data.repository.AnalyticsRepository
import com.kprflow.enterprise.data.repository.ReportGeneratorRepository
import com.kprflow.enterprise.ui.components.ChartData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExecutiveReportsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val reportRepository: ReportGeneratorRepository
) : ViewModel() {
    
    // UI States
    private val _uiState = MutableStateFlow<ExecutiveReportsUIState>(ExecutiveReportsUIState.Loading)
    val uiState: StateFlow<ExecutiveReportsUIState> = _uiState.asStateFlow()
    
    // Summary State
    private val _summaryState = MutableStateFlow(ExecutiveSummary(
        totalPortfolio = 0.0,
        activeApplications = 0,
        successRate = 0,
        avgProcessingTime = 0
    ))
    val summaryState: StateFlow<ExecutiveSummary> = _summaryState.asStateFlow()
    
    // Performance State
    private val _performanceState = MutableStateFlow<ExecutiveReportsState<PerformanceMetrics>>(ExecutiveReportsState.Loading)
    val performanceState: StateFlow<ExecutiveReportsState<PerformanceMetrics>> = _performanceState.asStateFlow()
    
    // Financial State
    private val _financialState = MutableStateFlow<ExecutiveReportsState<FinancialReports>>(ExecutiveReportsState.Loading)
    val financialState: StateFlow<ExecutiveReportsState<FinancialReports>> = _financialState.asStateFlow()
    
    init {
        loadExecutiveReports()
    }
    
    fun loadExecutiveReports() {
        viewModelScope.launch {
            _uiState.value = ExecutiveReportsUIState.Loading
            
            try {
                // Load summary data
                loadSummaryData()
                
                // Load performance metrics
                loadPerformanceMetrics()
                
                // Load financial reports
                loadFinancialReports()
                
                _uiState.value = ExecutiveReportsUIState.Success
            } catch (e: Exception) {
                _uiState.value = ExecutiveReportsUIState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private suspend fun loadSummaryData() {
        try {
            val portfolioValue = analyticsRepository.getTotalPortfolioValue()
            val activeApps = analyticsRepository.getActiveApplicationsCount()
            val successRate = analyticsRepository.getSuccessRate()
            val avgProcessingTime = analyticsRepository.getAverageProcessingTime()
            
            _summaryState.value = ExecutiveSummary(
                totalPortfolio = portfolioValue,
                activeApplications = activeApps,
                successRate = successRate,
                avgProcessingTime = avgProcessingTime
            )
        } catch (e: Exception) {
            // Set default values on error
            _summaryState.value = ExecutiveSummary(
                totalPortfolio = 0.0,
                activeApplications = 0,
                successRate = 0,
                avgProcessingTime = 0
            )
        }
    }
    
    fun loadPerformanceMetrics() {
        viewModelScope.launch {
            _performanceState.value = ExecutiveReportsState.Loading
            
            try {
                val conversionRate = analyticsRepository.getConversionRate()
                val currentRevenue = analyticsRepository.getCurrentMonthRevenue()
                
                // Funnel data
                val funnelData = listOf(
                    ChartData("Leads", 1000f, Color(0xFF4CAF50)),
                    ChartData("Applications", 750f, Color(0xFF2196F3)),
                    ChartData("Approved", 450f, Color(0xFFFF9800)),
                    ChartData("Disbursed", 380f, Color(0xFF9C27B0))
                )
                
                // Revenue trend data (last 6 months)
                val revenueData = listOf(
                    ChartData("Jul", 850000000f, Color(0xFF4CAF50)),
                    ChartData("Aug", 920000000f, Color(0xFF4CAF50)),
                    ChartData("Sep", 880000000f, Color(0xFF4CAF50)),
                    ChartData("Oct", 950000000f, Color(0xFF4CAF50)),
                    ChartData("Nov", 1020000000f, Color(0xFF4CAF50)),
                    ChartData("Dec", 1150000000f, Color(0xFF4CAF50))
                )
                
                // Department performance data
                val departmentData = listOf(
                    ChartData("Marketing", 85f, Color(0xFFFF9800)),
                    ChartData("Legal", 78f, Color(0xFF2196F3)),
                    ChartData("Finance", 92f, Color(0xFF9C27B0)),
                    ChartData("Operations", 88f, Color(0xFF4CAF50))
                )
                
                val performanceMetrics = PerformanceMetrics(
                    conversionRate = conversionRate,
                    currentRevenue = currentRevenue,
                    funnelData = funnelData,
                    revenueData = revenueData,
                    departmentData = departmentData
                )
                
                _performanceState.value = ExecutiveReportsState.Success(performanceMetrics)
            } catch (e: Exception) {
                _performanceState.value = ExecutiveReportsState.Error(e.message ?: "Failed to load performance metrics")
            }
        }
    }
    
    fun loadFinancialReports() {
        viewModelScope.launch {
            _financialState.value = ExecutiveReportsState.Loading
            
            try {
                val quarterlyRevenue = analyticsRepository.getQuarterlyRevenue()
                val quarterlyProfit = analyticsRepository.getQuarterlyProfit()
                val totalCosts = analyticsRepository.getTotalCosts()
                val operatingCosts = analyticsRepository.getOperatingCosts()
                val cashFlow = analyticsRepository.getCashFlow()
                val netCashFlow = analyticsRepository.getNetCashFlow()
                
                val financialReports = FinancialReports(
                    quarterlyRevenue = quarterlyRevenue,
                    quarterlyProfit = quarterlyProfit,
                    totalCosts = totalCosts,
                    operatingCosts = operatingCosts,
                    cashFlow = cashFlow,
                    netCashFlow = netCashFlow
                )
                
                _financialState.value = ExecutiveReportsState.Success(financialReports)
            } catch (e: Exception) {
                _financialState.value = ExecutiveReportsState.Error(e.message ?: "Failed to load financial reports")
            }
        }
    }
    
    fun refreshData() {
        loadExecutiveReports()
    }
    
    fun generateReport(reportType: String): String {
        return when (reportType) {
            "executive_summary" -> generateExecutiveSummaryReport()
            "revenue_analysis" -> generateRevenueAnalysisReport()
            "cost_analysis" -> generateCostAnalysisReport()
            "cash_flow" -> generateCashFlowReport()
            "full_executive" -> generateFullExecutiveReport()
            "board_presentation" -> generateBoardPresentationReport()
            "investor_report" -> generateInvestorReport()
            else -> "Report type not supported"
        }
    }
    
    private fun generateExecutiveSummaryReport(): String {
        val summary = summaryState.value
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        
        return buildString {
            appendLine("EXECUTIVE SUMMARY REPORT")
            appendLine("=" .repeat(50))
            appendLine("Generated: ${Date()}")
            appendLine()
            
            appendLine("KEY METRICS")
            appendLine("-".repeat(30))
            appendLine("Total Portfolio: ${formatter.format(summary.totalPortfolio)}")
            appendLine("Active Applications: ${summary.activeApplications}")
            appendLine("Success Rate: ${summary.successRate}%")
            appendLine("Avg Processing Time: ${summary.avgProcessingTime} days")
            appendLine()
            
            appendLine("PERFORMANCE HIGHLIGHTS")
            appendLine("-".repeat(30))
            appendLine("• Portfolio growth increased by 18.5% this quarter")
            appendLine("• Application success rate improved by 3.2%")
            appendLine("• Processing time reduced by 2.1 days")
            appendLine("• Customer satisfaction at 92%")
        }
    }
    
    private fun generateRevenueAnalysisReport(): String {
        val financial = (financialState.value as? ExecutiveReportsState.Success)?.data
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        
        return buildString {
            appendLine("REVENUE ANALYSIS REPORT")
            appendLine("=" .repeat(50))
            appendLine("Generated: ${Date()}")
            appendLine()
            
            appendLine("QUARTERLY PERFORMANCE")
            appendLine("-".repeat(30))
            financial?.let {
                appendLine("Quarterly Revenue: ${formatter.format(it.quarterlyRevenue)}")
                appendLine("Quarterly Profit: ${formatter.format(it.quarterlyProfit)}")
                appendLine("Profit Margin: ${(it.quarterlyProfit / it.quarterlyRevenue * 100).toInt()}%")
            }
            appendLine()
            
            appendLine("REVENUE BREAKDOWN")
            appendLine("-".repeat(30))
            appendLine("• KPR Disbursements: 65%")
            appendLine("• Processing Fees: 20%")
            appendLine("• Late Payment Fees: 10%")
            appendLine("• Other Services: 5%")
        }
    }
    
    private fun generateCostAnalysisReport(): String {
        val financial = (financialState.value as? ExecutiveReportsState.Success)?.data
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        
        return buildString {
            appendLine("COST ANALYSIS REPORT")
            appendLine("=" .repeat(50))
            appendLine("Generated: ${Date()}")
            appendLine()
            
            appendLine("COST STRUCTURE")
            appendLine("-".repeat(30))
            financial?.let {
                appendLine("Total Costs: ${formatter.format(it.totalCosts)}")
                appendLine("Operating Costs: ${formatter.format(it.operatingCosts)}")
                appendLine("Cost Efficiency: ${(it.quarterlyProfit / it.totalCosts * 100).toInt()}%")
            }
            appendLine()
            
            appendLine("COST BREAKDOWN")
            appendLine("-".repeat(30))
            appendLine("• Personnel Costs: 45%")
            appendLine("• Technology Infrastructure: 25%")
            appendLine("• Marketing & Sales: 15%")
            appendLine("• Administrative: 10%")
            appendLine("• Other: 5%")
        }
    }
    
    private fun generateCashFlowReport(): String {
        val financial = (financialState.value as? ExecutiveReportsState.Success)?.data
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        
        return buildString {
            appendLine("CASH FLOW REPORT")
            appendLine("=" .repeat(50))
            appendLine("Generated: ${Date()}")
            appendLine()
            
            appendLine("CASH FLOW SUMMARY")
            appendLine("-".repeat(30))
            financial?.let {
                appendLine("Total Cash Flow: ${formatter.format(it.cashFlow)}")
                appendLine("Net Cash Flow: ${formatter.format(it.netCashFlow)}")
                appendLine("Cash Flow Margin: ${(it.netCashFlow / it.cashFlow * 100).toInt()}%")
            }
            appendLine()
            
            appendLine("CASH FLOW BREAKDOWN")
            appendLine("-".repeat(30))
            appendLine("• Operating Activities: 75%")
            appendLine("• Investing Activities: 15%")
            appendLine("• Financing Activities: 10%")
        }
    }
    
    private fun generateFullExecutiveReport(): String {
        return buildString {
            appendLine(generateExecutiveSummaryReport())
            appendLine()
            appendLine(generateRevenueAnalysisReport())
            appendLine()
            appendLine(generateCostAnalysisReport())
            appendLine()
            appendLine(generateCashFlowReport())
        }
    }
    
    private fun generateBoardPresentationReport(): String {
        return buildString {
            appendLine("BOARD PRESENTATION REPORT")
            appendLine("=" .repeat(50))
            appendLine("Generated: ${Date()}")
            appendLine()
            
            appendLine("EXECUTIVE OVERVIEW")
            appendLine("-".repeat(30))
            appendLine("• Strong portfolio growth of 18.5% this quarter")
            appendLine("• Improved operational efficiency across all departments")
            appendLine("• Successful digital transformation initiatives")
            appendLine("• Enhanced customer satisfaction metrics")
            appendLine()
            
            appendLine("STRATEGIC INITIATIVES")
            appendLine("-".repeat(30))
            appendLine("• AI-powered document processing implementation")
            appendLine("• Mobile-first customer experience enhancement")
            appendLine("• Process automation for faster approvals")
            appendLine("• Expanded partnership network")
            appendLine()
            
            appendLine("FINANCIAL HIGHLIGHTS")
            appendLine("-".repeat(30))
            appendLine("• Revenue growth exceeding targets by 15%")
            appendLine("• Improved profit margins through cost optimization")
            appendLine("• Strong cash flow position")
            appendLine("• Healthy balance sheet metrics")
        }
    }
    
    private fun generateInvestorReport(): String {
        return buildString {
            appendLine("INVESTOR REPORT")
            appendLine("=" .repeat(50))
            appendLine("Generated: ${Date()}")
            appendLine()
            
            appendLine("INVESTMENT HIGHLIGHTS")
            appendLine("-".repeat(30))
            appendLine("• Consistent revenue growth trajectory")
            appendLine("• Strong market position in KPR sector")
            appendLine("• Innovative technology adoption")
            appendLine("• Experienced management team")
            appendLine()
            
            appendLine("FINANCIAL PERFORMANCE")
            appendLine("-".repeat(30))
            appendLine("• Year-over-year revenue growth: 22%")
            appendLine("• Return on investment: 18.5%")
            appendLine("• Debt-to-equity ratio: 0.45")
            appendLine("• Liquidity ratio: 2.3")
            appendLine()
            
            appendLine("GROWTH OPPORTUNITIES")
            appendLine("-".repeat(30))
            appendLine("• Market expansion into new regions")
            appendLine("• Digital product offerings")
            appendLine("• Strategic partnerships")
            appendLine("• Technology innovation pipeline")
        }
    }
}

// UI State for Executive Reports
sealed class ExecutiveReportsUIState {
    object Loading : ExecutiveReportsUIState()
    object Success : ExecutiveReportsUIState()
    data class Error(val message: String) : ExecutiveReportsUIState()
}

// Data classes
data class ExecutiveSummary(
    val totalPortfolio: Double,
    val activeApplications: Int,
    val successRate: Int,
    val avgProcessingTime: Int
)

data class PerformanceMetrics(
    val conversionRate: Int,
    val currentRevenue: Double,
    val funnelData: List<ChartData>,
    val revenueData: List<ChartData>,
    val departmentData: List<ChartData>
)

data class FinancialReports(
    val quarterlyRevenue: Double,
    val quarterlyProfit: Double,
    val totalCosts: Double,
    val operatingCosts: Double,
    val cashFlow: Double,
    val netCashFlow: Double
)

// Chart data class
data class ChartData(
    val label: String,
    val value: Float,
    val color: androidx.compose.ui.graphics.Color
)
